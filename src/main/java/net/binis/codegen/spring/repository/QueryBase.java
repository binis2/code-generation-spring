package net.binis.codegen.spring.repository;

public interface QueryBase<T> {

    T by();
    T all();
    T count();

}
