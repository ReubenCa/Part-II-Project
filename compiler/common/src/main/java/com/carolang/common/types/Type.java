package com.carolang.common.types;

import java.util.LinkedList;
import java.util.List;

public class Type  {
    public final static Type Int = new Type();
    public final static Type Float = new Type();
    public final static Type Boolean = new Type();
    public final static Type Unit = new Type();

    public final static Type getUnboundType()
    {
        return new Type();
    }

    Type()
    {

    }

    public static boolean isBound(Type t)
    {
        if(t == Int || t == Float || t == Boolean || t == Unit)
        {
            return true;
        }
        if(t instanceof FunctionType fType)
        {
            return isBound(fType.argumentType) && isBound(fType.outputType);
        }
        if(t instanceof ListType lType)
        {
            return isBound(lType.elementType);
        }
        return false;
    }

    public int getTopLevelArrowCount()
    {
        if(this instanceof FunctionType fType)
        {
           return 1 + fType.outputType.getTopLevelArrowCount();
        }
        return 0;

    }

    public List<Type> typeChain()
    {
        return typeChain_(this);
    }

    private LinkedList<Type> typeChain_(Type t)
    {
        if(t instanceof FunctionType fType)
        {
            Type currentType = fType.argumentType;
            LinkedList<Type> restOfChain = typeChain_(fType.outputType);
            restOfChain.addFirst(currentType);
            return restOfChain;
        }
        LinkedList<Type> chain = new LinkedList<>();
        chain.add(t);
        return chain;
    }

    @Override
    public String toString()
    {
        if(this == Int)
        {
            return "Int";
        }
        if(this == Float)
        {
            return "Float";
        }
        if(this == Boolean)
        {
            return "Boolean";
        }
        if(this == Unit)
        {
            return "Unit";
        }
        if(this instanceof FunctionType fType)
        {
            return  "(" + fType.argumentType.toString() + " -> " + fType.outputType.toString() + ")";
        }
        if(this instanceof ListType lType)
        {
            return lType.elementType.toString() + " list";
        }
        return "Unbound";
    }
}
