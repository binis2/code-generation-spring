package net.binis.codegen.spring.query.executor;

import net.binis.codegen.creator.EntityCreator;
import net.binis.codegen.spring.BasePersistenceOperations;
import net.binis.codegen.spring.query.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.nonNull;

public abstract class QueryExecutor<T, S, O, R, A> extends BasePersistenceOperations<R> implements QuerySelectOperation<S, O, R>, QueryOrderOperation<O, R>, QueryFilter<R>, QueryFunctions<T, QuerySelectOperation<S, O, R>>, QueryCollectionFunctions<T, QuerySelectOperation<S, O, R>>, QueryParam<R>, QueryStarter<R, S, A>, QueryCondition<S, O, R>, QueryAggregateOperation {

    private final StringBuilder query = new StringBuilder();
    private StringBuilder select;
    private int fieldsCount = 0;
    private final List<Object> params = new ArrayList<>();
    private QueryProcessor.ResultType resultType = QueryProcessor.ResultType.UNKNOWN;
    private Class<?> returnClass;
    private Class<?> mapClass;
    private Pageable pageable;
    private boolean isNative;
    private boolean isModifying;
    private O order;
    private A aggregate;
    private String enveloped = null;
    private Runnable onEnvelop = null;
    private boolean brackets;
    private boolean condition;

    private FlushModeType flushMode;
    private LockModeType lockMode;
    private Map<String, Object> hints;
    private List<Filter> filters;
    private Filter filter;

    public QueryExecutor(Class<?> returnClass) {
        this.returnClass = returnClass;
        mapClass = returnClass;
        query.append("from ").append(returnClass.getName()).append(" u where");
    }

    public QueryExecutor<T, S, O, R, A> identifier(String id, Object value) {
        if (query.charAt(query.length() - 1) != '.' && Objects.isNull(enveloped)) {
            query.append(" (");
            brackets = true;
        }
        if (Objects.isNull(value)) {
            query.append(id).append(" is null)");
        } else {
            params.add(value);
            query.append(id);
            if (nonNull(enveloped)) {
                if (nonNull(onEnvelop)) {
                    onEnvelop.run();
                    onEnvelop = null;
                } else {
                    query.append(enveloped);
                }
                enveloped = null;
            }
            query.append(" = ?").append(params.size()).append(")");
        }

        return this;
    }

    public QueryExecutor<T, S, O, R, A> identifier(String id) {
        if (query.charAt(query.length() - 1) != '.' && Objects.isNull(enveloped)) {
            query.append(" (");
            brackets = true;
        }
        query.append(id);
        if (nonNull(enveloped)) {
            if (nonNull(onEnvelop)) {
                onEnvelop.run();
                onEnvelop = null;
            } else {
                query.append(enveloped);
            }
            enveloped = null;
        }

        return this;
    }

    public void embedded(String id) {
        if (query.charAt(query.length() - 1) == '.') {
            query.append(id).append(".");
        } else {
            if (Objects.isNull(enveloped)) {
                query.append(" (");
            }
            query.append(id).append(".");
        }
    }

    protected void doNot() {
        query.append(" not ");
    }

    protected void doLower() {
        envelop("lower");
    }

    protected void doUpper() {
        envelop("upper");
    }

    protected void doTrim() {
        envelop("trim");
    }

    protected void doSubstring(int start) {
        envelop("substr", start);
    }

    public void doSubstring(int start, int len) {
        envelop("substr", start, len);
    }

    protected void doReplace(String what, String withWhat) {
        envelop("replace", what, withWhat);
    }

    private void backInsert(String func) {
        var idx = query.lastIndexOf("(");
        query.insert(idx + 1, func);
    }

    private void backEnvelop(String func) {
        backInsert(func + "(");
        query.append(")");
    }

    private void envelop(String func) {
        enveloped = ")";
        query.append(" (").append(func).append("(");
    }

    private void envelop(String func, Object... params) {
        if (params.length == 0) {
            envelop(func);
        } else {
            var s = new StringBuilder();
            for (Object param : params) {
                this.params.add(param);
                s.append(", ?").append(this.params.size());
            }
            enveloped = s.append(")").toString();

            query.append(" (").append(func).append("(");
        }
    }

