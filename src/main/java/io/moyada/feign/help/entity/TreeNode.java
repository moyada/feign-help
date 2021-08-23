package io.moyada.feign.help.entity;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;

/**
 * 引用节点
 * @author xueyikang
 * @since 1.0
 **/
public class TreeNode {

    public final Symbol.PackageSymbol pack;
    public final Name name;

    private TreeNode(Symbol.PackageSymbol pack, Name name) {
        this.pack = pack;
        this.name = name;
    }

    public static TreeNode of(Symbol.PackageSymbol pack, Name name) {
        if (pack == null) {
            return null;
        }
        if (pack.fullname.isEmpty()) {
            return null;
        }
        return new TreeNode(pack, name);
    }

    public static TreeNode[] asArr(TreeNode... nodes) {
        return nodes;
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "pack=" + pack +
                ", name=" + name +
                '}';
    }
}
