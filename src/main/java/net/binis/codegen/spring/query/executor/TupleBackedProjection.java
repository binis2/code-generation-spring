package net.binis.codegen.spring.query.executor;

import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.spring.BasePersistenceOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.access.InvalidInvocationException;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.function.Function;

import static java.util.Objects.nonNull;

public class TupleBackedProjection implements InvocationHandler {

    private final Tuple tuple;
    private final QueryExecutor executor;
    private static Method withRes;

    {
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

    private static String getFieldName(String name) {
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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
            var field = getFieldName(method.getName());
            try {
                return convert(tuple.get(field), method.getReturnType());
            } catch (IllegalArgumentException e) {
                try {
                    return processSubEntity(tuple.get(field + "_id"), method.getReturnType());
                } catch (IllegalArgumentException ex) {
                    throw e;
                }

            }
        }

        throw new InvalidInvocationException("Can't invoke method: " + method.getName());
    }

    private Object processSubEntity(Object id, Class<?> returnType) {
        var obj = CodeFactory.lookup(returnType);
        if (nonNull(obj)) {
            try {
                Function<EntityManager, Object> func = (m) -> m.find(obj, convert(id, m.getMetamodel().entity(obj).getIdType().getJavaType()));
                return withRes.invoke(executor,  func);
            } catch (Exception e) {
                return new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private Object convert(Object val, Class cls) {
        if (val instanceof BigInteger) {
            if (int.class.equals(cls) || Integer.class.equals(cls)) {
                return ((BigInteger) val).intValue();
            }
            if (long.class.equals(cls) || Long.class.equals(cls)) {
                return ((BigInteger) val).longValue();
            }
        }

        if (val instanceof BigDecimal) {
            if (double.class.equals(cls) || Double.class.equals(cls)) {
                return ((BigDecimal) val).doubleValue();
            }
            if (float.class.equals(cls) || Float.class.equals(cls)) {
                return ((BigDecimal) val).floatValue();
            }
        }

        if (val instanceof Timestamp) {
            if (LocalDateTime.class.equals(cls)) {
                return ((Timestamp) val).toLocalDateTime();
            }
            if (LocalDate.class.equals(cls)) {
                return ((Timestamp) val).toLocalDateTime().toLocalDate();
            }
            if (LocalTime.class.equals(cls)) {
                return ((Timestamp) val).toLocalDateTime().toLocalTime();
            }
            if (OffsetDateTime.class.equals(cls)) {
                return ((Timestamp) val).toLocalDateTime().atOffset(ZoneOffset.UTC);
            }
        }
        return val;
    }

}
