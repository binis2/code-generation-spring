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

public interface QueryCollectionFunctions<T, R> {

    QueryFunctions<Integer, R> size();
    R size(Integer size);
    R contains(T value);
    R notContains(T value);
    R containsAll(Collection<T> list);
    R containsOne(Collection<T> list);
    R containsNone(Collection<T> list);
    R isEmpty();
    R isNotEmpty();
    R joinFetch();
    R leftJoinFetch();

}
