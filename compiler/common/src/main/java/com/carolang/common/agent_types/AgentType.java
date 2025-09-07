package com.carolang.common.agent_types;

import com.carolang.common.SourceFilePosition;





public abstract class AgentType {

    private boolean doNotInline = false;
    public boolean getDoNotInline()
    {
        return doNotInline;
    }

    public void setDoNotInline(boolean doNotInline)
    {
        this.doNotInline = doNotInline;
    }

    SourceFilePosition sourceFilePosition;
    int auxiliaryPortCount;
    public int getAuxiliaryPortCount() {
		return auxiliaryPortCount;
	}

    String cProgramStructType;
    public String getCProgramStructType() {
        return cProgramStructType;
    }

    String cProgramAllocationFunction;
    public String getCProgramAllocationFunction() {
        return cProgramAllocationFunction;
    }

    String humanReadableId;

    public String getHumanReadableId() {
        return humanReadableId;
    }

    static long nextId;
    long Id;

    @Override
    public boolean equals(Object o)
    {
        return (o instanceof AgentType aType && aType.Id == Id);
    }

    @Override
    public String toString() {
        if(sourceFilePosition != null)
        {
        return String.format(
            "{\"humanReadableId\": \"%s\", \"position\": \"%s\", \"numericalId\": %d, \"auxiliaryPortCount\": %d}",
            humanReadableId, sourceFilePosition.toString(), Id, auxiliaryPortCount);
        }
        return String.format(
            "{\"humanReadableId\": \"%s\", \"numericalId\": %d, \"auxiliaryPortCount\": %d}",
            humanReadableId, Id, auxiliaryPortCount);

    }
    public int hashCode()
    {
        return Long.hashCode(Id);
    }

    public long getId()
    {
        return Id;
    }

    boolean isDuplicatorAgent = false;
    public boolean isDuplicatorAgent() {
        return isDuplicatorAgent;
    }
}
