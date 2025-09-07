package com.carolang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.carolang.common.interaction_rules.AgentImplementationBase;
import com.carolang.common.interaction_rules.NetBase;
import com.carolang.common.interaction_rules.Port;
import com.carolang.common.interaction_rules.Wire;
import com.carolang.common.ast_nodes.Node;

//Immutable - all getters return copies and merging clones the net before returning it
//This implements INet and contains a lot of extra information and methods used for the actual construction of the rules
public class constructionNet extends NetBase {

    private Map<Port, BindingBase> functionArgumentPortMap;

    public Map<Port, BindingBase> getFunctionArgumentPortMap() {
        return functionArgumentPortMap;
    }

    public Map<Port, AgentImplementationBase> getPortToAgentMap() {
        return portToAgentMap;
    }

    private Map<Node, Integer> functionBindingCounts = new HashMap<Node, Integer>();

    public Map<Node, Integer> getFunctionBindingCounts() {
        return functionBindingCounts;
    }

    private final Set<AgentImplementationBase> agents;
    private final Set<Wire> wires;
    private Map<Port, AgentImplementationBase> portToAgentMap; // Ports dont have to be attached to an agent - can be 'free'
                                                           // meaning they aren't part of an agent

    @Override
    public Set<AgentImplementationBase> getAgents() {
        return Set.copyOf(agents);
    }

    @Override
    public Set<Wire> getWires() {
        return Set.copyOf(wires);
    }

    public Port getOutputPort() {
      // assert(externalFacingPorts.size() == 1);
        return externalFacingPorts.get(0);
    }

    // So a port is involved in a wire XOR is in the unattachedPorts list XOR is the
    // output Port
    // Order does matter hence a list - maybe rethink this later with some sort of
    // mapping and labels?
    private List<Port> externalFacingPorts;

    public List<Port> getExternalFacingPorts() {
        return List.copyOf(externalFacingPorts);
    }



    /**
     * Constructs a Net
     * 
     * @param agents                  - Set of agents in the net
     * @param wires                   - Set of Wires
     * @param externalFacingPorts     - List of ports that are not attached to a
     *                                wire
     *                                AND agent
     * @param outputPort              - Optional Port designated as the output port
     *                                -
     *                                cannot be part of the external facing ports or
     *                                connected to a wire if so
     * @param functionArgumentPortMap - Map from ports to FunctionNodes - so if a
     *                                port is waiting to connect to a certain
     *                                function record that here
     */
    public constructionNet(Set<AgentImplementationBase> agents, Set<Wire> wires, List<Port> externalFacingPorts,
          Map<Port, BindingBase> functionArgumentPortMap, Map <Node, Integer> functionBindingCounts) {
        // Change to Free ports - no longer take list of them as argument - ports just
        // don't need to have an agent but must have an agent OR a wire

        this.agents = agents;
        this.wires = wires;
        this.externalFacingPorts = externalFacingPorts;
        this.functionArgumentPortMap = functionArgumentPortMap;
        this.functionBindingCounts = functionBindingCounts;

        portToAgentMap = new HashMap<>();
        for (AgentImplementationBase agent : agents) {
            portToAgentMap.put(agent.getPrinciplePort(), agent);
            for (Port p : agent.getAuxillaryPorts()) {
                portToAgentMap.put(p, agent);
            }
        }
        assert (verifyNonFreePorts());
        assert (verifyExternalPortsExist());
        assert (functionArgumentPortMap.entrySet().stream().allMatch(p -> p.getValue() != null));
        assert (verifyNotExternalANDFunctionArg());
        // assert (VerifyAllFreePortsAreInFreePortSet());

    }


