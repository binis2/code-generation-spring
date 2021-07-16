package net.binis.codegen.spring.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface QueryFilter<R> extends QueryExecute<R> {

    QueryFilter<R> parameter(String name, Object value);
    QueryFilter<R> disable();

}
