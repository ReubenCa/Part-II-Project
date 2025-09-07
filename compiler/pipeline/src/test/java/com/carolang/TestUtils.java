package com.carolang;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.carolang.asttolambdatree.App;
import com.carolang.common.exceptions.MalformedProgramException;
import com.carolang.common.ast_nodes.Node;
import com.carolang.frontend.carolangLexer;
import com.carolang.frontend.carolangParser;

public class TestUtils {
    static Node stringToLambdaTree(String program) throws MalformedProgramException
    {
        CharStream inputStream = CharStreams.fromString(program /*+ "\u001a"*/);
        carolangLexer lexer = new carolangLexer(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        carolangParser parser = new carolangParser(tokens);
        ParseTree tree = parser.carolang();
        Node lambdaTree = App.ASTtoLambdaTree(tree);
        TypeInferer.InferTypes(lambdaTree);
        System.out.println("Type: %s".formatted(lambdaTree.getType()));
        return lambdaTree;
    }

    static Node stringToHoistedLambdaTree(String program) throws MalformedProgramException
    {
        Node lambdaTree = stringToLambdaTree(program);
        return lambdaTree;
    }
}
