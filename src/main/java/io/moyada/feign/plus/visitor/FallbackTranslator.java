package io.moyada.feign.plus.visitor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import io.moyada.feign.plus.support.SyntaxTreeMaker;
import io.moyada.feign.plus.util.ElementUtil;
import io.moyada.feign.plus.util.TreeUtil;

import javax.annotation.processing.Messager;
import java.util.Iterator;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class FallbackTranslator extends BaseTranslator {

    private Trees trees;
    private Name name;

    public FallbackTranslator(SyntaxTreeMaker syntaxTreeMaker, Messager messager, Trees trees) {
        super(syntaxTreeMaker, messager);
        this.trees = trees;
        name = syntaxTreeMaker.getName("Fallback");
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        super.visitClassDef(jcClassDecl);
        JCTree.JCClassDecl classDecl = createClass(jcClassDecl);
        appendClass(jcClassDecl, classDecl);
    }

    /**
     * 创建类
     * @param interClass 接口
     * @return 方法元素
     */
    private void appendClass(JCTree.JCClassDecl interClass, JCTree.JCClassDecl classDecl) {
        PosScanner posScanner = new PosScanner(interClass);
        interClass.accept(posScanner);

        interClass.defs = interClass.defs.append(classDecl);
    }

    /**
     * 创建类
     * @param interClass 接口
     * @return 方法元素
     */
    private JCTree.JCClassDecl createClass(JCTree.JCClassDecl interClass) {
        java.util.List<JCTree.JCMethodDecl> methodList = ElementUtil.getStaticMethod(trees, syntaxTreeMaker, interClass);

        List<JCTree> list = null;
        if (methodList.isEmpty()) {
            list = List.<JCTree>nil();
        } else {
            Iterator<JCTree.JCMethodDecl> it = methodList.iterator();
            while (it.hasNext()) {
                JCTree.JCMethodDecl methodDecl = it.next();
                JCTree.JCMethodDecl jcMethod = createJCMethod(methodDecl);

                if (list == null) {
                    list = List.of((JCTree) jcMethod);
                } else {
                    list = list.append((JCTree) jcMethod);
                }
            }
        }

        JCTree.JCIdent ident = treeMaker.Ident(interClass.name);
        List<JCTree.JCExpression> inters = List.of((JCTree.JCExpression) ident);

        return treeMaker.ClassDef(treeMaker.Modifiers(Flags.PUBLIC),
                name,
                List.<JCTree.JCTypeParameter>nil(),
                null,
                inters,
                list);
    }

    /**
     * 实现空方法
     * @param jcMethodDecl 方法
     * @return 方法元素
     */
    private JCTree.JCMethodDecl createJCMethod(JCTree.JCMethodDecl jcMethodDecl) {
        JCTree.JCMethodDecl methodDecl = treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC),
                jcMethodDecl.name,
                jcMethodDecl.restype,
                jcMethodDecl.typarams,
                jcMethodDecl.params,
                jcMethodDecl.thrown,
                emptyBody(), null);

//        if (jcMethodDecl.sym == null || jcMethodDecl.sym.getAnnotationMirrors() == null) {
//            methodDecl.sym = jcMethodDecl.sym;
//            return methodDecl;
//        }
//        methodDecl.sym = jcMethodDecl.sym.clone(jcMethodDecl.sym);
//        methodDecl.sym.getAnnotationMirrors().appendList(jcMethodDecl.sym.getAnnotationMirrors());
        // methodDecl.sym.getAnnotationMirrors();
        return methodDecl;
    }

    private JCTree.JCBlock emptyBody() {
        ListBuffer<JCTree.JCStatement> statements = TreeUtil.newStatement();
        JCTree.JCReturn returnStatement = treeMaker.Return(syntaxTreeMaker.nullNode);
        statements.add(returnStatement);
        return getBlock(statements);
    }
}
