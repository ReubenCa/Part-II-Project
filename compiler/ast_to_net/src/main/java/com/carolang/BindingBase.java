package com.carolang;

import com.carolang.common.ast_nodes.Node;

public abstract class BindingBase {

    Integer externalPort;
    Node node;

    public int externalPort() {
        return externalPort;
    }

    public Node getNode() {
        return node;
    }


    @Override
    public abstract boolean equals(Object obj);
    
    @Override
    public abstract int hashCode();
}