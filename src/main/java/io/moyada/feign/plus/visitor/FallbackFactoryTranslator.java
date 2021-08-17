package io.moyada.feign.plus.visitor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import io.moyada.feign.plus.support.SyntaxTreeMaker;

import javax.annotation.processing.Messager;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class FallbackFactoryTranslator extends BaseTranslator {

    private Trees trees;

    public FallbackFactoryTranslator(SyntaxTreeMaker syntaxTreeMaker, Messager messager, Trees trees) {
        super(syntaxTreeMaker, messager);
        this.trees = trees;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        super.visitClassDef(jcClassDecl);
    }
}
