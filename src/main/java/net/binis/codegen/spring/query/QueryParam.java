package net.binis.codegen.spring.query;

import java.util.Collection;

public interface QueryParam<R> extends QueryExecute<R> {

    QueryParam<R> params(Collection<Object> params);
    QueryParam<R> param(Object param);
    int run();

}
