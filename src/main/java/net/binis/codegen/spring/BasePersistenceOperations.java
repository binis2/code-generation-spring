package net.binis.codegen.spring;

/*-
 * #%L
 * code-generator-spring
 * %%
 * Copyright (C) 2021 Binis Belev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.exception.GenericCodeGenException;
import net.binis.codegen.spring.component.ApplicationContextProvider;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.isNull;

@Slf4j
public class BasePersistenceOperations<R> {

    private static EntityManagerFactory factory;
    private static TransactionTemplate template;

    private static Function<EntityManagerFactory, EntityManager> entityManagerProvider = defaultEntityManagerProvider();

    public static void setEntityManagerProvider(Function<EntityManagerFactory, EntityManager> provider) {
        entityManagerProvider = provider;
    }

    public static EntityManager getEntityManager() {
        return entityManagerProvider.apply(factory);
    }


    private static void init() {
        if (isNull(factory)) {
            var context = ApplicationContextProvider.getApplicationContext();
            if (isNull(context)) {
                throw new GenericCodeGenException("Not in spring context!");
            }
            JpaTransactionManager tm = context.getBean(JpaTransactionManager.class);
            factory = tm.getEntityManagerFactory();
            template = context.getBean(TransactionTemplate.class);
        }
    }

    protected void with(Consumer<EntityManager> func) {
        init();
        var em = getEntityManager();
        if (isNull(em) || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("Attempt to do action outside of open transaction!");
            template.execute(s -> {
                var manager = getEntityManager();
                func.accept(manager);
                return null;
            });
        } else {
            func.accept(em);
        }
    }

    protected R withRes(Function<EntityManager, R> func) {
        init();
        var em = getEntityManager();
        if (isNull(em) || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("Attempt to do action outside of open transaction!");
            return template.execute(s ->
                func.apply(getEntityManager()));
        } else {
            return func.apply(em);
        }
    }

    protected R withNewTransactionRes(Function<EntityManager, R> func) {
        init();
        var em = getEntityManager();
        if (isNull(em) || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("Attempt to do action outside of open transaction!");
            return template.execute(s ->
                    func.apply(getEntityManager()));
        } else {
            var transactionTemplate = new TransactionTemplate(template.getTransactionManager());
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return transactionTemplate.execute(s -> func.apply(em));
        }
    }

    public static Function<EntityManagerFactory, EntityManager> defaultEntityManagerProvider() {
        return EntityManagerFactoryUtils::getTransactionalEntityManager;
    }

}
