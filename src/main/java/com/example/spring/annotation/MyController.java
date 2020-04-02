package com.example.spring.annotation;

import java.lang.annotation.*;

//此3个注解是元注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {
    /**
     * 表示给controller注册别名
     */
    String value() default "";
}
