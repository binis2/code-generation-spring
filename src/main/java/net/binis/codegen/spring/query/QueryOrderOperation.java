package net.binis.codegen.spring.query;

public interface QueryOrderOperation<R, S> extends QueryExecute<R> {

    S desc();
    S asc();

}
