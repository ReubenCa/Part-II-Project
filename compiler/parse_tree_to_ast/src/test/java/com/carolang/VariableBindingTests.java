package com.carolang;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import com.carolang.asttolambdatree.exceptions.UnrecognisedVariableException;
import com.carolang.asttolambdatree.exceptions.VisitorException;
import com.carolang.common.ast_nodes.FunctionArgumentNode;
import com.carolang.common.ast_nodes.FunctionNode;
import com.carolang.common.ast_nodes.IntegerNode;
import com.carolang.common.ast_nodes.LambdaNode;
import com.carolang.common.ast_nodes.Node;
import com.carolang.frontend.carolangParser;

public class VariableBindingTests {
    // Used Github Copilot to help generate more test cases as it can't really hurt
    // to have more coverage
    @Test
    void test1() throws VisitorException {
        carolangParser parser = TestUtils.parseString("let x = 5 in x");
        ParseTree tree = parser.expression();
        Node lambdaTree = TestUtils.getLambdaTree(parser, tree);

        assertTrue(lambdaTree instanceof LambdaNode);
        assertTrue(((LambdaNode)lambdaTree).getArgument() instanceof IntegerNode iNode && iNode.getValue() == 5);
        assertTrue(((LambdaNode)lambdaTree).getFunction() instanceof FunctionNode);
        FunctionNode fNode = (FunctionNode) ((LambdaNode)lambdaTree).getFunction();
        assertTrue(fNode.getDefinition() instanceof FunctionArgumentNode fArgNode && fArgNode.getDefiningFunction() == fNode);


    }

    @Test
    void test2() {
        assertUnrecognisedVariable("let x = 5 in x+y");
    }



    @Test
    void test3() {
        assertUnrecognisedVariable("if (==) 1 1 then (let x = 5 in x) else x");
    }

    @Test
    void test4() {
        assertUnrecognisedVariable("if (==) 1 1 then (let x = 5 in x) else x");
    }

    @Test
    void test5() {
        assertUnrecognisedVariable("let x = y in x");
    }
    
    @Test
    void test6() {
        assertUnrecognisedVariable("let f = f x in f");
    }

    @Test
    void test7() {
        assertUnrecognisedVariable("(+) y (let y = 5 in let rec f = f x in f y)");
    }
    // @Test
    // void test6() throws VisitorException {
    //     carolangParser parser = TestUtils.parseString("let y = 6 in let x = 5 in (+) x y");
    //     ParseTree tree = parser.expression();
    //     Node lambdaTree = TestUtils.getLambdaTree(parser, tree);

    //TODO

    // }

    private void assertUnrecognisedVariable(String program) {
        carolangParser parser = TestUtils.parseString(program);
        ParseTree tree = parser.expression();
        assertThrows(UnrecognisedVariableException.class, () -> TestUtils.getLambdaTree(parser, tree));

    }

}
