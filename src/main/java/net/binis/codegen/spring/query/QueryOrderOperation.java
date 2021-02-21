package net.binis.codegen.spring.query;

public interface QueryOrderOperation<S, R> extends QueryExecute<R> {

    S desc();
    S asc();

}
