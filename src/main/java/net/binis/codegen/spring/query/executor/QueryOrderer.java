package net.binis.codegen.spring.query.executor;

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

import net.binis.codegen.spring.query.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

@SuppressWarnings("unchecked")
public class QueryOrderer<R> implements QueryAccessor, QueryOrderOperation<Object, R>, QueryJoinAggregateOperation, QuerySelf, QueryIdentifier<Object, R, R> {

    protected final QueryExecutor<Object, Object, Object, R, Object, Object, Object> executor;
    protected final Function<String, Object> func;

    public QueryOrderer(QueryExecutor<Object, Object, Object, R, Object, Object, Object> executor, Function<String, Object> func) {
        this.executor = executor;
        executor.wrapper = this;
        this.func = func;
    }

    @Override
    public R ensure() {
        return executor.ensure();
    }

    @Override
    public Optional<R> reference() {
        return executor.reference();
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
    public List<R> references() {
        return executor.references();
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
    public <V> void paginated(long pageSize, Class<V> cls, Consumer<V> consumer) {
        executor.paginated(pageSize, cls, consumer);
    }

    @Override
    public <V> void paginated(Pageable pageable, Class<V> cls, Consumer<V> consumer) {
        executor.paginated(pageable, cls, consumer);
    }

    @Override
    public void paged(long pageSize, Consumer<Page<R>> consumer) {
        executor.paged(pageSize, consumer);
    }

    @Override
    public void paged(Pageable pageable, Consumer<Page<R>> consumer) {
        executor.paged(pageable, consumer);
    }

    @Override
    public <V> void paged(long pageSize, Class<V> cls, Consumer<Page<V>> consumer) {
        executor.paged(pageSize, cls, consumer);
    }

    @Override
    public <V> void paged(Pageable pageable, Class<V> cls, Consumer<Page<V>> consumer) {
        executor.paged(pageable, cls, consumer);
    }

    @Override
    public Optional<Tuple> tuple() {
        return executor.tuple();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Optional<Class<V>> tuple(Class<V> cls) {
        return executor.tuple(cls);
    }

    @Override
    public List<Tuple> tuples() {
        return executor.tuples();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> List<V> tuples(Class<V> cls) {
        return executor.tuples(cls);
    }

    @Override
    public PreparedQuery<R> prepare() {
        return executor.prepare();
    }

    @Override
    public <V> QueryExecute<V> projection(Class<V> projection) {
        return executor.projection(projection);
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
    public int run() {
        return executor.run();
    }

    @Override
    public String print() {
        return executor.print();
    }

    public R script(String script) {
        executor.script(script);
        return (R) this;
    }

    public R script(String script, Object... params) {
        executor.script(script, params);
        return (R) this;
    }

    public Object _open() {
        executor._open();
        return this;
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
    public Object distinct() {
        executor.distinct();
        return this;
    }

    @Override
    public Object group() {
        executor.group();
        return this;
    }

    @Override
    public Object desc() {
        return executor.desc();
    }

    @Override
    public Object asc() {
        return executor.asc();
    }

    public QueryAggregateOperation and() {
        return this;
    }

    public Object where() {
        executor.whereStart();
        return executor;
    }

    @Override
    public String getAccessorAlias() {
        return executor.getAccessorAlias();
    }

    @Override
    public StringBuilder getAccessorSelect() {
        return executor.getAccessorSelect();
    }

    @Override
    public StringBuilder getAccessorWhere() {
        return executor.getAccessorWhere();
    }

    @Override
    public StringBuilder getAccessorOrder() {
        return executor.getAccessorOrder();
    }

    @Override
    public List<Object> getParams() {
        return executor.getParams();
    }

    @Override
    public String getCountQuery() {
        return executor.getCountQuery();
    }

    @Override
    public String getExistsQuery() {
        return executor.getExistsQuery();
    }

    @Override
    public void setJoinSupplier(IntSupplier supplier) {
        executor.setJoinSupplier(supplier);
    }

    @Override
    public void setParams(List<Object> params) {
        executor.setParams(params);
    }

    @Override
    public Object _self() {
        return executor._self();
    }

    @Override
    public QueryFunctions length() {
        return executor.length();
    }

    @Override
    public R equal(Queryable query) {
        return (R) executor.equal(query);
    }

    @Override
    public R in(Queryable query) {
        return (R) executor.in(query);
    }

    @Override
    public R isNull() {
        return (R) executor.isNull();
    }

    @Override
    public R isNotNull() {
        return (R) executor.isNotNull();
    }

    @Override
    public R like(String value) {
        return (R) executor.like(value);
    }

    @Override
    public R starts(String value) {
        return (R) executor.starts(value);
    }

    @Override
    public R ends(String value) {
        return (R) executor.ends(value);
    }

    @Override
    public R contains(String value) {
        return (R) executor.contains(value);
    }

    @Override
    public R greater(Queryable query) {
        return (R) executor.greater(query);
    }

    @Override
    public R greaterEqual(Queryable query) {
        return (R) executor.greaterEqual(query);
    }

    @Override
    public R less(Queryable query) {
        return (R) executor.less(query);
    }

    @Override
    public R lessEqual(Queryable query) {
        return (R) executor.lessEqual(query);
    }

    @Override
    public R lessEqual(Object value) {
        return (R) executor.lessEqual(value);
    }

    @Override
    public R less(Object value) {
        return (R) executor.less(value);
    }

    @Override
    public R greaterEqual(Object value) {
        return (R) executor.greaterEqual(value);
    }

    @Override
    public R greater(Object value) {
        return (R) executor.greater(value);
    }

    @Override
    public R in(Object... values) {
        return (R) executor.in(values);
    }

    @Override
    public R in(Collection<Object> values) {
        return (R) executor.in(values);
    }

    @Override
    public R between(Object from, Object to) {
        return (R) executor.between(from, to);
    }

    @Override
    public R equal(Object value) {
        return (R) executor.equal(value);
    }

    @Override
    public QueryFunctions<Integer, R> size() {
        return (QueryFunctions) executor.size();
    }

    @Override
    public R size(Integer size) {
        return (R) executor.size(size);
    }

    @Override
    public R contains(Object value) {
        return (R) executor.contains(value);
    }

    @Override
    public R notContains(Object value) {
        return (R) executor.notContains(value);
    }

    @Override
    public R containsAll(Collection<Object> list) {
        return (R) executor.containsAll(list);
    }

    @Override
    public R containsOne(Collection<Object> list) {
        return (R) executor.containsOne(list);
    }

    @Override
    public R containsNone(Collection<Object> list) {
        return (R) executor.containsNone(list);
    }

    @Override
    public R isEmpty() {
        return (R) executor.isEmpty();
    }

    @Override
    public R isNotEmpty() {
        return (R) executor.isNotEmpty();
    }

    @Override
    public R joinFetch() {
        return (R) executor.joinFetch();
    }

    @Override
    public R leftJoinFetch() {
        return (R) executor.leftJoinFetch();
    }
}
