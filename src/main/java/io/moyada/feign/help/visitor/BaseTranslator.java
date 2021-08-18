package io.moyada.feign.help.visitor;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Name;
import io.moyada.feign.help.support.Printer;
import io.moyada.feign.help.support.SyntaxTreeMaker;

/**
 * 基础监视器
 * @author xueyikang
 * @since 1.0
 **/
abstract class BaseTranslator extends TreeTranslator {

    final Trees trees;
    final Printer printer;
    final SyntaxTreeMaker syntaxTreeMaker;
    final TreeMaker treeMaker;

    BaseTranslator(Trees trees, SyntaxTreeMaker syntaxTreeMaker, Printer printer) {
        this.trees = trees;
        this.syntaxTreeMaker = syntaxTreeMaker;
        this.printer = printer;
        this.treeMaker = syntaxTreeMaker.getTreeMaker();
    }

    /**
     * 创建类
     * @param classDecl 接口
     * @return 方法元素
     */
    protected Name importClass(JCTree.JCClassDecl classDecl, String pkg, String claaName) {
        Name fullname = syntaxTreeMaker.getName(pkg);
        Name simplename = syntaxTreeMaker.getName(claaName);

        JCTree.JCIdent fullbean = treeMaker.Ident(fullname);
        JCTree.JCFieldAccess select = treeMaker.Select(fullbean, simplename);
        JCTree.JCImport anImport = treeMaker.Import(select, false);

        TreePath treePath = trees.getPath(classDecl.sym);
        JCTree.JCCompilationUnit jccu = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();
        if (!jccu.defs.contains(select)) {
            jccu.defs = jccu.defs.append(anImport);
        }
        return simplename;
    }

    /**
     * 创建类
     * @param interClass 接口
     */
    protected void appendClass(JCTree.JCClassDecl interClass, JCTree.JCClassDecl classDecl) {
        PosScanner posScanner = new PosScanner(interClass);
        interClass.accept(posScanner);

        interClass.defs = interClass.defs.append(classDecl);
    }
}
