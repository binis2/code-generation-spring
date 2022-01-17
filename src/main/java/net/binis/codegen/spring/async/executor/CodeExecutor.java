package net.binis.codegen.spring.async.executor;

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
import net.binis.codegen.spring.async.AsyncDispatcher;
import net.binis.codegen.spring.async.AsyncExecutor;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CodeExecutor {

    public static final String DEFAULT = "default";

    private static final Dispatcher dispatcher = new Dispatcher();

    private static final RejectedExecutionHandler defaultHandler =
            new ThreadPoolExecutor.AbortPolicy();

    static {
        registerDefaultExecutor(defaultExecutor(DEFAULT));
    }

    private CodeExecutor() {
        //Do nothing.
    }

    public static void registerDefaultExecutor(AsyncExecutor executor) {
        registerExecutor(DEFAULT, executor);
    }

    public static void registerExecutor(String flow, AsyncExecutor executor) {
        dispatcher.register(flow, executor);
    }

    public static AsyncDispatcher defaultDispatcher() {
        return dispatcher;
    }

    public static AsyncExecutor defaultExecutor(String flow) {
        var executor = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new DefaultThreadFactory(flow),
                defaultHandler);

        return task -> executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("Failed to execute task!", e);
            }
        });
    }

    public static AsyncExecutor silentExecutor(String flow) {
        var executor = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new DefaultThreadFactory(flow),
                defaultHandler);

        return task -> executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                //Do nothing
            }
        });
    }

    public static AsyncExecutor syncExecutor() {
        return Runnable::run;
    }

    public static AsyncExecutor syncSilentExecutor() {
        return task -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("Failed to execute task!", e);
            }
        };
    }

    private static final class Dispatcher implements AsyncDispatcher {
        private final Map<String, AsyncExecutor> flows = new ConcurrentHashMap<>();

        @Override
        public AsyncExecutor flow(String flow) {
            return flows.computeIfAbsent(flow, CodeExecutor::defaultExecutor);
        }

        @Override
        public AsyncExecutor _default() {
            return flow(DEFAULT);
        }

        private void register(String flow, AsyncExecutor executor) {
            flows.put(flow, executor);
        }
    }

    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String flow) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = flow + "-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }




}
