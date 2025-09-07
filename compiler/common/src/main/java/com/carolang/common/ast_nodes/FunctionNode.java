package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class FunctionNode extends NonValueNode {
 

    public Node getDefinition()
    {
        return getChild(0);
    }

    public void setDefinition(Node definition)
    {
        //Immutable only set once - TODO could have the constructor return a consumer than sets this to enforce this method contraint
        
        this.addChild(definition);
    }

    public FunctionNode(SourceFilePosition pos, String varName)
    {
        super(pos);
        this.varName = varName;
    }

    private String varName;

    public String getVarName()
    {
        return varName;
    }
    
}