    private void envelop(String func, Runnable onEnvelop, Object... params) {
        this.onEnvelop = onEnvelop;
        envelop(func, params);
    }

    public void operation(String op, Object value) {
        params.add(value);
        if (nonNull(enveloped)) {
            if (nonNull(onEnvelop)) {
                onEnvelop.run();
                onEnvelop = null;
            } else {
                query.append(enveloped);
            }
            enveloped = null;
        }
        query.append(" ").append(op).append(" ?").append(params.size()).append(")");
        brackets = false;
    }

    protected QueryExecutor<T, S, O, R, A> orderIdentifier(String id) {
        query.append(" ").append(id);
        return this;
    }

    protected Object aggregateIdentifier(String id) {
        if (fieldsCount == 0) {
            resultType = QueryProcessor.ResultType.SINGLE;
        } else {
            resultType = QueryProcessor.ResultType.TUPLE;
        }

        fieldsCount++;
        select.append(id).append("),");
        return aggregate;
    }

    protected O orderStart(O order) {
        this.order = order;
        stripLast("where");
        query.append(" order by ");
        return order;
    }

    protected A aggregateStart(A aggregate) {
        this.aggregate = aggregate;
        select = new StringBuilder();
        return aggregate;
    }

    public QuerySelectOperation<S, O, R> script(String script) {
        query.append(" ").append(script);

        if (brackets) {
            query.append(")");
            brackets = false;
        }

        query.append(" ");
        return this;
    }

    @Override
    public S and() {
        query.append(" and ");
        return (S) this;
    }

    @Override
    public S or() {
        query.append(" or ");
        return (S) this;
    }

    @Override
    public QueryCondition<S, O, R> _if(boolean condition, Consumer<QuerySelectOperation<S, O, R>> query) {
        this.condition = condition;
        if (condition) {
            query.accept(this);
        }
        return this;
    }

    public S by() {
        resultType = QueryProcessor.ResultType.SINGLE;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Q> Q by(boolean condition, Function<S, Q> query) {
        if (condition) {
            query.apply((S) this);
        }
        return (Q) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Q> Q by(boolean condition, Function<S, Q> query, Function<S, Q> elseQuery) {
        if (condition) {
            query.apply((S) this);
        } else {
            elseQuery.apply((S) this);
        }
        return (Q) this;
    }

    public long count() {
        resultType = QueryProcessor.ResultType.COUNT;
        query.insert(0, "select count(*) ");
        return (long) execute();
    }

    @Override
    public Optional<R> top() {
        resultType = QueryProcessor.ResultType.SINGLE;
        pageable = PageRequest.of(0, 1);
        return (Optional) execute();
    }

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
        this.query.setLength(0);
        this.query.append(query);
        return this;
    }

    @Override
    public void transaction(Consumer<QueryStarter<R, S, A>> consumer) {
        with(manager -> consumer.accept(this));
    }

    public List<R> top(long records) {
        pageable = PageRequest.of(0, (int) records);
        resultType = QueryProcessor.ResultType.LIST;
        return (List) execute();
    }

    @Override
    public <V> List<V> top(long records, Class<V> cls) {
        mapClass = cls;
        return (List) top(records);
    }

    @Override
    public Page<R> page(Pageable pageable) {
        resultType = QueryProcessor.ResultType.PAGE;
        this.pageable = pageable;
        return (Page) execute();
    }

    @Override
    public <V> Page<V> page(Pageable pageable, Class<V> cls) {
        mapClass = cls;
        return (Page) page(pageable);
    }

    @Override
    public Page<R> page(long pageSize) {
        return page(PageRequest.of(0, (int) pageSize));
    }

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
        var page = page(pageable);
        while(!page.isEmpty()) {
            page.getContent().forEach(consumer);
            if (page.getContent().size() < pageable.getPageSize()) {
                break;
            }
            page = page(page.nextPageable());
        }
    }

    @Override
    public Optional<Tuple> tuple() {
        resultType = QueryProcessor.ResultType.TUPLE;
        return (Optional) execute();
    }

