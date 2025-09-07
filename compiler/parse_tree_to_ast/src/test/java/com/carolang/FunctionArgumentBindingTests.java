package com.carolang;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import com.carolang.asttolambdatree.exceptions.VisitorException;
import com.carolang.common.ast_nodes.FunctionArgumentNode;
import com.carolang.common.ast_nodes.FunctionNode;
import com.carolang.common.ast_nodes.IntegerNode;
import com.carolang.common.ast_nodes.LambdaNode;
import com.carolang.common.ast_nodes.MagicNode;
import com.carolang.common.ast_nodes.MagicNodeTag;
import com.carolang.common.ast_nodes.Node;
import com.carolang.common.ast_nodes.RecursiveReferenceNode;
import com.carolang.frontend.carolangParser;

public class FunctionArgumentBindingTests {
    @Test
    void test1() throws VisitorException {
        carolangParser parser = TestUtils.parseString("fun x -> (+) x x");
        ParseTree tree = parser.expression();
        Node lambdaTree = TestUtils.getLambdaTree(parser, tree);
        assertTrue(lambdaTree instanceof FunctionNode);

        Node lambda = ((FunctionNode) lambdaTree).getDefinition();
        assertTrue(lambda instanceof LambdaNode);

        Node var1 = ((LambdaNode) lambda).getChild(1);
        assertTrue(var1 instanceof FunctionArgumentNode f && f.getDefiningFunction() == lambdaTree);

        Node lowerLambda = ((LambdaNode) lambda).getChild(0);
        assertTrue(lowerLambda instanceof LambdaNode);
        Node var2 = ((LambdaNode) lowerLambda).getChild(1);
        assertTrue(var2 instanceof FunctionArgumentNode f2 && f2.getDefiningFunction() == lambdaTree);

    }

    @Test
    void test2() throws VisitorException {
        carolangParser parser = TestUtils.parseString("fun x -> (fun y -> (+) x y)");
        ParseTree tree = parser.expression();
        Node outerFunction = TestUtils.getLambdaTree(parser, tree);
        assertTrue(outerFunction instanceof FunctionNode);

        Node innerFunction = ((FunctionNode) outerFunction).getDefinition();
        assertTrue(innerFunction instanceof FunctionNode);

        Node outerLambda = ((FunctionNode) innerFunction).getDefinition();
        assertTrue(outerLambda instanceof LambdaNode);

        Node innerLambda = ((LambdaNode) outerLambda).getChild(0);
        assertTrue(innerLambda instanceof LambdaNode);

        Node var2 = ((LambdaNode) innerLambda).getFunction();
        assertTrue(var2 instanceof MagicNode m && m.getTag() == MagicNodeTag.PLUS_INT);

        // assertTrue((LambdaNode)var2).getArgument() instanceof FunctionArgumentNode
        // fvar2 && fvar2.getDefiningFunction() == innerFunction);
    }

    @Test
    void test3() throws VisitorException
    {
        carolangParser parser = TestUtils.parseString("let rec f = (fun x -> (f x)) in f ");
        ParseTree tree = parser.expression();
        Node outerVariable = TestUtils.getLambdaTree(parser, tree);
        assertTrue(outerVariable instanceof LambdaNode);
        LambdaNode root = (LambdaNode) outerVariable;
        assertTrue(root.getFunction() instanceof FunctionNode);
        FunctionNode f = (FunctionNode) root.getFunction();
        assertTrue(f.getDefinition() instanceof FunctionArgumentNode fNode && fNode.getDefiningFunction() == f);

        assertTrue(((LambdaNode)root).getArgument() instanceof FunctionNode);
        FunctionNode rightFunction = (FunctionNode) ((LambdaNode)root).getArgument();
        assertTrue(rightFunction.getDefinition() instanceof LambdaNode);
        LambdaNode rightLambda = (LambdaNode) rightFunction.getDefinition();
        assertTrue(rightLambda.getFunction() instanceof RecursiveReferenceNode rNode && rNode.getDefinition() == rightFunction);
        assertTrue(rightLambda.getArgument() instanceof FunctionArgumentNode fNode2 && fNode2.getDefiningFunction() == rightFunction);

    }

    @Test
    void test4() throws VisitorException
    {
        carolangParser parser = TestUtils.parseString("let one = 1 in one");
        ParseTree tree = parser.expression();
        Node root = TestUtils.getLambdaTree(parser, tree);
        assertTrue(root instanceof LambdaNode);
        Node function = ((LambdaNode)root).getFunction();
        assertTrue(function instanceof FunctionNode);
        assertTrue(((FunctionNode)function).getDefinition() instanceof FunctionArgumentNode faNode && faNode.getDefiningFunction() == function);
        assertTrue(((LambdaNode)root).getArgument() instanceof IntegerNode iNode && iNode.getValue() == 1);
    }

    @Test
    void test5() throws VisitorException
    {
        carolangParser parser = TestUtils.parseString("let id = (fun x->x) in id 1");
        //Should expand to (fun f -> f 1) (fun x -> x) 
        ParseTree tree = parser.expression();
        Node root = TestUtils.getLambdaTree(parser, tree);
        assertTrue(root instanceof LambdaNode);
        LambdaNode lroot = (LambdaNode)root;
        assertTrue(lroot.getFunction() instanceof FunctionNode);
        FunctionNode leftFunction = (FunctionNode)lroot.getFunction();
        assertTrue(leftFunction.getDefinition() instanceof LambdaNode);
        LambdaNode leftFuncDef = (LambdaNode)leftFunction.getDefinition();
        assertTrue(leftFuncDef.getFunction() instanceof FunctionArgumentNode);
        assertTrue(leftFuncDef.getArgument() instanceof IntegerNode iNode && iNode.getValue() == 1);

        assertTrue(lroot.getArgument() instanceof FunctionNode);
        FunctionNode identityFunc = (FunctionNode)lroot.getArgument();
        assertTrue(identityFunc.getDefinition() instanceof FunctionArgumentNode faNode && faNode.getDefiningFunction() == identityFunc);


    }
}
