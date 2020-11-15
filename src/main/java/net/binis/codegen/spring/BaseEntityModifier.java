package net.binis.codegen.spring;

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.annotation.Final;
import net.binis.codegen.modifier.Modifier;
import net.binis.codegen.spring.component.ApplicationContextProvider;

import javax.persistence.EntityManager;

import static java.util.Objects.isNull;

@Slf4j
public class BaseEntityModifier<T, R> implements Modifier<R> {

    private static EntityManager manager;

    private R parent;

    private static void init() {
        if (isNull(manager)) {
            manager = ApplicationContextProvider.getApplicationContext().getBean(EntityManager.class);
        }
    }

    @Final
    public R save() {
        init();
        manager.persist(parent);
        return parent;
    }

    @Final
    public R saveAndFlush() {
        save();
        manager.flush();
        return parent;
    }

    @Final
    public R merge() {
        init();
        manager.merge(parent);
        return parent;
    }

    @Final
    public R delete() {
        init();
        manager.remove(parent);
        return parent;
    }

    @Override
    public void setObject(R parent) {
        this.parent = parent;
    }
}
