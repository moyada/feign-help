package cn.moyada.test.example;

import java.util.List;

/**
 * @author xueyikang
 * @since 1.0
 **/
public interface Handler {
    
    Integer getId();

    String getType();

    List<Object> getParams();
}
