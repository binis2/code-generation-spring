package net.binis.codegen.spring.query;

public interface Preset {
    
    static QuerySelect<Preset> declare() {
        return null;
    }

    static <T> T param(T param) {
        return param;
    }

    static <T> T param() {
        return null;
    }

    interface QueryFields<QR> extends QueryScript<QR> {
        <T> QR field(T field, T value);
    }

    interface QueryFuncs<QR> {
        QueryFunctions<Object, QR> field(Object field);
    }

    interface QueryName<QS, QO, QR, QF> extends Preset.QueryFields<QuerySelectOperation<QS, QO, QR>>, Preset.QueryFuncs<QuerySelectOperation<QS, QO, QR>>, QueryFetch<QuerySelectOperation<QS, QO, QR>, QF> {
    }

    interface QueryOperationFields<QR> extends QueryScript<QR> {
        QR field(Object field);
    }

    interface QueryOrder<QR> extends QueryOperationFields<QueryOrderOperation<Preset.QueryOrder<QR>, QR>>, QueryExecute<QR>, QueryScript<QueryOrderOperation<Preset.QueryOrder<QR>, QR>> {
    }

    interface QuerySelect<QR> extends QueryExecute<QR>, QueryModifiers<Preset.QueryName<Preset.QuerySelect<QR>, Preset.QueryOrder<QR>, QR, Preset>>, Preset.QueryFields<QuerySelectOperation<Preset.QuerySelect<QR>, Preset.QueryOrder<QR>, QR>>, Preset.QueryFuncs<QuerySelectOperation<Preset.QuerySelect<QR>, Preset.QueryOrder<QR>, QR>>, QueryOrderStart<QueryOperationFields<QueryOrderOperation<Preset.QueryOrder<QR>, QR>>>, QueryBracket<QuerySelect<QR>> {
    }
    

}
