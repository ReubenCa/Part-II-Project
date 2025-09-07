package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class MatchStatementVariableNode extends Node {

    private MatchNode matchStatement;
    private boolean isHead; 
    public MatchStatementVariableNode(SourceFilePosition pos, MatchNode matchStatement, boolean isHead) {
        super(pos);
        assert(matchStatement != null);
        this.matchStatement = matchStatement;
        this.isHead = isHead;
        
    }

    public MatchNode getMatchStatement() {
        return matchStatement;
    }

    public boolean isHead() {
        return isHead;
    }
    
}
