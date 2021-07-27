package net.binis.codegen.spring.query;

import java.util.function.Consumer;

public interface QuerySelectOperation<S, O, R> extends QueryExecute<R>, QueryOrderStart<O> {
    S and();
    S or();
    QueryCondition<S, O, R> _if(boolean condition, Consumer<QuerySelectOperation<S, O, R>> query);
}
