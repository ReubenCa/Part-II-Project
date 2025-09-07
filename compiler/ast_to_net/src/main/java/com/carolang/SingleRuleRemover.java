package com.carolang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.agent_types.DataAgentType;
import com.carolang.common.agent_types.NonDataAgentType;
import com.carolang.common.data_sources.DataAgentImplementation;
import com.carolang.common.data_sources.DataFromReducedAgents;
import com.carolang.common.data_sources.DataFromReducedAgents.AgentOneOrTwo;
import com.carolang.common.data_sources.DataSource;
import com.carolang.common.interaction_rules.AgentImplementationBase;
import com.carolang.common.interaction_rules.ConditionTreeLeafNode;
import com.carolang.common.interaction_rules.ConditionalRewriteRule;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;
import com.carolang.common.interaction_rules.Net;
import com.carolang.common.interaction_rules.NetBase;
import com.carolang.common.interaction_rules.Port;
import com.carolang.common.interaction_rules.Program;
import com.carolang.common.interaction_rules.ProgramBase;
import com.carolang.common.interaction_rules.RewriteRule;
import com.carolang.common.interaction_rules.Wire;
import com.google.common.collect.Sets;

class SingleRuleRemover {
    static ProgramBase inlineAllNonRecursiveSingleRules(ProgramBase program) {
        Set<? extends ConditionalRewriteRule> allRules = program.getRules();
        Set<RewriteRule> singleRules = allRules.stream().filter(r -> r instanceof RewriteRule).map(r -> (RewriteRule) r)
                .filter(r -> !r.getAgentOption2().isPresent()).collect(Collectors.toSet());

        Set<RewriteRule> SingleRulesToInlineHere = new HashSet<>();
        Set<AgentType> agentTypesBeingRemovedHere = new HashSet<>();
        for (RewriteRule singleRule : singleRules) {
            boolean isRecursive = singleRule.getResult().getAgents().stream()
                    .anyMatch(a -> a.getAgentType().equals(singleRule.getAgent1()));
            if (isRecursive) {
                break;
            }
            SingleRulesToInlineHere.add(singleRule);
            agentTypesBeingRemovedHere.add(singleRule.getAgent1());
        }
        while (true) {
            // TODO: this is not efficient at all
            List<ConditionalRewriteRule> rulesThatCanBeInlined = Stream
                    .concat(program.getRules().stream(), Stream.of(program.getStartingRule()))
                    .filter(rule -> rule.getResults().stream().flatMap(r -> r.net().getAgents().stream())
                            .anyMatch(a -> agentTypesBeingRemovedHere.contains(a.getAgentType())))
                    .toList();
            if (rulesThatCanBeInlined.isEmpty()) {
                break;
            }

            for (RewriteRule singleRule : SingleRulesToInlineHere) {
                program = inlineSingleRule(program, singleRule);
            }

        }
        return program;
    }

    static Program removeSingleRules(ProgramBase program) {
        // So whenever we have two single rules we can merge so they both get reduced
        // together
        // But we ultimately need rules between every type (for now)
        // So process will likely be
        // For each rule
        // go over each type and if a single rule exists for that type merge the rules
        // Otherwise just create a new version of rule with that type.

        program = inlineAllNonRecursiveSingleRules(program);
        Set<? extends ConditionalRewriteRule> allRules = program.getRules();
        Set<RewriteRule> singleRules = allRules.stream().filter(r -> r instanceof RewriteRule).map(r -> (RewriteRule) r)
                .filter(r -> !r.getAgentOption2().isPresent()).collect(Collectors.toSet());

        Set<AgentType> allAgentTypes = program.getAgents();
        // So if a single rule exists with this type it will be in the map
        Map<AgentType, RewriteRule> agentTypeToSingleRule = new HashMap<>();
        for (RewriteRule rule : singleRules) {
            AgentType agent1 = rule.getAgent1();
            agentTypeToSingleRule.put(agent1, rule);
        }
        Set<RewriteRule> newRules = new HashSet<>();

        for (RewriteRule singleRule : singleRules) {

            for (AgentType agentType : allAgentTypes) {
                RewriteRule newRule;
                if (agentTypeToSingleRule.containsKey(agentType)) {
                    RewriteRule otherRule = agentTypeToSingleRule.get(agentType);
                    newRule = mergeRules(singleRule, otherRule);
                } else {
                    newRule = FillInWildcardAgent(singleRule, agentType);
                }
                newRules.add(newRule);
            }
        }

        Set<ConditionalRewriteRule> newProgramRules = Sets.union(Sets.difference(allRules, singleRules), newRules);
        return new Program(newProgramRules, program.getStartingRule());
    }

