package io.moyada.feign.help.util;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import io.moyada.feign.help.annotation.FallbackBuild;
import io.moyada.feign.help.annotation.FallbackFactoryBuild;
import io.moyada.feign.help.support.SyntaxTreeMaker;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 元素工具
 * @author xueyikang
 * @since 1.3.1
 **/
public final class ElementUtil {

    private ElementUtil() {
    }

    public static List<JCTree.JCClassDecl> getFallbackFactory(Trees trees, RoundEnvironment roundEnv) {
        Set<? extends Element> factoryEles = roundEnv.getElementsAnnotatedWith(FallbackFactoryBuild.class);
        if (factoryEles.isEmpty()) {
            return Collections.emptyList();
        }
        List<JCTree.JCClassDecl> list = new ArrayList<JCTree.JCClassDecl>(factoryEles.size());
        for (Element element : factoryEles) {
            if (element.getKind() == ElementKind.INTERFACE) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getTree(element);
                list.add(classDecl);
            }
        }
        return list;
    }

    public static List<JCTree.JCClassDecl> getFallback(Trees trees, RoundEnvironment roundEnv, List<JCTree.JCClassDecl> factoryEles) {
        Set<? extends Element> fallbackEles = roundEnv.getElementsAnnotatedWith(FallbackBuild.class);
        if (fallbackEles.isEmpty()) {
            return factoryEles;
        }

        List<JCTree.JCClassDecl> list = new ArrayList<JCTree.JCClassDecl>(fallbackEles.size());

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

    public static List<JCTree.JCMethodDecl> getStaticMethod(Trees trees, SyntaxTreeMaker treeMaker, JCTree.JCClassDecl element) {
        List<JCTree.JCMethodDecl> list = new ArrayList<JCTree.JCMethodDecl>();

        if (element.defs != null) {
            for (JCTree tree : element.defs) {
                if (tree.getKind() == Tree.Kind.METHOD) {
                    list.add((JCTree.JCMethodDecl) tree);
                }
            }
        }

        addSuperStaticMethod(list, trees, treeMaker, element);
        return list;
    }

    private static void addSuperStaticMethod(List<JCTree.JCMethodDecl> list, Trees trees, SyntaxTreeMaker treeMaker, JCTree.JCClassDecl element) {
        if (element.implementing == null) {
            return;
        }

        for (JCTree.JCExpression imple : element.implementing) {
            JCTree.JCIdent ident = (JCTree.JCIdent) imple.getTree();
            JCTree.JCClassDecl sup = (JCTree.JCClassDecl) trees.getTree(ident.sym);

            // get super method
            addSuperStaticMethod(list, trees, treeMaker, sup);
            if (sup.defs == null) {
                continue;
            }
            for (JCTree tree : sup.defs) {
                if (tree.getKind() == Tree.Kind.METHOD) {
                    list.add((JCTree.JCMethodDecl) tree);
                }
            }
        }
    }
}
