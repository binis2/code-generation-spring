package net.binis.demo.spring;

import com.spiralbank.core.tools.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import net.binis.demo.modifier.Modifier;

import javax.persistence.EntityManager;

import static java.util.Objects.isNull;

@Slf4j
public class BaseEntityModifier implements Modifier {

    private static EntityManager manager;

    private static Object parent;

    private static void init() {
        if (isNull(manager)) {
            manager = ApplicationContextProvider.getApplicationContext().getBean(EntityManager.class);
        }
    }

    public void save() {
        init();
        manager.persist(parent);
    }

    public void delete() {
        manager.remove(parent);
    }

    @Override
    public void setObject(Object parent) {
        this.parent = parent;
    }
}
