package net.binis.codegen.spring.query;

/*-
 * #%L
 * code-generator-spring
 * %%
 * Copyright (C) 2021 Binis Belev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface QueryExecute<R> extends Queryable {

    R ensure();
    Optional<R> reference();
    Optional<R> get();
    <V> Optional<V> get(Class<V> cls);
    List<R> list();
    List<R> references();
    <V> List<V> list(Class<V> cls);
    long count();
    Optional<R> top();
    <V> Optional<V> top(Class<V> cls);
    List<R> top(long records);
    <V> List<V> top(long records, Class<V> cls);

    Page<R> page(long pageSize);
    <V> Page<V> page(long pageSize, Class<V> cls);
    Page<R> page(Pageable pageable);
    <V> Page<V> page(Pageable pageable, Class<V> cls);
    void paginated(long pageSize, Consumer<R> consumer);
    void paginated(Pageable pageable, Consumer<R> consumer);
    <V> void paginated(long pageSize, Class<V> cls, Consumer<V> consumer);
    <V> void paginated(Pageable pageable, Class<V> cls, Consumer<V> consumer);
    void paged(long pageSize, Consumer<Page<R>> consumer);
    void paged(Pageable pageable, Consumer<Page<R>> consumer);
    <V> void paged(long pageSize, Class<V> cls, Consumer<Page<V>> consumer);
    <V> void paged(Pageable pageable, Class<V> cls, Consumer<Page<V>> consumer);

    Optional<Tuple> tuple();
    <V> Optional<Class<V>> tuple(Class<V> cls);
    List<Tuple> tuples();
    <V> List<V> tuples(Class<V> cls);
    PreparedQuery<R> prepare();

    <V> QueryExecute<V> projection(Class<V> projection);
    QueryExecute<R> flush(FlushModeType type);
    QueryExecute<R> lock(LockModeType type);
    QueryExecute<R> hint(String hint, Object value);
    QueryFilter<R> filter(String name);

    boolean exists();
    void delete();
    int remove();

}
