package net.binis.codegen.spring.query;

/*-
 * #%L
 * code-generator-spring
 * %%
 * Copyright (C) 2021 Binis Belev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.exception.GenericCodeGenException;
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.spring.query.exception.QueryBuilderException;
import net.binis.codegen.spring.query.executor.Filter;
import net.binis.codegen.spring.query.executor.QueryExecutor;
import net.binis.codegen.spring.query.executor.TupleBackedProjection;
import net.binis.codegen.tools.Reflection;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import javax.persistence.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class QueryProcessor {

    private static Processor processor = defaultProcessor();
    private static final ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

    private static Class<?> sessionClass;
    private static Method enableFilter;
    private static Method disableFilter;
    private static Method parameter;
    private static boolean logParams = false;
    private static boolean logQuery = false;

    private QueryProcessor() {
        //Do nothing
    }

    private static void initFilters() {
        sessionClass = Reflection.loadClass("org.hibernate.Session");
        if (nonNull(sessionClass)) {
            try {
                enableFilter = sessionClass.getDeclaredMethod("enableFilter", String.class);
                disableFilter = sessionClass.getDeclaredMethod("disableFilter", String.class);
                parameter = enableFilter.getReturnType().getDeclaredMethod("setParameter", String.class, Object.class);
            } catch (Exception e) {
                sessionClass = null;
                log.info("org.hibernate.Session is not present!. Filtering disabled!");
            }
        }
    }

    public static void logParams() {
        logParams = true;
    }

    public static void logQuery() {
        logQuery = true;
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
        return (QueryExecutor executor, EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, List<Filter> filters) -> {
            log.info(query);
            return p.process(executor, manager, query, params, resultType, returnClass, mapClass, isNative, modifying, pageable, flush, lock, hints, filters);
        };
    }

    public static Processor getProcessor() {
        return processor;
    }

    public static void setProcessor(Processor processor) {
        QueryProcessor.processor = processor;
    }

    @SuppressWarnings("unchecked")
    public static <R> R process(QueryExecutor executor, EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, List<Filter> filters) {
        return (R) processor.process(executor, manager, query, params, resultType, returnClass, mapClass, isNative, modifying, pageable, flush, lock, hints, filters);
    }

    @SuppressWarnings("unchecked")
    private static Object defaultProcess(QueryExecutor executor, EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, List<Filter> filters) {
        var time = logQuery ? System.currentTimeMillis() : 0;
        try {
            var original = returnClass;

            if (isNull(manager)) {
                throw new GenericCodeGenException("No entity manager present!\nUse '@ExtendWith(CodeGenExtension.class)' if you are in running unit test!");
            }

            if (BeanUtils.isSimpleValueType(mapClass)) {
                returnClass = mapClass;
            }

            var map = ResultType.TUPLE.equals(resultType) || ResultType.TUPLES.equals(resultType) || void.class.equals(mapClass) || Void.class.equals(mapClass) ? Tuple.class : returnClass;

            Query q;
            if (ResultType.REMOVE.equals(resultType) || ResultType.EXECUTE.equals(resultType)) {
                q = isNative ? manager.createNativeQuery(query)
                        : manager.createQuery(query);
            } else {
                q = isNative ? manager.createNativeQuery(query, nativeQueryClass(map))
                        : manager.createQuery(query, map);
            }
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

            if (ResultType.EXISTS.equals(resultType)) {
                q.setMaxResults(1);
            } else
            if (nonNull(pageable) && !ResultType.COUNT.equals(resultType)) {
                q.setFirstResult((int) pageable.getOffset());
                if (pageable.getPageSize() > -1) {
                    q.setMaxResults(pageable.getPageSize());
                }
            }

            if (nonNull(sessionClass) && nonNull(filters)) {
                var session = manager.unwrap(sessionClass);
                for (var filter : filters) {
                    try {
                        if (filter.isDisabled()) {
                            disableFilter.invoke(session, filter.getName());
                        } else {
                            var f = enableFilter.invoke(session, filter.getName());
                            for (var param : filter.getValues().entrySet()) {
                                parameter.invoke(f, param.getKey(), param.getValue());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Unable to set query filter ({})!", filter.getName(), e);
                    }
                }
            }

            switch (resultType) {
                case SINGLE:
                    try {
                        var result = q.getSingleResult();
                        if (void.class.equals(mapClass)) {
                            return Optional.ofNullable(((Tuple) result).get(0));
                        }

                        if (nonNull(result) && mapClass.equals(result.getClass())) {
                            return Optional.of(result);
                        }

                        return Optional.ofNullable(map(mapClass, result));
                    } catch (NoResultException ex) {
                        return Optional.empty();
                    }
                case EXISTS:
                    try {
                        return nonNull(q.getSingleResult());
                    } catch (NoResultException ex) {
                        return false;
                    }
                case COUNT:
                    try {
                        return q.getSingleResult();
                    } catch (NoResultException ex) {
                        return 0;
                    }
                case LIST:
                    var rslt = q.getResultList();

                    if (rslt.isEmpty()) {
                        return rslt;
                    }

                    if (void.class.equals(mapClass)) {
                        return rslt.stream().map(r -> ((Tuple) r).get(0)).collect(Collectors.toList());
                    }

                    if (mapClass.equals(rslt.getClass())) {
                        return rslt;
                    }

                    if (Tuple.class.equals(returnClass)) {
                        return rslt.stream().map(r -> createProxy((Tuple) r, mapClass, executor)).collect(Collectors.toList());
                    } else if (mapClass.isInterface()) {
                        return rslt.stream().map(r -> map(mapClass, r)).collect(Collectors.toList());
                    } else {
                        return rslt;
                    }
                case PAGE:
                    if (Tuple.class.equals(returnClass)) {
                        if (!Tuple.class.equals(mapClass) && mapClass.isInterface()) {
                            return new PageImpl((List) q.getResultList().stream().map(r -> createProxy((Tuple) r, mapClass, executor)).collect(Collectors.toList()), pageable, Integer.MAX_VALUE);
                        } else {
                            return new PageImpl(q.getResultList(), pageable, Integer.MAX_VALUE);
                        }
                    } else {
                        if (mapClass.isInterface() && !returnClass.isAssignableFrom(mapClass)) {
                            return new PageImpl((List) q.getResultList().stream().map(r -> map(mapClass, r)).collect(Collectors.toList()), pageable, Integer.MAX_VALUE);
                        } else {
                            return new PageImpl(q.getResultList(), pageable, Integer.MAX_VALUE);
                        }
                    }
                case REMOVE:
                case EXECUTE:
                    return q.executeUpdate();
                case TUPLE:
                    try {
                        var result = q.getSingleResult();

                        if (isNull(result)) {
                            return Optional.empty();
                        }

                        if (!Tuple.class.equals(mapClass) && mapClass.isInterface()) {
                            return Optional.of(createProxy((Tuple) result, mapClass, executor));
                        } else {
                            return Optional.of(result);
                        }
                    } catch (NoResultException ex) {
                        return Optional.empty();
                    }
                case TUPLES:
                    if (!Tuple.class.equals(mapClass) && mapClass.isInterface()) {
                        return q.getResultList().stream().map(r -> createProxy((Tuple) r, mapClass, executor)).collect(Collectors.toList());
                    } else {
                        return q.getResultList();
                    }

                case REFERENCE:
                    var impl = CodeFactory.lookup(original);
                    if (Objects.isNull(impl)) {
                        throw new QueryBuilderException("Can't find implementation class for " + original.getCanonicalName() + "!");
                    }

                    try {
                        var result = q.getSingleResult();
                        return Optional.of(manager.getReference(impl, result));
                    } catch (NoResultException ex) {
                        return Optional.empty();
                    }
                case REFERENCES:
                    var imp = CodeFactory.lookup(original);
                    if (Objects.isNull(imp)) {
                        throw new QueryBuilderException("Can't find implementation class for " + original.getCanonicalName() + "!");
                    }
                    return q.getResultList().stream().map(r -> manager.getReference(imp, r)).collect(Collectors.toList());
                default:
                    throw new GenericCodeGenException("Unknown query return type!");
            }
        } catch (Exception e) {
            log.error("Failed to execute query\n{}", logQuery(query, params, time), e);
            throw e;
        } finally {
            if (logQuery) {
                log.info(logQuery(query, params, time));
            }
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

    private static Object nullProcess(QueryExecutor executor, EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, List<Filter> filters) {
        return null;
    }

    private static Object createProxy(Tuple tuple, Class<?> mapClass, QueryExecutor<?, ?, ?, ?, ?, ?, ?> executor) {
        var elements = tuple.getElements();
        if (elements.size() == 1 && nonNull(tuple.get(0)) && mapClass.isInstance(tuple.get(0))) {
            return tuple.get(0);
        }

        return Proxy.newProxyInstance(
                mapClass.getClassLoader(),
                new Class[]{mapClass},
                new TupleBackedProjection(tuple, executor));
    }

    private static String logQuery(String query, List<Object> params, long time) {
        var elapsed = time == 0 ? 0 : System.currentTimeMillis() - time;
        if (logParams && nonNull(params)) {
            return "Query '" + query + "' with params [" + params.stream().map(Objects::toString).map(s -> "(" + s + ")").collect(Collectors.joining(", ")) + "]" + (time != 0 ? " (" + elapsed + "ms.)" : "");
        } else {
            return "Query '" + query + "'"  + (time != 0 ? " (" + elapsed + "ms.)" : "");
        }
    }

    public enum ResultType {
        UNKNOWN,
        SINGLE,
        LIST,
        PAGE,
        COUNT,
        EXISTS,
        REMOVE,
        EXECUTE,
        TUPLE,
        TUPLES,
        REFERENCE,
        REFERENCES
    }

    @FunctionalInterface
    public interface Processor {
        Object process(QueryExecutor executor, EntityManager manager, String query, List<Object> params, ResultType resultType, Class<?> returnClass, Class<?> mapClass, boolean isNative, boolean modifying, Pageable pageable, FlushModeType flush, LockModeType lock, Map<String, Object> hints, List<Filter> filters);
    }

}
