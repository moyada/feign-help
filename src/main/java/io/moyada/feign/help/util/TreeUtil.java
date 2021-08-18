package io.moyada.feign.help.util;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 语法树工具
 * @author xueyikang
 * @since 1.0
 **/
public final class TreeUtil {

    private TreeUtil() {
    }

    /**
     * 空参数
     * @return 空参数
     */
    public static List<JCTree.JCExpression> emptyExpression(){
        return List.<JCTree.JCExpression>nil();
    }

    /**
     * 新建语句链
     * @return 语句链
     */
    public static ListBuffer<JCTree.JCStatement> newStatement(){
        return new ListBuffer<JCTree.JCStatement>();
    }

    /**
     * 获取注解数据
     * @param symbol 元素
     * @param className 注解类名
     * @param attrKey 属性名
     * @return 数据
     */
    public static String getAnnotationValue(Symbol symbol, String className, String attrKey) {
        Attribute.Compound annotationAttr = getAnnotationAttr(symbol.getAnnotationMirrors(), className);
        if (null == annotationAttr) {
            return null;
        }
        System.out.println(annotationAttr.toString());
        return getAnnotationValue(annotationAttr, attrKey);
    }

    /**
     * 获取注解类型属性
     * @param ms 元素注解镜像
     * @param className 注解类名
     * @return 对应注解镜像
     */
    private static Attribute.Compound getAnnotationAttr(List<Attribute.Compound> ms, String className) {
        for (Attribute.Compound m : ms) {
            if (!m.getAnnotationType().toString().equals(className)) {
                continue;
            }

            return m;
        }
        return null;
    }

    /**
     * 获取注解参数值
     * @param annotationAttr 注解镜像
     * @param key 属性名
     * @return 数值
     */
    private static String getAnnotationValue(Attribute.Compound annotationAttr, String key) {
        if (null == annotationAttr) {
            return null;
        }

        for (Map.Entry<Symbol.MethodSymbol, Attribute> entry : annotationAttr.getElementValues().entrySet()) {
            if (entry.getKey().toString().equals(key)) {
                return entry.getValue().getValue().toString();
            }
        }
        return null;
    }

    /**
     * 获取注解数据
     * @param symbol 元素
     * @param anno 注解类
     * @param <T> 类型
     * @return 注解数据
     */
    @SuppressWarnings("deprecation")
    public static <T extends Annotation> T getAnnotation(Symbol symbol, Class<T> anno) {
        if (null == symbol) {
            return null;
        }
        return symbol.getAnnotation(anno);
    }

    /**
     * 获取实际类型名
     * @param symbol 元素
     * @return 类名
     */
    public static String getOriginalTypeName(Symbol symbol) {
        return symbol.asType().asElement().toString();
    }

    /**
     * 是否可用默认方法
     * @return 版本在 8 以下则返回 false
     */
    public static boolean hasDefaultInterface() {
        return Compiler.CURRENT_VERSION >= Compiler.JAVA_8;
    }
}
