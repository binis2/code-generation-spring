package net.binis.codegen.spring.query;

public interface QueryJoinAggregateOperation<R, S> extends QueryAggregateOperation<R> {

    S where();

}
