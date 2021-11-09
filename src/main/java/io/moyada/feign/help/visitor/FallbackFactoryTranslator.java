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

        printer.info(jcClassDecl.toString());
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
        JCTree.JCExpression newExp = syntaxTreeMaker.NewClass(name, List.<JCTree.JCExpression>nil());
        JCTree.JCReturn state = treeMaker.Return(newExp);
        statements.add(state);
        return syntaxTreeMaker.getBlock(statements);
    }

    private void updateFeignClient(JCTree.JCClassDecl interClass) {
        for (JCTree.JCAnnotation anno : interClass.mods.annotations) {
            String className = anno.annotationType.type.toString();
            if (!className.equals("org.springframework.cloud.openfeign.FeignClient")) {
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

                    JCTree.JCFieldAccess indent =(JCTree.JCFieldAccess) assign.rhs;

                    printer.info(indent.name.toString());
                    printer.info(indent.type.toString());
                    printer.info(indent.selected.toString());
                    printer.info(indent.selected.getClass().toString());
                    printer.info(indent.toString());

                    printer.info("sym ====");
                    // [INFO] class com.sun.tools.javac.code.Symbol$ClassSymbol
                    printer.info(indent.sym.getClass().toString());
                    // [INFO] name class
                    printer.info("name " + indent.sym.name.toString());
                    // type class com.sun.tools.javac.code.Type$ErrorType:so.dian.azeroth.guldan.components.client.ManageSubjectClient.FallbackFactory.class
                    printer.info("type " + indent.sym.type.getClass().toString() + ":" + indent.sym.type);
                    Type.ErrorType errorType = (Type.ErrorType) indent.sym.type;
                    // [INFO] so.dian.azeroth.guldan.components.client.ManageInfoClient.FallbackFactory
                    // [INFO] so.dian.azeroth.guldan.components.client.ManageInfoClient.FallbackFactory.class
                    printer.info(errorType.getOriginalType().toString());
                    printer.info(errorType.tsym.toString());

                    // owner class com.sun.tools.javac.code.Symbol$ClassSymbol:so.dian.azeroth.guldan.components.client.ManageSubjectClient.FallbackFactory
                    printer.info("owner " + indent.sym.owner.getClass().toString() + ":" + indent.sym.owner);

                    Symbol.ClassSymbol owner = (Symbol.ClassSymbol) indent.sym.owner;
                    newSymbol(owner);

                    //[INFO] fname so.dian.azeroth.guldan.components.client.ManageInfoClient.FallbackFactory.class
                    printer.info("fname " + indent.sym.getQualifiedName().toString());
                    //[INFO] kind 63
                    printer.info("kind " + indent.sym.kind);
                    //[INFO] flags_field 1073741833
                    printer.info("flags_field" + indent.sym.flags_field);

//                    Name sname = syntaxTreeMaker.getName("class");
//
//                    new Symbol.ClassSymbol();
//                    JCTree.JCFieldAccess classIdent =
//                            treeMaker.Select(treeMaker.Ident(interClass.sym.packge()), interClass.sym.name);
//                    String fname = interClass.sym.toString() + "$FallbackFactory.class";
//                    printer.info(fname);
//
//                    Symbol.ClassSymbol symbol = syntaxTreeMaker.getTypeElement(fname);
//                    printer.info("" + (symbol == null));

//                    Name infer = syntaxTreeMaker.getName(interClass.name.toString() + ".FallbackFactory.class");
//                    JCTree.JCExpression select = treeMaker.Ident(infer);
//                    assign.rhs = select;
//                    printer.info("  >  ");
//
////                    printer.info(select.name.toString());
////                    printer.info(select.sym.toString());
////                    printer.info(select.type.toString());
////                    printer.info(select.selected.toString());
//                    printer.info(select.toString());

                    printer.info("-----------");
                }
            }

            if (!hasName) {
                JCTree.JCIdent l = treeMaker.Ident(syntaxTreeMaker.getName("fallbackFactory"));
                Name infer = syntaxTreeMaker.getName(interClass.name.toString() + ".FallbackFactory.class");
                JCTree.JCExpression r = treeMaker.Ident(infer);
                JCTree.JCAssign ar = treeMaker.Assign(l, r);
                anno.args = args.append(ar);
            }
        }

    }

    private void newSymbol(Symbol.ClassSymbol owner) {
        printer.info("++^^^^^^^^^++++");
        //[INFO] name FallbackFactory
        Name sname = syntaxTreeMaker.getName("FallbackFactory");
        //[INFO] type class com.sun.tools.javac.code.Type$ErrorType:so.dian.azeroth.guldan.components.client.ManageInfoClient.FallbackFactory
        //[INFO] owner class com.sun.tools.javac.code.Symbol$ClassSymbol:so.dian.azeroth.guldan.components.client.ManageInfoClient
        //[INFO] fname so.dian.azeroth.guldan.components.client.ManageInfoClient.FallbackFactory
        printer.info("name " + owner.name.toString());
        printer.info("type " + owner.type.getClass().toString() + ":" + owner.type);
        printer.info("owner " + owner.owner.getClass().toString() + ":" + owner.owner);
        printer.info("fname " + owner.getQualifiedName().toString());
        //[INFO] kind 63
        printer.info("kind " + owner.kind);
        //[INFO] flags_field 1073741833
        printer.info("flags_field" + owner.flags_field);

        // [INFO] so.dian.azeroth.guldan.components.client.ManageInfoClient.FallbackFactory
        //[INFO] FallbackFactory
        //[INFO] so.dian.azeroth.guldan.components.client.ManageInfoClient.FallbackFactory
        //[INFO] so.dian.azeroth.guldan.components.client.ManageInfoClient
//        Symbol.ClassSymbol owner1 = new Symbol.ClassSymbol();
    }
}
