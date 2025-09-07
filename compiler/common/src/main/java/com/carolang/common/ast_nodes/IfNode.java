package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class IfNode extends NonValueNode {


    public IfNode(Node Condition, Node Then, Node Else, SourceFilePosition pos) {
        super(pos);
        addChild(Condition);
        addChild(Then);
        addChild(Else);
    }

    public Node getCondition() {
        return getChild(0);
    }

    public Node getThen() {
        return getChild(1);
    }

    public Node getElse() {
        return getChild(2);
    }
    
}
