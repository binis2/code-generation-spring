package net.binis.codegen.spring.query.executor;

import net.binis.codegen.spring.query.QueryAggregateOperation;
import net.binis.codegen.spring.query.QueryExecute;
import net.binis.codegen.spring.query.QueryFilter;
import net.binis.codegen.spring.query.QueryOrderOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class QueryOrderer<R> implements QueryExecute<R>, QueryOrderOperation<Object, R>, QueryAggregateOperation {

    protected final QueryExecutor<?, ?, ?, R, ?> executor;
    protected final Function<String, Object> func;

    public QueryOrderer(QueryExecutor<?, ?, ?, R, ?> executor, Function<String, Object> func) {
        this.executor = executor;
        this.func = func;
    }

    @Override
    public R ensure() {
        return executor.ensure();
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
    public Page<R> page(long pageSize) {
        return executor.page(pageSize);
    }

    @Override
    public <V> Page<V> page(long pageSize, Class<V> cls) {
        return executor.page(pageSize, cls);
    }

    @Override
    public void paginated(long pageSize, Consumer<R> consumer) {
        executor.paginated(pageSize, consumer);
    }

    @Override
    public void paginated(Pageable pageable, Consumer<R> consumer) {
        executor.paginated(pageable, consumer);
    }

    @Override
    public Optional<Tuple> tuple() {
        return executor.tuple();
    }

    @Override
    public List<Tuple> tuples() {
        return executor.tuples();
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
    public QueryFilter<R> filter(String name) {
        return executor.filter(name);
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

    @Override
    public Object sum() {
        executor.sum();
        return this;
    }

    @Override
    public Object min() {
        executor.min();
        return this;
    }

    @Override
    public Object max() {
        executor.max();
        return this;
    }

    @Override
    public Object avg() {
        executor.avg();
        return this;
    }

    @Override
    public Object cnt() {
        executor.cnt();
        return this;
    }

    @Override
    public Object desc() {
        return null;
    }

    @Override
    public Object asc() {
        return null;
    }

    public QueryAggregateOperation and() {
        return this;
    }

    public Object where() {
        executor.whereStart();
        return executor;
    }

}
