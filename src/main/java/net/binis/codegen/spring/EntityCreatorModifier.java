package net.binis.codegen.spring;

import net.binis.codegen.modifier.Modifiable;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

public class EntityCreatorModifier {

    private static final Map<Class<?>, ObjectFactory> implementors = new HashMap<>();

    private EntityCreatorModifier() {
        //Do nothing
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Modifiable<T> create(Class<?> cls) {
        var factory = implementors.get(cls);
        if (nonNull(factory)) {
            return (Modifiable<T>) factory.create();
        }
        return null;
    }

    public static void register(Class<?> intf, ObjectFactory factory) {
        implementors.put(intf, factory);
    }

}
