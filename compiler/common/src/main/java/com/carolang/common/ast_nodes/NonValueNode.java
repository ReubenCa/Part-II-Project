package com.carolang.common.ast_nodes;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import com.carolang.common.SourceFilePosition;

public abstract class NonValueNode extends Node{
   

    private ArrayList<Node> children = new ArrayList<>(); 

    public void setChildren(ArrayList<Node> children)
    {
        this.children =  children;
        for(Node c : children)
        {
            c.setParent(this);
        }
    }
  

    protected void addChild(Node child)
    {
        child.setParent(this);
        children.add(child);
    } 


    public Node getChild(int index)
    {
        return children.get(index);
    }

    public void replaceChild(Node toReplace, Node toReplaceWith)
    {
        for(int i = 0; i < children.size(); i++)
        {
            if(children.get(i)==toReplace)
            {
                children.set(i, toReplaceWith);
                return;
            }
        }
        throw new InvalidParameterException();
    }

    public Iterable<Node> getChildrenIterator()
    {
        return (ArrayList<Node>)children.clone();
    }

    public NonValueNode(SourceFilePosition pos)
    {
        super(pos);
    }

}
