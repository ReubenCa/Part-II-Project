package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class MatchNode extends NonValueNode {


    public MatchNode( SourceFilePosition pos, Node expressionBeingMatched) {
        super(pos);
        addChild(expressionBeingMatched);
    }

    public Node getExpressionBeingMatched() {
        return getChild(0);
    }

    public Node getEmptyCase() {
        return getChild(1);
    }

    public Node getNonEmptyCase() {
        return getChild(2);
    }

    public void setDefinitions(Node emptyCase, Node nonEmptyCase) {
        addChild(emptyCase);
        addChild(nonEmptyCase);
    }
}
