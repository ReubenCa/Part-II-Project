// package com.carolang.NetInerpreter;

// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.Comparator;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.LinkedList;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.Queue;
// import java.util.Set;
// import java.util.function.Function;
// import java.uti>
// import org.antlr.v4.Tool;
// import org.antlr.v4.parse.ANTLRParser.elementOptions_return;
// import org.checkerframework.checker.units.qual.A;

// import com.carolang.AgentImplementation;
// import com.carolang.DataAgentImplementation;
// import com.carolang.constructionProgram;
// import com.carolang.constructionNet;
// import com.carolang.common.interaction_rules.AgentType;
// import com.carolang.common.interaction_rules.IAgentImplementation;
// import com.carolang.common.interaction_rules.IProgram;
// import com.carolang.common.interaction_rules.IRewriteRule;
// import com.carolang.common.interaction_rules.Net;
// import com.carolang.common.interaction_rules.InteractionNet;
// import com.carolang.common.interaction_rules.MagicRewriteRule;
// import com.carolang.common.interaction_rules.Port;
// import com.carolang.common.interaction_rules.RewriteRule;
// import com.carolang.common.interaction_rules.UnlabelledPort;
// import com.carolang.common.interaction_rules.Wire;
// import com.google.common.collect.Sets;
// import com.google.common.collect.Sets.SetView;

// //A fairly inefficient interpreter for Nets that can be used in unit testing.
// public class NetInterpreter {

//     private static AgentType outputAgentType = new AgentType(0, "STDOUT", null);
//     private static AgentType curriedAdditionType = new AgentType(1, "CurriedAddition", null, "intDataAgent", "allocateIntDataAgent");
//     private static AgentType curriedEqualityType = new AgentType(1, "CurriedEquality", null, "intDataAgent", "allocateIntDataAgent");

//     private static Set<RewriteRule> getAdditionalRules() {

//         MagicRewriteRule outputIntRule = new MagicRewriteRule(outputAgentType,
//                 constructionProgram.getIntegerAgentType(),
//                 new InteractionNet(Set.of(), Set.of(), null), Map.of(), Map.of(), "OutputINT");

//         MagicRewriteRule outputTrueRule = new MagicRewriteRule(outputAgentType, constructionProgram.getTrueAgentType(),
//                 new InteractionNet(Set.of(), Set.of(), null), Map.of(), Map.of(), "OutputTRUE");

//         MagicRewriteRule outputFalseRule = new MagicRewriteRule(outputAgentType,
//                 constructionProgram.getFalseAgentType(),
//                 new InteractionNet(Set.of(), Set.of(), null), Map.of(), Map.of(), "OutputFALSE");

//         MagicRewriteRule additionOneRule = new MagicRewriteRule(constructionProgram.getAdditionAgent(),
//                 constructionProgram.getIntegerAgentType(), null, Map.of(), Map.of(), "curriedAddition");

//         MagicRewriteRule additionTwoRule = new MagicRewriteRule(curriedAdditionType,
//                 constructionProgram.getIntegerAgentType(),
//                 null, Map.of(), Map.of(), "AppliedAddition");

//         MagicRewriteRule equalityOneRule = new MagicRewriteRule(constructionProgram.getEqualityAgentType(),
//                 constructionProgram.getIntegerAgentType(),
//                 null, Map.of(), Map.of(), "curriedEquality");

//         MagicRewriteRule equalityTwoRule = new MagicRewriteRule(curriedEqualityType,
//                 constructionProgram.getIntegerAgentType(),
//                 null, Map.of(), Map.of(), "AppliedEquality");

//         return Set.of(outputIntRule, additionOneRule, additionTwoRule, equalityOneRule, equalityTwoRule, outputTrueRule,
//                 outputFalseRule);
//     }

//     private static Net addOutputAgent(Net net) {
//         Port outputPort = net.getOutputPort2();
//         Port stdOutPort = new UnlabelledPort();
//         Wire newWire = new Wire(outputPort, stdOutPort);
//         AgentImplementation stdOutAgent = new AgentImplementation(List.of(), stdOutPort, outputAgentType);
//         return new InteractionNet(Sets.union(net.getAgents(), Set.of(stdOutAgent)),
//                 Sets.union(net.getWires(), Set.of(newWire)), null);
//     }

