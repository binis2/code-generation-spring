package net.binis.codegen.spring.query;

public interface QueryScript<R> {

    R script(String script);

}
