package cn.moyada.test.example;

import io.moyada.feign.plus.annotation.NotBlank;

/**
 * @author xueyikang
 * @since 1.0
 **/
public abstract class Person {

    @NotBlank
    public abstract String getName();

    @NotBlank
    public abstract StringBuffer getAddress();
}
