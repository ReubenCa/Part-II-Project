package com.carolang.asttolambdatree.exceptions;

import com.carolang.common.SourceFilePosition;

public class UnrecognisedVariableException extends VisitorException {
    
    public UnrecognisedVariableException(String varName, SourceFilePosition pos) {
        super(pos);
        setErrorForUser(String.format("Unrecognised Variable '%s'", varName));
    }
}
