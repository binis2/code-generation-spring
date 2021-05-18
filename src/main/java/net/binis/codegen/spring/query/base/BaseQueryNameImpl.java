package net.binis.codegen.spring.query.base;

import net.binis.codegen.spring.query.QueryScript;
import net.binis.codegen.spring.query.executor.QueryExecutor;

public class BaseQueryNameImpl<T> implements QueryScript<T> {

    protected QueryExecutor executor;

    protected String name;

    public void setParent(String name, Object executor) {
        this.name = name;
        this.executor = (QueryExecutor) executor;
        this.executor.embedded(name);
    }

    @Override
    public T script(String script) {
        return (T) ((QueryScript) executor).script(script);
    }

}
