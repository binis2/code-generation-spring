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
import java.util.function.Function;

import static java.util.Objects.isNull;

@Slf4j
public class BaseEntityModifier<T, R> extends BasePersistenceOperations<R> implements Modifier<R> {

    private R parent;

    @Final
    public R save() {
        with(manager -> manager.persist(parent));
        return parent;
    }

    @Final
    public R saveAndFlush() {
        save();
        with(EntityManager::flush);
        return parent;
    }

    @Final
    public R merge() {
        return withRes(manager -> manager.merge(parent));
    }

    @Final
    public R delete() {
        with(manager -> manager.remove(parent));
        return parent;
    }

    @Final
    public R refresh() {
        with(manager -> manager.refresh(parent));
        return parent;
    }

    @Override
    public void setObject(R parent) {
        this.parent = parent;
    }

}
