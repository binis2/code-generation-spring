package net.binis.codegen.spring.query;

/*-
 * #%L
 * code-generator-spring
 * %%
 * Copyright (C) 2021 - 2022 Binis Belev
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
        QueryName<QS, QO, QR, Preset> prototype(Object field);
    }

    interface QueryOperationFields<QR> extends QueryScript<QR> {
        QR field(Object field);
    }

    interface QueryOrder<QR> extends QueryOperationFields<QueryOrderOperation<Preset.QueryOrder<QR>, QR>>, QueryScript<QueryOrderOperation<Preset.QueryOrder<QR>, QR>> {
    }

    interface QuerySelect<QR> extends QueryModifiers<Preset.QueryName<Preset.QuerySelect<QR>, Preset.QueryOrder<QR>, QR, Preset>>, Preset.QueryFields<QuerySelectOperation<Preset.QuerySelect<QR>, Preset.QueryOrder<QR>, QR>>, Preset.QueryFuncs<QuerySelectOperation<Preset.QuerySelect<QR>, Preset.QueryOrder<QR>, QR>>, QueryOrderStart<QueryOperationFields<QueryOrderOperation<Preset.QueryOrder<QR>, QR>>>, QueryBracket<QuerySelect<QR>> {
        QueryName<Preset.QuerySelect<QR>, Preset.QueryOrder<QR>, QR, Preset> prototype(Object field);
        QueryCollectionFunctions<QR, QuerySelectOperation<QuerySelect<QR>, QueryOperationFields<QueryOrderOperation<QueryOrder<QR>, QR>>, QR>> collection(Object field);
    }
    

}
