package com.carolang;

import com.carolang.common.ast_nodes.MatchNode;

public class MatchBinding extends BindingBase {

    private boolean isHead;
    public MatchBinding(MatchNode node, Integer externalPort, boolean isHead) {
        this.node = node;
        this.externalPort = externalPort;
        this.isHead = isHead;
    }

    public MatchNode getMatchNode()
    {
        return (MatchNode) node;
    }

    public boolean isHead()
    {
        return isHead;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MatchBinding fobj)
        {
            return this.externalPort.equals(fobj.externalPort) && this.node.equals(fobj.node) && this.isHead == fobj.isHead;
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.externalPort.hashCode() ^ this.node.hashCode() ^ (isHead ? 0x9876 : 0x6789);
    }

    
}
