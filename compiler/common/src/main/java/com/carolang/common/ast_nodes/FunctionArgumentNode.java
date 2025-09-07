package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class FunctionArgumentNode extends Node {


    private FunctionNode definingFunction;

    public FunctionArgumentNode(FunctionNode definingFunction, SourceFilePosition pos)
    {
        super(pos);
        this.definingFunction = definingFunction;
    }

    public FunctionNode getDefiningFunction()
    {
        return definingFunction;
    }
}
