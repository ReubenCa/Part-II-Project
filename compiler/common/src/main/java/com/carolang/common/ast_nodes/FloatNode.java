package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class FloatNode extends Node {
    private float value;

    public FloatNode(float value, SourceFilePosition pos) {
        super(pos);
        this.value = value;
    }

    public float getValue() {
        return value;
    }
    
}
