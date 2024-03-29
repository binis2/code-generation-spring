package net.binis.codegen.spring.query.executor;

/*-
 * #%L
 * code-generator-spring
 * %%
 * Copyright (C) 2021 - 2024 Binis Belev
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

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.async.AsyncDispatcher;
import net.binis.codegen.async.executor.CodeGenCompletableFuture;
import net.binis.codegen.creator.EntityCreator;
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.spring.collection.ObservableList;
import net.binis.codegen.spring.modifier.BasePersistenceOperations;
import net.binis.codegen.spring.query.*;
import net.binis.codegen.spring.query.exception.QueryBuilderException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static net.binis.codegen.tools.Reflection.isGetter;

@SuppressWarnings("unchecked")
@Slf4j
public abstract class QueryExecutor<T, S, O, R, A, F, U> extends BasePersistenceOperations<T, R> implements QueryAccessor, QueryIdentifier<T, QuerySelectOperation<S, O, R>, R>, QuerySelectOperation<S, O, R>, QueryOrderOperation<O, R>, QueryFilter<R>, QueryFunctions<T, QuerySelectOperation<S, O, R>>, QueryJoinCollectionFunctions<T, QuerySelectOperation<S, O, R>, Object>, QueryParam<R>, QueryStarter<R, S, A, F, U>, QueryCondition<S, O, R>, QueryJoinAggregateOperation, PreparedQuery<R>, MockedQuery, QuerySelf, QueryEmbed, UpdatableQuery {

    protected static final String DEFAULT_ALIAS = "u";
    protected static final Map<Class<?>, Map<Class<?>, List<String>>> projections = new ConcurrentHashMap<>();
    public static final String CODE_EXECUTOR = "net.binis.codegen.async.executor.CodeExecutor";
    protected QueryExecutor parent;
    protected Object wrapper;

    protected int fieldsCount = 0;
    protected List<Object> params = new ArrayList<>();
    protected QueryProcessor.ResultType resultType = QueryProcessor.ResultType.UNKNOWN;
    protected Supplier<QueryEmbed> queryName;
    protected final UnaryOperator<QueryExecutor> fieldsExecutor;
    protected Class<?> returnClass;
    protected Class<?> aggregateClass;
    protected Class<?> mapClass;
    protected Class<?> existsClass;
    protected Pageable pageable;
    protected boolean isNative;
    protected boolean isCustom;
    protected boolean isModifying;
    protected boolean prepared;
    protected O order;
    protected A aggregate;
    protected String enveloped = null;
    protected Runnable onEnvelop = null;
    protected boolean brackets;
    protected boolean condition;
    protected int lastIdStartPos;
    protected boolean skipNext;
    protected boolean fields;
    protected boolean update;
    protected boolean distinct;
    protected boolean isGroup;
    protected boolean selectOrAggregate;
    protected boolean projection;

    protected Function<Object, Object> mocked;

    protected final StringBuilder query = new StringBuilder();
    protected StringBuilder countQuery;
    protected StringBuilder existsQuery;
    protected String alias = DEFAULT_ALIAS;
    protected StringBuilder select;
    protected StringBuilder where;
    protected StringBuilder orderPart;
    protected StringBuilder group;
    protected StringBuilder join;
    protected StringBuilder current;
    protected int joins;

    protected IntSupplier joinSupplier = () -> joins++;

    protected boolean joinFetch;
    protected Class joinClass;
    protected String joinField;
    protected StringBuilder lastIdentifier;

    protected FlushModeType flushMode;
    protected LockModeType lockMode;
    protected Map<String, Object> hints;
    protected List<Filter> filters;
    protected Filter filter;
    protected int bracketCount;

    protected boolean pagedLoop;

    protected QueryExecutor(Class<?> returnClass, Supplier<QueryEmbed> queryName, UnaryOperator<QueryExecutor> fieldsExecutor) {
        super(null);
        this.returnClass = returnClass;
        this.queryName = queryName;
        this.fieldsExecutor = fieldsExecutor;
        mapClass = returnClass;
    }

    public QueryExecutor<T, S, O, R, A, F, U> $identifier(String id, Object value) {
        if (update && Objects.isNull(where)) {
            return _identifierUpdate(id, value);
        }

        if (Objects.isNull(where)) {
            whereStart();
        }

        var idStart = where.isEmpty() || where.charAt(where.length() - 1) != '.';

        if (idStart) {
            lastIdStartPos = where.length();
            lastIdentifier = new StringBuilder(id);
        }

        if (Objects.isNull(enveloped) && idStart) {
            where.append(" (").append(alias).append(".");
            brackets = true;
        }

        if (nonNull(mocked)) {
            value = mocked.apply(value);
        }

        if (Objects.isNull(value)) {
            where.append(id).append(" is null)");
        } else {
            params.add(value);
            where.append(id);
            if (nonNull(enveloped)) {
                if (nonNull(onEnvelop)) {
                    onEnvelop.run();
                    onEnvelop = null;
                } else {
                    where.append(enveloped);
                }
                enveloped = null;
            }
            where.append(" = ?").append(params.size()).append(")");
        }

        return this;
    }

    public QueryExecutor<T, S, O, R, A, F, U> _identifierUpdate(String id, Object value) {
        if (Objects.isNull(select)) {
            select = new StringBuilder();
        }

        if (nonNull(mocked)) {
            value = mocked.apply(value);
        }

        params.add(value);
        select.append(alias).append('.').append(id).append(" = ?").append(params.size()).append(',');

        return this;
    }


    public QueryIdentifier $identifier(String id) {
        var _par = nonNull(parent) ? $parent() : this;
        if (_par.fields) {
            var _sel = _par.select;
            if (_sel.length() == 0 || _sel.charAt(_sel.length() - 1) != '.') {
                _sel.append(alias).append(".");
            }
            _sel.append(id);
            if (nonNull(_par.aggregate)) {
                if (!_par.distinct && !_par.isGroup) {
                    _sel.append(")");
                } else {
                    if (_par.isGroup) {
                        if (Objects.isNull(_par.group)) {
                            _par.group = new StringBuilder();
                        }
                        _par.group.append(_par.current.substring(_par.lastIdStartPos)).append(",");
                    }
                    _par.distinct = false;
                    _par.isGroup = false;
                }
            } else {
                _sel.append(" as ").append(id);
            }
            _sel.append(",");
            _par.fieldsCount++;
        } else if (_par.current == _par.orderPart) {
            _orderIdentifier(id);
        } else {
            var _where = _par.where;
            if (Objects.isNull(_where)) {
                _where = whereStart();
            }

            var idStart = _where.isEmpty() || _where.charAt(_where.length() - 1) != '.';

            if (idStart) {
                lastIdStartPos = _where.length();
                lastIdentifier = new StringBuilder(id);
            }

            if (Objects.isNull(enveloped) && idStart) {
                _where.append(" (").append(alias).append(".");
                brackets = true;
            }
            _where.append(id);
            if (nonNull(enveloped)) {
                if (nonNull(onEnvelop)) {
                    onEnvelop.run();
                    onEnvelop = null;
                } else {
                    _where.append(enveloped);
                }
                enveloped = null;
            }
        }
        return $retParent();
    }

    public void embedded(String id) {
        var _current = $current();

        if (!_current.isEmpty() && _current.charAt(_current.length() - 1) == '.') {
            _current.append(id).append(".");
            if (nonNull(lastIdentifier)) {
                lastIdentifier.append(".").append(id);
            }
        } else {
            if (_current == where) {
                lastIdStartPos = where.length();
                lastIdentifier = new StringBuilder(id);
            }
            if (Objects.isNull(enveloped)) {
                _current.append(" (");
                if (!alias.equals(id)) {
                    _current.append(alias).append(".");
                }
            }
            _current.append(id).append(".");
        }
    }

    protected void _doNot() {
        $current().append(" not ");
    }

    protected void _doLower() {
        _envelop("lower");
    }

    protected void _doUpper() {
        _envelop("upper");
    }

    protected void _doTrim() {
        _envelop("trim");
    }

    protected void _doSubstring(int start) {
        _envelop("substr", start);
    }

    protected void _doSubstring(int start, int len) {
        _envelop("substr", start, len);
    }

    protected void _doReplace(String what, String withWhat) {
        _envelop("replace", what, withWhat);
    }

    protected void _backInsert(String func) {
        var idx = $current().lastIndexOf("(");
        $current().insert(idx + 1, func);
    }

    protected void _backEnvelop(String func) {
        _backInsert(func + "(");
        $current().append(")");
    }

    protected void _envelop(String func) {
        enveloped = ")";
        $current().append(" (").append(func).append("(");
    }

    protected void _envelop(String func, Object... params) {
        if (params.length == 0) {
            _envelop(func);
        } else {
            var s = new StringBuilder();
            for (Object param : params) {
                this.params.add(param);
                s.append(", ?").append(this.params.size());
            }
            enveloped = s.append(")").toString();

            $current().append(" (").append(func).append("(");
        }
    }

    protected void _envelop(String func, Runnable onEnvelop, Object... params) {
        this.onEnvelop = onEnvelop;
        _envelop(func, params);
    }

    protected void _operation(String op, Object value) {
        if (nonNull(mocked)) {
            value = mocked.apply(value);
        }

        params.add(value);
        if (nonNull(enveloped)) {
            if (nonNull(onEnvelop)) {
                onEnvelop.run();
                onEnvelop = null;
            } else {
                $current().append(enveloped);
            }
            enveloped = null;
        }
        $current().append(' ').append(op).append(" ?").append(params.size()).append(")");
        brackets = false;
    }

    protected QueryExecutor<T, S, O, R, A, F, U> _orderIdentifier(String id) {
        var _order = $orderPart();
        if (Objects.isNull(_order)) {
            _order = _orderStart();
        }

        if (_order.isEmpty() || _order.charAt(_order.length() - 1) != '.') {
            _order.append(' ').append(alias).append(".");
        }

        _order.append(id);
        return this;
    }

    protected Object _aggregateIdentifier(String id) {
        if (fieldsCount == 0) {
            resultType = QueryProcessor.ResultType.SINGLE;
        } else {
            resultType = QueryProcessor.ResultType.TUPLE;
        }

        $fieldsInc();
        select.append(alias).append(".").append(id).append(distinct || isGroup ? " " : ")").append(",");
        if (isGroup) {
            if (Objects.isNull(group)) {
                group = new StringBuilder();
            }

            group.append(alias).append(".").append(id).append(",");
        }
        distinct = false;
        isGroup = false;
        return aggregate;
    }

    protected O _orderStart(O order) {
        this.order = order;
        if (Objects.isNull(orderPart)) {
            orderPart = new StringBuilder();
        }
        current = orderPart;
        return order;
    }

    protected A _aggregateStart(A aggregate) {
        this.aggregate = aggregate;
        select = new StringBuilder();
        current = select;
        selectOrAggregate = true;
        fields = true;
        return aggregate;
    }

    public QuerySelectOperation<S, O, R> script(String script) {
        _stripLast(" ");
        _stripLast(".");
        $current().append(' ').append(script);

        if (brackets) {
            $current().append(")");
            brackets = false;
        }

        $current().append(' ');
        return this;
    }

    public QuerySelectOperation<S, O, R> script(String script, Object... params) {
        script(script);
        Collections.addAll(this.params, params);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S and() {
        if (!skipNext) {
            $current().append(" and ");
            brackets = false;
        } else {
            skipNext = false;
        }
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S or() {
        if (!skipNext) {
            $current().append(" or ");
            brackets = false;
        } else {
            skipNext = false;
        }
        return (S) this;
    }

    public QuerySelectOperation<S, O, R> _close() {
        if (bracketCount <= 0) {
            throw new QueryBuilderException("Closing bracket without opening one!");
        }
        bracketCount--;
        $current().append(") ");
        return this;
    }

    public Object _open() {
        bracketCount++;
        $current().append(" (");
        return this;
    }


    @Override
    public QueryCondition<S, O, R> _if(boolean condition, Consumer query) {
        this.condition = condition;
        if (condition) {
            query.accept(this);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public S by() {
        whereStart();
        resultType = QueryProcessor.ResultType.SINGLE;
        return (S) this;
    }

    public S by(Class<?> projection) {
        buildProjection(projection);
        return by();
    }


    @SuppressWarnings("unchecked")
    @Override
    public F select() {
        select = new StringBuilder();
        current = select;
        fields = true;
        selectOrAggregate = true;
        return (F) fieldsExecutor.apply(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public U update() {
        update = true;
        select = new StringBuilder();
        current = select;
        return (U) this;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <Q> Q by(boolean condition, Function<S, Q> query) {
        whereStart();
        if (condition) {
            query.apply((S) this);
        }
        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Q> Q by(boolean condition, Function<S, Q> query, Function<S, Q> elseQuery) {
        whereStart();
        if (condition) {
            query.apply((S) this);
        } else {
            elseQuery.apply((S) this);
        }
        return (Q) this;
    }

    public long count() {
        resultType = QueryProcessor.ResultType.COUNT;

        if (Objects.isNull(countQuery)) {
            getCountQuery();
        }

        return (long) execute();
    }

    @Override
    public Optional<R> top() {
        resultType = QueryProcessor.ResultType.SINGLE;
        if (Objects.isNull(pageable) || pageable.getPageNumber() != 0 || pageable.getPageSize() != 1) {
            pageable = PageRequest.of(0, 1);
        }
        return get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Optional<V> top(Class<V> cls) {
        mapClass = cls;
        return (Optional) top();
    }

    public QueryParam<R> nativeQuery(String query) {
        isNative = true;
        return query(query);
    }

    public QueryParam<R> query(String query) {
        isCustom = true;
        this.query.setLength(0);
        this.query.append(query);
        return this;
    }

    @Override
    public void transaction(Consumer<QueryStarter<R, S, A, F, U>> consumer) {
        with(manager -> consumer.accept(this));
    }

    @Override
    public CompletableFuture<Void> asyncC(Consumer<QueryStarter<R, S, A, F, U>> consumer) {
        return CodeGenCompletableFuture.runAsync(CodeFactory.createDefault(AsyncDispatcher.class, CODE_EXECUTOR)._default(), () ->
                transaction(consumer));
    }

    @Override
    public <J> CompletableFuture<J> async(Function<QueryStarter<R, S, A, F, U>, J> func) {
        return (CompletableFuture<J>) CodeGenCompletableFuture.newSupplyAsync(CodeFactory.createDefault(AsyncDispatcher.class, CODE_EXECUTOR)._default(), () ->
                withRes(manager -> (R) func.apply(this)));
    }

    @Override
    public CompletableFuture<Void> asyncC(String flow, Consumer<QueryStarter<R, S, A, F, U>> consumer) {
        return CodeGenCompletableFuture.runAsync(CodeFactory.createDefault(AsyncDispatcher.class, CODE_EXECUTOR).flow(flow), () ->
                transaction(consumer));
    }

    @Override
    public <J> CompletableFuture<J> async(String flow, Function<QueryStarter<R, S, A, F, U>, J> func) {
        return (CompletableFuture<J>) CodeGenCompletableFuture.newSupplyAsync(CodeFactory.createDefault(AsyncDispatcher.class, CODE_EXECUTOR).flow(flow), () ->
                withRes(manager -> (R) func.apply(this)));
    }

    @Override
    public CompletableFuture<Void> asyncC(long delay, TimeUnit unit, Consumer<QueryStarter<R, S, A, F, U>> consumer) {
        return CodeGenCompletableFuture.runAsync(CompletableFuture.delayedExecutor(delay, unit, CodeFactory.createDefault(AsyncDispatcher.class, CODE_EXECUTOR)._default()), () ->
                transaction(consumer));
    }

    @Override
    public <J> CompletableFuture<J> async(long delay, TimeUnit unit, Function<QueryStarter<R, S, A, F, U>, J> func) {
        return (CompletableFuture<J>) CodeGenCompletableFuture.newSupplyAsync(CompletableFuture.delayedExecutor(delay, unit, CodeFactory.createDefault(AsyncDispatcher.class, CODE_EXECUTOR)._default()), () ->
                withRes(manager -> (R) func.apply(this)));
    }

    @Override
    public CompletableFuture<Void> asyncC(String flow, long delay, TimeUnit unit, Consumer<QueryStarter<R, S, A, F, U>> consumer) {
        return CodeGenCompletableFuture.runAsync(CompletableFuture.delayedExecutor(delay, unit, CodeFactory.createDefault(AsyncDispatcher.class, CODE_EXECUTOR).flow(flow)), () ->
                transaction(consumer));
    }

    @Override
    public <J> CompletableFuture<J> async(String flow, long delay, TimeUnit unit, Function<QueryStarter<R, S, A, F, U>, J> func) {
        return (CompletableFuture<J>) CodeGenCompletableFuture.newSupplyAsync(CompletableFuture.delayedExecutor(delay, unit, CodeFactory.createDefault(AsyncDispatcher.class, CODE_EXECUTOR).flow(flow)), () ->
                withRes(manager -> (R) func.apply(this)));
    }

    @Override
    public CompletableFuture<Void> asyncC(Duration duration, Consumer<QueryStarter<R, S, A, F, U>> consumer) {
        return asyncC(duration.toMillis(), TimeUnit.MILLISECONDS, consumer);
    }

    @Override
    public <J> CompletableFuture<J> async(Duration duration, Function<QueryStarter<R, S, A, F, U>, J> func) {
        return async(duration.toMillis(), TimeUnit.MILLISECONDS, func);
    }

    @Override
    public CompletableFuture<Void> asyncC(String flow, Duration duration, Consumer<QueryStarter<R, S, A, F, U>> consumer) {
        return asyncC(flow, duration.toMillis(), TimeUnit.MILLISECONDS, consumer);
    }

    @Override
    public <J> CompletableFuture<J> async(String flow, Duration duration, Function<QueryStarter<R, S, A, F, U>, J> func) {
        return async(flow, duration.toMillis(), TimeUnit.MILLISECONDS, func);
    }

    public List<R> top(long records) {
        pageable = PageRequest.of(0, (int) records);
        return list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> List<V> top(long records, Class<V> cls) {
        mapClass = cls;
        return (List) top(records);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Page<R> page(Pageable pageable) {
        resultType = QueryProcessor.ResultType.PAGE;
        this.pageable = pageable;
        return _checkPage((Page) execute());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Page<V> page(Pageable pageable, Class<V> cls) {
        mapClass = cls;
        return (Page) page(pageable);
    }

    @Override
    public Page<R> page(long pageSize) {
        return page(PageRequest.of(0, (int) pageSize));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Page<V> page(long pageSize, Class<V> cls) {
        mapClass = cls;
        return (Page) page(PageRequest.of(0, (int) pageSize));
    }

    @Override
    public void paginated(long pageSize, Consumer<R> consumer) {
        paginated(PageRequest.of(0, (int) pageSize), consumer);
    }

    @Override
    public void paginated(Pageable pageable, Consumer<R> consumer) {
        pagedLoop = true;
        try {
            var page = page(pageable);
            while (!page.isEmpty()) {
                page.getContent().forEach(consumer);
                if (page.getContent().size() < pageable.getPageSize()) {
                    break;
                }
                page = page(page.nextPageable());
            }
        } finally {
            pagedLoop = false;
        }
    }

    @Override
    public <V> void paginated(long pageSize, Class<V> cls, Consumer<V> consumer) {
        paginated(PageRequest.of(0, (int) pageSize), cls, consumer);
    }

    @Override
    public <V> void paginated(Pageable pageable, Class<V> cls, Consumer<V> consumer) {
        pagedLoop = true;
        try {
            var page = page(pageable, cls);
            while (!page.isEmpty()) {
                page.getContent().forEach(consumer);
                if (page.getContent().size() < pageable.getPageSize()) {
                    break;
                }
                page = page(page.nextPageable(), cls);
            }
        } finally {
            pagedLoop = false;
        }
    }

    @Override
    public void paged(long pageSize, Consumer<Page<R>> consumer) {
        paged(PageRequest.of(0, (int) pageSize), consumer);
    }

    @Override
    public void paged(Pageable pageable, Consumer<Page<R>> consumer) {
        pagedLoop = true;
        try {
            var page = page(pageable);
            while (!page.isEmpty()) {
                consumer.accept(page);
                if (page.getContent().size() < pageable.getPageSize()) {
                    break;
                }
                page = page(page.nextPageable());
            }
        } finally {
            pagedLoop = false;
        }
    }

    @Override
    public <V> void paged(long pageSize, Class<V> cls, Consumer<Page<V>> consumer) {
        paged(PageRequest.of(0, (int) pageSize), cls, consumer);
    }

    @Override
    public <V> void paged(Pageable pageable, Class<V> cls, Consumer<Page<V>> consumer) {
        pagedLoop = true;
        try {
            var page = page(pageable, cls);
            while (!page.isEmpty()) {
                consumer.accept(page);
                if (page.getContent().size() < pageable.getPageSize()) {
                    break;
                }
                page = page(page.nextPageable(), cls);
            }
        } finally {
            pagedLoop = false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Tuple> tuple() {
        return (Optional) tuple(fieldsCount == 1 ? mapClass : Tuple.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional tuple(Class cls) {
        mapClass = cls;
        resultType = QueryProcessor.ResultType.TUPLE;
        return (Optional) execute();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Tuple> tuples() {
        return (List) tuples(fieldsCount == 1 ? mapClass : Tuple.class);
    }

    @Override
    public PreparedQuery<R> prepare() {
        if (!prepared) {
            buildQuery(query, false);
            prepared = true;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List tuples(Class cls) {
        resultType = QueryProcessor.ResultType.TUPLES;
        mapClass = cls;
        return (List) execute();
    }

    @Override
    public QueryExecute<R> flush(FlushModeType type) {
        flushMode = type;
        return this;
    }

    @Override
    public QueryExecute<R> lock(LockModeType type) {
        lockMode = type;
        return this;
    }

    @Override
    public QueryExecute<R> hint(String hint, Object value) {
        if (Objects.isNull(hints)) {
            hints = new LinkedHashMap<>();
        }
        hints.put(hint, value);
        return this;
    }

    @Override
    public QueryFilter<R> filter(String name) {
        if (Objects.isNull(filters)) {
            filters = new ArrayList<>();
        }

        filter = Filter.builder().name(name).values(new LinkedHashMap<>()).build();
        filters.add(filter);

        return this;
    }

    @Override
    public <V> QueryExecute<V> projection(Class<V> projection) {
        buildProjection(projection);
        mapClass = projection;
        return (QueryExecute) this;
    }

    @Override
    public boolean exists() {
        resultType = QueryProcessor.ResultType.EXISTS;

        if (Objects.isNull(existsQuery)) {
            getExistsQuery();
        }
        return (boolean) execute();
    }

    @Override
    public boolean notExists() {
        return !exists();
    }

    @Override
    public void delete() {
        remove();
    }

    @Override
    public int remove() {
        isModifying = true;
        resultType = QueryProcessor.ResultType.REMOVE;
        var result = execute();
        return nonNull(result) ? (int) result : 0;
    }

    @Override
    public String print() {
        var result = new StringBuilder();
        buildQuery(result, false);
        return result.toString();
    }

    @Override
    public O desc() {
        var _orderPart = $orderPart();
        _stripLast(_orderPart, ".");
        _stripLast(_orderPart, ",");
        _orderPart.append(" desc,");
        return $order();
    }

    @Override
    public O asc() {
        var _orderPart = $orderPart();
        _stripLast(_orderPart, ".");
        _stripLast(_orderPart, ",");
        _orderPart.append(" asc,");
        return $order();
    }

    public Object execute() {
        var retClass = returnClass;
        StringBuilder actualQuery;
        if (resultType.equals(QueryProcessor.ResultType.COUNT) && nonNull(countQuery)) {
            actualQuery = countQuery;
            retClass = Long.class;
        } else {
            if (resultType.equals(QueryProcessor.ResultType.EXISTS) && nonNull(existsQuery)) {
                actualQuery = existsQuery;
                if (nonNull(existsClass)) {
                    retClass = existsClass;
                }
            } else {
                prepare();
                actualQuery = query;
            }

            if (nonNull(aggregateClass) && fieldsCount == 1) {
                retClass = aggregateClass;
            } else if (selectOrAggregate) {
                retClass = Tuple.class;
            }
        }

        var r = retClass;
        return withRes(manager ->
                QueryProcessor.process(this, manager, actualQuery.toString(), params, resultType, r, mapClass, isNative, isModifying, pageable, flushMode, lockMode, hints, filters));
    }

    protected void buildQuery(StringBuilder query, boolean countQuery) {
        if (bracketCount != 0) {
            throw new QueryBuilderException("Missing closing bracket!");
        }

        if (nonNull(select)) {
            _stripLast(select, ",");
            if (update) {
                query.append("update ");
            } else {
                query.append("select ").append(select).append(' ');
            }
        } else {
            if (nonNull(join)) {
                query.append("select ").append(alias).append(' ');
            }
        }
        if (query.length() == 0 && resultType == QueryProcessor.ResultType.REMOVE) {
            query.append("delete ");
        }
        if (!isCustom) {
            if (!update) {
                query.append("from ");
            }

            //query.append(returnClass.getName()).append(' ').append(alias).append(' ');
            //TODO: Remove this when hibernate bugs are fixed
            var cls = CodeFactory.lookup(returnClass);

            if (nonNull(cls)) {
                query.append(cls.getName());
            } else {
                query.append(returnClass.getName());
            }

            query.append(' ').append(alias).append(' ');
            //
        }

        if (update) {
            query.append("set ").append(select).append(' ');
        }

        if (nonNull(join) && !join.isEmpty()) {
            query.append(join);
        }

        if (nonNull(where) && where.length() > 1) {
            _stripLast(where, ",");
            query.append("where").append(where);
        }

        if (nonNull(group)) {
            _stripLast(group, ",");
            query.append(" group by ").append(group).append(' ');
        }

        if (nonNull(orderPart) && !countQuery) {
            _stripLast(orderPart, ",");
            query.append(" order by ").append(orderPart);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> List<V> list(Class<V> cls) {
        mapClass = cls;
        return (List) list();
    }

    @Override
    public R reference(Object id) {
        var impl = CodeFactory.lookup(returnClass);
        if (Objects.isNull(impl)) {
            throw new QueryBuilderException("Can't find implementation class for " + returnClass.getCanonicalName() + "!");
        }
        return withRes(manager -> (R) manager.getReference(impl, id));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<R> reference() {
        checkReferenceConditions();
        resultType = QueryProcessor.ResultType.REFERENCE;
        return (Optional) execute();
    }

    @Override
    public List<R> references() {
        checkReferenceConditions();
        resultType = QueryProcessor.ResultType.REFERENCES;
        return (List) execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public R ensure() {
        return get().orElseGet(() -> (R) EntityCreator.create(returnClass));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<R> get() {
        if (QueryProcessor.ResultType.UNKNOWN.equals(resultType)) {
            resultType = QueryProcessor.ResultType.SINGLE;
        }

        if (nonNull(select) && !Number.class.isAssignableFrom(mapClass) && !void.class.equals(mapClass)) {
            resultType = QueryProcessor.ResultType.TUPLE;
        }

        return (Optional) execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> Optional<V> get(Class<V> cls) {
        mapClass = cls;
        return (Optional<V>) get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<R> list() {
        if (projection || (selectOrAggregate && fieldsCount > 1)) {
            resultType = QueryProcessor.ResultType.TUPLES;
        } else {
            resultType = QueryProcessor.ResultType.LIST;
        }
        return (List) execute();
    }

    protected void _stripLast(String what) {
        _stripLast($current(), what);
    }

    protected void _stripLast(StringBuilder builder, String what) {
        var qlen = builder.length();
        var wlen = what.length();
        var idx = builder.lastIndexOf(what);
        if (idx > -1 && idx == qlen - wlen) {
            builder.setLength(qlen - wlen);
        }
    }

    protected void _stripToLast(StringBuilder builder, String what) {
        var idx = builder.lastIndexOf(what);
        if (idx > -1) {
            builder.setLength(idx + what.length());
        }
    }

    protected void _stripToLastInclude(StringBuilder builder, String what) {
        var idx = builder.lastIndexOf(what);
        if (idx > -1) {
            builder.setLength(idx);
        }
    }

    protected void _stripLastOperator() {
        where.setLength(lastIdStartPos);
        _stripLast(" ");
        _stripLast(" not");
        _stripLast(" ");
        _stripToLastInclude(where, " ");
        skipNext = where.length() == 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public QueryFunctions<Long, QuerySelectOperation<S, O, R>> length() {
        _backEnvelop("length");
        return (QueryFunctions) this;
    }

    @Override
    public QuerySelectOperation<S, O, R> equal(T value) {
        _stripLast(".");
        _operation("=", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> equal(Queryable query) {
        _subQueryOperation("=", query);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> between(T from, T to) {
        _stripLast(".");
        _operation("between", from);
        _stripLast(")");
        _operation("and", to);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> in(Collection<T> values) {
        if (Objects.isNull(values) || values.isEmpty()) {
            _stripToLast(current, "(");
            $current().append("0 <> 0) ");
        } else {
            _stripLast(".");
            _operation("in", values);
        }
        return this;
    }

    public QuerySelectOperation<S, O, R> in(T... values) {
        return in(Arrays.asList(values));
    }

    @Override
    public QuerySelectOperation<S, O, R> in(Queryable query) {
        _subQueryOperation("in", query);
        return this;
    }

    protected void _subQueryOperation(String op, Queryable query) {
        _stripLast(".");
        if (nonNull(enveloped)) {
            if (nonNull(onEnvelop)) {
                onEnvelop.run();
                onEnvelop = null;
            } else {
                $current().append(enveloped);
            }
            enveloped = null;
        }
        var access = (QueryAccessor) query;
        var s = query.print();
        var newAlias = "s" + joins++;
        var sub = s.replaceAll("\\(" + access.getAccessorAlias() + "\\.", "(" + newAlias + ".")
                .replaceAll(" " + access.getAccessorAlias() + "\\.", " " + newAlias + ".")
                .replaceAll(" " + access.getAccessorAlias() + " ", " " + newAlias + " ");
        for (int i = access.getParams().size(); i > 0; i--) {
            sub = sub.replaceAll("\\?" + i, "?" + (i + params.size()));
        }

        if (Objects.isNull(access.getAccessorSelect())) {
            sub = "select " + newAlias + " " + sub;
        }

        params.addAll(access.getParams());
        $current().append(" ").append(op).append(" (")
                .append(sub)
                .append(")) ");
    }

    @Override
    public QuerySelectOperation<S, O, R> isNull() {
        _stripLast(".");
        where.append(" is null)");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> isNotNull() {
        _stripLast(".");
        where.append(" is not null)");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> like(String value) {
        _stripLast(".");
        _operation("like", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> starts(String value) {
        _stripLast(".");
        _operation("like", value + "%");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> ends(String value) {
        _stripLast(".");
        _operation("like", "%" + value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> contains(String value) {
        _stripLast(".");
        _operation("like", "%" + value + "%");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> greater(T value) {
        _stripLast(".");
        _operation(">", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> greater(Queryable query) {
        _subQueryOperation(">", query);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> greaterEqual(T value) {
        _stripLast(".");
        _operation(">=", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> greaterEqual(Queryable query) {
        _subQueryOperation(">=", query);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> less(T value) {
        _stripLast(".");
        _operation("<", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> less(Queryable query) {
        _subQueryOperation("<", query);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> lessEqual(T value) {
        _stripLast(".");
        _operation("<=", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> lessEqual(Queryable query) {
        _subQueryOperation("<=", query);
        return this;
    }

    @Override
    public QueryExecutor<T, S, O, R, A, F, U> params(Collection<Object> params) {
        this.params = new ArrayList<>(params);
        return this;
    }

    @Override
    public PreparedQuery<R> param(int idx, Object param) {
        params.set(idx, param);
        return this;
    }

    @Override
    public QueryParam<R> param(Object param) {
        this.params.add(param);
        return this;
    }

    @Override
    public int run() {
        isModifying = true;
        resultType = QueryProcessor.ResultType.EXECUTE;
        return (int) execute();
    }

    @Override
    public QuerySelectOperation<S, O, R> _else(Consumer<QuerySelectOperation<S, O, R>> query) {
        if (!condition) {
            query.accept(this);
        }
        return this;
    }

    @Override
    public QueryFilter<R> parameter(String name, Object value) {
        if (nonNull(filter)) {
            filter.getValues().put(name, value);
        }
        return this;
    }

    @Override
    public QueryFilter<R> disable() {
        if (nonNull(filter)) {
            filter.setDisabled(true);
        }
        return this;
    }

    @Override
    public Object sum() {
        aggregateFunction("sum");
        return aggregate;
    }

    @Override
    public Object min() {
        aggregateFunction("min");
        return aggregate;
    }

    @Override
    public Object max() {
        aggregateFunction("max");
        return aggregate;
    }

    @Override
    public Object avg() {
        aggregateFunction("avg");
        if (fieldsCount == 0) {
            mapClass = Double.class;
        }
        return aggregate;
    }

    @Override
    public Object cnt() {
        aggregateFunction("count");
        if (fieldsCount == 0) {
            aggregateClass = Long.class;
            mapClass = Long.class;
        }
        return aggregate;
    }

    @Override
    public Object distinct() {
        select.append("distinct ");
        distinct = true;
        mapClass = Tuple.class;
        return aggregate;
    }

    @Override
    public Object group() {
        isGroup = true;
        return aggregate;
    }

    public void aggregateFunction(String sum) {
        select.append(sum).append("(");
        if (fieldsCount == 0) {
            aggregateClass = Double.class;
            mapClass = void.class;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public QueryFunctions<Integer, QuerySelectOperation<S, O, R>> size() {
        _backEnvelop("size");
        return (QueryFunctions) this;
    }

    public QuerySelectOperation<S, O, R> size(Integer size) {
        _backEnvelop("size");
        _stripLast(".");
        _operation("=", size);
        return this;
    }

    public void collection(String id, Object value) {
        throw new IllegalCallerException();
    }


    @Override
    public QuerySelectOperation<S, O, R> contains(T value) {
        params.add(value);
        _backInsert("?" + params.size() + " member of ");
        where.append(")");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> notContains(T value) {
        params.add(value);
        _backInsert("?" + params.size() + " not member of ");
        where.append(")");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> containsAll(Collection<T> list) {
        handleContainsCollection(list, " member of ", " and ");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> containsOne(Collection<T> list) {
        handleContainsCollection(list, " member of ", " or ");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> containsNone(Collection<T> list) {
        handleContainsCollection(list, " not member of ", " and ");
        return this;
    }

    protected void handleContainsCollection(Collection<T> list, String member, String oper) {
        var idx = $current().lastIndexOf("(");
        var col = $current().substring(idx + 1);
        $current().setLength(idx + 1);
        if (Objects.isNull(list) || list.isEmpty()) {
            $current().append("0 = 0");
        } else {
            for (var val : list) {
                params.add(val);
                $current().append("?").append(params.size()).append(member).append(col).append(oper);
            }
            _stripLast(oper);
        }
        $current().append(")");
    }

    @Override
    public QuerySelectOperation<S, O, R> isEmpty() {
        where.append(" is empty)");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> isNotEmpty() {
        where.append(" is not empty)");
        return this;
    }

    public StringBuilder whereStart() {
        if (nonNull(parent)) {
            return $parent().whereStart();
        }
        fields = false;
        where = new StringBuilder();
        current = where;
        return where;
    }

    public StringBuilder _orderStart() {
        if (nonNull(parent)) {
            return $parent()._orderStart();
        }
        orderPart = new StringBuilder();
        current = orderPart;
        return orderPart;
    }


    public Object joinStart(String id, Class cls) {
        joinClass = cls;
        joinField = id;
        $identifier(id);
        return this;
    }


    @Override
    public Object where() {
        if (nonNull(parent)) {
            var p = $parent();
            p.whereStart();
            return p;
        }
        whereStart();
        return this;
    }

    public QuerySelectOperation<S, O, R> join() {
        return _internalFetch("join");
    }

    public QuerySelectOperation<S, O, R> leftJoin() {
        return _internalFetch("left join");
    }


    @Override
    public QuerySelectOperation<S, O, R> join(Function<Object, Queryable> joinQuery) {
        handleJoin(joinQuery, "join");
        return this;
    }

    public QuerySelectOperation<S, O, R> leftJoin(Function<Object, Queryable> joinQuery) {
        handleJoin(joinQuery, "left join");
        return this;
    }

    @SuppressWarnings("unchecked")
    protected void handleJoin(Function<Object, Queryable> joinQuery, String joinOperation) {
        _stripToLastInclude(where, " (");
        if (nonNull(joinQuery)) {
            var query = (QueryOrderer) CodeFactory.create(joinClass);
            if (nonNull(query)) {
                var access = (QueryAccessor) query;
                access.setJoinSupplier(joinSupplier);
                access.setParams(params);
                var q = (QueryAccessor) joinQuery.apply(query);

                if (nonNull(q.getAccessorSelect()) && !q.getAccessorSelect().isEmpty()) {
                    if (Objects.isNull(select)) {
                        select = new StringBuilder();
                        if (DEFAULT_ALIAS.equals(alias)) {
                            select.append(alias).append(", ");
                        }
                    }

                    select.append(q.getAccessorSelect());

                    if (Objects.isNull(group)) {
                        group = new StringBuilder(DEFAULT_ALIAS);
                    }

                    if (nonNull(q.getAccessorOrder())) {
                        if (Objects.isNull(orderPart)) {
                            orderPart = new StringBuilder();
                        }

                        orderPart.append(_buildAggregatedOrder(q.getAccessorOrder(), q.getAccessorSelect()));
                    }

                } else {
                    if (nonNull(q.getAccessorOrder())) {
                        throw new QueryBuilderException("Unable to perform order on unselected column.");
                    }
                }

                if (nonNull(q.getAccessorWhere())) {
                    if (Objects.isNull(where)) {
                        where = new StringBuilder(" ");
                    }

                    if (q.getAccessorWhere().isEmpty()) {
                        _stripLastOperator();
                    } else {
                        where.append(q.getAccessorWhere()).append(' ');
                    }
                }

                if (Objects.isNull(select)) {
                    select = new StringBuilder();
                    if (DEFAULT_ALIAS.equals(alias)) {
                        select.append("distinct ").append(alias).append(",");
                    }
                }

                if (Objects.isNull(join)) {
                    join = new StringBuilder();
                }

                join.append(joinOperation).append(' ').append(alias).append(".").append(joinField).append(' ').append(q.getAccessorAlias()).append(' ');
            } else {
                log.warn("Can't find creator for {}", joinClass.getCanonicalName());
            }
        } else {
            if (Objects.isNull(join)) {
                join = new StringBuilder();
            }

            join.append(joinOperation).append(' ').append(alias).append(".").append(joinField).append(" j").append(joinSupplier.getAsInt()).append(' ');
        }
    }

    @Override
    public QuerySelectOperation<S, O, R> joinFetch(Function<Object, Queryable> joinQuery) {
        handleJoin(joinQuery, "join fetch");
        joinFetch = true;
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> joinFetch() {
        return _internalFetch("join fetch");
    }

    @Override
    public QuerySelectOperation<S, O, R> leftJoinFetch() {
        return _internalFetch("left join fetch");
    }

    protected QuerySelectOperation<S, O, R> _internalFetch(String clause) {
        joinField = lastIdentifier.toString();
        joinFetch = true;
        handleJoin(null, clause);
        if (nonNull(where) && where.length() > 0) {
            _stripLast(where, " ");
            _stripToLast(where, " ");
        }
        if (Objects.isNull(where) || where.length() == 0) {
            skipNext = true;
        }
        return this;
    }

    protected StringBuilder _buildAggregatedOrder(StringBuilder order, StringBuilder select) {
        StringBuilder result = new StringBuilder();
        var o = order.toString().strip().split(", ");
        var s = select.toString().split(",");

        for (var ord : o) {
            var or = ord.split("\\s|,");
            result.append(Arrays.stream(s).filter(sel -> sel.strip().endsWith(or[0] + ")")).findFirst().orElseThrow(() -> new QueryBuilderException("Unable to perform order on unselected column.")));
            if (or.length > 1) {
                result.append(' ').append(or[1]);
            }
            result.append(",");
        }

        return result;
    }

    @Override
    public String getAccessorAlias() {
        return alias;
    }

    @Override
    public StringBuilder getAccessorSelect() {
        return select;
    }

    @Override
    public StringBuilder getAccessorWhere() {
        return where;
    }

    @Override
    public StringBuilder getAccessorOrder() {
        return orderPart;
    }

    @Override
    public List<Object> getParams() {
        return params;
    }

    @Override
    public String getCountQuery() {
        if (Objects.isNull(countQuery)) {
            String old = null;

            if (Objects.isNull(select)) {
                select = new StringBuilder("count(*)");
            } else {
                old = select.toString();
                if (select.toString().equals("distinct " + alias + ",")) {
                    select = new StringBuilder("count(distinct " + alias + ")");
                } else {
                    select = new StringBuilder("count(*)");
                }
            }

            countQuery = new StringBuilder();
            buildQuery(countQuery, true);
            select = nonNull(old) ? new StringBuilder(old) : null;
        }
        return countQuery.toString();
    }

    @Override
    public String getExistsQuery() {
        if (Objects.isNull(existsQuery)) {
            var oldSelect = nonNull(select) ? select.toString() : null;
            try {
                checkReferenceConditions();
            } catch (QueryBuilderException e) {
                //Do nothing
            }
            existsQuery = new StringBuilder();
            buildQuery(existsQuery, true);
            select = nonNull(oldSelect) ? new StringBuilder(oldSelect) : null;
        }
        return existsQuery.toString();
    }

    @Override
    public void setJoinSupplier(IntSupplier supplier) {
        alias = "j" + supplier.getAsInt();
        joinSupplier = supplier;
    }

    @Override
    public void setParams(List<Object> params) {
        this.params = params;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setMocked(UnaryOperator<Object> onValue, UnaryOperator<Object> onParamAdd) {
        mocked = onValue;
        params = new ObservableList(params, onParamAdd);
    }

    protected Object _getQueryName() {
        var result = queryName.get();
        result.setParent(alias, this);
        return result;
    }

    public Object lower() {
        _doLower();
        return _getQueryName();
    }

    public Object not() {
        _doNot();
        return _getQueryName();
    }

    public Object replace(String what, String withWhat) {
        _doReplace(what, withWhat);
        return _getQueryName();
    }

    public Object substring(int start) {
        _doSubstring(start);
        return _getQueryName();
    }

    public Object substring(int start, int len) {
        _doSubstring(start, len);
        return _getQueryName();
    }

    public Object trim() {
        _doTrim();
        return _getQueryName();
    }

    public Object upper() {
        _doUpper();
        return _getQueryName();
    }

    public Object _self() {
        var _par = nonNull(parent) ? $parent() : this;
        var _current = _par.current;
        if (Objects.isNull(parent)) {
            _current.append(alias);
            lastIdentifier = new StringBuilder("self");
        }
        _stripLast(".");
        if (_par.fields) {
            if (Objects.isNull(_par.aggregate)) {
                _current.append(" as ").append(lastIdentifier);
            } else {
                if (!_par.distinct && !_par.isGroup) {
                    _current.append(")");
                } else {
                    if (_par.isGroup) {
                        if (Objects.isNull(_par.group)) {
                            _par.group = new StringBuilder();
                        }
                        _par.group.append(_par.current.substring(_par.lastIdStartPos)).append(",");
                    }
                    _par.distinct = false;
                    _par.isGroup = false;
                }
            }
            _par.fieldsCount++;
        }
        _current.append(",");

        if (nonNull(_par.aggregate)) {
            return _par.aggregate;
        }

        return $retParent();
    }

    public void buildProjection(Class<?> projection) {
        var list = projections.computeIfAbsent(returnClass, c -> new HashMap<>())
                .computeIfAbsent(projection, c -> _calcProjection(projection));

        this.projection = true;
        if (!list.isEmpty()) {
            selectOrAggregate = true;
            fieldsCount = list.size();
            select = new StringBuilder(list.stream().collect(Collectors.joining("," + alias + ".", alias + ".", "")));
        } else {
            log.warn("Projection ({}) did not produce any fields!", projection.getCanonicalName());
        }
    }

    protected List<String> _calcProjection(Class<?> projection) {
        if (!projection.isInterface()) {
            throw new QueryBuilderException("Projection must be interface!");
        }

        var list = _calcProjection(projection, new ArrayList<>());
        list.sort(Comparator.naturalOrder());

        _mapProperties(returnClass, list);

        return list.stream().filter(s -> s.contains(" as ")).collect(Collectors.toList());
    }

    protected List<String> _calcProjection(Class<?> projection, List<String> list) {
        for (var inh : projection.getInterfaces()) {
            _calcProjection(inh, list);
        }

        for (var method : projection.getDeclaredMethods()) {
            if (method.getParameters().length == 0 && !void.class.equals(method.getReturnType()) && isGetter(method) && !list.contains(method.getName())) {
                list.add(method.getName());
            }
        }

        return list;
    }

    protected void _mapProperties(Class<?> cls, List<String> list) {
        for (var inh : cls.getInterfaces()) {
            _mapProperties(inh, list);
        }

        for (var i = 0; i < list.size(); i++) {
            var field = _getFieldName(cls, list.get(i), "");
            if (nonNull(field)) {
                list.set(i, field + " as " + TupleBackedProjection.getFieldName(list.get(i)));
            }
        }
    }

    protected String _getFieldName(Class<?> cls, String methodName, String prefix) {
        if (!methodName.contains(" as ")) {
            var methods = _getMethods(cls);
            var method = methods.get(methodName);
            if (nonNull(method)) {
                return prefix + TupleBackedProjection.getFieldName(methodName);
            } else {
                method = methods.values().stream()
                        .filter(m -> m.getParameters().length == 0 && methodName.startsWith(m.getName()))
                        .findFirst().orElse(null);
                if (nonNull(method)) {
                    var name = methodName.charAt(0) == 'i' ?
                            "is" + methodName.substring(method.getName().length()) :
                            "get" + methodName.substring(method.getName().length());
                    return _getFieldName(method.getReturnType(), name, prefix + TupleBackedProjection.getNativeFieldName(method.getName()) + ".");
                }
            }
        }
        return null;
    }

    protected Map<String, Method> _getMethods(Class<?> cls) {
        var result = new HashMap<String, Method>();
        _getMethods(cls, result);
        return result;
    }

    protected void _getMethods(Class<?> cls, Map<String, Method> map) {
        for (var intf : cls.getInterfaces()) {
            _getMethods(intf, map);
        }

        for (var method : cls.getDeclaredMethods()) {
            map.put(method.getName(), method);
        }
    }


    public void checkReferenceConditions() {
        if (isCustom || isNative) {
            throw new QueryBuilderException("Can't get reference for custom queries!");
        }

        if (nonNull(select)) {
            throw new QueryBuilderException("Can't use combination of select and reference!");
        }

        var entry = CodeFactory.lookupId(returnClass);

        if (Objects.isNull(entry)) {
            throw new QueryBuilderException("Class " + returnClass.getCanonicalName() + " have no declared identifier column!");
        }

        select = new StringBuilder(entry.getName());
        existsClass = entry.getType();
    }

    protected StringBuilder $current() {
        if (nonNull(parent)) {
            return $parent().current;
        }
        return current;
    }

    protected StringBuilder $orderPart() {
        if (nonNull(parent)) {
            return $parent().orderPart;
        }
        return orderPart;
    }


    protected StringBuilder $select() {
        if (nonNull(parent)) {
            return $parent().select;
        }
        return select;
    }

    protected StringBuilder $where() {
        if (nonNull(parent)) {
            return $parent().where;
        }
        return where;
    }

    protected O $order() {
        if (nonNull(parent)) {
            return (O) $parent().order;
        }
        return order;
    }

    protected boolean $fields() {
        if (nonNull(parent)) {
            return $parent().fields;
        }
        return fields;
    }

    protected String $alias() {
        if (nonNull(parent)) {
            return $parent().alias;
        }
        return alias;
    }

    protected A $aggregate() {
        if (nonNull(parent)) {
            return (A) $parent().aggregate;
        }
        return aggregate;
    }

    protected boolean $distinct() {
        if (nonNull(parent)) {
            return $parent().distinct;
        }
        return distinct;
    }

    protected void $fieldsInc() {
        if (nonNull(parent)) {
            $parent().fieldsCount++;
        } else {
            fieldsCount++;
        }
    }

    protected QueryExecutor $parent() {
        var p = parent;

        while (nonNull(p.parent)) {
            p = p.parent;
        }

        return p;
    }

    protected QueryIdentifier $retParent() {

        if (Objects.isNull(parent)) {
            return this;
        }

        var last = this;
        var p = this;
        while (nonNull(p.parent)) {
            p = p.parent;
            if (p instanceof EmbeddedFields) {
                last = p;
            }

        }

        if (nonNull(last.parent) && nonNull(last.parent.aggregate)) {
            return (QueryIdentifier) last.parent.aggregate;
        }

        return last;
    }

    public void setParent(String name, Object executor) {
        this.parent = (QueryExecutor) executor;
        var _current = $current();
        if (_current.isEmpty() || _current.charAt(_current.length() - 1) != '.') {
            _current.append($alias()).append('.');
        }
        _current.append(name).append(".");
        if ($fields()) {
            lastIdentifier = new StringBuilder(name);
        }
    }

    protected Page _checkPage(Page org) {
        if (pagedLoop) {
            return org;
        }

        return new PageImpl(org.getContent(), org.getPageable(), count());
    }

    public void _alias(String alias) {
        var _sel = $select();
        _stripLast(_sel, ",");
        _sel.append(" as ").append(alias).append(",");
    }
}
