package net.binis.codegen.spring.query;

public interface QueryCollectionFunctions<T, R> {

    QueryFunctions<Long, R> size();
    R contains(T value);
    R notContains(T value);
    R isEmpty();
    R isNotEmpty();

}
