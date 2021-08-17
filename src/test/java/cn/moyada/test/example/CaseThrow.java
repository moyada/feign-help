package cn.moyada.test.example;

import io.moyada.feign.plus.annotation.Min;
import io.moyada.feign.plus.annotation.NotNull;
import io.moyada.feign.plus.annotation.Size;
import io.moyada.feign.plus.annotation.Throw;

import java.util.List;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class CaseThrow {

    public boolean hasReturn(@Throw @NotNull String name,
                             @Throw(NumberFormatException.class) @Min(0) double price,
                             boolean putaway) {
        System.out.println("hasReturn");
        return true;
    }

    public void nonReturn(@Throw(value = IllegalStateException.class, message = "unknown error") Product product,
                          @Throw(message = "price error") @Size(min = 1, max = 20) List<String> param) {
        System.out.println("nonReturn");
    }
}
