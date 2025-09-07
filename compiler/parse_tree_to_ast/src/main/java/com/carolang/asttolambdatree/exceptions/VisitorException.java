package com.carolang.asttolambdatree.exceptions;

import com.carolang.common.SourceFilePosition;
import com.carolang.common.exceptions.MalformedProgramException;

public abstract class VisitorException extends MalformedProgramException{
    public VisitorException(SourceFilePosition pos)
    {
        super(pos);
    }
}
