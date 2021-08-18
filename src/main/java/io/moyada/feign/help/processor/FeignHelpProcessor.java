package io.moyada.feign.help.processor;


import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import io.moyada.feign.help.support.SyntaxTreeMaker;
import io.moyada.feign.help.util.ClassUtil;
import io.moyada.feign.help.util.ElementUtil;
import io.moyada.feign.help.visitor.FallbackFactoryTranslator;
import io.moyada.feign.help.visitor.FallbackTranslator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * 校验注解处理器
 * @author xueyikang
 * @since 0.0.1
 **/
@SupportedAnnotationTypes("io.moyada.feign.help.annotation.*")
public class FeignHelpProcessor extends AbstractProcessor {

    // 处理器上下文
    private Context context;
    // 文件处理器
    private Filer filer;
    // 语法树
    private Trees trees;
    // 信息输出体
    private Messager messager;

    public FeignHelpProcessor() {
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

        messager.printMessage(Diagnostic.Kind.NOTE, "Running Feign Help Processor.");
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

        TreeTranslator translator = new FallbackTranslator(trees, syntaxTreeMaker, messager);
        for (JCTree.JCClassDecl fallbackEle : fallbackEles) {
            fallbackEle.accept(translator);
        }

        translator = new FallbackFactoryTranslator(trees, syntaxTreeMaker, messager);
        for (JCTree.JCClassDecl factoryEle: factoryEles) {
            factoryEle.accept(translator);
        }
        return true;
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
