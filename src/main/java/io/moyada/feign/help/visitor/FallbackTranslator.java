package io.moyada.feign.help.visitor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import io.moyada.feign.help.annotation.FallbackBuild;
import io.moyada.feign.help.constant.ClassName;
import io.moyada.feign.help.support.SyntaxTreeMaker;
import io.moyada.feign.help.util.ElementUtil;
import io.moyada.feign.help.util.TreeUtil;

import javax.annotation.processing.Messager;
import java.util.Iterator;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class FallbackTranslator extends BaseTranslator {

    private Name name;

    public FallbackTranslator(Trees trees, SyntaxTreeMaker syntaxTreeMaker, Messager messager) {
        super(trees, syntaxTreeMaker, messager);
        name = syntaxTreeMaker.getName(ClassName.BEAN_NAME);
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        super.visitClassDef(jcClassDecl);
        // 过滤非接口
        if ((jcClassDecl.getModifiers().flags & Flags.INTERFACE) == 0) {
            return;
        }
        // 过滤内部类
        String localClass = jcClassDecl.sym.outermostClass().toString();
        String name = jcClassDecl.sym.toString();
        if (!localClass.equals(name)) {
            return;
        }

        JCTree.JCClassDecl classDecl = createClass(jcClassDecl);
        super.appendClass(jcClassDecl, classDecl);
    }

    /**
     * 创建类
     * @param interClass 接口
     * @return 方法元素
     */
    private JCTree.JCClassDecl createClass(JCTree.JCClassDecl interClass) {
        List<JCTree> list = buildMethod(interClass);

        JCTree.JCIdent ident = treeMaker.Ident(interClass.name);
        List<JCTree.JCExpression> inters = List.of((JCTree.JCExpression) ident);

        JCTree.JCModifiers mod;
        String value = TreeUtil.getAnnotationValue(interClass.sym, FallbackBuild.class.getName(), "bean()");
        if (value == null || value.equals("false")) {
            mod = treeMaker.Modifiers(Flags.PUBLIC);
        } else {
            // import
            Name name = importClass(interClass, "org.springframework.stereotype", "Component");
            JCTree.JCIdent bean = treeMaker.Ident(name);
            JCTree.JCAnnotation annotation = treeMaker.Annotation(bean, List.<JCTree.JCExpression>nil());
            mod = treeMaker.Modifiers(Flags.PUBLIC, List.of(annotation));
        }

        return treeMaker.ClassDef(mod,
                name,
                List.<JCTree.JCTypeParameter>nil(),
                null,
                inters,
                list);
    }

    private List<JCTree> buildMethod(JCTree.JCClassDecl interClass) {
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
        return list;
    }

    /**
     * 实现空方法
     * @param jcMethodDecl 方法
     * @return 方法元素
     */
    private JCTree.JCMethodDecl createJCMethod(JCTree.JCMethodDecl jcMethodDecl) {
        JCTree.JCModifiers mod;
        if (jcMethodDecl.sym == null || jcMethodDecl.sym.getAnnotationMirrors().isEmpty()) {
            mod = treeMaker.Modifiers(Flags.PUBLIC);
        } else {
            List<JCTree.JCAnnotation> annotations = treeMaker.Annotations(jcMethodDecl.sym.getAnnotationMirrors());
            mod = treeMaker.Modifiers(Flags.PUBLIC, annotations);
        }

        return treeMaker.MethodDef(mod,
                jcMethodDecl.name,
                jcMethodDecl.restype,
                jcMethodDecl.typarams,
                jcMethodDecl.params,
                jcMethodDecl.thrown,
                emptyBody(), null);
    }

    private JCTree.JCBlock emptyBody() {
        ListBuffer<JCTree.JCStatement> statements = TreeUtil.newStatement();
        JCTree.JCReturn returnStatement = treeMaker.Return(syntaxTreeMaker.nullNode);
        statements.add(returnStatement);
        return syntaxTreeMaker.getBlock(statements);
    }
}
