package cn.moyada.test.example;


import io.moyada.feign.help.annotation.FallbackFactoryBuild;

/**
 * @author xueyikang
 * @since 1.0
 **/
@FallbackFactoryBuild

public class MyApp {

    public Boolean run(Args args) {
        return true;
    }

    class Args {

        String param;
    }
}
