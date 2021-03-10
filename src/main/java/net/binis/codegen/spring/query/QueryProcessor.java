package net.binis.codegen.spring.query;

import net.binis.codegen.exception.GenericCodeGenException;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class QueryProcessor {

    private static Processor processor = defaultProcessor();
    private static final ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

    private QueryProcessor() {
        //Do nothing
    }

    public static Processor defaultProcessor() {
        return QueryProcessor::defaultProcess;
    }

    public static Processor nullProcessor() {
        return QueryProcessor::nullProcess;
    }

    public static void setProcessor(Processor processor) {
        QueryProcessor.processor = processor;
    }

    public static <R> R process(EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, Long first, Long pageSize) {
        return (R) processor.process(manager, query, params, resultType, returnClass, mapClass, isNative, first, pageSize);
    }

    private static Object defaultProcess(EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, Long first, Long pageSize) {
        var map = ResultType.COUNT.equals(resultType) ? Long.class : returnClass;
        var q = isNative ? manager.createNativeQuery(query, map)
                : manager.createQuery(query, map);
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }

        if (nonNull(first)) {
            q.setFirstResult(first.intValue());
        }

        if (nonNull(pageSize)) {
            q.setMaxResults(pageSize.intValue());
        }

        switch (resultType) {
            case SINGLE:
                try {
                    return Optional.of(map(mapClass, q.getSingleResult()));
                } catch (NoResultException ex) {
                    return Optional.empty();
                }
            case COUNT:
                return q.getSingleResult();
            case LIST:
                if (nonNull(mapClass) && mapClass.isInterface()) {
                    return q.getResultList().stream().map(r -> map(mapClass, r)).collect(Collectors.toList());
                } else {
                    return q.getResultList();
                }
            default:
                throw new GenericCodeGenException("Unknown query return type!");
        }
    }

    private static Object map(Class<?> mapClass, Object result) {
        if (nonNull(result) && mapClass.isInterface()) {
            return factory.createProjection(mapClass, result);
        }
        return result;
    }

    private static Object nullProcess(EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, Long first, Long pageSize) {
        return null;
    }

    public enum ResultType {
        UNKNOWN,
        SINGLE,
        LIST,
        COUNT,
        REMOVE;
    }

    @FunctionalInterface
    public interface Processor {
        Object process(EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, Long first, Long pageSize);
    }

}