    @Override
    public List<Tuple> tuples() {
        resultType = QueryProcessor.ResultType.TUPLES;
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
        return null;
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
    public boolean exists() {
        return count() > 0;
    }

    @Override
    public void delete() {
        remove();
    }

    @Override
    public int remove() {
        isModifying = true;
        resultType = QueryProcessor.ResultType.REMOVE;
        query.insert(0, "delete ");
        return (int) execute();
    }

    @Override
    public O desc() {
        QueryExecutor.this.query.append(" desc,");
        return order;
    }

    @Override
    public O asc() {
        QueryExecutor.this.query.append(" asc,");
        return order;
    }

    public Object execute() {
        stripLast(",");
        stripLast("where");
        if (nonNull(select)) {
            stripLast(select, ",");
            select.insert(0, "select ").append(" ");
            query.insert(0, select);
        }
        return withRes(manager ->
            QueryProcessor.process(manager, query.toString(), params, resultType, returnClass, mapClass, isNative, isModifying, pageable, flushMode, lockMode, hints, filters));
    }

    @Override
    public <V> List<V> list(Class<V> cls) {
        mapClass = cls;
        return (List) list();
    }

    @Override
    public R ensure() {
        return get().orElseGet(() -> (R) EntityCreator.create(returnClass));
    }

    @Override
    public Optional<R> get() {
        return (Optional) execute();
    }

    @Override
    public <V> Optional<V> get(Class<V> cls) {
        mapClass = cls;
        return (Optional<V>) get();
    }

    @Override
    public List<R> list() {
        resultType = QueryProcessor.ResultType.LIST;
        return (List) execute();
    }

    private void stripLast(String what) {
        stripLast(query, what);
    }

    private void stripLast(StringBuilder builder, String what) {
        var qlen = builder.length();
        var wlen = what.length();
        var idx = builder.lastIndexOf(what);
        if (idx > -1 && idx == qlen - wlen) {
            builder.setLength(qlen - wlen);
        }
    }


    @Override
    public QueryFunctions<Long, QuerySelectOperation<S, O, R>> length() {
        backEnvelop("length");
        return (QueryFunctions) this;
    }

    @Override
    public QuerySelectOperation<S, O, R> equal(T value) {
        stripLast(".");
        operation("=", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> between(T from, T to) {
        stripLast(".");
        operation("between", from);
        operation("and", to);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> in(Collection<T> values) {
        stripLast(".");
        operation("in", values);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> isNull() {
        stripLast(".");
        query.append(" is null)");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> isNotNull() {
        stripLast(".");
        query.append(" is not null)");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> like(String value) {
        stripLast(".");
        operation("like", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> starts(String value) {
        stripLast(".");
        operation("like", value + "%");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> ends(String value) {
        stripLast(".");
        operation("like", "%" + value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> contains(String value) {
        stripLast(".");
        operation("like", "%" + value + "%");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> greater(T value) {
        stripLast(".");
        operation(">", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> greaterEqual(T value) {
        stripLast(".");
        operation(">=", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> less(T value) {
        stripLast(".");
        operation("<", value);
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> lessEqual(T value) {
        stripLast(".");
        operation("<=", value);
        return this;
    }

    @Override
    public QueryParam<R> params(Collection<Object> params) {
        this.params.addAll(params);
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
        return aggregate;
    }

    @Override
    public Object cnt() {
        aggregateFunction("count");
        if (fieldsCount == 0) {
            returnClass = Long.class;
        }
        return aggregate;
    }

    public void aggregateFunction(String sum) {
        select.append(sum).append("(");
        if (fieldsCount == 0) {
            returnClass = Double.class;
            mapClass = Double.class;
        }
    }

    @Override
    public QueryFunctions<Long, QuerySelectOperation<S, O, R>> size() {
        backEnvelop("size");
        return (QueryFunctions) this;
    }

    public void collection(String id, Object value) {
        throw new IllegalCallerException();
    }


    @Override
    public QuerySelectOperation<S, O, R> contains(T value) {
        params.add(value);
        backInsert("?" + params.size() + " member of ");
        query.append(")");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> notContains(T value) {
        params.add(value);
        backInsert("?" + params.size() + " not member of ");
        query.append(")");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> isEmpty() {
        query.append(" is empty)");
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> isNotEmpty() {
        query.append(" is not empty)");
        return this;
    }
}
