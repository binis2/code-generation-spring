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
import net.binis.codegen.annotation.Final;
import net.binis.codegen.modifier.Modifiable;
import net.binis.codegen.modifier.Modifier;

import javax.persistence.EntityManager;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class BaseEntityModifier<T, R> extends BasePersistenceOperations<R> implements Modifier<R> {

    @Final
    public R save() {
        with(manager -> manager.persist(parent));
        return parent;
    }

    @Final
    public R saveAndFlush() {
        save();
        with(EntityManager::flush);
        return parent;
    }

    @Final
    public R merge() {
        return withRes(manager -> manager.merge(parent));
    }

    @Final
    public R delete() {
        with(manager -> manager.remove(parent));
        return parent;
    }

    @Final
    public R refresh() {
        with(manager -> manager.refresh(parent));
        return parent;
    }

    @Final
    public R detach() {
        with(manager -> manager.detach(parent));
        return parent;
    }

    @SuppressWarnings("unchecked")
    @Final(imports = {"java.util.function.Function"}, description = "Function<{R}, {T}> function")
    public R transaction(Function<T, R> function) {
        return withRes(manager -> function.apply((T)((Modifiable) manager.merge(parent)).with()));
    }

    @SuppressWarnings("unchecked")
    public T _if(boolean condition, Consumer<T> consumer) {
        if (condition) {
            consumer.accept((T) this);
        }
        return (T) this;
    }

    @Override
    public void setObject(R parent) {
        this.parent = parent;
    }

}