    public static constructionNet Merge(List<constructionNet> nets, Map<Port, Port> portMappings)
            {   
                return Merge(nets, p -> Optional.ofNullable(portMappings.get(p)));
            }
    /**
     * 
     * @param nets
     * @param portMappings  For two ports to be merged when taking in one
     *                      portMappings must return the other, doesn't necessarily
     *                      need to hold both ways round but it can
     * @param newOutputPort
     * @return
     */
    public static constructionNet Merge(List<constructionNet> nets, Function<Port, Optional<Port>> portMappings) {

        Map<Node, Integer> functionBindingCounts = new HashMap<Node, Integer>();

        Set<AgentImplementationBase> Agents = new HashSet<>();
        Set<Wire> Wires = new HashSet<>();
        List<Port> potentialMergablePorts = new ArrayList<>();
        List<Port> externalFacingPorts = new ArrayList<>();
        Map<Port, BindingBase> FunctionArgumentPortMap = new HashMap<>();
        for (constructionNet net : nets) {
            Agents.addAll(net.getAgents());
            Wires.addAll(net.getWires());
            externalFacingPorts.addAll(net.getExternalFacingPorts());
            potentialMergablePorts.addAll(net.getExternalFacingPorts());
          
            potentialMergablePorts.addAll(net.getFunctionArgumentPortMap().keySet());
            FunctionArgumentPortMap.putAll(net.getFunctionArgumentPortMap());
            functionBindingCounts.putAll(net.getFunctionBindingCounts());
        }

        Set<Port> MergedPorts = new HashSet<>();// Stop us merging same port twice
        {
           

            for (Port p : potentialMergablePorts) {
                Optional<Port> otherPort = portMappings.apply(p);
                if (otherPort.isEmpty() || MergedPorts.contains(p)) {
                    continue;
                }
           
                Port portToMerge = otherPort.get();
                // Merging a function Argument Port causes it to demote to a unlabelled port

                MergedPorts.add(portToMerge);
                MergedPorts.add(p);
                
                FunctionArgumentPortMap.remove(p);
                FunctionArgumentPortMap.remove(portToMerge);
                Wire w = new Wire(p, portToMerge);
                Wires.add(w);

            }
        }
        List<Port> NewUnattachedPorts = externalFacingPorts.stream().filter(p -> !MergedPorts.contains(p))
                .toList();

        // Need to collapse situations where A,B,C are free and A-B-C can replace with
        // A-C
        // So look for any Wire where both ports are free - if either of these ports are
        // connected to another wire we can collapse it

        // We also need to repeatedly find all wires with one free port and delete them
        // and the free port
        // Ie if agent A is connected to port P which is wired to free port F
        // Might as well just have Agent with port P
        Map<Port, AgentImplementationBase> PortToAgentMap = new HashMap<>();
        for (AgentImplementationBase agent : Agents) {
            PortToAgentMap.put(agent.getPrinciplePort(), agent);
            for (Port p : agent.getAuxillaryPorts()) {
                PortToAgentMap.put(p, agent);
            }
        }
        //TODO: this seems like a bug?
        Predicate<Port> isFreePort = p -> !PortToAgentMap.containsValue(p);

        final Queue<Wire> WiresToCollapse = new LinkedList<>(Wires);
        while (WiresToCollapse.size() > 0) {

            final Wire w = WiresToCollapse.poll();
            final Port p1 = w.getPort1();
            final Port p2 = w.getPort2();

            // Merge Wire Wire -> Wire
            /**
             * Takes in Wire and returns new wire that is it merged with w
             */
            BinaryOperator<Wire> MergeWires = (w1, w2) -> {
                Port w2Port1 = w2.getPort1();
                Port w2Port2 = w2.getPort2();
                Wires.remove(w1);
                // WiresToCollapse.remove(w1);
                if (w1.getPort1() == w2Port1) {
                    return new Wire(w1.getPort2(), w2Port2);
                } else if (w1.getPort1() == w2Port2) {
                    return new Wire(w1.getPort2(), w2Port1);
                } else if (w1.getPort2() == w2Port1) {
                    return new Wire(w1.getPort1(), w2Port2);
                } else if (w1.getPort2() == w2Port2) {
                    return new Wire(w1.getPort1(), w2Port1);
                } else {
                    throw new RuntimeException("Wire doesn't contain either port");
                }
            };

            if (isFreePort.test(p1) && isFreePort.test(p2)) {
                // Get all wires that need to merge with w - shouldn't be more than 2
                List<Wire> wiresToMerge = Wires.stream().filter(w1 -> w != w1
                        && (w1.getPort1() == p1 || w1.getPort2() == p1 || w1.getPort1() == p2 || w1.getPort2() == p2))
                        .toList();
                wiresToMerge = new LinkedList<>(wiresToMerge);
                assert (wiresToMerge.size() <= 2); // can't have more than merging one wire at each end - should be
                                                   // impossible to get two wires both attached to same end of this one
                // Merge them
                // TODO: doesn't do what was intended
                // List<Wire> MergedWires = wiresToMerge.stream().map(MergeWires).toList();
                Wire newWire = w;
                while (wiresToMerge.size() > 0) {
                    Wire w2 = wiresToMerge.get(0);
                    wiresToMerge.remove(0);

                    newWire = MergeWires.apply(newWire, w2);
                    Wires.remove(w2);
                    Wires.remove(w);
                    WiresToCollapse.remove(w2);

                }
                // Make sure we visit the new wires as they may need to be merged
                if (newWire != w) {
                    WiresToCollapse.add(newWire);
                    Wires.add(newWire);
                }
                // if (MergedWires.size() > 0) {
                // Wires.remove(w);
                // }

            }
        }

        WiresToCollapse.clear();
        WiresToCollapse.addAll(Wires);

        while (WiresToCollapse.size() > 0) {
            Wire w = WiresToCollapse.poll();
            Port p1 = w.getPort1();
            Port p2 = w.getPort2();
            if (!(isFreePort.test(p1) ^ isFreePort.test(p2))) {
                continue;
            }
            // Wire has exactly one free port
            Port freePort = isFreePort.test(p1) ? p1 : p2;
            Port otherPort = isFreePort.test(p1) ? p2 : p1;
            Wires.remove(w);
            AgentImplementationBase otherPortOwner = PortToAgentMap.get(otherPort);
            assert (otherPortOwner != null); // Whole point is that otherPort isn't a free port
            if (otherPortOwner.getPrinciplePort() == otherPort) {
                otherPortOwner.changePrinciplePort(freePort);
            } else {
                otherPortOwner.ChangeAuxPort(otherPort, freePort);
            }

        }
        // throw new RuntimeException("Not implemented");
        // Could merge func argument ports here? - better to just leave them and let
        // func arg node handle it as then it can just use a single dup node
        constructionNet unclonedNet = new constructionNet(Agents, Wires, NewUnattachedPorts,
                FunctionArgumentPortMap, functionBindingCounts);
        return unclonedNet;// .Clone();
    }

