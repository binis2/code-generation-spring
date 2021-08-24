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

import java.util.Collection;
import java.util.List;

public interface QueryFunctions<T, R> extends QueryScript<R> {

    QueryFunctions<Long, R> length();
    R equal(T value);
    R equal(Queryable query);
    R between(T from, T to);
    R in(Collection<T> values);
    R in(Queryable query);
    R isNull();
    R isNotNull();
    R like(String value);
    R starts(String value);
    R ends(String value);
    R contains(String value);
    R greater(T value);
    R greater(Queryable query);
    R greaterEqual(T value);
    R greaterEqual(Queryable query);
    R less(T value);
    R less(Queryable query);
    R lessEqual(T value);
    R lessEqual(Queryable query);

}
