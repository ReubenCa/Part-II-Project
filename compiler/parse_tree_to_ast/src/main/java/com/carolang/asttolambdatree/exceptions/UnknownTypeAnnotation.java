package com.carolang.asttolambdatree.exceptions;

import com.carolang.common.SourceFilePosition;

public class UnknownTypeAnnotation extends  VisitorException {

    public UnknownTypeAnnotation(SourceFilePosition pos) {
        super(pos);
        setErrorForUser("Unknown type annotation");
    }
    
}
