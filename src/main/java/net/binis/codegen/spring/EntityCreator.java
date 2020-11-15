package net.binis.codegen.spring;

import net.binis.codegen.factory.CodeFactory;

public class EntityCreator {

    private EntityCreator() {
        //Do nothing
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> cls) {
        return CodeFactory.create(cls);
    }

}
