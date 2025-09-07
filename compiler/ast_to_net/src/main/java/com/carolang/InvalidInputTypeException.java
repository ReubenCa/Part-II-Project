package com.carolang;

import com.carolang.common.types.Type;
import com.carolang.common.exceptions.MalformedProgramException;

public class InvalidInputTypeException extends MalformedProgramException {

    public InvalidInputTypeException(Type t) {
        super(null);
        setErrorForUser("Cannot take input of type %s".formatted(t));
    }
    
}
