package com.daddyno1.apt_compiler;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;

/**
 * https://liuyehcf.github.io/2018/02/02/Java-JSR-269-%E6%8F%92%E5%85%A5%E5%BC%8F%E6%B3%A8%E8%A7%A3%E5%A4%84%E7%90%86%E5%99%A8/
 */
public class ASTVisitor extends TreeTranslator {

    /**
     * visitModifiers:  访问标志语法树节点
     * visitClassDef:   类定义语法树节点
     * visitMethodDef:  方法定义语法树节点
     * visitVarDef:     字段/变量定义语法树节点 : field / local var / param
     * visitIdent:      标识符语法树节点
     * visitReturn:     return语句语法树节点
     * visitLiteral:    常量语法树节点
     * visitSelect:     域访问/方法访问语法树节点（方法调用、变量引用，所有带 . 的）
     * visitBlock:      代码块语法树节点   { }
     * visitApply:      方法调用语法树节点
     * visitNewClass:   new语句语法树节点
     * visitAssign:     赋值语句语法树节点
     * visitExec:       可执行语句 语法树节点
     */
}
