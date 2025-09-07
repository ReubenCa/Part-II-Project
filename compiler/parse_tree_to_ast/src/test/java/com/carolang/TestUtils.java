package com.carolang;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.carolang.asttolambdatree.lambdaTreeVisitor;
import com.carolang.asttolambdatree.exceptions.VisitorException;
import com.carolang.common.ast_nodes.Node;
import com.carolang.frontend.carolangLexer;
import com.carolang.frontend.carolangParser;

public class TestUtils {
    static carolangParser parseString(String input) {
        CharStream inputStream = CharStreams.fromString(input);
        carolangLexer lexer = new carolangLexer(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        carolangParser parser = new carolangParser(tokens);
        return parser;
    }

    static Node getLambdaTree(carolangParser parser, ParseTree tree) throws VisitorException {
        System.out.println(tree.toStringTree(parser));
        lambdaTreeVisitor visitor = new lambdaTreeVisitor();
        try {
            Node lambdaTree = visitor.visit(tree);
            return lambdaTree;
        } catch (lambdaTreeVisitor.visitorExceptionWrapper e) {
            throw e.getInner();
        }

    }

    public static Node stringToHoistedLambdaTree(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stringToHoistedLambdaTree'");
    }

}
