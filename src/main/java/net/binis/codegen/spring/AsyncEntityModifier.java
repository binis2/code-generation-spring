package net.binis.codegen.spring;

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
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.spring.async.AsyncDispatcher;
import net.binis.codegen.spring.async.AsyncModifier;
import net.binis.codegen.spring.async.executor.CodeExecutor;
import net.binis.codegen.spring.async.executor.CodeGenCompletableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class AsyncEntityModifier<T, R> extends BaseEntityModifier<T, R> {

    static {
        CodeFactory.registerType(AsyncDispatcher.class, CodeFactory.singleton(CodeExecutor.defaultDispatcher()), null);
    }

    public AsyncModifier<T, R> async() {
        return new AsyncImpl();
    }

    protected class AsyncImpl implements AsyncModifier<T, R> {

        private String flow = CodeExecutor.DEFAULT;

        @Override
        public AsyncModifier<T, R> flow(String flow) {
            this.flow = flow;
            return this;
        }

        @Override
        public CompletableFuture<R> save() {
            return CodeGenCompletableFuture.supplyAsync(CodeFactory.create(AsyncDispatcher.class).flow(flow), AsyncEntityModifier.this::save);
        }

        @Override
        public CompletableFuture<R> delete() {
            return CodeGenCompletableFuture.supplyAsync(CodeFactory.create(AsyncDispatcher.class).flow(flow), AsyncEntityModifier.this::delete);
        }

        @Override
        public CompletableFuture<R> execute(Consumer<T> task) {
            return CodeGenCompletableFuture.supplyAsync(CodeFactory.create(AsyncDispatcher.class).flow(flow), () ->
                    AsyncEntityModifier.this.transaction(m -> {
                        task.accept(m);
                        return null;
                    }));
        }
    }

}
