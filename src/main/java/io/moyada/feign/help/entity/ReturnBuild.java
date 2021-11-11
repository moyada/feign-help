package io.moyada.feign.help.entity;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import io.moyada.feign.help.annotation.FeignReturn;
import io.moyada.feign.help.support.SyntaxTreeMaker;
import io.moyada.feign.help.support.TypeTag;
import io.moyada.feign.help.util.TreeUtil;
import io.moyada.feign.help.util.TypeUtil;

import javax.lang.model.element.ElementKind;
import java.util.Arrays;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class ReturnBuild {

    private final SyntaxTreeMaker syntaxTreeMaker;

    public ReturnBuild(SyntaxTreeMaker syntaxTreeMaker) {
        this.syntaxTreeMaker = syntaxTreeMaker;
    }

    public JCTree.JCReturn getReturn(JCTree.JCClassDecl classDecl) {
        FeignReturn returnAttr = TreeUtil.getAnnotation(classDecl.sym, FeignReturn.class);
        if (returnAttr == null) {
            return null;
        }
        String target = TreeUtil.getAnnotationValue(classDecl.sym, FeignReturn.class.getName(), "target()");
        Symbol.ClassSymbol targetClass = syntaxTreeMaker.getTypeElement(target);
        if (targetClass == null) {
            throw new RuntimeException("FeignReturn target() cannot be found: " + target);
        }

        String staticMethod = returnAttr.staticMethod();
        if (staticMethod.isEmpty()) {
            return constMethod(target, targetClass, returnAttr.params());
        }
        return staticMethod(target, targetClass, returnAttr.staticMethod(), returnAttr.params());
    }

    private JCTree.JCReturn constMethod(String className, Symbol.ClassSymbol targetClass, String[] params) {
        // 类构造方法
        List<JCTree.JCExpression> paramType = getParamType(targetClass, true, params);
        if (null == paramType) {
            // 无匹配的构造方法
            throw new RuntimeException("[Return Error] Can't find match param constructor from " + className + " by " + Arrays.toString(params));
        }
        JCTree.JCExpression jcExpression = syntaxTreeMaker.NewObject(className, paramType);
        return syntaxTreeMaker.getTreeMaker().Return(jcExpression);
    }

    private JCTree.JCReturn staticMethod(String className, Symbol.ClassSymbol targetClass, String staticMethod, String[] params) {
        List<JCTree.JCExpression> paramType;
        if (params.length == 0) {
            paramType = TreeUtil.emptyExpression();
        } else {
            paramType = getParamType(targetClass, false, params);
            if (null == paramType) {
                // 无匹配的静态方法
                throw new RuntimeException("[Return Error] Can't find match param static method from " + className + " by " + Arrays.toString(params));
            }
        }

        JCTree.JCExpression clazzType = syntaxTreeMaker.findClass(className);
        JCTree.JCMethodInvocation method = syntaxTreeMaker.getMethod(clazzType, staticMethod, paramType);
        return syntaxTreeMaker.getTreeMaker().Return(method);
    }

    /**
     * 参数转换
     * @param classSymbol 解析类节点
     * @param isConstruct 解析构造方法还是静态方法
     * @param values 参数数据
     * @return 返回对应参数元素
     */
    private List<JCTree.JCExpression> getParamType(Symbol.ClassSymbol classSymbol, boolean isConstruct, String[] values) {
        int length = values.length;

        List<JCTree.JCExpression> param = null;

        boolean findParam;
        for (Symbol element : classSymbol.getEnclosedElements()) {

            // 构造方法
            if (isConstruct) {
                if (!element.isConstructor()) {
                    continue;
                }
            } else {
                // 静态方法
                if (element.getKind() != ElementKind.METHOD) {
                    continue;
                }
                if (!element.isStatic()) {
                    continue;
                }
            }

            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
            List<Symbol.VarSymbol> parameters = methodSymbol.getParameters();

            // 参数个数一致
            if (parameters.size() != values.length) {
                continue;
            }

            findParam = true;
            for (int i = 0; findParam && i < length; i++) {
                Symbol.VarSymbol varSymbol = parameters.get(i);
                String value = values[i];

                String typeName = TreeUtil.getOriginalTypeName(varSymbol);
                TypeTag baseType = TypeUtil.getBaseType(typeName);

                JCTree.JCExpression argsVal;

                // 不支持复杂对象
                if (null == baseType) {
                    param = null;
                    findParam = false;
                    continue;
                }

                Object data = TreeUtil.getValue(baseType, value);
                // 数据与类型不匹配
                if (null == data) {
                    param = null;
                    findParam = false;
                    continue;
                }
                argsVal = syntaxTreeMaker.newElement(baseType, data);

                if (null == param) {
                    param = List.of(argsVal);
                } else {
                    param = param.append(argsVal);
                }
            }

            if (param != null) {
                return param;
            }
        }

        return null;
    }
}
