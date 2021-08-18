package cn.moyada.test.example;


import io.moyada.feign.help.annotation.FallbackFactoryBuild;

/**
 * @author xueyikang
 * @since 1.0
 **/
@FallbackFactoryBuild
public interface Product extends Handler {

    String getName();
}
