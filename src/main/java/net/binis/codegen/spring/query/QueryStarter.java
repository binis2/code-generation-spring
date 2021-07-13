package net.binis.codegen.spring.query;

import java.util.function.Consumer;
import java.util.function.Function;

public interface QueryStarter<R, S> {

    S by();

    <T> T by(boolean condition, Function<S, T> query);

    <T> T by(boolean condition, Function<S, T> query, Function<S, T> elseQuery);

    QueryParam<R> nativeQuery(String query);

    QueryParam<R> query(String query);

    void transaction(Consumer<QueryStarter<R, S>> consumer);

}
