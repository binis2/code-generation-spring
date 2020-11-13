package net.binis.codegen.spring;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

public class EntityCreator {

    private static final Map<Class<?>, ObjectFactory> implementors = new HashMap<>();

    private EntityCreator() {
        //Do nothing
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> cls) {
        var factory = implementors.get(cls);
        if (nonNull(factory)) {
            return (T) factory;
        }
        return null;
    }

    public static void register(Class<?> intf, ObjectFactory factory) {
        implementors.put(intf, factory);
    }

}
