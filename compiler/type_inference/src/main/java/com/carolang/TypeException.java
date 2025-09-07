package com.carolang;

import com.carolang.common.exceptions.MalformedProgramException;

public class TypeException extends MalformedProgramException {
    public TypeException() {
        super(null);
    }


    public TypeException(String message) {
        super(null);
        setErrorForUser(message);
    }
}
