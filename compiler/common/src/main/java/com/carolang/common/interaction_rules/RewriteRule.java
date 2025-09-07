package com.carolang.common.interaction_rules;


import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.carolang.common.agent_types.AgentType;

public class RewriteRule extends ConditionalRewriteRule  {


    NetBase result;
    Map<Integer, Port> agent1Index;
    Optional<Map<Integer, Port>> agent2Index;



    public Optional<AgentType> getAgentOption2() {
        return Optional.ofNullable(agent2);
    }


    public NetBase getResult() {
        return result;
    }

    private Optional<Port> outputPort;

    public Optional<Port> getOutputPort()
    {
        return outputPort;
    }

    

    public Map<Integer, Port> getAgent1Index() {
        return agent1Index;
    }


    public Optional<Map<Integer, Port>> getAgent2Index() {
        return agent2Index;
    }
    // And probably a MAP of agents to where they go in the result
    // If agent 2 is not specified it can be any agent

    /**
     * 
     * @param agent1      - Agent that is being rewritten
     * @param agent1Index - This takes the Index of the port in Agent 1's aux ports
     *                    and maps to the index of the unattached port on the result
     * @param result      - The resulting Net
     */
    public RewriteRule(AgentType agent1, Map<Integer, Port> agent1Index, NetBase result) {
        super(agent1, null, result, agent1Index, null);
        this.result = result;
        this.agent1Index = agent1Index;
        this.agent2Index = Optional.empty();
        indexHasAllEntries(agent1Index, agent1.getAuxiliaryPortCount());
        this.outputPort = Optional.of(result.getOutputPort());
        assertIndexContainsPortsInResult(agent1Index);
        //If this is a one agent rule why don't we want to specify where the principle port goes?
        //It is always the output port of the result that connects to the principle port of Agent2
        //Remember that this is still a 2 way interaction (all interactions are) it is just that the second agent is not specified
    }

    /**
     * 
     * @param agent1      - First Agent that is being rewritten
     * @param agent2      - Second Agent that is being rewritten
     * @param result      - The resulting Net
     * @param agent1Index - This takes the Index of the port in Agent 1's aux ports
     *                    and maps to the index of the unattached port on the result
     * @param agent2Index - This takes the Index of the port in Agent 2's aux ports
     *                    and maps to the index of the unattached port on the result
     */
    public RewriteRule(AgentType agent1, AgentType agent2, NetBase result, Map<Integer, Port> agent1Index,
            Map<Integer, Port> agent2Index) {
        super(agent1, agent2, result, agent1Index, agent2Index);
        this.result = result;
        this.agent1Index = agent1Index;
        this.agent2Index = Optional.of(agent2Index);
        assertIndexContainsPortsInResult(agent1Index);
        assertIndexContainsPortsInResult(agent2Index);
        
        // indexHasAllEntries(agent1Index, agent1.getAuxiliaryPortCount()); - magic Rewrite rule doesnt need this assert condition
        // indexHasAllEntries(agent2Index, agent2.getAuxiliaryPortCount());

    }

    private void indexHasAllEntries(Map <Integer, Port> index, int auxPortCount)
    {
        for(int i = 0; i< auxPortCount; i++)
        {
            assert(index.containsKey(i));
      //      assert(index.get(i) != getOutputPort().get());
        }
    }
    private void assertIndexContainsPortsInResult(Map<Integer, Port> index)
    {
        if(result == null)
        {
            return;
        }
        Stream<Port> portsInResult = result.getAgents().stream()
            .flatMap(agent -> agent.getAuxillaryPorts().stream())
            .map(port ->  port);
        portsInResult = Stream.concat(portsInResult, result.getAgents().stream().map(a -> a.getPrinciplePort()));
        portsInResult = Stream.concat(portsInResult, result.getWires().stream().map(wire -> wire.getPort1()));
        portsInResult = Stream.concat(portsInResult, result.getWires().stream().map(wire -> wire.getPort2()));
        Set<Port> portsInResultSet = portsInResult.collect(Collectors.toSet());
        for(Port port : index.values())
        {
           assert(portsInResultSet.contains(port)); 
        }
    }

    // public RewriteRule Clone()
    // {
    //     if(result == null)
    //     {
    //         return this;//Stupid but used for magic rules in interpreter - this is never actually used by the compiler but rather just the interpreter when testing so I'm slightly more okay with it being messy.
    //     }
    //     Map<Port, Port> portMap = new HashMap<Port, Port>();
    
    //     NetBase clonedResult = result.Clone(portMap);
    //     Map<Integer, Port> clonedAgent1Index = new HashMap<Integer, Port>();
    //     for(Map.Entry<Integer, Port> entry : agent1Index.entrySet())
    //     {
    //         Port clonedPort = portMap.get(entry.getValue());
    //         assert(clonedPort != null);
    //         clonedAgent1Index.put(entry.getKey(), clonedPort);
    //     }
       
    //     if(agent2Index.isPresent())
    //     {
    //         Map<Integer, Port> clonedAgent2IndexMap = new HashMap<Integer, Port>();
    //         for(Map.Entry<Integer, Port> entry : agent2Index.get().entrySet())
    //         {
    //             Port clonedPort = portMap.get(entry.getValue());
    //             assert(clonedPort != null);
    //             clonedAgent2IndexMap.put(entry.getKey(), clonedPort);
    //         }
            
    //         return new RewriteRule(agent1, agent2.get(), clonedResult, clonedAgent1Index, clonedAgent2IndexMap);
    //     }
    //     else
    //     {
    //         return new RewriteRule(agent1, clonedAgent1Index, clonedResult);
    //     }
    // }
 


   


    public int hashCode() {
        if(agent1 == null)//This is the starting rule
        {
            return 0;
        }
        return agent1.hashCode() ^ getAgentOption2().hashCode();
    }




    private String indexToString(Map<Integer, Port> index)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        String prefix = "\n";
        for (Map.Entry<Integer, Port> entry : index.entrySet()) {
            sb.append(prefix);
            sb.append(String.format("\"%d\" : %s", entry.getKey(), entry.getValue()));
            
            prefix = ",\n";
        }
        sb.append("}");
        return sb.toString();
    }





    public RewriteRuleResult getRewriteRuleResult()
    {
        return new RewriteRuleResult(result, agent1Index, agent1Index);
    }
}
