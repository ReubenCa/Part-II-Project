package com.carolang.common.data_sources;

import java.util.Set;

import org.pcollections.HashPMap;

public class DataFromReducedAgents extends DataSource {
    

    //So if we have a rule agent1 and agent2 are being replaced with a result net.
    //This source indicates the data either comes from agent1 or agent2
    //So the first of a plus operation would have this source for agent2 as data for the curriedAdd agent
    //The second stage would have OPERATION(+, THIS(agent1), THIS(agent2))


    
    public enum AgentOneOrTwo
    {
        AGENT_ONE,
        AGENT_TWO
    }
    private AgentOneOrTwo agentOneOrTwo;

    public DataFromReducedAgents(AgentOneOrTwo agentOneOrTwo, String resultCType) {
        super(resultCType);
        this.agentOneOrTwo = agentOneOrTwo;

    }

    @Override
    public DataSource innerClone() {
        return new DataFromReducedAgents(this.agentOneOrTwo, resultCType);
    }

    @Override
    protected String innerCreateCProgramForData(
            String resultVariableName, String agent1ParameterName, String agent2ParameterName, HashPMap<DataSource, String> alreadyAllocatedDatasources) {
        String paramName = agentOneOrTwo == AgentOneOrTwo.AGENT_ONE ? agent1ParameterName : agent2ParameterName;
        return "%s %s = %s -> data;//Data from reduced agents\n".formatted(resultCType, resultVariableName, paramName);
    }

    @Override
    protected DataSource innerSwitchSides() {
        if (agentOneOrTwo == AgentOneOrTwo.AGENT_ONE) {
            return new DataFromReducedAgents(AgentOneOrTwo.AGENT_TWO, resultCType);
        } else {
            return new DataFromReducedAgents(AgentOneOrTwo.AGENT_ONE, resultCType);
        }
    }

	@Override
	DataSource innerInline(DataSource reducedAgent1DataSource, DataSource reducedAgent2DataSource) {
		return agentOneOrTwo == AgentOneOrTwo.AGENT_ONE ? reducedAgent1DataSource : reducedAgent2DataSource;
	}

    @Override
    int innerGetSize() {
        return 1;
    }

    @Override
    boolean innerEquals(DataSource other) {
        if (other instanceof DataFromReducedAgents) {
            DataFromReducedAgents otherData = (DataFromReducedAgents) other;
            return this.agentOneOrTwo == otherData.agentOneOrTwo && this.resultCType.equals(otherData.resultCType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return agentOneOrTwo.hashCode() ^ resultCType.hashCode();
    }

    @Override
    public Set<DataSource> allDataSourcesNeeded() {
        return Set.of(this);
    }
}
