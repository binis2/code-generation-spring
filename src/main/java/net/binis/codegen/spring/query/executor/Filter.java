package net.binis.codegen.spring.query.executor;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Filter {

    private String name;
    private Map<String, Object> values;
    private boolean disabled;

}
