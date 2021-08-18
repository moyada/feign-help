package io.moyada.feign.plus.processor;


import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import io.moyada.feign.plus.support.ElementOptions;
import io.moyada.feign.plus.support.SyntaxTreeMaker;
import io.moyada.feign.plus.util.ClassUtil;
import io.moyada.feign.plus.util.ElementUtil;
import io.moyada.feign.plus.visitor.FallbackFactoryTranslator;
import io.moyada.feign.plus.visitor.FallbackTranslator;
import io.moyada.feign.plus.visitor.UtilMethodTranslator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 校验注解处理器
 * @author xueyikang
 * @since 0.0.1
 **/
@SupportedAnnotationTypes("io.moyada.feign.plus.annotation.*")
public class ValidationGenerateProcessor extends AbstractProcessor {

    // 处理器上下文
    private Context context;
    // 文件处理器
    private Filer filer;
    // 语法树
    private Trees trees;
    // 信息输出体
    private Messager messager;

    public ValidationGenerateProcessor() {
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        ClassUtil.disableJava9SillyWarning();

        if (!(processingEnv instanceof JavacProcessingEnvironment)) {
            processingEnv = jbUnwrap(ProcessingEnvironment.class, processingEnv);
            System.out.println(processingEnv.getClass());
        }

        this.context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.filer = processingEnv.getFiler();
        this.trees = Trees.instance(processingEnv);
        this.messager = processingEnv.getMessager();

        messager.printMessage(Diagnostic.Kind.NOTE, "run feign plus processor");
    }

    private static <T> T jbUnwrap(Class<? extends T> iface, T wrapper) {
        T unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            unwrapped = iface.cast(unwrapMethod.invoke(null, iface, wrapper));
        }
        catch (Throwable ignored) {}
        return unwrapped != null? unwrapped : wrapper;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<JCTree.JCClassDecl> factoryEles = ElementUtil.getFallbackFactory(trees, roundEnv);
        List<JCTree.JCClassDecl> fallbackEles = ElementUtil.getFallback(trees, roundEnv, factoryEles);
        if (factoryEles.isEmpty() && fallbackEles.isEmpty()) {
            return true;
        }

        SyntaxTreeMaker syntaxTreeMaker = SyntaxTreeMaker.newInstance(context);

        TreeTranslator translator = new FallbackTranslator(syntaxTreeMaker, messager, trees);
        for (JCTree.JCClassDecl fallbackEle : fallbackEles) {
            fallbackEle.accept(translator);
        }

        translator = new FallbackFactoryTranslator(syntaxTreeMaker, messager, trees);
        for (JCTree.JCClassDecl factoryEle: factoryEles) {
            factoryEle.accept(translator);
        }
        return true;
    }

    /**
     * 创建工具方法，如指定方法则不生效
     * 根据配置选择创建新文件提供方法，或者选择一个 public class 创建工具方法
     * @param roundEnv 根环境
     * @param elements 元素集合
     * @param syntaxTreeMaker 语句构造器
     */
    private void createUtilMethod(RoundEnvironment roundEnv, Collection<? extends Element> elements, SyntaxTreeMaker syntaxTreeMaker) {
        boolean createFile = !Boolean.FALSE.toString().equalsIgnoreCase(ElementOptions.UTIL_CREATE);
        if (createFile) {
            ElementUtil.createUtil(filer, roundEnv);
            messager.printMessage(Diagnostic.Kind.NOTE, "Created util class " + ElementOptions.UTIL_CLASS);
            return;
        }

        Element classElement = ElementUtil.findFirstPublicClass(elements);
        if (classElement == null) {
            messager.printMessage(Diagnostic.Kind.ERROR, "cannot find any public class");
            return;
        }

        JCTree tree = (JCTree) trees.getTree(classElement);
        tree.accept(new UtilMethodTranslator(syntaxTreeMaker, messager, classElement.toString()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        if (SourceVersion.latest().compareTo(SourceVersion.RELEASE_6) > 0) {
            return SourceVersion.latest();
        } else {
            return SourceVersion.RELEASE_6;
        }
    }
}
