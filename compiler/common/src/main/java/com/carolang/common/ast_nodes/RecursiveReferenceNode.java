package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class RecursiveReferenceNode extends Node {
    public RecursiveReferenceNode(SourceFilePosition pos) {
        super(pos);
    }
    FunctionNode function;
    
    public FunctionNode getDefinition()
    {
        return function;
    }

    public void setDefinition(FunctionNode function) {
        this.function = function;
    }
    
}
