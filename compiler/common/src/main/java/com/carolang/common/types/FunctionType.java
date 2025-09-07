package com.carolang.common.types;

public class FunctionType extends Type {
    public final Type argumentType;
    public final Type outputType;

    public FunctionType(Type argumentType, Type outputType) {
        this.argumentType = argumentType;
        this.outputType = outputType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FunctionType that = (FunctionType) obj;
        return argumentType.equals(that.argumentType) && outputType.equals(that.outputType);
    }

    @Override
    public int hashCode() {
        return argumentType.hashCode() ^ outputType.hashCode();
    }
}
