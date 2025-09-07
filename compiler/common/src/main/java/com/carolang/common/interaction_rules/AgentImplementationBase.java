package com.carolang.common.interaction_rules;

import java.util.List;

import org.pcollections.HashPMap;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.data_sources.DataSource;

public abstract class AgentImplementationBase {

    protected List<Port> auxillaryPorts;
    protected Port principlePort;
    protected static Long nextId = 0L;
    protected Long id = nextId++;

    public List<Port> getAuxillaryPorts() {
        return List.copyOf(auxillaryPorts);
    }

    public Port getPrinciplePort() {
        return principlePort;
    }

    public abstract AgentType getAgentType();

    public abstract String toString();

    public abstract AgentImplementationBase Clone();

    public String getHumanReadableIdentifier() {
        return "%s_%d".formatted(getAgentType().getHumanReadableId(), id);
    }

    public abstract String cCodeToAllocate(String resultVariableName, String agent1ParameterName,
            String agent2ParameterName, HashPMap<DataSource, String> dataSourcesAlreadyAllocated );

    /**
     * Annoyingly does technically violate immutability but ends up making wire
     * collapsing a lot cleaner - is only to be called before the actual net is
     * constructing so isn't really stopping nets being immutable but I can't
     * enforce that in the code which is annoying
     * 
     * @param wire
     */
    public void changePrinciplePort(Port newPort) {
        principlePort = newPort;
    }

    // See Above
    public void ChangeAuxPort(Port oldPort, Port newPort) {
        int index = auxillaryPorts.indexOf(oldPort);
        auxillaryPorts.set(index, newPort);
    }
}