package net.binis.codegen.spring.query;

public interface QueryModifiers<R> {

    R not();
    R lower();
    R upper();

}
