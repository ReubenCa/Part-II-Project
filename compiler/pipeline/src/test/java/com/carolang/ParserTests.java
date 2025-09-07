package com.carolang;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import com.carolang.frontend.carolangLexer;
import com.carolang.frontend.carolangParser;


public class ParserTests {
    @Test
    public void Parse1() {
        assertFalse(parseString("Hello World"));
    }
    
    @Test
    public void Parse2() {
        assertTrue(parseString("(fun x -> x*x) 5"));
    }

    @Test
    public void Parse3() {
        assertTrue(parseString("(fun var -> if (==) var 10 then (+) 3 3 else (fun x -> (*) x x) 5)"));
    }

    @Test 
    public void Parse4()
    {
        assertTrue(parseString("let x = 5 in x "));  
    }

    @Test
    public void Parse5()
    {
        assertTrue(parseString("let x = 5 in let y = 10 in (+) x y"));
    }

    @Test
    public void Parse6()
    {
        assertTrue(parseString("let prepend = (fun lis -> (fun x -> x::lis)) in prepend [1;2;3] 4"));
    }

  

    private boolean parseString(String input) {
        CharStream inputStream = CharStreams.fromString(input);
        carolangLexer lexer = new carolangLexer(inputStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new FailFastErrorListener());
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        carolangParser parser = new carolangParser(tokens);

        parser.removeErrorListeners(); 
        parser.addErrorListener(new FailFastErrorListener());

        try {
            ParseTree tree = parser.carolang();
            System.out.println(tree.toStringTree(parser)); 
            return true;
        } catch (ParseCancellationException e) {
            return false;
        }
    }

   
    private class FailFastErrorListener extends BaseErrorListener {
     
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }

     
}

}
