package net.binis.codegen.spring.query;

public interface QueryStarter<R, Q> {

    Q by();

    QueryParam<R> nativeQuery(String query);

    QueryParam<R> query(String query);

}
