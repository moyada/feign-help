package cn.moyada.test.example;

import io.moyada.feign.plus.annotation.Max;
import io.moyada.feign.plus.annotation.Min;
import io.moyada.feign.plus.annotation.NotBlank;
import io.moyada.feign.plus.annotation.Return;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class Example {

    public boolean save(@Return("false") Person name) {
        System.out.println(name);
        return true;
    }

    class Person {

        @NotBlank
        private String name;

        @Min(0)
        @Max(300)
        private int age;
    }
}
