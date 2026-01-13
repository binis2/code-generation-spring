package net.binis.codegen.spring.query.executor;

/*-
 * #%L
 * code-generator-spring
 * %%
 * Copyright (C) 2021 - 2026 Binis Belev
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.spring.modifier.BasePersistenceOperations;
import org.springframework.jmx.access.InvalidInvocationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.function.Function;

import static java.util.Objects.nonNull;

public class TupleBackedProjection implements InvocationHandler {

    protected transient final Tuple tuple;
    protected transient final QueryExecutor executor;
    protected static final Method withRes;

    static {
        try {
            withRes = BasePersistenceOperations.class.getDeclaredMethod("withRes", Function.class);
            withRes.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Invalid executor class!");
        }
    }

    public TupleBackedProjection(Tuple tuple, QueryExecutor executor) {
        this.tuple = tuple;
        this.executor = executor;
    }

    static String getNativeFieldName(String name) {
        var n = new StringBuilder(name);
        var res = new StringBuilder();

        var start = 3;
        if (name.charAt(0) == 'i') {
            start = 2;
        }

        res.append(Character.toLowerCase(n.charAt(start)));
        for (var i = start + 1; i < n.length(); i++) {
            var ch = n.charAt(i);
            if (Character.isUpperCase(ch)) {
                res.append('_').append(Character.toLowerCase(ch));
            } else {
                res.append(ch);
            }
        }

        return res.toString();
    }

    static String getFieldName(String name) {
        var start = 3;
        if (name.charAt(0) == 'i') {
            start = 2;
        }

        var result = new StringBuilder(name.substring(start));
        result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
        return result.toString();
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
            var field = getFieldName(method.getName());
            try {
                return convert(tuple.get(field), method.getReturnType());
            } catch (IllegalArgumentException e) {
                field = getNativeFieldName(method.getName());
                try {
                    return convert(tuple.get(field), method.getReturnType());
                } catch (IllegalArgumentException exc) {
                    try {
                        return processSubEntity(tuple.get(field + "_id"), method.getReturnType());
                    } catch (IllegalArgumentException ex) {
                        throw exc;
                    }
                }
            }
        } else if ("toString".equals(method.getName())) {
            return tupleToString();
        }

        throw new InvalidInvocationException("Can't invoke method: " + method.getName());
    }

    protected Object processSubEntity(Object id, Class<?> returnType) {
        if (nonNull(id)) {
            var obj = CodeFactory.lookup(returnType);
            if (nonNull(obj)) {
                try {
                    Function<EntityManager, Object> func = m -> m.find(obj, convert(id, m.getMetamodel().entity(obj).getIdType().getJavaType()));
                    return withRes.invoke(executor, func);
                } catch (Exception e) {
                    return new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            return null;
        }
    }

    protected Object convert(Object val, Class cls) {
        if (nonNull(val)) {
            if (val instanceof BigInteger v) {
                if (int.class.equals(cls) || Integer.class.equals(cls)) {
                    return v.intValue();
                }
                if (long.class.equals(cls) || Long.class.equals(cls)) {
                    return v.longValue();
                }
            }

            if (val instanceof BigDecimal v) {
                if (double.class.equals(cls) || Double.class.equals(cls)) {
                    return v.doubleValue();
                }
                if (float.class.equals(cls) || Float.class.equals(cls)) {
                    return v.floatValue();
                }
            }

            if (val instanceof Timestamp v) {
                if (LocalDateTime.class.equals(cls)) {
                    return v.toLocalDateTime();
                }
                if (LocalDate.class.equals(cls)) {
                    return v.toLocalDateTime().toLocalDate();
                }
                if (LocalTime.class.equals(cls)) {
                    return v.toLocalDateTime().toLocalTime();
                }
                if (OffsetDateTime.class.equals(cls)) {
                    return v.toLocalDateTime().atOffset(ZoneOffset.UTC);
                }
            }
        }
        return val;
    }

    protected String tupleToString() {
        var result = new StringBuilder("(");
        var elements = tuple.getElements();
        for (var i = 0; i < tuple.getElements().size(); i++) {
            var element = elements.get(i);
            if (nonNull(element.getAlias())) {
                result.append("[\"").append(element.getAlias()).append("\"]");
            } else {
                result.append(i);
            }
            result.append("=").append(tuple.get(i)).append("; ");
        }

        if (!elements.isEmpty()) {
            result.setLength(result.length() - 2);
        }

        return result.append(")").toString();
    }

}
