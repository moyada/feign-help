package io.moyada.feign.plus.annotation;

import java.lang.annotation.*;

/**
 * @author xueyikang
 * @since 1.0
 **/
@Documented
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Valid {
}
