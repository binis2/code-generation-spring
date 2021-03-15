package net.binis.codegen.spring.query.executor;

import net.binis.codegen.spring.query.QueryExecute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
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
    public Page<R> page(Pageable pageable) {
        return executor.page(pageable);
    }

    @Override
    public <V> Page<V> page(Pageable pageable, Class<V> cls) {
        return executor.page(pageable, cls);
    }

    @Override
    public QueryExecute<R> flush(FlushModeType type) {
        return executor.flush(type);
    }

    @Override
    public QueryExecute<R> lock(LockModeType type) {
        return executor.lock(type);
    }

    @Override
    public QueryExecute<R> hint(String hint, Object value) {
        return executor.hint(hint, value);
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
