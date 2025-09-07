package com.carolang;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import com.carolang.asttolambdatree.lambdaTreeVisitor;
import com.carolang.common.types.FunctionType;
import com.carolang.common.types.Type;
import com.carolang.common.exceptions.MalformedProgramException;
import com.carolang.common.ast_nodes.Node;
import com.carolang.frontend.carolangLexer;
import com.carolang.frontend.carolangParser;


public class TypeInferenceTests {
    
    static Type inferType(String program) throws MalformedProgramException
    {
        Node lambdaTree=  MakeTypeAnnotatedLambdaTree(program);
        return lambdaTree.getType();
    }

    static Node MakeTypeAnnotatedLambdaTree(String program) throws MalformedProgramException
    {
        CharStream inputStream = CharStreams.fromString(program);
        carolangLexer lexer = new carolangLexer(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        carolangParser parser = new carolangParser(tokens);
        ParseTree tree = parser.expression();
        System.out.println(tree.toStringTree(parser));
        lambdaTreeVisitor visitor = new lambdaTreeVisitor();
        Node lambdaTree;
        try {
            lambdaTree = visitor.visit(tree);
            
        } catch (lambdaTreeVisitor.visitorExceptionWrapper e) {
            throw e.getInner();
        }
        TypeInferer.InferTypes(lambdaTree);
        return lambdaTree;
    }

    @Test
    public void test1() throws MalformedProgramException
    {
        Type type = inferType("5");
        assertTrue(type.equals( Type.Int));
    }

    @Test
    public void test2() throws MalformedProgramException
    {
        Type type = inferType("(fun x->3) 5");
        assertTrue(type.equals( Type.Int));
    }

    @Test
    public void test3() throws MalformedProgramException
    {
        Type type = inferType("let x = 5 in x");
        assertTrue(type.equals( Type.Int));
    }

    @Test
    public void test4() throws MalformedProgramException
    { 
        Type type = inferType("(+) 1 2");
        assertTrue(type.equals( Type.Int));
    }

    @Test
    public void test5() throws MalformedProgramException
    { 
        Type type = inferType("fun x -> (+) x 1");
        assertTrue(type.equals(new FunctionType(Type.Int, Type.Int)));
    }

    @Test
    public void test6() throws MalformedProgramException
    { 
        Type type = inferType("let f = (fun x-> (+) x 1) in f 5");
        assertTrue(type.equals( Type.Int));
    }

    @Test
    public void test7() throws MalformedProgramException
    {
        Type type = inferType("(+) 1");
        assertTrue(type.equals(new FunctionType(Type.Int, Type.Int)));
    }

    @Test
    public void test8() throws MalformedProgramException
    {
        Type type = inferType("fun x-> ((fun f -> f x 1) (+))");
        assertTrue(type.equals( new FunctionType(Type.Int, Type.Int)));
    }

    @Test
    public void test9() throws MalformedProgramException
    {
        assertThrows(TypeException.class, () -> inferType( "let rec f = (fun x -> f) in f 5"));
    }


    //Type Checking in Ocaml doesn't terminate for this program so I'll let Caro off
    // @Test
    // public void test10() throws MalformedProgramException
    // {
    //     Type type = inferType("let rec f = (fun x -> f x) in f 5");
    //     //(fun f -> f 5) (fun x -> (*) x)
    //     assertTrue(type.equals( Type.Int));
    // }

    @Test
    public void test10() throws MalformedProgramException
    {
        assertThrows(TypeException.class, () -> inferType("fun x -> x"));
    }

    @Test 
    void test11() throws MalformedProgramException
    {
        Type type = inferType("fun x:int -> x");
        assertTrue(type.equals(new FunctionType(Type.Int, Type.Int)));
    }

    @Test 
    void test12() throws MalformedProgramException
    {
        Type type = inferType("fun x:(int -> int) -> x");
        assertTrue(type.equals(new FunctionType(new FunctionType(Type.Int, Type.Int),new FunctionType(Type.Int, Type.Int))));
    }

    @Test 
    void test13() throws MalformedProgramException
    {
        Type type = inferType("let identity = (fun x->x) in identity 1");
        assertTrue(type.equals(Type.Int));
    }

    @Test 
    void test14() throws MalformedProgramException
    {
        Type type = inferType("if ((==) ((-) (( * ) 5 5) ((/) 10 2)) 20) then 5 else 6");
        assertTrue(type.equals(Type.Int));
    }

    @Test 
    void test15() throws MalformedProgramException
    {
        Type type = inferType("if ((==) 15 20) then 5 else 6");
        assertTrue(type.equals(Type.Int));
    }
    
}
