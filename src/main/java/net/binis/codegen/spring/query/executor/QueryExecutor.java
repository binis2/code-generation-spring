package net.binis.codegen.spring.query.executor;

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.exception.GenericCodeGenException;
import net.binis.codegen.spring.BasePersistenceOperations;
import net.binis.codegen.spring.query.QueryExecute;
import net.binis.codegen.spring.query.QueryOrderOperation;
import net.binis.codegen.spring.query.QuerySelectOperation;

import java.util.ArrayList;
import java.util.List;

public class QueryExecutor<S, O, R> extends BasePersistenceOperations<R> implements QuerySelectOperation<S, O, R>, QueryOrderOperation<O, R>, QueryExecute<R> {

    private final StringBuilder query = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private ResultType resultType = ResultType.UNKNOWN;
    private final Class<?> returnClass;
    protected O order;

    public QueryExecutor(Class<?> returnClass) {
        this.returnClass = returnClass;
        query.append("from ").append(returnClass.getName()).append(" where");
    }

    protected void identifier(String id, Object value) {
        params.add(value);
        query.append(" ").append(id).append(" = ?,");
    }

    protected void orderIdentifier(String id) {
        query.append(" ").append(id);
    }

    protected void orderStart() {
        stripLast(",");
        stripLast("where");
        query.append(" order by ");
    }

    @Override
    public S and() {
        query.append(" and ");
        return (S) this;
    }

    @Override
    public S like(String what) {
        query.append(" like '").append(what).append("' ");
        return (S) this;
    }

    public S not() {
        query.append(" not ");
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
        resultType = ResultType.SINGLE;
        return (S) this;
    }

    public S all() {
        resultType = ResultType.LIST;
        return (S) this;
    }

    public S count() {
        resultType = ResultType.COUNT;
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
        System.out.println(query.toString());
        return withRes(manager -> {
            var q = manager.createQuery(query.toString(), returnClass);
            for (int i = 0; i < params.size(); i++) {
                q.setParameter(i, params.get(i));
            }
            switch (resultType) {
                case SINGLE:
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
