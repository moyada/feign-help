package io.moyada.feign.plus.visitor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import io.moyada.feign.plus.support.SyntaxTreeMaker;
import io.moyada.feign.plus.util.TreeUtil;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ElementKind;
import javax.tools.Diagnostic;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class FallbackFactoryTranslator extends BaseTranslator {

    private Name name;
    private Symbol.ClassSymbol factorClass;
    private Symbol.MethodSymbol factorMethod;

    public FallbackFactoryTranslator(SyntaxTreeMaker syntaxTreeMaker, Messager messager, Trees trees) {
        super(syntaxTreeMaker, messager);
        this.trees = trees;

        this.name = syntaxTreeMaker.getName("Factory");

        this.factorClass = syntaxTreeMaker.getTypeElement("feign.hystrix.FallbackFactory");

        for (Symbol symbol : factorClass.getEnclosedElements()) {
            if (symbol.getKind() == ElementKind.METHOD){
                factorMethod = (Symbol.MethodSymbol) symbol;
            }
        }

//        Name name = syntaxTreeMaker.getName("feign.hystrix.FallbackFactory");
//        JCTree.JCIdent ident = treeMaker.Ident(name);
//        JCTree.JCFieldAccess select = treeMaker.Select(ident, name);
//
//        name = syntaxTreeMaker.getName("java.lang.Throwable");
//        ident = treeMaker.Ident(name);
//        JCTree.JCFieldAccess thro = treeMaker.Select(ident, name);
//
//        factorMethod = syntaxTreeMaker.getMethod(select, "create", List.of((JCTree.JCExpression) thro));
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
        messager.printMessage(Diagnostic.Kind.NOTE, jcClassDecl.name.toString());

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
        JCTree.JCMethodDecl methodDecl = newMethod(interClass);

        Name impName = importClass(interClass, "feign.hystrix", "FallbackFactory");
        JCTree.JCIdent ident = treeMaker.Ident(impName);

        List<JCTree.JCExpression> inters = List.of((JCTree.JCExpression) ident);

        // @annotaion
        Name name = importClass(interClass, "org.springframework.stereotype", "Component");
        JCTree.JCIdent bean = treeMaker.Ident(name);
        JCTree.JCAnnotation annotation = treeMaker.Annotation(bean, List.<JCTree.JCExpression>nil());
        JCTree.JCModifiers mod = treeMaker.Modifiers(Flags.PUBLIC, List.of(annotation));

        return treeMaker.ClassDef(mod,
                this.name,
                List.<JCTree.JCTypeParameter>nil(),
                null,
                inters,
                List.of((JCTree) methodDecl));
    }


    /**
     * 实现空方法
     * @param interClass 方法
     * @return 方法元素
     */
    private JCTree.JCMethodDecl newMethod(JCTree.JCClassDecl interClass) {
        Name impName = importClass(interClass, "java.lang", "Override");
        JCTree.JCIdent ov = treeMaker.Ident(impName);
        JCTree.JCAnnotation annotation = treeMaker.Annotation(ov, List.<JCTree.JCExpression>nil());
        JCTree.JCModifiers mod = treeMaker.Modifiers(Flags.PUBLIC, List.of(annotation));

        JCTree.JCIdent restype = treeMaker.Ident(interClass.sym.name);

        Symbol.VarSymbol param = factorMethod.params().head;
        JCTree.JCVariableDecl jcVariableDecl = treeMaker.Param(param.name, param.type, param.owner);

        return treeMaker.MethodDef(mod,
                factorMethod.name,
                restype,
                List.<JCTree.JCTypeParameter>nil(),
                List.of(jcVariableDecl),
                TreeUtil.emptyExpression(),
                newFactory(interClass),
                null
        );
    }

    private JCTree.JCBlock newFactory(JCTree.JCClassDecl interClass) {
        ListBuffer<JCTree.JCStatement> statements = TreeUtil.newStatement();
        String name = interClass.name.toString() + ".Fallback";
        JCTree.JCExpression newExp = syntaxTreeMaker.NewClass(name, List.<JCTree.JCExpression>nil());
        JCTree.JCReturn state = treeMaker.Return(newExp);
        statements.add(state);
        return getBlock(statements);
    }
}
