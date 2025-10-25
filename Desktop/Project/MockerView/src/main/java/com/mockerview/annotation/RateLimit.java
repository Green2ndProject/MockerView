package com.mockerview.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 5;
    int duration() default 3600;
    String message() default "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.";
}