    // constructionNet demoteOutput()// Returns a net where the output port has been demoted to last external port
    // {
    //     List<UnlabelledPort> newExternalPorts = new ArrayList<>(externalFacingPorts);
    //     newExternalPorts.add(outputPort);
    //     return new constructionNet(agents, wires, newExternalPorts, null, functionArgumentPortMap);
    // }

    /**
     * Returns a new net with the selected external port as the first one
     * @return
     */
    constructionNet swapOutput(Port newOutputPort)
    {
        assert(externalFacingPorts.contains(newOutputPort));
        assert(newOutputPort != null);
        assert(!functionArgumentPortMap.containsKey(newOutputPort));
        List<Port> newExternalPorts = new ArrayList<>();
     
        newExternalPorts.add(newOutputPort);
        newExternalPorts.addAll(externalFacingPorts.stream().filter(p -> p != newOutputPort).toList());
        

        return new constructionNet(agents, wires, newExternalPorts,  functionArgumentPortMap, functionBindingCounts);

    }

    private boolean verifyExternalPortsExist() {
        Set<Port> allPorts = new HashSet<>();
        for (AgentImplementationBase agent : agents) {
            allPorts.add(agent.getPrinciplePort());
            allPorts.addAll(agent.getAuxillaryPorts());
        }
        for (Wire w : wires) {
            allPorts.add(w.getPort1());
            allPorts.add(w.getPort2());
        }
        for (Port p : externalFacingPorts) {
            if (!allPorts.contains(p)) {
                return false;
            }
        }
        return true;
    }