//     public static Integer Interpret(IProgram program) throws TooLongToReduceException {

//         Set<? extends IRewriteRule> rules = program.getRules();
//         rules = Sets.union(rules, getAdditionalRules());

//         Net net = program.getStartingNet();
//         net = addOutputAgent(net);
//         System.out.println("Program\n%s".formatted(program));
//         System.out.println("Adjusted Starting Net\n%s".formatted(net));
//         for (int i = 0; i < 50; i++) {
//             ReductionResult reduction = makeReduction(rules, net);
//             if (reduction.output.isPresent()) {
//                 return reduction.output.get();
//             }
//             net = reduction.resultNet.get();
//             System.out.println("Net\n%s".formatted(net));
//         }
//         throw new TooLongToReduceException();
//     }

//     private static class ReductionResult {
//         Optional<Net> resultNet;
//         Optional<Integer> output;

//         ReductionResult(Net net) {
//             resultNet = Optional.of(net);
//             output = Optional.empty();

//         }

//         ReductionResult(Integer output) {
//             this.output = Optional.of(output);
//             resultNet = Optional.empty();
//         }
//     }

//     private static ReductionResult makeReduction(Set<? extends IRewriteRule> rules, Net net) {
//         List<IAgentImplementation> sortedAgents = new ArrayList<>(net.getAgents());
//         sortedAgents.sort(new Comparator<IAgentImplementation>() {
//             public int compare(IAgentImplementation o1, IAgentImplementation o2) {

//                 String id1 = o1.getType().getHumanReadableId();
//                 String id2 = o2.getType().getHumanReadableId();
//                 int compare = id1.compareTo(id2);
//                 if (compare != 0) {
//                     return -compare;
//                 }
//                 return -o1.toString().compareTo(o2.toString());
//             }
//         });

//         for (IAgentImplementation agent : sortedAgents) {
//             Port principlePort = agent.getPrinciplePort();
//             // Note if there are two nodes facing each other that both represent single
//             // agent rewrites it is unspecified which gets picked first
//             List<Wire> matchingWires = net.getWires().stream()
//                     .filter(w -> w.getPort1().equals(principlePort) || w.getPort2().equals(principlePort))
//                     .toList();
//             assert (matchingWires.size() <= 1); // can't have two wires connected to same port
//             if (matchingWires.isEmpty()) {
//                 continue;
//             }
//             Wire wire = matchingWires.get(0);
//             Port otherPort = wire.getPort1().equals(principlePort) ? wire.getPort2() : wire.getPort1();

//             List<? extends IAgentImplementation> matchingAgents = net.getAgents().stream()
//                     .filter(a -> a.getPrinciplePort().equals(otherPort))
//                     .toList();
//             assert (matchingAgents.size() <= 1); // Shouldn't have two agents connected to the same port
//             if (matchingAgents.isEmpty()) {
//                 continue;
//             }
//             IAgentImplementation otherAgent = matchingAgents.get(0);
//             List<? extends IRewriteRule> matchingRules = rules.stream()
//                     .filter(r -> r.getAgent1().equals(agent.getType())
//                             && (r.getAgent2().isEmpty() || r.getAgent2().get().equals(otherAgent.getType())))
//                     .toList();
//             assert (matchingRules.size() <= 1); // Don't want ambiguity
//             if (matchingRules.isEmpty()) {
//                 continue;
//             }
//             System.out.println("Applying rule: " + matchingRules.get(0));

//             IRewriteRule rule = matchingRules.get(0);
//             if (rule instanceof MagicRewriteRule mRule && (mRule.getMagicTag() == "OutputINT"
//                     || mRule.getMagicTag() == "OutputTRUE" || mRule.getMagicTag() == "OutputFALSE")) {
//                 assert (agent.getType() == outputAgentType || otherAgent.getType() == outputAgentType);
//                 IAgentImplementation intAgent = agent.getType() == outputAgentType ? otherAgent : agent;
//                 if (intAgent instanceof DataAgentImplementation<?> dataAgent) {
//                     if (dataAgent.getData() instanceof Boolean) {
//                         return new ReductionResult(((Boolean) dataAgent.getData()) ? 1 : 0);
//                     }
//                     assert (dataAgent.getData() instanceof Integer);

