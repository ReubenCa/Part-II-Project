package com.carolang.asttolambdatree.exceptions;

import com.carolang.common.SourceFilePosition;

public class NonFunctionRecursiveDefinitionException extends VisitorException{
     public NonFunctionRecursiveDefinitionException(String varName, SourceFilePosition pos) {
        super(pos);
        setErrorForUser(String.format("Recursive Definition '%s' of Non-function is not allowed", varName));
    }
}
