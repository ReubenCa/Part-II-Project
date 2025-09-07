package com.carolang;

import com.carolang.common.ast_nodes.FunctionNode;

public class FunctionArgumentBinding extends BindingBase {

    public FunctionArgumentBinding(FunctionNode node, Integer externalPort) {
        this.node = node;
        this.externalPort = externalPort;
    }
    
    public FunctionNode getFunctionArgumentNode()
    {
        return (FunctionNode) node;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof FunctionArgumentBinding fobj)
        {
            return this.externalPort.equals(fobj.externalPort) && this.node.equals(fobj.node);
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.externalPort.hashCode() ^ this.node.hashCode() ^ 0x12345;
    }
    
}
