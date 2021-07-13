package net.binis.codegen.spring.query;

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.exception.GenericCodeGenException;
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.tools.Reflection;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Slf4j
public class QueryProcessor {

    private static Processor processor = defaultProcessor();
    private static final ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

    private static Class<?> sessionClass;
    private static Method enableFilter;
    private static Method parameter;

    private QueryProcessor() {
        //Do nothing
    }

    private static void initFilters() {
        sessionClass = Reflection.loadClass("org.hibernate.Session");
        if (nonNull(sessionClass)) {
            try {
                enableFilter = sessionClass.getDeclaredMethod("enableFilter", String.class);
                parameter = enableFilter.getReturnType().getDeclaredMethod("setParameter", String.class, Object.class);
            } catch (Exception e) {
                sessionClass = null;
                log.info("org.hibernate.Session is not present!. Filtering disabled!");
            }
        }
    }

    public static Processor defaultProcessor() {
        initFilters();
        return QueryProcessor::defaultProcess;
    }

    public static Processor nullProcessor() {
        return QueryProcessor::nullProcess;
    }

    public static Processor logProcessor() {
        var p = processor;
        return (EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, Map<String, Map<String, Object>> filters) -> {
            log.info(query);
            return p.process(manager, query, params, resultType, returnClass, mapClass, isNative, modifying, pageable, flush, lock, hints, filters);
        };
    }

    public static Processor getProcessor() {
        return processor;
    }

    public static void setProcessor(Processor processor) {
        QueryProcessor.processor = processor;
    }

    @SuppressWarnings("unchecked")
    public static <R> R process(EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, Map<String, Map<String, Object>> filters) {
        return (R) processor.process(manager, query, params, resultType, returnClass, mapClass, isNative, modifying, pageable, flush, lock, hints, filters);
    }

    private static Object defaultProcess(EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, Map<String, Map<String, Object>> filters) {
        var map = ResultType.COUNT.equals(resultType) ? Long.class : returnClass;
    var q = isNative ? manager.createNativeQuery(query, nativeQueryClass(map))
                : manager.createQuery(query, map);
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }

        if (nonNull(flush)) {
            q.setFlushMode(flush);
        }

        if (nonNull(lock)) {
            q.setLockMode(lock);
        }

        if (nonNull(hints)) {
            hints.forEach(q::setHint);
        }

        if (nonNull(pageable)) {
            q.setFirstResult((int) pageable.getOffset());
            if (pageable.getPageSize() > -1) {
                q.setMaxResults(pageable.getPageSize());
            }
        }

        if (nonNull(sessionClass) && nonNull(filters)) {
            var session = manager.unwrap(sessionClass);
            for (var filter : filters.entrySet()) {
                try {
                    var f = enableFilter.invoke(session, filter.getKey());
                    for (var param : filter.getValue().entrySet()) {
                        parameter.invoke(f, param.getKey(), param.getValue());
                    }
                } catch (Exception e) {
                    log.error("Unable to set query filter ({})!", filter.getKey(), e);
                }
            }
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
            case PAGE:
                if (nonNull(mapClass) && mapClass.isInterface() && !returnClass.isAssignableFrom(mapClass)) {
                    return new PageImpl((List) q.getResultList().stream().map(r -> map(mapClass, r)).collect(Collectors.toList()), pageable, Integer.MAX_VALUE);
                } else {
                    return new PageImpl(q.getResultList(), pageable, Integer.MAX_VALUE);
                }
            case REMOVE:
            case EXECUTE:
                return q.executeUpdate();
            default:
                throw new GenericCodeGenException("Unknown query return type!");
        }
    }

    private static Class<?> nativeQueryClass(Class<?> map) {
        var result = map;
        if (map.isInterface()) {
            var impl = CodeFactory.lookup(map);
            if (nonNull(impl)) {
                result = impl;
            }
        }
        //TODO: Handle unregistered classes.

        return result;
    }

    private static Object map(Class<?> mapClass, Object result) {
        if (nonNull(result) && mapClass.isInterface()) {
            return factory.createProjection(mapClass, result);
        }
        return result;
    }

    private static Object nullProcess(EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, Map<String, Map<String, Object>> filters) {
        return null;
    }

    public enum ResultType {
        UNKNOWN,
        SINGLE,
        LIST,
        PAGE,
        COUNT,
        REMOVE,
        EXECUTE
    }

    @FunctionalInterface
    public interface Processor {
        Object process(EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, Map<String, Map<String, Object>> filters);
    }

}
