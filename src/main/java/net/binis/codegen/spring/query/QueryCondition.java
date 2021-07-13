package net.binis.codegen.spring.query;

import java.util.function.Consumer;
import java.util.function.Function;

public interface QueryCondition<S, O, R> extends QuerySelectOperation<S, O, R> {

    QuerySelectOperation<S, O, R> _else(Consumer<QuerySelectOperation<S, O, R>> query);

}
