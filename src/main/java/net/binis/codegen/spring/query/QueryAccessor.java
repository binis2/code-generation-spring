package net.binis.codegen.spring.query;

import java.util.List;
import java.util.function.IntSupplier;

public interface QueryAccessor {

    String getAccessorAlias();
    StringBuilder getAccessorSelect();
    StringBuilder getAccessorWhere();
    StringBuilder getAccessorOrder();
    List<Object> getParams();

    void setJoinSupplier(IntSupplier supplier);
    void setParams(List<Object> params);

}
