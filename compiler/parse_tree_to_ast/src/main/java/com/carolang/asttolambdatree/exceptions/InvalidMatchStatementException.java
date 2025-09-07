package com.carolang.asttolambdatree.exceptions;

import com.carolang.common.SourceFilePosition;

public class InvalidMatchStatementException extends VisitorException{

    public InvalidMatchStatementException(SourceFilePosition pos, String error) {
        super(pos);
        setErrorForUser(error);
    }
    
}
