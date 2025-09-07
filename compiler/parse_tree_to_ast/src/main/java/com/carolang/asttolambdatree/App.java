package com.carolang.asttolambdatree;

import org.antlr.v4.runtime.tree.ParseTree;

import com.carolang.asttolambdatree.lambdaTreeVisitor.visitorExceptionWrapper;
import com.carolang.common.exceptions.MalformedProgramException;
import com.carolang.common.ast_nodes.Node;

/**
 * Hello world!
 */
public class App {

    public static Node ASTtoLambdaTree(ParseTree tree) throws MalformedProgramException {
        try {
            lambdaTreeVisitor visitor = new lambdaTreeVisitor();
            Node lambdaTree = visitor.visit(tree);
            return lambdaTree;
        } catch (visitorExceptionWrapper e) {
            throw e.getInner();
        }

    }
}
