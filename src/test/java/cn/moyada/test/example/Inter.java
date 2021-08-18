package cn.moyada.test.example;

import io.moyada.feign.help.annotation.FallbackBuild;

/**
 * @author xueyikang
 * @since 1.0
 **/
@FallbackBuild
public interface Inter {
    
    Integer id();

    String name();
}
