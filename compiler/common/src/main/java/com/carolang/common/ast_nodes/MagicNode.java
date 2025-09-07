package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;
import com.carolang.common.types.FunctionType;
import com.carolang.common.types.Type;

public class MagicNode extends Node{
    //Nodes such as built in functions that can have their own rules on how they are compiled.

    private MagicNodeTag tag;

    public MagicNode(MagicNodeTag type, SourceFilePosition pos)
    {
        super(pos);
        this.tag = type;
        //Have this for now just so built ins like "+" can be used in unit tests etc
    }

    public MagicNodeTag getTag()
    {
        return tag;
    }

    @Override
    public Type getType() {
        Type IntBiFunction = new FunctionType(Type.Int, new FunctionType(Type.Int, Type.Int));
        Type TwoIntToBool = new FunctionType(Type.Int, new FunctionType(Type.Int, Type.Boolean));
        Type FloatBiFunction = new FunctionType(Type.Float, new FunctionType(Type.Float, Type.Float));
        Type FloatToBool = new FunctionType(Type.Float, new FunctionType(Type.Float, Type.Boolean));
        switch(tag)
        {
            case PLUS_INT:
            case MINUS_INT:
            case MULTIPLY_INT:
            case DIVIDE_INT:
            case MOD_INT:
            return IntBiFunction;
            case GREATER_INT:
            case GREATER_EQUALS_INT:
            case LESS_INT:
            case LESS_EQUALS_INT:
            case EQUALS_INT:
            case NOT_EQUALS_INT:
            return TwoIntToBool;
            case PLUS_FLOAT:
            case MINUS_FLOAT:
            case MULTIPLY_FLOAT:
            case DIVIDE_FLOAT:
            return FloatBiFunction;
            case EQUALS_FLOAT:
            case NOT_EQUALS_FLOAT:
            case GREATER_FLOAT:
            case GREATER_EQUALS_FLOAT:
            case LESS_FLOAT:
            case LESS_EQUALS_FLOAT:
            return FloatToBool;
            default:
            throw new RuntimeException("MagicNode type not supported: " + tag);
        }
    }

    @Override
    public void setType(Type type)
    {
        assert(type.equals(getType()));//We already know our type
    }

    public static int getAuxiliaryPortCount(MagicNodeTag type)
    {
        switch(type)
        {
            case PLUS_INT:
            case PLUS_FLOAT:
            return 2;
            case MINUS_INT:
            case MINUS_FLOAT:
            return 2;
            case MULTIPLY_INT:
            case MULTIPLY_FLOAT:
            return 2;
            case DIVIDE_INT:
            case DIVIDE_FLOAT:
            return 2;
            case MOD_INT:
            return 2;
            case GREATER_INT:
            case GREATER_FLOAT:
            return 2;
            case GREATER_EQUALS_INT:
            case GREATER_EQUALS_FLOAT:
            return 2;
            case LESS_INT:
            case LESS_FLOAT:
            return 2;
            case LESS_EQUALS_INT:
            case LESS_EQUALS_FLOAT:
            return 2;
            case EQUALS_INT:
            case EQUALS_FLOAT:
            return 2;
            case NOT_EQUALS_INT:
            case NOT_EQUALS_FLOAT:
            return 2;
            case OUTPUT:
            return 1;
            case CURRIED_PLUS_INT:
            case CURRIED_PLUS_FLOAT:
            return 1;
            case CURRIED_MINUS_INT:
            case CURRIED_MINUS_FLOAT:
            return 1;
            case CURRIED_MULTIPLY_INT:
            case CURRIED_MULTIPLY_FLOAT:
            return 1;
            case CURRIED_DIVIDE_INT:
            case CURRIED_DIVIDE_FLOAT:
            return 1;
            case CURRIED_GREATER_INT:
            case CURRIED_GREATER_FLOAT:
            return 1;
            case CURRIED_GREATER_EQUALS_INT:
            case CURRIED_GREATER_EQUALS_FLOAT:
            return 1;
            case CURRIED_LESS_INT:
            case CURRIED_LESS_FLOAT:
            return 1;
            case CURRIED_LESS_EQUALS_INT:
            case CURRIED_LESS_EQUALS_FLOAT:
            return 1;
            case CURRIED_EQUALS_INT:
            case CURRIED_EQUALS_FLOAT:
            return 1;
            case CURRIED_NOT_EQUALS_INT:
            case CURRIED_NOT_EQUALS_FLOAT:
            return 1;
            case INPUT_INT:
            return 0;
            case CURRIED_MOD_INT:
            return 1;
            default:
            throw new RuntimeException("MagicNode type not supported: " + type);
        }
    }
    
}
