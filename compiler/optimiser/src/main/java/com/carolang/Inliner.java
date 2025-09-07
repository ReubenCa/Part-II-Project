package com.carolang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.data_sources.DataAgentImplementation;
import com.carolang.common.data_sources.DataSource;
import com.carolang.common.interaction_rules.AgentImplementationBase;
import com.carolang.common.interaction_rules.ConditionTreeNode;
import com.carolang.common.interaction_rules.ConditionalRewriteRule;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;
import com.carolang.common.interaction_rules.Net;
import com.carolang.common.interaction_rules.NetBase;
import com.carolang.common.interaction_rules.Port;
import com.carolang.common.interaction_rules.Program;
import com.carolang.common.interaction_rules.ProgramBase;
import com.carolang.common.interaction_rules.Wire;
import com.google.common.collect.Sets;

public class Inliner {

    

    public static record Substitution(AgentImplementationBase agent1, AgentImplementationBase agent2,
            ConditionalRewriteRule RuleToSubstituteIn) {

    }

    public static ProgramBase inlineProgram(IinliningHeuristic heuristic, ProgramBase program) {
        HashSet<ConditionalRewriteRule> newRules = new HashSet<>(program.getRules());
        for (ConditionalRewriteRule rule : program.getRules()) {
            newRules.add(inlineAllPositiveSubtitutions(rule, heuristic, program.getRules(), heuristic.MaxInterations()));
        }
        ConditionalRewriteRule newStartingRule = inlineAllPositiveSubtitutions(program.getStartingRule(),
                heuristic, program.getRules(), heuristic.MaxInterations());
        return new Program(newRules, newStartingRule);
    }

    static ConditionalRewriteRule inlineAllPositiveSubtitutions(ConditionalRewriteRule rule,
            IinliningHeuristic heuristic,
            Set<? extends ConditionalRewriteRule> allRules, int MaxInterations) {
        int iteration = 0;
        boolean changesMade = true;
        while (changesMade && iteration < MaxInterations) {
            iteration++;
            changesMade = false;
            Map<RewriteRuleResult, ConditionTreeNode> inlinedResults = new HashMap<>();
            for (RewriteRuleResult result : rule.getResults()) {
                ConditionTreeNode inlinedResult = singleInlineOnSingleResult(result, allRules, heuristic.getHeuristic(rule).apply(result));
                
                if (inlinedResult != null) {
                    inlinedResult.getResults().stream().forEach(r -> assertAllPortsInIndexesExist(r));
                    inlinedResults.put(result, inlinedResult);
                    changesMade = true;
                }

            }
            if (changesMade) {
                for (Entry<RewriteRuleResult, ConditionTreeNode> entry : inlinedResults.entrySet()) {
                    rule = rule.ReplaceResultWithConditionalResult(entry.getKey(), entry.getValue());
                }
            }
        }
        return rule;
    }

