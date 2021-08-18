package io.moyada.feign.help.annotation;


import java.lang.annotation.*;

/**
 * feign fallback factory
 * @author xueyikang
 * @since 1.0.0
 **/
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface FallbackFactoryBuild {
}
