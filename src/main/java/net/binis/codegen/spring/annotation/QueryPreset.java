package net.binis.codegen.spring.annotation;

import net.binis.codegen.annotation.CodeAnnotation;
import net.binis.codegen.annotation.Ignore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@CodeAnnotation
@Ignore(forClass = true, forInterface = true, forField = true, forModifier = true)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface QueryPreset {
}
