package net.binis.codegen.spring.query;

public interface QueryAggregator<Q, A> {

    A and();
    Q where();

}
