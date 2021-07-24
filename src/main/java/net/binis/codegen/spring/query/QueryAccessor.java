package net.binis.codegen.spring.query;

import java.util.function.IntSupplier;

public interface QueryAccessor {

    String getAccessorAlias();
    StringBuilder getAccessorSelect();
    StringBuilder getAccessorWhere();
    StringBuilder getAccessorOrder();

    void setJoinSupplier(IntSupplier supplier);
}
