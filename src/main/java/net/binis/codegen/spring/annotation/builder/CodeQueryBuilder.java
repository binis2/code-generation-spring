package net.binis.codegen.spring.annotation.builder;

/*-
 * #%L
 * code-generator-core
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

import net.binis.codegen.annotation.CodeAnnotation;
import net.binis.codegen.annotation.CodePrototypeTemplate;
import net.binis.codegen.enrich.*;
import net.binis.codegen.options.CodeOption;
import net.binis.codegen.spring.modifier.BaseEntityModifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@CodeAnnotation
@CodePrototypeTemplate
@Retention(RetentionPolicy.RUNTIME)
public @interface CodeQueryBuilder {

    String name() default "";
    String interfaceName() default "";
    boolean generateConstructor() default true;
    boolean generateImplementation() default true;
    boolean generateInterface() default true;
    boolean base() default false;
    boolean interfaceSetters() default false;
    boolean classGetters() default true;
    boolean classSetters() default false;
    Class<?> baseModifierClass() default BaseEntityModifier.class;
    Class<?> mixInClass() default void.class;
    String implementationPackage() default "";
    String basePath() default "";
    String interfacePath() default "";
    String implementationPath() default "";

    Class<? extends Enricher>[] enrichers() default {QueryEnricher.class, ValidationEnricher.class, CreatorModifierEnricher.class, ModifierEnricher.class, RegionEnricher.class};
    Class<? extends Enricher>[] inheritedEnrichers() default {};
    Class<? extends CodeOption>[] options() default {};

}
