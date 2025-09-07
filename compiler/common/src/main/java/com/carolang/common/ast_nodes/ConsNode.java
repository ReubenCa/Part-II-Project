package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class ConsNode extends NonValueNode{




    public ConsNode(SourceFilePosition pos, Node Head, Node Tail) {
        super(pos);
        addChild(Head);
        addChild(Tail);
    }

    public Node getHead()
    {
        return getChild(0);
    }
    
    public Node getTail()
    {
        return getChild(1);
    }
}
