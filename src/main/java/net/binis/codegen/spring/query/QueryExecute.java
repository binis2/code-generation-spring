package net.binis.codegen.spring.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface QueryExecute<R> {

    R ensure();
    Optional<R> get();
    <V> Optional<V> get(Class<V> cls);
    List<R> list();
    <V> List<V> list(Class<V> cls);
    long count();
    Optional<R> top();
    <V> Optional<V> top(Class<V> cls);
    List<R> top(long records);
    <V> List<V> top(long records, Class<V> cls);
    Page<R> page(Pageable pageable);
    <V> Page<V> page(Pageable pageable, Class<V> cls);
    Page<R> page(long pageSize);
    <V> Page<V> page(long pageSize, Class<V> cls);
    void paginated(long pageSize, Consumer<R> consumer);
    void paginated(Pageable pageable, Consumer<R> consumer);
    Optional<Tuple> tuple();
    List<Tuple> tuples();

    QueryExecute<R> flush(FlushModeType type);
    QueryExecute<R> lock(LockModeType type);
    QueryExecute<R> hint(String hint, Object value);
    QueryFilter<R> filter(String name);

    boolean exists();
    void delete();
    int remove();

}
