package com.carolang.common.interaction_rules;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.carolang.common.agent_types.AgentType;

public abstract class NetBase {


    Optional<Port> stdOutPort = Optional.empty();

    public Optional<Port> getSTDOUTPort() {
        return stdOutPort;
    }

    public void setSTDOUTPort(Port stdOutPort) {
        if (stdOutPort == null) {
            this.stdOutPort = Optional.empty();
            return;
        }
        this.stdOutPort = Optional.of(stdOutPort);
    }

    public void setSTDOUTPort(Optional<Port> stdOutPort) {
        this.stdOutPort = stdOutPort;
    }
    public abstract Set<? extends AgentImplementationBase> getAgents();

    public abstract Set<Wire> getWires();

    public abstract Port getOutputPort();


    //You will need the Map to update agent indexes in rule to point to new clones
    public abstract NetBase Clone(Map<Port,Port> portMap_Out);

    public abstract Map<Port, ? extends AgentImplementationBase> getPortToAgentMap();

    @Override
    public String toString() {
        Set<AgentType> agentTypes = new HashSet<>();
        for (AgentImplementationBase agent : getAgents()) {
            agentTypes.add(agent.getAgentType());
        }
        StringBuilder sb = new StringBuilder();
        String prefix = "";
         sb.append("{\n");
        // sb.append("\"AgentTypes\": [\n");
        // for (AgentType aType : agentTypes) {
        //     sb.append(prefix).append(aType.toString());
        //     prefix = ",\n";
        // }

        // sb.append("],\n");
        sb.append("\"Agents\": [\n");
        prefix = "";
        for (AgentImplementationBase agent : getAgents()) {
            sb.append(prefix).append(agent.toString());
            prefix = ",\n";
        }
        sb.append("],\n");
        sb.append("\"Wires\": [\n");
        prefix = "";
        for (Wire w : getWires()) {
            sb.append(prefix).append(w.toString());
            prefix = ",\n";
        }
        sb.append("]");
        if(getOutputPort() != null)
        {
            sb.append(",\n\"OutputPort\": ").append(getOutputPort().toString()).append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}