package com.carolang.common.data_sources;

import java.util.List;

import org.pcollections.HashPMap;

import com.carolang.common.agent_types.DataAgentType;
import com.carolang.common.interaction_rules.AgentImplementationBase;
import com.carolang.common.interaction_rules.Port;

public class DataAgentImplementation extends AgentImplementationBase {

    DataAgentType agentType;
    DataSource dataSource;

    public DataAgentImplementation(List<Port> auxillaryPorts, Port principlePort, DataAgentType agentType,
            DataSource dataSource) {
        this.auxillaryPorts = auxillaryPorts;
        this.principlePort = principlePort;
        this.agentType = agentType;
        this.dataSource = dataSource;
    }

   

    @Override
    public DataAgentImplementation Clone() {
        List<Port> ClonedPorts = auxillaryPorts.stream().map(Port::Clone).toList();
        return new DataAgentImplementation(ClonedPorts, principlePort.Clone(), agentType, dataSource.Clone());
    }

    private static long VarUniqueTag = 0;

    String getUniqueTag() {
        return "TAG_%d_".formatted(VarUniqueTag++);
    }

    @Override
    public String cCodeToAllocate(String resultVariableName, String agent1ParameterName, String agent2ParameterName, HashPMap<DataSource, String> dataSourcesAlreadyAllocated ) {
        String dataVariableName = "dataAgentData_%s".formatted(getUniqueTag());
        String dataSourceCode = dataSource.CreateCProgramForData(dataVariableName, agent1ParameterName,
                agent2ParameterName, dataSourcesAlreadyAllocated);
        String r = "%s *%s = %s(%d,%s, queue);\n".formatted(agentType.getCProgramStructType(),
                resultVariableName, agentType.getCProgramAllocationFunction(), agentType.getAuxiliaryPortCount(),
                dataVariableName);
        return dataSourceCode + r;
    }

    public DataAgentImplementation SwitchSides() {
        return new DataAgentImplementation(auxillaryPorts, principlePort, agentType, dataSource.SwitchSides());
    }

    public DataAgentImplementation Inline(DataSource firstReducedAgentSource, DataSource secondReducedAgentSource)
    {
        return new DataAgentImplementation(auxillaryPorts, principlePort, agentType, dataSource.Inline(firstReducedAgentSource, secondReducedAgentSource));
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public DataAgentType getAgentType() {
        return agentType;
    }

    @Override
    public String toString() {
        return String.format("{\"principlePort\": %s, \"auxiliary Ports\": %s, \"type\": %s, \"dataSource\": %s}",
                principlePort.toString(), auxillaryPorts.toString(), agentType.toString(), dataSource.toString());
    }
}