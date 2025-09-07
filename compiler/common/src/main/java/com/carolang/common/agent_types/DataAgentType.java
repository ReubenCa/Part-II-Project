package com.carolang.common.agent_types;

import com.carolang.common.SourceFilePosition;

public class DataAgentType extends AgentType{

    final private String cTypeForData;
    public String getCTypeForData() {
        return cTypeForData;
    }

    public DataAgentType(int auxiliaryPortCount, String humanReadableId, SourceFilePosition sourceFilePosition, String cProgramStructType, String cProgramAllocationFunction, String cTypeForData) {
        this.sourceFilePosition = sourceFilePosition;
        this.Id = nextId++;
        this.auxiliaryPortCount = auxiliaryPortCount;
        this.humanReadableId = humanReadableId;
        this.cProgramStructType = cProgramStructType;
        this.cProgramAllocationFunction = cProgramAllocationFunction;
        this.cTypeForData = cTypeForData;
        
    }
}
