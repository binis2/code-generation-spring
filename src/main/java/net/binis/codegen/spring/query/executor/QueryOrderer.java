package net.binis.codegen.spring.query.executor;

import net.binis.codegen.spring.query.QueryExecute;

import java.util.List;
import java.util.Optional;

public class QueryOrderer<R> implements QueryExecute<R> {

    protected final QueryExecutor<?, ?, ?, R> executor;

    public QueryOrderer(QueryExecutor<?, ?, ?, R> executor) {
        this.executor = executor;
    }

    @Override
    public Optional<R> get() {
        return executor.get();
    }

    @Override
    public <V> Optional<V> get(Class<V> cls) {
        return executor.get(cls);
    }

    @Override
    public List<R> list() {
        return executor.list();
    }

    @Override
    public <V> List<V> list(Class<V> cls) {
        return executor.list(cls);
    }

    @Override
    public long count() {
        return executor.count();
    }

    @Override
    public Optional<R> top() {
        return executor.top();
    }

    @Override
    public <V> Optional<V> top(Class<V> cls) {
        return executor.top(cls);
    }

    @Override
    public List<R> top(long records) {
        return executor.top(records);
    }

    @Override
    public <V> List<V> top(long records, Class<V> cls) {
        return executor.top(records, cls);
    }

    @Override
    public boolean exists() {
        return executor.exists();
    }

    @Override
    public void delete() {
        executor.delete();
    }

    @Override
    public int remove() {
        return executor.remove();
    }

}
