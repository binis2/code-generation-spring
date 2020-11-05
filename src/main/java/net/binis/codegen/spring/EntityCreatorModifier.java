package net.binis.codegen.spring;

import net.binis.codegen.exception.GenericCodeGenException;
import net.binis.codegen.modifier.Modifiable;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

public class EntityCreatorModifier {

    private static final Map<Class<?>, Constructor<?>> implementors = new HashMap<>();

    private EntityCreatorModifier() {
        //Do nothing
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Modifiable<T> create(Class<?> cls) {
        var impl = implementors.get(cls);
        if (nonNull(impl)) {
            try {
                return (Modifiable<T>) impl.newInstance();
            } catch (Exception e) {
                throw new GenericCodeGenException("Cannot create instance for "+ cls.getCanonicalName());
            }
        }
        return null;
    }

    public static void register(Class<?> intf, Class<?> impl) {
        try {
            implementors.put(intf, impl.getConstructor());
        } catch (Exception e) {
            throw new GenericCodeGenException("Class "+ impl.getCanonicalName() +" have no default constructor!");
        }
    }


}