//                     return new ReductionResult((Integer) (dataAgent.getData()));
//                 }

//             }

//             if (!rule.getAgent2().isPresent()) {

//                 List<? extends IRewriteRule> secondMatchingRules = rules.stream()
//                         .filter(r -> r.getAgent1().equals(otherAgent.getType())
//                                 && (r.getAgent2().isEmpty()))
//                         .toList();

//                 assert (secondMatchingRules.size() <= 1);

//                 if (secondMatchingRules.size() != 0) {
//                     IRewriteRule secondMatchingRule = secondMatchingRules.get(0);
//                     assert (secondMatchingRule.getAgent2().isEmpty());
//                     rule = mergeRules(rule, secondMatchingRule);
//                     System.out.println("Merging with rule: " + secondMatchingRule);
//                 }

//             }
//             return new ReductionResult(applyRule(net, rule, agent, otherAgent, wire));

//         }
//         throw new UnsupportedOperationException("Program not outputted but has no reduction rules");
//     }

//     private static RewriteRule mergeRules(IRewriteRule rule1, IRewriteRule rule2) {
//         AgentType agent1 = rule1.getAgent1();
//         AgentType agent2 = rule2.getAgent1();
//         // Just need to add a wire that maps the two outputs
//         Net result1 = rule1.getResult();
//         Net result2 = rule2.getResult();
//         Set<Wire> allCurrentWires = new HashSet<>(Sets.union(result1.getWires(), result2.getWires()));
//         // Wire newWire = new Wire(result1.getOutputPort2(), result2.getOutputPort2());
//         // Not enough, need to collapse this wire
//         List<Wire> matchingInResult1 = result1.getWires().stream().filter(
//                 w -> result1.getOutputPort2().equals(w.getPort1()) || result1.getOutputPort2().equals(w.getPort2()))
//                 .toList();
//         List<Wire> matchingInResult2 = result2.getWires().stream().filter(
//                 w -> result2.getOutputPort2().equals(w.getPort1()) || result2.getOutputPort2().equals(w.getPort2()))
//                 .toList();
//         assert (matchingInResult1.size() <= 1);// Think this holds
//         assert (matchingInResult2.size() <= 1);
//         Wire newWire = new Wire(result1.getOutputPort2(), result2.getOutputPort2());
//         Stream<Wire> overlappingWires = Stream.concat(matchingInResult1.stream(), matchingInResult2.stream());
//         for (Wire w : overlappingWires.collect(Collectors.toList())) {
//             allCurrentWires.remove(w);
//             if (w.getPort1().equals(newWire.getPort1())) {
//                 newWire = new Wire(newWire.getPort2(), w.getPort2());
//             } else if (w.getPort1().equals(newWire.getPort2())) {
//                 newWire = new Wire(newWire.getPort1(), w.getPort2());
//             } else if (w.getPort2().equals(newWire.getPort1())) {
//                 newWire = new Wire(newWire.getPort2(), w.getPort1());
//             } else {
//                 assert (w.getPort2().equals(newWire.getPort2()));
//                 newWire = new Wire(newWire.getPort1(), w.getPort1());
//             }

//         }

//         Set<IAgentImplementation> agents = Sets.union(result1.getAgents(), result2.getAgents());

//         Set<Wire> wires = Sets.union(allCurrentWires, Set.of(newWire));
//         Net mergedResult = new InteractionNet(agents, wires, null);
//         return new RewriteRule(agent1, agent2, mergedResult, rule1.getAgent1Index(), rule2.getAgent1Index());

//     }

//     private static Net applyRule(Net net, IRewriteRule rule, IAgentImplementation agent1, IAgentImplementation agent2,
//             Wire wire) {

