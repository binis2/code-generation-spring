package net.binis.codegen.spring.query.base;

import net.binis.codegen.spring.query.QueryParam;
import net.binis.codegen.spring.query.QueryStarter;

import java.util.function.Consumer;

public abstract class BaseQueryStart<R, S> implements QueryStarter<R, S> {

    @Override
    public S by() {
        return null;
    }

    @Override
    public QueryParam<R> nativeQuery(String query) {
        return null;
    }

    @Override
    public QueryParam<R> query(String query) {
        return null;
    }

    @Override
    public void transaction(Consumer<QueryStarter<R, S>> consumer) {

    }
}
