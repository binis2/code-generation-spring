package net.binis.codegen.spring.modifier.impl;

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

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.factory.CodeFactory;
import net.binis.codegen.modifier.Modifiable;
import net.binis.codegen.spring.modifier.BaseEntityModifier;
import net.binis.codegen.spring.modifier.BasePersistenceOperations;

import java.util.function.Function;

@Slf4j
public abstract class BaseEntityModifierImpl<T, R> extends BasePersistenceOperations<T, R> implements BaseEntityModifier<T, R> {

    protected BaseEntityModifierImpl(R parent) {
        super(parent);
    }

    public R save() {
        with(manager -> manager.persist(parent));
        return parent;
    }

    public <P> P save(Class<P> projection) {
        save();
        return withProjection(projection);
    }

    public R saveAndFlush() {
        save();
        with(EntityManager::flush);
        return parent;
    }

    public <P> P saveAndFlush(Class<P> projection) {
        saveAndFlush();
        return withProjection(projection);
    }


    public R merge() {
        return withRes(manager -> manager.merge(parent));
    }

    public R delete() {
        with(manager -> manager.remove(parent));
        return parent;
    }

    public R refresh() {
        with(manager -> manager.refresh(parent));
        return parent;
    }

    public R detach() {
        with(manager -> manager.detach(parent));
        return parent;
    }

    @SuppressWarnings("unchecked")
    public R transaction(Function<T, R> function) {
        return withRes(manager -> function.apply((T)((Modifiable) manager.merge(parent)).with()));
    }

    protected <P> P withProjection(Class<P> projection) {
        return CodeFactory.projection(parent, projection);
    }


}
