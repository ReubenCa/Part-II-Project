package com.carolang;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import com.carolang.asttolambdatree.exceptions.VisitorException;
import com.carolang.common.ast_nodes.FunctionNode;
import com.carolang.common.ast_nodes.IntegerNode;
import com.carolang.common.ast_nodes.LambdaNode;
import com.carolang.common.ast_nodes.MagicNode;
import com.carolang.common.ast_nodes.Node;
import com.carolang.frontend.carolangParser;

/**
 * Unit test for simple App.
 */
public class BasicTests {

    @Test
    public void test1() throws VisitorException {
        String inpString = "(+ 1 2)";
        carolangParser test = TestUtils.parseString(inpString);
        ParseTree tree = test.expression();
        Node lambdaTree = TestUtils.getLambdaTree(test, tree);
        assertTrue(lambdaTree instanceof LambdaNode);
        LambdaNode upperLambdaNode = (LambdaNode) lambdaTree;
        assertTrue(upperLambdaNode.getChild(0) instanceof LambdaNode);
        LambdaNode lowerLambdaNode = (LambdaNode) upperLambdaNode.getChild(0);
        assertTrue(lowerLambdaNode.getChild(0) instanceof MagicNode);
        assertTrue(lowerLambdaNode.getChild(1) instanceof IntegerNode one && one.getValue() == 1);
        assertTrue(upperLambdaNode.getChild(1) instanceof IntegerNode two && two.getValue() == 2);
    }

    @Test
    public void test2() throws VisitorException {
        carolangParser parser = TestUtils.parseString("1");
        ParseTree tree = parser.expression();
        Node lambdaTree = TestUtils.getLambdaTree(parser, tree);
        assertTrue(lambdaTree instanceof IntegerNode);
    }

    @Test
    public void test3() throws VisitorException {
        carolangParser parser = TestUtils.parseString("(+)");
        ParseTree tree = parser.expression();
        Node lambdaTree = TestUtils.getLambdaTree(parser, tree);
        assertTrue(lambdaTree instanceof MagicNode);
    }

    @Test
    public void test4() {
        carolangParser parser = TestUtils.parseString("9999999999999999999999999999999999999");
        ParseTree tree = parser.expression();
        assertThrows(NumberFormatException.class, () -> TestUtils.getLambdaTree(parser, tree));
    }

    @Test
    public void test5() throws VisitorException
    {
        carolangParser parser = TestUtils.parseString("fun x:int -> x");
        ParseTree tree = parser.expression();
        Node lambdaTree = TestUtils.getLambdaTree(parser, tree);
        assertTrue(lambdaTree instanceof FunctionNode);
    }

    @Test
    public void test6() throws VisitorException
    {
        carolangParser parser = TestUtils.parseString("fun x:(int -> int) -> x");
        ParseTree tree = parser.expression();
        Node lambdaTree = TestUtils.getLambdaTree(parser, tree);
        assertTrue(lambdaTree instanceof FunctionNode);
    }


    @Test
    public void test7() throws VisitorException
    {
        carolangParser parser = TestUtils.parseString("fun x:((int -> float) -> (int -> int)) -> x");
        ParseTree tree = parser.expression();
        Node lambdaTree = TestUtils.getLambdaTree(parser, tree);
        assertTrue(lambdaTree instanceof FunctionNode);
    }

    @Test
    public void test8() throws VisitorException
    {
        carolangParser parser = TestUtils.parseString("(==) 5 5");
        ParseTree tree = parser.expression();
        Node lambdaTree = TestUtils.getLambdaTree(parser, tree);
        assertTrue(lambdaTree instanceof LambdaNode);
    }
}
