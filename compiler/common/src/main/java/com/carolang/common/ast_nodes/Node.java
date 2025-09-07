package com.carolang.common.ast_nodes;


import com.carolang.common.types.Type;

import com.carolang.common.SourceFilePosition;

public abstract class Node {
    private Type type;

    public NonValueNode getParent() {
        return parent;
    }

    private NonValueNode parent;


    protected void setParent(NonValueNode parent)
    {
        this.parent = parent;
    }

    public  Node(SourceFilePosition pos)
    {
        this.sourceFilePosition = pos;
    }

    private SourceFilePosition sourceFilePosition;

    public SourceFilePosition getSourceFilePosition() {
        return sourceFilePosition;
    }


    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