//         // Originally used Merge function but didn't really work as it isnt really what
//         // merging is for
//         if (rule instanceof RewriteRule r) {
//             rule = r.Clone();// Otherwise we get confused over which port is which
//         }
//         final boolean twoWayRule = rule.getAgent2().isPresent(); // If rule specifies type of second agent we want to
//                                                                  // also destroy it otherwise we just want to keep it as
//                                                                  // is
//         Map<Integer, Port> agent1Index = rule.getAgent1Index();
//         Optional<Map<Integer, Port>> agent2Index = rule.getAgent2Index();
//         Net result;
//         if (!(rule instanceof MagicRewriteRule)) {
//             result = rule.getResult();
//         } else if (rule instanceof MagicRewriteRule mRule
//                 && (mRule.getMagicTag().equals("curriedAddition") || mRule.getMagicTag().equals("curriedEquality"))) {
//             IAgentImplementation integerAgent = agent1 instanceof DataAgentImplementation ? agent1 : agent2;
//             Integer data = (Integer) ((DataAgentImplementation<?>) integerAgent).getData();
//             UnlabelledPort principlePort = new UnlabelledPort();
//             UnlabelledPort auxPort = new UnlabelledPort();
//             AgentType type = mRule.getMagicTag().equals("curriedAddition") ? curriedAdditionType : curriedEqualityType;
//             AgentImplementation curriedResultAgent = new DataAgentImplementation<Integer>(List.of(auxPort),
//                     principlePort,
//                     type, data);
//             result = new InteractionNet(Set.of(curriedResultAgent), Set.of(), auxPort);
//             agent1Index = Map.of(0, principlePort, 1, auxPort);

//         } else if (rule instanceof MagicRewriteRule mRule
//                 && (mRule.getMagicTag().equals("AppliedAddition") || mRule.getMagicTag().equals("AppliedEquality"))) {
//             IAgentImplementation integerAgent = agent1 instanceof DataAgentImplementation ? agent1 : agent2;
//             Integer integerAgentValue = (Integer) ((DataAgentImplementation<?>) integerAgent).getData();
//             IAgentImplementation curriedAdditionAgent = agent1 == integerAgent ? agent2 : agent1;
//             Integer CurriedAgentValue = (Integer) ((DataAgentImplementation<?>) curriedAdditionAgent).getData();

//             UnlabelledPort integerOutputPort = new UnlabelledPort();
//             IAgentImplementation resultagent;
//             if (mRule.getMagicTag().equals("AppliedAddition")) {
//                 resultagent = new DataAgentImplementation<Integer>(List.of(), integerOutputPort,
//                         constructionProgram.getIntegerAgentType(), integerAgentValue + CurriedAgentValue);
//             } else {
//                 Boolean value = integerAgentValue == CurriedAgentValue;
//                 AgentType type = value ? constructionProgram.getTrueAgentType()
//                         : constructionProgram.getFalseAgentType();
//                         //TODO: low priority - but we treat bools as two distinct agent types shouldnt be dataagentimplementation
//                 resultagent = new DataAgentImplementation<Boolean>(List.of(), integerOutputPort,
//                         type, value);
//             }
//             result = new InteractionNet(Set.of(resultagent), Set.of(), integerOutputPort);
//             agent1Index = Map.of(0, integerOutputPort);
//         } else {
//             throw new UnsupportedOperationException("Invalid Magic Rule type");
//         }
//         Set<IAgentImplementation> agents = Stream.concat(
//                 net.getAgents().stream().filter(a -> a != agent1 && a != (twoWayRule ? agent2 : null)),
//                 result.getAgents().stream()).collect(Collectors.toSet());
//         Set<Wire> wires = new HashSet<>(
//                 Sets.union(net.getWires().stream().filter(w -> w != wire).collect(Collectors.toSet()),
//                         result.getWires()));

//         // This takes ports in the outer net and maps them to the ports in the inner
//         // result net they should be attached to
//         final Map<Integer, Port> finalAgent1Index = new HashMap<>(agent1Index);
//         Function<Port, Port> portMappings = p -> {
//             if (agent1.getAuxillaryPorts().contains(p)) {
//                 int index = agent1.getAuxillaryPorts().indexOf(p);

