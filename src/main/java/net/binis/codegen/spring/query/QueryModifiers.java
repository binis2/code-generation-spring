package net.binis.codegen.spring.query;

public interface QueryModifiers<R> {

    R not();
    R lower();
    R upper();
    R substring(int start);
    R substring(int start, int len);
    R length();
    R replace(String what, String withWhat);
    R trim();

}
