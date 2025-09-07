package com.carolang.common.ast_nodes;

import com.carolang.common.SourceFilePosition;

public class LambdaNode  extends NonValueNode {
    
    //A lambda Node is when we have both a function and its argument as children in the tree.
    //Not when we have a function defined like let fun x -> x+1 which would be a Function Node

    private Node Function;
    private Node Argument;

    public LambdaNode(Node Function, Node Argument, SourceFilePosition pos) {
        super(pos);
        addChild(Function);
        addChild(Argument);
        this.Function = Function;
        this.Argument = Argument;
    }

    public Node getFunction()
    {
        return getChild(0);
    }

    public Node getArgument()
    {
        return getChild(1);
    }

    //used in Lambda Hoisting
    //Moves the Function node to have this nodes parent and removes this node from the tree
    //Sets parent of Argument node to Null
    //Returns the Argument Node as a separate tree (parent set to null)
    public Node Collapse()
    {
        NonValueNode parent = getParent();
        Function.setParent(parent);
        if(parent != null)
        {
            parent.replaceChild(this, Function);
        }
        Argument.setParent(null);
        Node toReturn = Function;
        Argument = null;
        Function = null;
        return toReturn;
    }

}
