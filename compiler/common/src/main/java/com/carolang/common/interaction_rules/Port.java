package com.carolang.common.interaction_rules;

public class Port  {
    // Maybe a string with some human readable ID
    private static long nextPortID = 0;
    private static long nextCloneAwareID = 0;
    long id;
    long CloneAwareID;

    public Port Clone() {
        return new Port(id);
    }

    public Port() {
        id = nextPortID++;
    }

    private Port(long id) {
        this.id = id;
        CloneAwareID = nextCloneAwareID++;
    }



    @Override
    public String toString() {
        if (CloneAwareID != 0) {
            return String.format("\"Port %d (%d)\"", id, CloneAwareID);
        }
        return String.format("\"Port %d\"", id);
    }
}