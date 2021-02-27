package net.binis.codegen.spring.query;

public interface QuerySelectOperation<S, O, R> extends QueryExecute<R> {

    S and();
    QuerySelectOperation<S, O, R> like();
    QuerySelectOperation<S, O, R> notLike();
    QuerySelectOperation<S, O, R> not();
    S or();
    O order();
}
