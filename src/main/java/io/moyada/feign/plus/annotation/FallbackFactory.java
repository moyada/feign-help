package io.moyada.feign.plus.annotation;


import java.lang.annotation.*;

/**
 * feign fallback factory
 * @author xueyikang
 * @since 1.0.0
 **/
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface FallbackFactory {
}
