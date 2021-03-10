package net.binis.codegen.spring.query;

import java.util.List;
import java.util.Optional;

public interface QueryExecute<R> {

    Optional<R> get();
    <V> Optional<V> get(Class<V> cls);
    List<R> list();
    <V> List<V> list(Class<V> cls);
    long count();
    Optional<R> top();
    <V> Optional<V> top(Class<V> cls);
    List<R> top(long records);
    <V> List<V> top(long records, Class<V> cls);
    boolean exists();
    void delete();
    int remove();

}
