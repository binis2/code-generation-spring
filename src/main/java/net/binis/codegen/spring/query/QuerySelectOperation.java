package net.binis.codegen.spring.query;

public interface QuerySelectOperation<S> {

    S and();
    S like(String what);
    S not();
    S or();

}
