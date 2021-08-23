package io.moyada.feign.help.entity;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;


/**
 * @author honghai
 * @date 2021-08-23 11:26
 */
public class TreeNode {

    public final Symbol.PackageSymbol pack;
    public final Name name;

    private TreeNode(Symbol.PackageSymbol pack, Name name) {
        this.pack = pack;
        this.name = name;
    }

    public static TreeNode of(Symbol.PackageSymbol pack, Name name) {
        return new TreeNode(pack, name);
    }

    public static TreeNode[] asArr(TreeNode... nodes) {
        return nodes;
    }
}