    static ConditionTreeNode singleInlineOnSingleResult(RewriteRuleResult result,
            Set<? extends ConditionalRewriteRule> allRules,
            Function<Substitution, Float> ranking) {

        Set<AgentPair> hotAgents = getAllHotAgents(result.net());
        float MaxValue = 0;
        Substitution bestSubstitution = null;
        ConditionalRewriteRule BestMatchingRule;
        for (AgentPair ap : hotAgents) {
            ConditionalRewriteRule matchingRule = findMatchingRule(ap.agent1.getAgentType(), ap.agent2.getAgentType(),
                    allRules);
            if (matchingRule == null) {
                continue;//This can happen since we don't allow all rules to be inlined (output rules)
            }
            Substitution s = new Substitution(ap.agent1, ap.agent2, matchingRule);

            if (ranking.apply(s) > MaxValue) {
                bestSubstitution = s;
                MaxValue = ranking.apply(s);

            }
        }
        if (bestSubstitution == null) {
            return null;// No sub scored above 0
        }

        BestMatchingRule = bestSubstitution.RuleToSubstituteIn;
        // So the goal here is to take our matching rule
        // We then map the results from the matching to the results of performing our
        // substitution
        // This creates a condition Tree that we then can then return
        Set<RewriteRuleResult> matchingRuleResults = BestMatchingRule.getResults();
        Map<RewriteRuleResult, RewriteRuleResult> matchingRuleResultToSubstitutionResult = new HashMap<>();
        boolean dontNeedToSwapAgent1And2Around = bestSubstitution.agent1.getAgentType() == BestMatchingRule.getAgent1()
        && bestSubstitution.agent2.getAgentType() == BestMatchingRule.getAgent2();
        assert (dontNeedToSwapAgent1And2Around  || bestSubstitution.agent1.getAgentType() == BestMatchingRule.getAgent2()
        && bestSubstitution.agent2.getAgentType() == BestMatchingRule.getAgent1());
        for (RewriteRuleResult matchingRuleResult : matchingRuleResults) {

            NetBase substitutedNet;
            if (dontNeedToSwapAgent1And2Around) {
                substitutedNet = ReplaceAgentsWithRewriteResult(result.net(), matchingRuleResult,
                        bestSubstitution.agent1, bestSubstitution.agent2);
            } else {

                substitutedNet = ReplaceAgentsWithRewriteResult(result.net(), matchingRuleResult,
                        bestSubstitution.agent2, bestSubstitution.agent1);
            }
            substitutedNet.setSTDOUTPort(result.net().getSTDOUTPort());
            matchingRuleResultToSubstitutionResult.put(matchingRuleResult,
                    new RewriteRuleResult(substitutedNet, result.agent1Index(), result.agent2Index()));
        }

        //Is is possible for these data sources to be the wrong way round?
        //Ie agent1 and agent2 aren't guaranteed to be aligned same way round as rule hence if statements above
        //We need to also inline the operands of conditions
        DataSource dataSource1 = (bestSubstitution.agent1 instanceof DataAgentImplementation dataAgent1) ? dataAgent1.getDataSource()
                : null;
        DataSource dataSource2 = (bestSubstitution.agent2 instanceof DataAgentImplementation dataAgent2) ? dataAgent2.getDataSource()
                : null;
        ConditionTreeNode mappingButNotInliningConditions = BestMatchingRule.getConditionTree().MapResults(matchingRuleResultToSubstitutionResult);
        if(dontNeedToSwapAgent1And2Around)
        {
        return mappingButNotInliningConditions.InlineConditions(dataSource1, dataSource2);
        }
        else
        {
            return mappingButNotInliningConditions.InlineConditions(dataSource2, dataSource1);
        }
    }

    private static void assertAllPortsInIndexesExist(RewriteRuleResult result)
    {
        Set<Port> allPorts = new HashSet<>();
        NetBase net = result.net();
        for(AgentImplementationBase Agent : net.getAgents())
        {
            allPorts.add(Agent.getPrinciplePort());
            allPorts.addAll(Agent.getAuxillaryPorts());
        }
        for(Wire w : net.getWires())
        {
            allPorts.add(w.getPort1());
            allPorts.add(w.getPort2());
        }
        for(Port p : result.agent1Index().values())
        {
            assert(allPorts.contains(p));
        }
        for(Port p : result.agent2Index().values())
        {
            assert(allPorts.contains(p));
        }
    }

    private static ConditionalRewriteRule findMatchingRule(AgentType agent1, AgentType agent2,
            Set<? extends ConditionalRewriteRule> allRules) {

        if (agent1.getDoNotInline() || agent2.getDoNotInline()) {
            return null;
        }
        for (ConditionalRewriteRule rule : allRules) {
            if (rule.getAgent1() == agent1 && rule.getAgent2() == agent2
                    || rule.getAgent2() == agent1 && rule.getAgent1() == agent2) {
                return rule;
            }
        }
        return null;
    }

