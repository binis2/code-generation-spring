package net.binis.codegen.spring.query.executor;

import net.binis.codegen.spring.BasePersistenceOperations;
import net.binis.codegen.spring.query.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class QueryExecutor<T, S, O, R> extends BasePersistenceOperations<R> implements QuerySelectOperation<S, O, R>, QueryOrderOperation<O, R>, QueryExecute<R>, QueryFunctions<T, QuerySelectOperation<S, O, R>> {

    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private QueryProcessor.ResultType resultType = QueryProcessor.ResultType.UNKNOWN;
    private final Class<?> returnClass;
    private Class<?> mapClass;
    private Long first;
    private Long pageSize;
    private boolean isNative;
    protected O order;

    public QueryExecutor(Class<?> returnClass) {
        this.returnClass = returnClass;
        mapClass = returnClass;
        query.append("from ").append(returnClass.getName()).append(" u where");
    }

    public void identifier(String id, Object value) {
        if (query.charAt(query.length() - 1) != '.') {
            query.append(" (");
        }
        if (Objects.isNull(value)) {
            query.append(id).append(" is null)");
        } else {
            params.add(value);
            query.append(id).append(" = ?").append(params.size()).append(")");
        }
    }

    public void identifier(String id) {
        if (query.charAt(query.length() - 1) != '.') {
            query.append(" (");
        }
        query.append(id);
    }

    public void embedded(String id) {
        if (query.charAt(query.length() - 1) == '.') {
            query.append(id).append(".");
        } else {
            query.append(" (").append(id).append(".");
        }
    }

    public void doNot() {
        query.append(" not ");
    }

    public void operation(String op, Object value) {
        params.add(value);
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
        pageSize = 1L;
        return (Optional) execute();
    }

    @Override
    public <V> Optional<V> top(Class<V> cls) {
        mapClass = cls;
        return (Optional) top();
    }

    public <QR> QueryExecute<QR> nativeQuery(String query) {
        isNative = true;
        return query(query);
    }

    public <QR> QueryExecute<QR> query(String query) {
        this.query.setLength(0);
        this.query.append(query);
        return (QueryExecute) this;
    }

    public List<R> top(long records) {
        pageSize = records;
        resultType = QueryProcessor.ResultType.LIST;
        return (List) execute();
    }

    @Override
    public <V> List<V> top(long records, Class<V> cls) {
        mapClass = cls;
        return (List) top(records);
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
//        ProjectionFactory pf = new SpelAwareProxyProjectionFactory();
//        ResourceProjection rp = pf.createProjection(ResourceProjection.class, resourceEntity)
        stripLast(",");
        stripLast("where");
        return withRes(manager ->
            QueryProcessor.process(manager, query.toString(), params, resultType, returnClass, mapClass, isNative, first, pageSize));
    }

    @Override
    public <V> List<V> list(Class<V> cls) {
        mapClass = cls;
        return (List) list();
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
    public QuerySelectOperation<S, O, R> in(List<T> values) {
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

}
