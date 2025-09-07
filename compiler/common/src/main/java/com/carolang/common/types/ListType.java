package com.carolang.common.types;

public class ListType extends Type{
    public final Type elementType;

    public ListType(Type elementType) {
        this.elementType = elementType;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other == null || !(other instanceof ListType)) return false;
        ListType listType = (ListType) other;
        return elementType.equals(listType.elementType);
    }

    @Override
    public int hashCode() {
        return elementType.hashCode() ^ 0x123456;
    }
}