//                 Port portToMapTo = finalAgent1Index.get(index);
//                 assert (portToMapTo != null);
//                 return portToMapTo;
//             } else if (twoWayRule && agent2.getAuxillaryPorts().contains(p)) {
//                 int index = agent2.getAuxillaryPorts().indexOf(p);

//                 Port portToMapTo = agent2Index.get().get(index);
//                 assert (portToMapTo != null);
//                 return portToMapTo;
//             } else {
//                 return p;
//             }
//         };

//         Stream<Port> auxPortsStream = agent1.getAuxillaryPorts().stream();
//         if (twoWayRule) {
//             auxPortsStream = Stream.concat(auxPortsStream, agent2.getAuxillaryPorts().stream());
//         }

//         List<Wire> newWires = new ArrayList<>(auxPortsStream.map(p -> new Wire(p, portMappings.apply(p))).toList());
//         if (!twoWayRule) {
//             // newWires.add(new Wire(result.getOutputPort(), agent1.getPrinciplePort()));
//             Port otherEndOfInteractionWire = wire.getPort1().equals(agent1.getPrinciplePort()) ? wire.getPort2()
//                     : wire.getPort1();
//             newWires.add(new Wire(rule.getResult().getOutputPort2(), otherEndOfInteractionWire));
//         }

//         // Want to add new wire or if it would form double wire merge instead
//         Queue<Wire> newWiresQueue = new LinkedList<>(newWires);// Needed a queue as we can have lots of wires in a row
//                                                                // (arbitrarily many) so newWires need to re-enter queue
//                                                                // to see if they can merge again
//         while (newWiresQueue.size() > 0) {
//             Wire w = newWiresQueue.poll();
//             final Wire wCopy = w;
//             List<Wire> overlappingWires = wires.stream()
//                     .filter(w2 -> w2.getPort1().equals(wCopy.getPort1()) || w2.getPort1().equals(wCopy.getPort2())
//                             || w2.getPort2().equals(wCopy.getPort1()) || w2.getPort2().equals(wCopy.getPort2()))
//                     .toList();
//             assert (overlappingWires.size() <= 2);
//             // Wire overlappingWire = overlappingWires.get(0);
//             for (int i = 0; i < overlappingWires.size(); i++) {
//                 Wire overlappingWire = overlappingWires.get(i);
//                 wires.remove(overlappingWire);
//                 Wire mergedWire;
//                 // Create and add a new wire of the ports that arent equal
//                 if (overlappingWire.getPort1() == w.getPort1()) {
//                     mergedWire = new Wire(overlappingWire.getPort2(), w.getPort2());
//                 } else if (overlappingWire.getPort1() == w.getPort2()) {
//                     mergedWire = new Wire(overlappingWire.getPort2(), w.getPort1());
//                 } else if (overlappingWire.getPort2() == w.getPort1()) {
//                     mergedWire = new Wire(overlappingWire.getPort1(), w.getPort2());
//                 } else {
//                     assert (overlappingWire.getPort2() == w.getPort2());
//                     mergedWire = new Wire(overlappingWire.getPort1(), w.getPort1());
//                 }
//                 w = mergedWire;// In case of needing to merge at either end of the wire
//             }

//             if (overlappingWires.size() == 0) {
//                 wires.add(w);
//             } else {
//                 // Might be further merges to do
//                 newWiresQueue.add(w);
//             }
//         }

//         return new InteractionNet(agents, wires, net.getOutputPort2());

//     }

//     // TODO let this return more than ints
//     // interface InterpreterResult
//     // {

//     // }

//     // class ValueResult<T> implements InterpreterResult
//     // {
//     // public T get()
//     // {

//     // }
//     // }

//     // class FunctionResult<T extends InterpreterResult, E extends
//     // InterpreterResult> implements InterpreterResult
//     // {
//     // public E apply(T input)
//     // {
//     // throw new UnsupportedOperationException("Not implemented yet");
//     // }
//     // }

// }