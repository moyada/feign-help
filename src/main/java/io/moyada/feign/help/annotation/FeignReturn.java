package io.moyada.feign.help.annotation;

import java.lang.annotation.*;

/**
 * @author xueyikang
 * @since 1.0
 **/
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface FeignReturn {

    /**
     * 指定返回类型，需为返回类型或子类或实现类，基础类型无需设置
     * @return 对象类
     */
    Class<?> target();

    /**
     * 使用静态方法创建返回数据，需要在可访问范围内
     * @return 静态方法名
     */
    String staticMethod() default "";

    /**
     * 基本类型直接设置值，如 "23", "true", "test"
     * 对象类型可设置返回 "null"
     * 非基本类型可使用构造函数，支持参数列表为基本类型
     * 默认返回空构造方法对象
     * @return 返回值的构造数据
     */
    String[] params() default {};
}
