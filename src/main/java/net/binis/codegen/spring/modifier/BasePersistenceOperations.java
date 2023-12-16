package net.binis.codegen.spring.modifier;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.exception.GenericCodeGenException;
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.modifier.impl.BaseModifierImpl;
import net.binis.codegen.spring.component.ApplicationContextProvider;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public abstract class BasePersistenceOperations<T, R> extends BaseModifierImpl<T, R> {

    public static final String NO_TRANSACTION_DEBUG_WARNING = "Attempt to do action outside of open transaction!";

    protected static EntityManagerFactory factory;
    protected static TransactionTemplate template;

    @Setter
    protected static Function<EntityManagerFactory, EntityManager> entityManagerProvider = defaultEntityManagerProvider();

    protected BasePersistenceOperations(R parent) {
        super(parent);
    }

    public static EntityManager getEntityManager() {
        return entityManagerProvider.apply(factory);
    }

    protected static void init() {
        if (isNull(factory)) {
            var context = ApplicationContextProvider.getApplicationContext();
            if (isNull(context)) {
                throw new GenericCodeGenException("Not in spring context!\nUse '@ExtendWith(CodeGenExtension.class)' if you are in running unit test!");
            }
            JpaTransactionManager tm;
            try {
                tm = context.getBean(JpaTransactionManager.class);
            } catch (NoSuchBeanDefinitionException e) {
                tm = CodeFactory.create(JpaTransactionManager.class);
                if (isNull(tm)) {
                    throw new GenericCodeGenException("No JpaTransactionManager found!");
                }
            }
            factory = tm.getEntityManagerFactory();
            try {
                template = context.getBean(TransactionTemplate.class);
            } catch (NoSuchBeanDefinitionException e) {
                template = CodeFactory.create(TransactionTemplate.class);
                if (isNull(template)) {
                    throw new GenericCodeGenException("No TransactionTemplate found!");
                }
            }
        }
    }

    protected void with(Consumer<EntityManager> func) {
        init();
        var em = getEntityManager();
        if (isNull(em) || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug(NO_TRANSACTION_DEBUG_WARNING);
            template.execute(s -> {
                checkMerge(m -> {
                    func.accept(m);
                    return null;
                });
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
            log.debug(NO_TRANSACTION_DEBUG_WARNING);
            return template.execute(s ->
                checkMerge(func));
        } else {
            return func.apply(em);
        }
    }

    protected R checkMerge(Function<EntityManager, R> func) {
        var manager = getEntityManager();
        if (nonNull(parent)) {
            parent = manager.merge(parent);
        }
        return func.apply(manager);
    }

    protected R withNewTransactionRes(Function<EntityManager, R> func) {
        init();
        var em = getEntityManager();
        if (isNull(em) || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug(NO_TRANSACTION_DEBUG_WARNING);
            return template.execute(s ->
                    checkMerge(func));
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
