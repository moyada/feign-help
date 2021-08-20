package io.moyada.feign.help.util;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import io.moyada.feign.help.annotation.FallbackBuild;
import io.moyada.feign.help.annotation.FallbackFactoryBuild;
import io.moyada.feign.help.support.SyntaxTreeMaker;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 元素工具
 * @author xueyikang
 * @since 1.3.1
 **/
public final class ElementUtil {

    private ElementUtil() {
    }

    public static Collection<JCTree.JCClassDecl> getFallbackFactory(Trees trees, RoundEnvironment roundEnv) {
        Set<? extends Element> factoryEles = roundEnv.getElementsAnnotatedWith(FallbackFactoryBuild.class);
        if (factoryEles.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.List<JCTree.JCClassDecl> list = new java.util.ArrayList<JCTree.JCClassDecl>(factoryEles.size());
        for (Element element : factoryEles) {
            if (element.getKind() == ElementKind.INTERFACE) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getTree(element);
                list.add(classDecl);
            }
        }
        return list;
    }

    public static Collection<JCTree.JCClassDecl> getFallback(Trees trees, RoundEnvironment roundEnv, Collection<JCTree.JCClassDecl> factoryEles) {
        Set<? extends Element> fallbackEles = roundEnv.getElementsAnnotatedWith(FallbackBuild.class);
        if (fallbackEles.isEmpty()) {
            return factoryEles;
        }

        Collection<JCTree.JCClassDecl> list = new java.util.ArrayList<JCTree.JCClassDecl>(fallbackEles.size());

        for (Element element : fallbackEles) {
            if (element.getKind() == ElementKind.INTERFACE) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getTree(element);
                list.add(classDecl);
            }
        }
        for (JCTree.JCClassDecl element : factoryEles) {
            if (!list.contains(element)) {
                list.add(element);
            }
        }
        return list;
    }

    public static Collection<JCTree.JCMethodDecl> getStaticMethod(Trees trees, SyntaxTreeMaker treeMaker, JCTree.JCClassDecl element) {
        Collection<JCTree.JCMethodDecl> list = new ArrayList<JCTree.JCMethodDecl>();
        // 获取目标类中方法
        if (element.defs != null) {
            for (JCTree tree : element.defs) {
                if (tree.getKind() == Tree.Kind.METHOD) {
                    list.add((JCTree.JCMethodDecl) tree);
                }
            }
        }
        // 注入父类方法
        addSuperStaticMethod(list, trees, treeMaker, element);
        return list;
    }

    private static void addSuperStaticMethod(Collection<JCTree.JCMethodDecl> list, Trees trees, SyntaxTreeMaker treeMaker, JCTree.JCClassDecl element) {
        if (element.implementing == null) {
            return;
        }

        // 遍历继承接口
        for (JCTree.JCExpression imple : element.implementing) {
            JCTree.JCIdent ident = (JCTree.JCIdent) imple.getTree();

            // 获取当前类节点
            JCTree.JCClassDecl sup = (JCTree.JCClassDecl) trees.getTree(ident.sym);
            if (sup == null) {
                // 当前类中方法
                for (Symbol symbol : ident.sym.getEnclosedElements()) {
                    if (symbol.getKind() == ElementKind.METHOD) {
                        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
                        JCTree.JCMethodDecl methodDecl = buildFHMethodDecl(treeMaker, methodSymbol);
                        list.add(methodDecl);
                    }
                }
                continue;
            }

            // 获取节点属性
            if (sup.defs != null) {
                for (JCTree tree : sup.defs) {
                    if (tree.getKind() == Tree.Kind.METHOD) {
                        list.add((JCTree.JCMethodDecl) tree);
                    }
                }
            }

            // get super method
            addSuperStaticMethod(list, trees, treeMaker, sup);
        }
    }

    private static JCTree.JCMethodDecl buildFHMethodDecl(SyntaxTreeMaker syntaxTreeMaker, Symbol.MethodSymbol symbol) {
        TreeMaker treeMaker = syntaxTreeMaker.getTreeMaker();

        JCTree.JCModifiers mod = treeMaker.Modifiers(Flags.PUBLIC);

        Name name = symbol.name;

        Type returnType = symbol.getReturnType();
        JCTree.JCIdent restype = treeMaker.Ident(returnType.tsym);

        List<JCTree.JCTypeParameter> typarams = null;
        List<Symbol.TypeVariableSymbol> typeParameters = symbol.getTypeParameters();
        if (typeParameters == null || typeParameters.isEmpty()) {
            typarams = List.nil();
        } else {
            for (Symbol.TypeVariableSymbol typeParameter : typeParameters) {
                JCTree.JCTypeParameter jcTypeParameter = treeMaker.TypeParam(typeParameter.name, (Type.TypeVar) typeParameter.type);

                if (typarams == null) {
                    typarams = List.of(jcTypeParameter);
                } else {
                    typarams = typarams.append(jcTypeParameter);
                }
            }
        }

        List<JCTree.JCVariableDecl> params = null;
        List<Symbol.VarSymbol> parameters = symbol.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            params = List.nil();
        } else {
            for (Symbol.VarSymbol parameter : parameters) {
                JCTree.JCVariableDecl param = treeMaker.Param(parameter.name, parameter.type, parameter.owner);
                if (params == null) {
                    params = List.of(param);
                } else {
                    params = params.append(param);
                }
            }
        }

        List<JCTree.JCExpression> thrown = null;
        List<Type> thrownTypes = symbol.getThrownTypes();
        if (thrownTypes == null || thrownTypes.isEmpty()) {
            thrown = List.nil();
        } else {
            for (Type thrownType : thrownTypes) {
                JCTree.JCIdent ident = treeMaker.Ident(thrownType.tsym);
                if (thrown == null) {
                    thrown = List.of((JCTree.JCExpression) ident);
                } else {
                    thrown = thrown.append(ident);
                }
            }
        }

        return treeMaker.MethodDef(mod, name, restype, typarams, params, thrown, null, null);
    }
}
