package net.binis.codegen.spring.query.executor;

import net.binis.codegen.spring.query.QueryExecute;

public class QueryOrderer<R> implements QueryExecute<R> {

    protected final QueryExecutor<?, ?, R> executor;

    public QueryOrderer(QueryExecutor<?, ?, R> executor) {
        this.executor = executor;
    }

    @Override
    public R go() {
        return executor.go();
    }

}