    private boolean verifyNonFreePorts() {
        // Non-Free (attached to an Agent) Port can be exactly one of:
        // In a wire
        // input
        // output
        // Function Port

        Stream<Port> nonFuncArgPortsInAgents = Stream
                .concat(agents.stream().flatMap(agent -> agent.getAuxillaryPorts().stream()),
                        (agents.stream().map(agent -> agent.getPrinciplePort())))
                .filter(p -> !(functionArgumentPortMap.containsKey(p)));
        Predicate<Port> portInNet = port -> (externalFacingPorts.contains(port) ^
                wires.stream().anyMatch(wire -> wire.getPort1().equals(port) || wire.getPort2().equals(port)));
        boolean result = nonFuncArgPortsInAgents.allMatch(portInNet);
        return result;
    }

    private boolean verifyNotExternalANDFunctionArg()
    {
        Set<Port> allPorts = new HashSet<>();
        for (AgentImplementationBase agent : agents) {
            allPorts.add(agent.getPrinciplePort());
            allPorts.addAll(agent.getAuxillaryPorts());
        }
        for (Wire w : wires) {
            allPorts.add(w.getPort1());
            allPorts.add(w.getPort2());
        }
        functionArgumentPortMap.keySet().forEach(p -> allPorts.add(p));
        for(Port p : allPorts)
        {
            if(externalFacingPorts.contains(p) && functionArgumentPortMap.containsKey(p))
            {
                return false;
            }
        }
        return true;
    }

    public constructionNet Clone(Map<Port, Port> portMap)
    {
        Map<AgentImplementationBase, AgentImplementationBase> agentToCloneMap = new HashMap<>();
        Set<AgentImplementationBase> clonedAgents = agents.stream().map(agent -> {
            AgentImplementationBase clonedAgent = agent.Clone();
            agentToCloneMap.put(agent, clonedAgent);
            return clonedAgent;
        }).collect(Collectors.toSet());

        final Map<Port, Port> memoisedPortToClone = new HashMap<>();
        Function<Port, Port> portToClonedPort = p -> {
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
            portMap.put(p, toReturn);
            return toReturn;
        };
        Set<Wire> clonedWires = wires.stream().map(wire -> new Wire(portToClonedPort.apply(wire.getPort1()), portToClonedPort.apply(wire.getPort2()))).collect(Collectors.toSet());
        List<Port> clonedExternalPorts = externalFacingPorts.stream().map(portToClonedPort).map(p -> p).collect(Collectors.toList());
        Map<Port, BindingBase> clonedFunctionArgumentPortMap = new HashMap<>();
        functionArgumentPortMap.entrySet().forEach(entry -> clonedFunctionArgumentPortMap.put(portToClonedPort.apply(entry.getKey()), entry.getValue()));        
        return new constructionNet(clonedAgents, clonedWires, clonedExternalPorts, clonedFunctionArgumentPortMap, new HashMap<>(functionBindingCounts));
    }
     

    // private boolean VerifyAllFreePortsAreInFreePortSet()
    // {
    // Stream<Port> portsInAgents = Stream.concat(agents.stream().flatMap(agent ->
    // agent.getAuxillaryPorts().stream()),
    // (agents.stream().map(agent -> agent.getPrinciplePort())));
    // Stream<Port> portsInWires = wires.stream().flatMap(wire ->
    // Stream.of(wire.getPort1(), wire.getPort2()));
    // Stream<Port> allPorts = Stream.concat(portsInAgents, portsInWires);//May have
    // duplicates
    // for(Port p : allPorts.toList())
    // {
    // if(FunctionArgument)
    // {
    // if(!functionArgumentPorts.contains(p))
    // {
    // return false;
    // }
    // }
    // else if(p instanceof UnlabelledPort up)
    // {
    // boolean hasAWire = wires.stream().anyMatch(wire -> wire.getPort1().equals(up)
    // || wire.getPort2().equals(up));
    // boolean isExternal = externalFacingPorts.contains(up);
    // boolean hasAgent = agents.stream().anyMatch(agent ->
    // agent.getPrinciplePort().equals(up) ||
    // agent.getAuxillaryPorts().contains(up));
    // boolean isOutput = up.equals(outputPort);
    // //Needs two of the above
    // if((hasAWire ? 1 : 0) + (isExternal ? 1 : 0) + (hasAgent ? 1 : 0) + (isOutput
    // ? 1 : 0) != 2)
    // {
    // return false;
    // }
    // }
    // }
    // return true;
    // }

}