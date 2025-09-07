package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class BooleanNode extends Node {
    private boolean value;

    public BooleanNode(boolean value, SourceFilePosition pos) {
        super(pos);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
    
}
