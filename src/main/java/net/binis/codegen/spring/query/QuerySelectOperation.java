package net.binis.codegen.spring.query;

public interface QuerySelectOperation<S, O, R> extends QueryExecute<R> {

    S and();
    S like(String what);
    S or();
    O order();
}
