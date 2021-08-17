package cn.moyada.test.example;

import io.moyada.feign.plus.annotation.Min;
import io.moyada.feign.plus.annotation.NotBlank;
import io.moyada.feign.plus.annotation.Size;

import java.util.List;

/**
 * @author xueyikang
 * @since 1.0
 **/
public interface Handler {
    
    @Min(0)
    int getId();

    @NotBlank
    String getType();

    @Size(min = 1, max = 20)
    List<Param> getParams();
}
