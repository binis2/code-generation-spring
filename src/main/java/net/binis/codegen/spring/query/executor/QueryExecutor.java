package net.binis.codegen.spring.query.executor;

import net.binis.codegen.creator.EntityCreator;
import net.binis.codegen.spring.BasePersistenceOperations;
import net.binis.codegen.spring.query.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import java.util.*;

import static java.util.Objects.nonNull;

public class QueryExecutor<T, S, O, R> extends BasePersistenceOperations<R> implements QuerySelectOperation<S, O, R>, QueryOrderOperation<O, R>, QueryExecute<R>, QueryFunctions<T, QuerySelectOperation<S, O, R>>, QueryParam<R> {

    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private QueryProcessor.ResultType resultType = QueryProcessor.ResultType.UNKNOWN;
    private final Class<?> returnClass;
    private Class<?> mapClass;
    private Pageable pageable;
    private boolean isNative;
    private boolean isModifying;
    protected O order;
    private String enveloped = null;
    private Runnable onEnvelop = null;

    private FlushModeType flushMode;
    private LockModeType lockMode;
    private Map<String, Object> hints;

    public QueryExecutor(Class<?> returnClass) {
        this.returnClass = returnClass;
        mapClass = returnClass;
        query.append("from ").append(returnClass.getName()).append(" u where");
    }

    public void identifier(String id, Object value) {
        if (query.charAt(query.length() - 1) != '.' && Objects.isNull(enveloped)) {
            query.append(" (");
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
    }

    public void identifier(String id) {
        if (query.charAt(query.length() - 1) != '.' && Objects.isNull(enveloped)) {
            query.append(" (");
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

    private void backEnvelop(String func) {
        var idx = query.lastIndexOf("(");
        query.insert(idx, "(" + func).append(")");
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
    }

    public void collection(String id, Object value) {
        params.add(value);
        query.append(" (?").append(params.size()).append(" member of ").append(id).append(")");
    }

    protected void orderIdentifier(String id) {
        query.append(" ").append(id);
    }

    protected void orderStart() {
        stripLast("where");
        query.append(" order by ");
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
    public O order() {
        orderStart();
        return order;
    }

    public S by() {
        resultType = QueryProcessor.ResultType.SINGLE;
        return (S) this;
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

    public <QR> QueryParam<QR> nativeQuery(String query) {
        isNative = true;
        return query(query);
    }

    public <QR> QueryParam<QR> query(String query) {
        this.query.setLength(0);
        this.query.append(query);
        return (QueryParam) this;
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
        return withRes(manager ->
            QueryProcessor.process(manager, query.toString(), params, resultType, returnClass, mapClass, isNative, isModifying, pageable, flushMode, lockMode, hints));
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
        var qlen = query.length();
        var wlen = what.length();
        var idx = query.lastIndexOf(what);
        if (idx > -1 && idx == qlen - wlen) {
            query.setLength(qlen - wlen);
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

}
