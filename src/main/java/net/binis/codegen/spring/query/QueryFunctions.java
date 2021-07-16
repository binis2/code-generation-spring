package net.binis.codegen.spring.query;

import java.util.Collection;
import java.util.List;

public interface QueryFunctions<T, R> {

    QueryFunctions<Long, R> length();
    R equal(T value);
    R between(T from, T to);
    R in(Collection<T> values);
    R isNull();
    R isNotNull();
    R like(String value);
    R starts(String value);
    R ends(String value);
    R contains(String value);
    R greater(T value);
    R greaterEqual(T value);
    R less(T value);
    R lessEqual(T value);
    R script(String script);

}
