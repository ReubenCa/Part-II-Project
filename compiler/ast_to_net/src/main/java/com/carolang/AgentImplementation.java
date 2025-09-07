package com.carolang;

import java.util.List;

import org.pcollections.HashPMap;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.agent_types.NonDataAgentType;
import com.carolang.common.data_sources.DataSource;
import com.carolang.common.interaction_rules.AgentImplementationBase;
import com.carolang.common.interaction_rules.Port;

public class AgentImplementation extends AgentImplementationBase {
    private NonDataAgentType agentType;

    public AgentImplementation(List<Port> auxillaryPorts, Port principlePort, NonDataAgentType type) {
        this.auxillaryPorts = auxillaryPorts;
        this.principlePort = principlePort;
        this.agentType = type;
        assert (auxillaryPorts.size() == type.getAuxiliaryPortCount());
    }

    @Override
    public AgentType getAgentType() {
        return agentType;
    }

    public AgentImplementation Clone() {
        List<Port> ClonedPorts = auxillaryPorts.stream().map(Port::Clone).toList();
        return new AgentImplementation(ClonedPorts, principlePort.Clone(), agentType);
    }

    @Override
    public String toString() {
        return String.format("{\"principlePort\": %s, \"auxiliary Ports\": %s, \"type\": %s}",
                principlePort.toString(), auxillaryPorts.toString(), agentType.toString());
    }



    @Override
    public String cCodeToAllocate(String variableName, String agent1ParameterName, String agent2ParameterName, HashPMap<DataSource, String> dataSourcesAlreadyAllocated ) {
        assert (agentType.getCProgramStructType().equals("Agent"));
        assert (agentType.getCProgramAllocationFunction().equals("allocateAgent"));
        String r = "%s *%s = %s(%d, queue);\n".formatted(agentType.getCProgramStructType(), variableName,
                agentType.getCProgramAllocationFunction(), agentType.getAuxiliaryPortCount());
        return r;
    }

}