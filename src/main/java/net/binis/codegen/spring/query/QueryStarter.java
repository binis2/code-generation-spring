package net.binis.codegen.spring.query;

import java.util.function.Consumer;

public interface QueryStarter<R, S> {

    S by();

    QueryParam<R> nativeQuery(String query);

    QueryParam<R> query(String query);

    void transaction(Consumer<QueryStarter<R, S>> consumer);

}
