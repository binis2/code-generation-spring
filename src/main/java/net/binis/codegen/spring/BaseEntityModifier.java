package net.binis.codegen.spring;

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.annotation.Final;
import net.binis.codegen.modifier.Modifier;
import net.binis.codegen.spring.component.ApplicationContextProvider;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

@Slf4j
public class BaseEntityModifier<T, R> implements Modifier<R> {

    private static EntityManagerFactory factory;
    private static TransactionTemplate template;


    private R parent;

    private static void init() {
        if (isNull(factory)) {
            JpaTransactionManager tm = ApplicationContextProvider.getApplicationContext().getBean(JpaTransactionManager.class);
            factory = tm.getEntityManagerFactory();
            template = ApplicationContextProvider.getApplicationContext().getBean(TransactionTemplate.class);
        }
    }

    @Final
    public R save() {
        return with(manager -> manager.persist(parent));
    }

    @Final
    public R saveAndFlush() {
        save();
        return with(EntityManager::flush);
    }

    @Final
    public R merge() {
        return with(manager -> manager.merge(parent));
    }

    @Final
    public R delete() {
        return with(manager -> manager.remove(parent));
    }

    @Override
    public void setObject(R parent) {
        this.parent = parent;
    }

    private R with(Consumer<EntityManager> func) {
        init();
        var em = EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
        if (isNull(em) || !TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("Attempt to do action outside of open transaction!");
            template.execute(s -> {
                var manager = EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
                func.accept(manager);
                return null;
            });
        } else {
            func.accept(em);
        }
        return parent;
    }

}
