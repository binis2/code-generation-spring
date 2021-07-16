package net.binis.codegen.spring.query;

import java.util.function.Consumer;
import java.util.function.Function;

public interface QueryAggregateOperation<R> {

    R cnt();
    R sum();
    R min();
    R max();
    R avg();

}
