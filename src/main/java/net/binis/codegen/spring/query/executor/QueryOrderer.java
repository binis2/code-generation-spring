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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

public class QueryOrderer<R> implements QueryAccessor, QueryExecute<R>, QueryOrderOperation<Object, R>, QueryJoinAggregateOperation, QuerySelf {

    protected final QueryExecutor<?, ?, ?, R, ?, ?, ?> executor;
    protected final Function<String, Object> func;

    public QueryOrderer(QueryExecutor<?, ?, ?, R, ?, ?, ?> executor, Function<String, Object> func) {
        this.executor = executor;
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

    public Object script(String script) {
        executor.script(script);
        return this;
    }

    public Object script(String script, Object... params) {
        executor.script(script, params);
        return this;
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
    public void checkReferenceConditions() {
        executor.checkReferenceConditions();
    }

    @Override
    public String getCountQuery() {
        return executor.getCountQuery();
    }

    @Override
    public boolean isAltered() {
        return executor.isAltered();
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
}
