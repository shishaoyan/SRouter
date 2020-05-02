package com.ssy.srouter.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Route {

    String path();

    String group() default "";

    String name() default "";

    int extras() default Integer.MIN_VALUE;

    int priority() default -1;
}
