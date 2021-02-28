package net.binis.codegen.spring.query.executor;

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.exception.GenericCodeGenException;
import net.binis.codegen.spring.BasePersistenceOperations;
import net.binis.codegen.spring.query.QueryExecute;
import net.binis.codegen.spring.query.QueryOrderOperation;
import net.binis.codegen.spring.query.QuerySelectOperation;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class QueryExecutor<S, O, R> extends BasePersistenceOperations<R> implements QuerySelectOperation<S, O, R>, QueryOrderOperation<O, R>, QueryExecute<R> {

    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private ResultType resultType = ResultType.UNKNOWN;
    private final Class<?> returnClass;
    private Class<?> mapClass;
    private Long first;
    private Long pageSize;
    private boolean isNative;
    protected O order;

    public QueryExecutor(Class<?> returnClass) {
        this.returnClass = returnClass;
        mapClass = returnClass;
        query.append("from ").append(returnClass.getName()).append(" where");
    }

    public void identifier(String id, Object value) {
        params.add(value);
        query.append(" (").append(id).append(" = ?").append(params.size()).append(")");
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
    public QuerySelectOperation<S, O, R> like() {
        var idx = query.lastIndexOf(" = ");
        var oIdx = query.lastIndexOf(" (");
        if (idx > oIdx) {
            query.replace(idx, idx + 3, " like ");
        } else {
            throw new IllegalStateException("Invalid usage of like operand!");
        }
        return this;
    }

    @Override
    public QuerySelectOperation<S, O, R> notLike() {
        var idx = query.lastIndexOf(" = ");
        var oIdx = query.lastIndexOf(" (");
        if (idx > oIdx) {
            query.replace(idx, idx + 3, " like ");
            query.replace(oIdx, oIdx + 2, " not (");
        } else {
            throw new IllegalStateException("Invalid usage of like operand!");
        }
        return this;
    }


    public QuerySelectOperation<S, O, R> not() {
        var idx = query.lastIndexOf(" (");
        query.replace(idx, idx + 2, " not (");
        return this;
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
        resultType = ResultType.SINGLE;
        return (S) this;
    }

    public S all() {
        resultType = ResultType.LIST;
        return (S) this;
    }

    public S count() {
        resultType = ResultType.COUNT;
        query.insert(0, "select count(*) ");
        return (S) this;
    }

    public <QR> QueryExecute<List<QR>> nativeQuery(String query, Class<?> cls) {
        isNative = true;
        mapClass = cls;
        this.query.setLength(0);
        this.query.append(query);
        resultType = ResultType.LIST;
        return (QueryExecute) this;
    }

    public <QR> QueryExecute<List<QR>> query(String query, Class<?> cls) {
        mapClass = cls;
        this.query.setLength(0);
        this.query.append(query);
        resultType = ResultType.LIST;
        return (QueryExecute) this;
    }

    public S top(long records) {
        pageSize = records;
        resultType = ResultType.LIST;
        return (S) this;
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

    @Override
    public R go() {
        stripLast(",");
        stripLast("where");
        System.out.println(query.toString());
        return withRes(manager -> {
            var map = ResultType.COUNT.equals(resultType) ? Long.class : mapClass;
            var q = isNative ? manager.createNativeQuery(query.toString(), map)
                    : manager.createQuery(query.toString(), map);
            for (int i = 0; i < params.size(); i++) {
                q.setParameter(i + 1, params.get(i));
            }

            if (nonNull(first)) {
                q.setFirstResult(first.intValue());
            }

            if (nonNull(pageSize)) {
                q.setMaxResults(pageSize.intValue());
            }

            switch (resultType) {
                case SINGLE:
                    try {
                        return (R) Optional.of(q.getSingleResult());
                    } catch (NoResultException ex) {
                        return (R) Optional.empty();
                    }
                case COUNT:
                    return (R) q.getSingleResult();
                case LIST:
                    return (R) q.getResultList();
                default:
                    throw new GenericCodeGenException("Unknown query return type!");
            }
        });
    }

    private void stripLast(String what) {
        var qlen = query.length();
        var wlen = what.length();
        var idx = query.lastIndexOf(what);
        if (idx > -1 && idx == qlen - wlen) {
            query.setLength(qlen - wlen);
        }
    }

    private enum ResultType {
        UNKNOWN,
        SINGLE,
        LIST,
        COUNT;
    }

}
