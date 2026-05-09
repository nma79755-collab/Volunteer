package com.example.registrationservice.Aspect;

import java.lang.annotation.*;

/**
 * Operation Log Annotation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogAnnotation {
    /**
     * Operation type
     */
    String type();
    
    /**
     * Operation object
     */
    String object();
}
