package io.moyada.feign.help.visitor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import io.moyada.feign.help.annotation.FallbackFactoryBuild;
import io.moyada.feign.help.constant.ClassName;
import io.moyada.feign.help.support.Printer;
import io.moyada.feign.help.support.SyntaxTreeMaker;
import io.moyada.feign.help.util.TreeUtil;

import javax.lang.model.element.ElementKind;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class FallbackFactoryTranslator extends BaseTranslator {

    private final Name name;
    private Symbol.MethodSymbol factorMethod;

    public FallbackFactoryTranslator(Trees trees, SyntaxTreeMaker syntaxTreeMaker, Printer printer) {
        super(trees, syntaxTreeMaker, printer);
        this.name = syntaxTreeMaker.getName(ClassName.FACTORY_NAME);

        Symbol.ClassSymbol factorClass = syntaxTreeMaker.getTypeElement("feign.hystrix.FallbackFactory");
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

        JCTree.JCClassDecl classDecl = createClass(jcClassDecl);
        super.appendClass(jcClassDecl, classDecl);
        // 修改@FeignClient注解
        updateFeignClient(jcClassDecl);
    }

    /**
     * 创建类
     * @param interClass 接口
     * @return 方法元素
     */
    private JCTree.JCClassDecl createClass(JCTree.JCClassDecl interClass) {
        JCTree.JCMethodDecl methodDecl = newMethod(interClass);

        Name pkg = syntaxTreeMaker.getName("feign.hystrix");
        Name infer = syntaxTreeMaker.getName("FallbackFactory");
        JCTree.JCFieldAccess select = treeMaker.Select(treeMaker.Ident(pkg), infer);
        List<JCTree.JCExpression> inters = List.of((JCTree.JCExpression) select);

        // @annotaion
        String value = TreeUtil.getAnnotationValue(interClass.sym, FallbackFactoryBuild.class.getName(), "bean()");
        JCTree.JCModifiers mod;
        if (value == null || value.equals("true")) {
            Name name = addImport(interClass, "org.springframework.stereotype", "Component");
            JCTree.JCIdent bean = treeMaker.Ident(name);
            JCTree.JCAnnotation annotation = treeMaker.Annotation(bean, List.<JCTree.JCExpression>nil());
            mod = treeMaker.Modifiers(Flags.PUBLIC, List.of(annotation));
        } else {
            mod = treeMaker.Modifiers(Flags.PUBLIC);
        }

        return treeMaker.ClassDef(mod,
                this.name,
                List.<JCTree.JCTypeParameter>nil(),
                null,
                inters,
                List.of((JCTree) methodDecl));
    }

    /**
     * 实现create方法
     * @param interClass 接口名
     * @return 方法元素
     */
    private JCTree.JCMethodDecl newMethod(JCTree.JCClassDecl interClass) {
        Name pkg = syntaxTreeMaker.getName("java.lang");
        Name name = syntaxTreeMaker.getName("Override");
        JCTree.JCIdent ident = treeMaker.Ident(pkg);
        JCTree.JCFieldAccess select = treeMaker.Select(ident, name);
        JCTree.JCAnnotation annotation = treeMaker.Annotation(select, List.<JCTree.JCExpression>nil());

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
        String name = interClass.name.toString() + "." + ClassName.BEAN_NAME;
        JCTree.JCExpression newExp = syntaxTreeMaker.NewObject(name, List.<JCTree.JCExpression>nil());
        JCTree.JCReturn state = treeMaker.Return(newExp);
        statements.add(state);
        return syntaxTreeMaker.getBlock(statements);
    }

    private void updateFeignClient(JCTree.JCClassDecl interClass) {
        for (JCTree.JCAnnotation anno : interClass.mods.annotations) {
            String className = anno.annotationType.type.toString();
            if (!className.equals("org.springframework.cloud.openfeign.FeignClient") &&
                    !className.equals("org.springframework.cloud.netflix.feign.FeignClient")) {
                continue;
            }

            boolean hasName = false;
            List<JCTree.JCExpression> args = anno.args;
            for (JCTree.JCExpression arg : args) {
                if (arg instanceof JCTree.JCAssign) {
                    JCTree.JCAssign assign = (JCTree.JCAssign) arg;
                    String name = assign.lhs.toString();
                    if (!name.equals("fallbackFactory")) {
                        continue;
                    }
                    hasName = true;
                    assign.rhs = newSymbol(interClass);
                }
            }

            if (!hasName) {
                JCTree.JCIdent l = treeMaker.Ident(syntaxTreeMaker.getName("fallbackFactory"));
                JCTree.JCFieldAccess r = newSymbol(interClass);
                JCTree.JCAssign ar = treeMaker.Assign(l, r);
                anno.args = args.append(ar);
            }
        }

    }

    /**
     * 创建类中FallbackFactory的class对象引用
     * @param interClass 父类
     * @return class引用
     */
    private JCTree.JCFieldAccess newSymbol(JCTree.JCClassDecl interClass) {
        // FallbackFactory 类符号
        Symbol.ClassSymbol factorySymbol = syntaxTreeMaker.newClassSymbol(interClass.sym, ClassName.FACTORY_NAME);
        // FallbackFactory.class 类符号
        Symbol.ClassSymbol factoryClassSymbol = syntaxTreeMaker.newClassSymbol(factorySymbol, "class");

        // FallbackFactory 引用
        JCTree.JCFieldAccess classIdent = treeMaker.Select(treeMaker.Ident(interClass.name), syntaxTreeMaker.getName(ClassName.FACTORY_NAME));
        Type.ErrorType ftype = new Type.ErrorType(factorySymbol, syntaxTreeMaker.symtab.errType);
        classIdent.setType(ftype);
        classIdent.sym = factorySymbol;

        // FallbackFactory.class 引用
        Name fname = syntaxTreeMaker.getName("class");
        JCTree.JCFieldAccess select = treeMaker.Select(classIdent, fname);
        Type.ErrorType errType = new Type.ErrorType(ftype, factoryClassSymbol);
        select.setType(errType);
        return select;
    }
}