    private static RewriteRule FillInWildcardAgent(RewriteRule rule, AgentType type) {
        Port newAgentPrinciplePort = new Port();
        Set<Wire> newWires = new HashSet<>();
        NetBase result = rule.getResult();
        newWires.addAll(result.getWires());
        // We also need agent to be wired to old output of net
        if (result.getPortToAgentMap().containsKey(result.getOutputPort())) {
            // The output port is connected to an agent
            Wire newWire = new Wire(result.getOutputPort(), newAgentPrinciplePort);
            newWires.add(newWire);
        } else {
            // Port is free so connect directly to agent
            newAgentPrinciplePort = result.getOutputPort();
        }
        List<Port> auxPorts = new ArrayList<>();
        for (int i = 0; i < type.getAuxiliaryPortCount(); i++) {
            auxPorts.add(new Port());
        }
        // : Doesnt account for fact that this might be DataAgent
        // If it is a Data Agent we need to allocate a new Data agent with a Data source
        // pointing to Agent2 (ie we just copy data across)
        // But need to be able to check if it is indeed a data agent
        // Agent Type should probably have a flag
        // Or even better AgentType class with Data and Non Data subclasses really
        // Agent Implementation and DataAgentImplementation can then take the correct
        // type only
        AgentImplementationBase agent2;
        if (type instanceof NonDataAgentType ndType) {
            agent2 = new AgentImplementation(auxPorts, newAgentPrinciplePort, ndType);// Need agent to still exist after
                                                                                      // rewrite
        } else if (type instanceof DataAgentType dType) {
            agent2 = new DataAgentImplementation(auxPorts, newAgentPrinciplePort, dType,
                    new DataFromReducedAgents(AgentOneOrTwo.AGENT_TWO, dType.getCTypeForData()));
        } else {
            throw new RuntimeException("Agent type not supported");
        }

        Map<Integer, Port> agent2Index = new HashMap<>();
        for (int i = 0; i < auxPorts.size(); i++) {
            agent2Index.put(i, auxPorts.get(i));
        }

        NetBase newResult = new Net(Sets.union(result.getAgents(), Set.of(agent2)), newWires, result.getOutputPort());

        return new RewriteRule(rule.getAgent1(), type, newResult, rule.getAgent1Index(), agent2Index);
    }

