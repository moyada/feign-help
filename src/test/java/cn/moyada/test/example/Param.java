package cn.moyada.test.example;

import io.moyada.feign.plus.annotation.NotNull;
import io.moyada.feign.plus.annotation.Variable;

/**
 * @author xueyikang
 * @since 1.0
 **/
@Variable("check0")
public class Param {

    @NotNull
    private String name;

    @NotNull
    private Object value;
}
