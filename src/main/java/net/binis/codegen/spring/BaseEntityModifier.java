package net.binis.codegen.spring;

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.modifier.Modifier;
import net.binis.codegen.spring.component.ApplicationContextProvider;

import javax.persistence.EntityManager;

import static java.util.Objects.isNull;

@Slf4j
public class BaseEntityModifier<T> implements Modifier<T> {

    private static EntityManager manager;

    private Object parent;

    private static void init() {
        if (isNull(manager)) {
            manager = ApplicationContextProvider.getApplicationContext().getBean(EntityManager.class);
        }
    }

    @SuppressWarnings("unchecked")
    public T save() {
        init();
        manager.persist(parent);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T delete() {
        init();
        manager.remove(parent);
        return (T) this;
    }

    @Override
    public void setObject(Object parent) {
        this.parent = parent;
    }
}
