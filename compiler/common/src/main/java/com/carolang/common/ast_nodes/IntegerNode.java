package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class IntegerNode extends Node {
    private int value;

    public IntegerNode(int value, SourceFilePosition pos) {
        super(pos);
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    
}
