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
        if (TypeUtil.isPrimitive(target) || TypeUtil.isStr(target)) {
            if (returnAttr.params().length != 1) {
                throw new RuntimeException("FeignReturn params() size error: " + target);
            }

            TypeTag baseType = TypeUtil.getBaseType(target);
            Object data = TreeUtil.getValue(baseType, returnAttr.params()[0]);
            JCTree.JCExpression argsVal = syntaxTreeMaker.newElement(baseType, data);
            return syntaxTreeMaker.getTreeMaker().Return(argsVal);
        }

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
        List<JCTree.JCExpression> paramList = getConstParam(targetClass, params);
        if (null == paramList) {
            // 无匹配的构造方法
            throw new RuntimeException("[Return Error] Can't find match param constructor from " + className + " by " + Arrays.toString(params));
        }
        JCTree.JCExpression jcExpression = syntaxTreeMaker.NewObject(className, paramList);
        return syntaxTreeMaker.getTreeMaker().Return(jcExpression);
    }

    private JCTree.JCReturn staticMethod(String className, Symbol.ClassSymbol targetClass, String staticMethod, String[] params) {
        List<JCTree.JCExpression> paramList;
        if (params.length == 0) {
            paramList = TreeUtil.emptyExpression();
        } else {
            paramList = getStaticParam(targetClass, params);
            if (null == paramList) {
                // 无匹配的静态方法
                throw new RuntimeException("[Return Error] Can't find match param static method from " + className + " by " + Arrays.toString(params));
            }
        }

        JCTree.JCExpression clazzType = syntaxTreeMaker.findClass(className);
        JCTree.JCMethodInvocation method = syntaxTreeMaker.getMethod(clazzType, staticMethod, paramList);
        return syntaxTreeMaker.getTreeMaker().Return(method);
    }

    /**
     * 参数转换
     * @param classSymbol 解析类节点
     * @param values 参数数据
     * @return 返回对应参数元素
     */
    private List<JCTree.JCExpression> getConstParam(Symbol.ClassSymbol classSymbol, String[] values) {
        for (Symbol element : classSymbol.getEnclosedElements()) {
            // 构造方法
            if (!element.isConstructor()) {
                continue;
            }

            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
            List<Symbol.VarSymbol> parameters = methodSymbol.getParameters();

            // 参数个数一致
            if (parameters.size() != values.length) {
                continue;
            }

            if (values.length == 0) {
                return List.<JCTree.JCExpression>nil();
            }

            List<JCTree.JCExpression> param = getParam(parameters, values);
            if (param != null) {
                return param;
            }
        }
        return null;
    }


    /**
     * 参数转换
     * @param classSymbol 解析类节点
     * @param values 参数数据
     * @return 返回对应参数元素
     */
    private List<JCTree.JCExpression> getStaticParam(Symbol.ClassSymbol classSymbol, String[] values) {
        for (Symbol element : classSymbol.getEnclosedElements()) {
            // 静态方法
            if (element.getKind() != ElementKind.METHOD || !element.isStatic()) {
                continue;
            }

            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
            List<Symbol.VarSymbol> parameters = methodSymbol.getParameters();

            // 参数个数一致
            if (parameters.size() != values.length) {
                continue;
            }
            if (values.length == 0) {
                return List.<JCTree.JCExpression>nil();
            }

            List<JCTree.JCExpression> param = getParam(parameters, values);
            if (param != null) {
                return param;
            }
        }
        return null;
    }

    private List<JCTree.JCExpression> getParam(List<Symbol.VarSymbol> parameters, String[] values) {
        List<JCTree.JCExpression> param = null;
        for (int i = 0; i < parameters.size(); i++) {
            Symbol.VarSymbol varSymbol = parameters.get(i);
            String value = values[i];

            String typeName = TreeUtil.getOriginalTypeName(varSymbol);
            TypeTag baseType = TypeUtil.getBaseType(typeName);

            // 不支持复杂对象
            if (null == baseType) {
                return null;
            }

            Object data = TreeUtil.getValue(baseType, value);
            // 数据与类型不匹配
            if (null == data) {
                return null;
            }

            JCTree.JCExpression argsVal = syntaxTreeMaker.newElement(baseType, data);
            if (null == param) {
                param = List.of(argsVal);
            } else {
                param = param.append(argsVal);
            }
        }
        return null;
    }
}
