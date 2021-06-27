package net.binis.codegen.spring;

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.annotation.Final;
import net.binis.codegen.exception.GenericCodeGenException;
import net.binis.codegen.modifier.Modifier;
import net.binis.codegen.spring.component.ApplicationContextProvider;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
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
        var em = entityManagerProvider.apply(factory);
        if (isNull(em) || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("Attempt to do action outside of open transaction!");
            template.execute(s -> {
                var manager = entityManagerProvider.apply(factory);
                func.accept(manager);
                return null;
            });
        } else {
            func.accept(em);
        }
    }

    protected R withRes(Function<EntityManager, R> func) {
        init();
        var em = entityManagerProvider.apply(factory);
        if (isNull(em) || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("Attempt to do action outside of open transaction!");
            return template.execute(s ->
                func.apply(entityManagerProvider.apply(factory)));
        } else {
            return func.apply(em);
        }
    }

    private static Function<EntityManagerFactory, EntityManager> defaultEntityManagerProvider() {
        return EntityManagerFactoryUtils::getTransactionalEntityManager;
    }

}
