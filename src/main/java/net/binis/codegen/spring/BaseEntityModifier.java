package net.binis.codegen.spring;

import lombok.extern.slf4j.Slf4j;
import net.binis.codegen.annotation.Final;
import net.binis.codegen.modifier.Modifiable;
import net.binis.codegen.modifier.Modifier;

import javax.persistence.EntityManager;
import java.util.function.Function;

@Slf4j
public class BaseEntityModifier<T, R> extends BasePersistenceOperations<R> implements Modifier<R> {

    private R parent;

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

    @Final(imports = {"java.util.function.Function"}, description = "Function<{R}, {T}> function")
    public R transaction(Function<T, R> function) {
        return withRes(manager -> function.apply((T)((Modifiable) manager.merge(parent)).with()));
    }


    @Override
    public void setObject(R parent) {
        this.parent = parent;
    }

}
