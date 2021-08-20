package net.binis.codegen.spring.query;

import java.util.function.Function;

public interface MockedQuery {

    void setMocked(Function<Object, Object> onValue, Function<Object, Object> onParamAdd);

}