    private static RewriteRule mergeRules(RewriteRule rule1, RewriteRule rule2) {
        AgentType agent1 = rule1.getAgent1();
        AgentType agent2 = rule2.getAgent1();
        // Just need to add a wire that maps the two outputs

        // Need to update agent indexes to use the new cloned ports
        Map<Port, Port> rule1ClonePortMap = new HashMap<>();
        Map<Port, Port> rule2ClonePortMap = new HashMap<>();
        NetBase result1 = rule1.getResult().Clone(rule1ClonePortMap);
        NetBase result2 = rule2.getResult().Clone(rule2ClonePortMap);

        Map<Integer, Port> newAgent1Index = rule1.getAgent1Index().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> rule1ClonePortMap.get(e.getValue())));
        Map<Integer, Port> newAgent2Index = rule2.getAgent1Index().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> rule2ClonePortMap.get(e.getValue())));

        Set<Wire> allCurrentWires = new HashSet<>(Sets.union(result1.getWires(), result2.getWires()));
        // Wire newWire = new Wire(result1.getOutputPort2(), result2.getOutputPort2());
        // Not enough, need to collapse this wire
        List<Wire> matchingInResult1 = result1.getWires().stream().filter(
                w -> result1.getOutputPort().equals(w.getPort1()) || result1.getOutputPort().equals(w.getPort2()))
                .toList();
        List<Wire> matchingInResult2 = result2.getWires().stream().filter(
                w -> result2.getOutputPort().equals(w.getPort1()) || result2.getOutputPort().equals(w.getPort2()))
                .toList();
        assert (matchingInResult1.size() <= 1);// Think this holds
        assert (matchingInResult2.size() <= 1);
        Wire newWire = new Wire(result1.getOutputPort(), result2.getOutputPort());
        Stream<Wire> overlappingWires = Stream.concat(matchingInResult1.stream(), matchingInResult2.stream());
        for (Wire w : overlappingWires.collect(Collectors.toList())) {
            allCurrentWires.remove(w);
            if (w.getPort1().equals(newWire.getPort1())) {
                newWire = new Wire(newWire.getPort2(), w.getPort2());
            } else if (w.getPort1().equals(newWire.getPort2())) {
                newWire = new Wire(newWire.getPort1(), w.getPort2());
            } else if (w.getPort2().equals(newWire.getPort1())) {
                newWire = new Wire(newWire.getPort2(), w.getPort1());
            } else {
                assert (w.getPort2().equals(newWire.getPort2()));
                newWire = new Wire(newWire.getPort1(), w.getPort1());
            }

        }

        // Need to swap the sides of all data agents in the second set
        Set<AgentImplementationBase> secondRuleAgents = new HashSet<>();
        for (AgentImplementationBase agent : result2.getAgents()) {
            if (agent instanceof DataAgentImplementation dAgent) {
                secondRuleAgents.add(dAgent.SwitchSides());
            } else {
                secondRuleAgents.add(agent);
            }
        }

        Set<AgentImplementationBase> agents = Sets.union(result1.getAgents(), secondRuleAgents);

        Set<Wire> wires = Sets.union(allCurrentWires, Set.of(newWire));
        NetBase mergedResult = new Net(agents, wires, null);

        // These assertions dont seem to take into account free ports (not sure how they
        // seemed to work before??)
        // for(Port port : newAgent1Index.values())
        // {
        // assert(mergedResult.getPortToAgentMap().containsKey(port));
        // }
        // for(Port port : newAgent2Index.values())
        // {
        // assert(mergedResult.getPortToAgentMap().containsKey(port));
        // }
        // for(Wire wire : wires)
        // {
        // assert(mergedResult.getPortToAgentMap().containsKey(wire.getPort1()));
        // assert(mergedResult.getPortToAgentMap().containsKey(wire.getPort2()));
        // }

        return new RewriteRule(agent1, agent2, mergedResult, newAgent1Index, newAgent2Index);

    }

    private static ProgramBase inlineSingleRule(ProgramBase program, RewriteRule singleRule) {
        assert (!singleRule.getAgentOption2().isPresent());
        Set<? extends ConditionalRewriteRule> allRules = program.getRules();
        allRules = Sets.difference(allRules, Set.of(singleRule));
        // Since this isn't going to work for recursive rules we know single rule's
        // result does not contain its agent.
        // So we can just the inlining to each rule once.

        // Actually no if we have a reduces to b which reduces to c
        // We might replace all b's with c's then a's with b's which still leaves b's
        Set<ConditionalRewriteRule> newRules = allRules.stream()
                .map(rule -> inlineSingleRuleIntoConditionalRule(rule, singleRule)).collect(Collectors.toSet());
        ConditionalRewriteRule newStartingRule = inlineSingleRuleIntoConditionalRule(program.getStartingRule(),
                singleRule);
        return new Program(newRules, newStartingRule);

    }

    private static ConditionalRewriteRule inlineSingleRuleIntoConditionalRule(ConditionalRewriteRule condRule,
            RewriteRule singleRule) {
        Set<RewriteRuleResult> results = condRule.getResults();
        ConditionalRewriteRule newRule = condRule;
        for (RewriteRuleResult result : results) {
            RewriteRuleResult newResult = result;
            Set<AgentImplementationBase> agents = result.net().getAgents().stream()
                    .filter(a -> a.getAgentType().equals(singleRule.getAgent1())).collect(Collectors.toSet());
            for (AgentImplementationBase agent : agents) {

                NetBase newNet = applySingleRuleToNet(newResult.net(), singleRule, agent);
                newNet.setSTDOUTPort(newResult.net().getSTDOUTPort());
                newResult = new RewriteRuleResult(newNet, newResult.agent1Index(), newResult.agent2Index());

            }
            newRule = newRule.ReplaceResultWithConditionalResult(result, new ConditionTreeLeafNode(newResult));

        }
        return newRule;
    }

    private static NetBase applySingleRuleToNet(NetBase netInliningHappeningTo, RewriteRule singleRuleBeingApplied,
            AgentImplementationBase agentBeingReplaced) {

        RewriteRuleResult ResultSubbingInto = singleRuleBeingApplied.getRewriteRuleResult().Clone();

        DataSource dataSource1 = (agentBeingReplaced instanceof DataAgentImplementation dataAgent1)
                ? dataAgent1.getDataSource()
                : null;
        Set<AgentImplementationBase> agents = new HashSet<>(ResultSubbingInto.net().getAgents());

        agents = agents.stream().map(a -> {
            if (a instanceof DataAgentImplementation DataAgent) {
                return DataAgent.Inline(dataSource1, null);
            } else {
                return a;
            }
        }).collect(Collectors.toSet());

        agents.addAll(netInliningHappeningTo.getAgents());
        agents.remove(agentBeingReplaced);

        Set<Wire> wires = new HashSet<>(netInliningHappeningTo.getWires());
        wires.removeAll(ResultSubbingInto.net().getWires());
        wires.addAll(ResultSubbingInto.net().getWires());

        for (Entry<Integer, Port> entry : ResultSubbingInto.agent1Index().entrySet()) {
            wires.add(new Wire(agentBeingReplaced.getAuxillaryPorts().get(entry.getKey()), entry.getValue()));
        }
        wires.add(new Wire(agentBeingReplaced.getPrinciplePort(), ResultSubbingInto.net().getOutputPort()));

        wires = Wire.flattenWires(wires);

        return new Net(agents, wires, netInliningHappeningTo.getOutputPort());

    }
}
