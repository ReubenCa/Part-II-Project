package com.carolang.common.exceptions;

import com.carolang.common.SourceFilePosition;

public abstract class MalformedProgramException extends Exception {
    private String errorForUser;
    private SourceFilePosition position;

    protected void setErrorForUser(String error)
    {
        errorForUser = error;
    }

    public String getError()
    {
        //TODO: better type errors that include a description and source file position would be good
        if(position != null)
        {
        return String.format("%s\n At Line: %d Column: %d", errorForUser, position.lineNumber(), position.columnNumber());
        }
        else
        {
            return errorForUser;
        }
    }
    
    public MalformedProgramException(SourceFilePosition pos)
    {
        position = pos;
    }
}
