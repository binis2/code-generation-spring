package net.binis.codegen.spring.query;

import java.util.Collection;

public interface PreparedQuery<R> extends QueryExecute<R> {
    PreparedQuery<R> params(Collection<Object> params);
    PreparedQuery<R> param(int idx, Object param);
}
