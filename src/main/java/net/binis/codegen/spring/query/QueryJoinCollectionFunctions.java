package net.binis.codegen.spring.query;

import java.util.function.Function;

public interface QueryJoinCollectionFunctions<T, R, J> extends QueryCollectionFunctions<T, R> {

    R join(Function<J, Queryable> join);
    R joinFetch(Function<J, Queryable> join);

}