    private static Set<AgentPair> getAllHotAgents(NetBase net) {
        Map<Port, ? extends AgentImplementationBase> PortToAgent = net.getPortToAgentMap();
        Set<AgentImplementationBase> agentsAlreadyAdded = new HashSet<>();
        Set<AgentPair> hotAgents = new HashSet<>();
        for (AgentImplementationBase agent : net.getAgents()) {
            if (agentsAlreadyAdded.contains(agent)) {
                continue;
            }
            Port agentPort = agent.getPrinciplePort();
            Stream<Wire> wires = net.getWires().stream()
                    .filter(wire -> wire.getPort1() == agentPort || wire.getPort2() == agentPort);
            Wire wire = wires.findFirst().orElse(null);
            // assert (wires.count() <= 1);
            if (wire == null) {
                continue;
            }
            Port otherPort = (wire.getPort1() == agentPort) ? wire.getPort2() : wire.getPort1();
            AgentImplementationBase otherAgent = PortToAgent.get(otherPort);
            if (otherAgent == null) {
                continue;
            }
            if (otherPort == otherAgent.getPrinciplePort()) {
                hotAgents.add(new AgentPair(agent, otherAgent));
                assert (net.getAgents().contains(agent));
                assert (net.getAgents().contains(otherAgent));
            }
        }
        return hotAgents;
    }

    private static record AgentPair(AgentImplementationBase agent1, AgentImplementationBase agent2) {
    }

    public static NetBase ReplaceAgentsWithRewriteResult(NetBase netBeingSubbedInto, RewriteRuleResult ResultSubbingIntoNet,
            AgentImplementationBase agent1,
            AgentImplementationBase agent2) {
        
        ResultSubbingIntoNet = ResultSubbingIntoNet.Clone();
        assert (netBeingSubbedInto.getAgents().contains(agent1));
        assert (netBeingSubbedInto.getAgents().contains(agent2));
        assert (ResultSubbingIntoNet.agent1Index().size() == agent1.getAuxillaryPorts().size());
        assert (ResultSubbingIntoNet.agent2Index().size() == agent2.getAuxillaryPorts().size());

        DataSource dataSource1 = (agent1 instanceof DataAgentImplementation dataAgent1) ? dataAgent1.getDataSource()
                : null;
        DataSource dataSource2 = (agent2 instanceof DataAgentImplementation dataAgent2) ? dataAgent2.getDataSource()
                : null;

        // Agents:
        // All data agents
        // Need to go through entire DataSource Graph and replace each occurrence of
        // DataFromReducedAgent with the DataSource from that agent
        Set<AgentImplementationBase> agents = new HashSet<>(Sets.union(ResultSubbingIntoNet.net().getAgents(), netBeingSubbedInto.getAgents()));
        agents.remove(agent1);
        agents.remove(agent2);
        agents = agents.stream().map(a -> {
            if (a instanceof DataAgentImplementation DataAgent) {
                return DataAgent.Inline(dataSource1, dataSource2);
            } else {
                return a;
            }
        }).collect(Collectors.toSet());

        // Wires:
        Set<Wire> wires = new HashSet<>(Sets.union(ResultSubbingIntoNet.net().getWires(), netBeingSubbedInto.getWires()));
        Wire wireBetweenAgentOneAndTwo = wires.stream()
                .filter(wire -> wire.getPort1() == agent1.getPrinciplePort()
                        || wire.getPort2() == agent1.getPrinciplePort())
                .findFirst().orElse(null);
        assert (agent2.getPrinciplePort() == wireBetweenAgentOneAndTwo.getPort1()
                || agent2.getPrinciplePort() == wireBetweenAgentOneAndTwo.getPort2());
        wires.remove(wireBetweenAgentOneAndTwo);
        // Need to iterate over the indexes and sort the wires there
        for (Entry<Integer, Port> entry : ResultSubbingIntoNet.agent1Index().entrySet()) {
            wires.add(new Wire(agent1.getAuxillaryPorts().get(entry.getKey()), entry.getValue()));
        }

        for (Entry<Integer, Port> entry : ResultSubbingIntoNet.agent2Index().entrySet()) {
            wires.add(new Wire(agent2.getAuxillaryPorts().get(entry.getKey()), entry.getValue()));
        }
        wires = Wire.flattenWires(wires);
        // Need to remove the wire between the two agents

        return new Net(agents, wires, netBeingSubbedInto.getOutputPort());
    }

    


}
