package io.moyada.feign.help.visitor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import io.moyada.feign.help.annotation.FallbackBuild;
import io.moyada.feign.help.constant.ClassName;
import io.moyada.feign.help.entity.TreeNode;
import io.moyada.feign.help.support.Printer;
import io.moyada.feign.help.support.SyntaxTreeMaker;
import io.moyada.feign.help.support.TypeTag;
import io.moyada.feign.help.util.ClassUtil;
import io.moyada.feign.help.util.ElementUtil;
import io.moyada.feign.help.util.TreeUtil;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class FallbackTranslator extends BaseTranslator {

    private final Name name;

    public FallbackTranslator(Trees trees, SyntaxTreeMaker syntaxTreeMaker, Printer printer) {
        super(trees, syntaxTreeMaker, printer);
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
            Name name = addImport(interClass, "org.springframework.stereotype", "Component");
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
        Collection<JCTree.JCMethodDecl> methodList = ElementUtil.getStaticMethod(trees, syntaxTreeMaker, interClass);
        if (methodList.isEmpty()) {
            return List.<JCTree>nil();
        }

        List<JCTree> list = null;
        for (JCTree.JCMethodDecl methodDecl : methodList) {
            addImport(interClass, methodDecl);
            JCTree.JCMethodDecl jcMethod = createJCMethod(methodDecl);
            if (list == null) {
                list = List.of((JCTree) jcMethod);
            } else {
                list = list.append((JCTree) jcMethod);
            }
        }
        return list;
    }

    private void addImport(JCTree.JCClassDecl thisClass, JCTree.JCMethodDecl methodDecl) {
        TreeNode[] restype = ElementUtil.analyze(methodDecl.restype);
        if (restype != null) {
            for (TreeNode node : restype) {
                addImport(thisClass, node.pack.fullname, node.name);
            }
        }

        for (JCTree.JCVariableDecl param : methodDecl.params) {
            TreeNode[] params = ElementUtil.analyze(param.vartype);
            if (params != null) {
                for (TreeNode node : params) {
                    addImport(thisClass, node.pack.fullname, node.name);
                }
            }
        }

        for (JCTree.JCExpression expression : methodDecl.thrown) {
            TreeNode[] treeNodes = ElementUtil.analyze(expression);
            if (treeNodes != null) {
                for (TreeNode node : treeNodes) {
                    addImport(thisClass, node.pack.fullname, node.name);
                }
            }
        }
    }

    /**
     * 实现空方法
     * @param jcMethodDecl 方法
     * @return 方法元素
     */
    private JCTree.JCMethodDecl createJCMethod(JCTree.JCMethodDecl jcMethodDecl) {
        JCTree.JCModifiers mod = treeMaker.Modifiers(Flags.PUBLIC);

        List<JCTree.JCVariableDecl> params = null;
        for (JCTree.JCVariableDecl param : jcMethodDecl.params) {
            JCTree.JCModifiers mods = treeMaker.Modifiers(param.mods.flags);
            JCTree.JCVariableDecl varDef = treeMaker.VarDef(mods, param.name, param.vartype, param.init);
//            varDef.sym = param.sym;
            if (params == null) {
                params = List.of(varDef);
            } else {
                params = params.append(varDef);
            }
        }

        if (params == null) {
            params = List.nil();
        }

        return treeMaker.MethodDef(mod,
                jcMethodDecl.name,
                jcMethodDecl.restype,
                jcMethodDecl.typarams,
                params,
                jcMethodDecl.thrown,
                emptyBody(jcMethodDecl.restype), null);
    }

    private JCTree.JCBlock emptyBody(JCTree.JCExpression restype) {
        ListBuffer<JCTree.JCStatement> statements = TreeUtil.newStatement();
        JCTree.JCReturn returnStatement = staticMethod();
        statements.add(returnStatement);
        return syntaxTreeMaker.getBlock(statements);
    }

    private JCTree.JCReturn staticMethod() {
        JCTree.JCExpression code = syntaxTreeMaker.newElement(TypeTag.INT, -1);
        JCTree.JCExpression msg = syntaxTreeMaker.newElement(TypeTag.CLASS, "Remote invoke error");
        List<JCTree.JCExpression> paramType = List.of(code, msg);
        JCTree.JCExpression clazzType = syntaxTreeMaker.findClass("so.dian.fuxi.result.BizResult");
        JCTree.JCExpression method = syntaxTreeMaker.getMethod(clazzType, "error", paramType);
        return treeMaker.Return(method);
    }
}
