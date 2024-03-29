package net.binis.codegen.spring.query;

/*-
 * #%L
 * code-generator-spring
 * %%
 * Copyright (C) 2021 - 2024 Binis Belev
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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public interface QueryStarter<R, S, A, F, U> {

    S by();

    S by(Class<?> projection);

    A aggregate();

    F select();

    U update();

    <T> T by(boolean condition, Function<S, T> query);

    <T> T by(boolean condition, Function<S, T> query, Function<S, T> elseQuery);

    QueryParam<R> nativeQuery(String query);

    QueryParam<R> query(String query);

    void transaction(Consumer<QueryStarter<R, S, A, F, U>> consumer);

    CompletableFuture<Void> asyncC(Consumer<QueryStarter<R, S, A, F, U>> consumer);
    <T> CompletableFuture<T> async(Function<QueryStarter<R, S, A, F, U>, T> func);

    CompletableFuture<Void> asyncC(long delay, TimeUnit unit, Consumer<QueryStarter<R, S, A, F, U>> consumer);
    <T> CompletableFuture<T> async(long delay, TimeUnit unit, Function<QueryStarter<R, S, A, F, U>, T> func);

    CompletableFuture<Void> asyncC(Duration duration, Consumer<QueryStarter<R, S, A, F, U>> consumer);
    <T> CompletableFuture<T> async(Duration duration, Function<QueryStarter<R, S, A, F, U>, T> func);

    CompletableFuture<Void> asyncC(String flow, Consumer<QueryStarter<R, S, A, F, U>> consumer);
    <T> CompletableFuture<T> async(String flow, Function<QueryStarter<R, S, A, F, U>, T> func);

    CompletableFuture<Void> asyncC(String flow, long delay, TimeUnit unit, Consumer<QueryStarter<R, S, A, F, U>> consumer);
    <T> CompletableFuture<T> async(String flow, long delay, TimeUnit unit, Function<QueryStarter<R, S, A, F, U>, T> func);

    CompletableFuture<Void> asyncC(String flow, Duration duration, Consumer<QueryStarter<R, S, A, F, U>> consumer);
    <T> CompletableFuture<T> async(String flow, Duration duration, Function<QueryStarter<R, S, A, F, U>, T> func);

    R reference(Object id);

}
