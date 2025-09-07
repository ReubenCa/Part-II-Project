package com.carolang.common.interaction_rules;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Net extends NetBase {
    private Set<AgentImplementationBase> agents;
    private Set<Wire> wires;
    private Port outputPort;

   

 

    private Map<Port, AgentImplementationBase> portToAgentMap;

    public Net(Set<AgentImplementationBase> agents, Set<Wire> wires, Port outputPort) {
        this.agents = agents;
        this.wires = wires;
        this.outputPort = outputPort;
        portToAgentMap = new HashMap<>();
        agents.forEach(agent -> {
            portToAgentMap.put(agent.getPrinciplePort(), agent);
            agent.getAuxillaryPorts().forEach(port -> portToAgentMap.put(port, agent));
        });
    }

    public NetBase Clone(Map<Port, Port> portMap)
    {
        Map<Port, AgentImplementationBase> portToAgentMap = new HashMap<>();
        agents.forEach(agent -> {
            portToAgentMap.put(agent.getPrinciplePort(), agent);
            agent.getAuxillaryPorts().forEach(port -> portToAgentMap.put(port, agent));
        });

        Map<AgentImplementationBase, AgentImplementationBase> agentToCloneMap = new HashMap<>();
        Set<AgentImplementationBase> clonedAgents = agents.stream().map(agent -> {
            AgentImplementationBase clonedAgent = agent.Clone();
            agentToCloneMap.put(agent, clonedAgent);
            return clonedAgent;
        }).collect(Collectors.toSet());

        for(Entry<AgentImplementationBase, AgentImplementationBase> entry : agentToCloneMap.entrySet())
        {
            portMap.put(entry.getKey().getPrinciplePort(), entry.getValue().getPrinciplePort());
            for(int i = 0; i < entry.getKey().getAuxillaryPorts().size(); i++)
            {
                portMap.put(entry.getKey().getAuxillaryPorts().get(i), entry.getValue().getAuxillaryPorts().get(i));
            }
        } 

        final Map<Port, Port> memoisedPortToClone = new HashMap<>();
        Function<Port, Port> portToClonedPort = p -> {
            assert(p != null);
            if(memoisedPortToClone.containsKey(p))
            {
                return memoisedPortToClone.get(p);
            }
            Port toReturn;
            if (portToAgentMap.containsKey(p)) {
                AgentImplementationBase agentWithP = portToAgentMap.get(p);
                if(agentWithP.getPrinciplePort().equals(p))
                {
                    toReturn = agentToCloneMap.get(agentWithP).getPrinciplePort();
                }
                else
                {
                    int index = agentWithP.getAuxillaryPorts().indexOf(p);
                    toReturn  = agentToCloneMap.get(agentWithP).getAuxillaryPorts().get(index);
                }
            }
            else
            {
                toReturn = new Port();
            }
            memoisedPortToClone.put(p, toReturn);
            portMap.put(p, toReturn);//TODO: not sufficeint not every port has to go through here
            
            return toReturn;
        };
        Set<Wire> clonedWires = wires.stream().map(wire -> new Wire(portToClonedPort.apply(wire.getPort1()), portToClonedPort.apply(wire.getPort2()))).collect(Collectors.toSet());
        Net netToReturn = new Net(clonedAgents, clonedWires,outputPort == null ? null : portToClonedPort.apply(outputPort));
        netToReturn.setSTDOUTPort(stdOutPort.isEmpty() ? null  : portToClonedPort.apply(stdOutPort.get()));
        return netToReturn;
    }

    @Override
    public Map<Port, AgentImplementationBase> getPortToAgentMap() {
        return portToAgentMap;
    }

    @Override
    public Set<AgentImplementationBase> getAgents() {
        return agents;
    }

    @Override
    public Set<Wire> getWires() {
        return wires;
    }

    @Override
    public Port getOutputPort() {
        return outputPort;
    }
  
    
}
