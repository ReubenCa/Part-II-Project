package com.carolang.common.agent_types;

import com.carolang.common.SourceFilePosition;

public class NonDataAgentType extends AgentType {


   
    public NonDataAgentType(int auxiliaryPortCount, String humanReadableId, SourceFilePosition sourceFilePosition) {
        this.sourceFilePosition = sourceFilePosition;
        this.Id = nextId++;
        this.auxiliaryPortCount = auxiliaryPortCount;
        this.humanReadableId = humanReadableId;
        this.cProgramStructType = "Agent";
        this.cProgramAllocationFunction = "allocateAgent";
    }

    public NonDataAgentType(int auxiliaryPortCount, String humanReadableId, SourceFilePosition sourceFilePosition, boolean isDuplicatorAgent) {
        this(auxiliaryPortCount, humanReadableId, sourceFilePosition);
        this.isDuplicatorAgent = isDuplicatorAgent;
    }
}
