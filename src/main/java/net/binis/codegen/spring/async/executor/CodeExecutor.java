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
import net.binis.codegen.spring.async.AsyncExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CodeExecutor {

    private CodeExecutor() {
        //Do nothing.
    }

    public static AsyncExecutor defaultExecutor() {
        var executor = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        return task -> executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.warn("Failed to execute task!", e);
            }
        });
    }

    public static AsyncExecutor silentExecutor() {
        var executor = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        return task -> executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                //Do nothing
            }
        });
    }


}
