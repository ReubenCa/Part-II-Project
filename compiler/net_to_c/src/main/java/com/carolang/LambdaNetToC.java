package com.carolang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;

import org.pcollections.HashPMap;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.data_sources.DataSource;
import com.carolang.common.types.ListType;
import com.carolang.common.types.Type;
import com.carolang.common.interaction_rules.AgentImplementationBase;
import com.carolang.common.interaction_rules.ConditionalRewriteRule;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;
import com.carolang.common.interaction_rules.NetBase;
import com.carolang.common.interaction_rules.Port;
import com.carolang.common.interaction_rules.ProgramBase;
import com.carolang.common.interaction_rules.Wire;

public class LambdaNetToC {
    private Map<AgentType, String> agentTypeToProgramVar = new HashMap<>();
    private Map<ConditionalRewriteRule, String> ruleNames = new HashMap<>();

    private final ProgramBase Program;
    private final Type programOutputType;
    private final List<Type> typeChain;

    private final static String includes = "#include \"structs.h\"\n" + //
            "#include \"workStealingQueue.h\"\n" + //
            "#include \"utilities.h\"\n" + //
            "#include <stdatomic.h>\n" + //
            "#include <stdlib.h>\n" + //
            "#include <stdio.h>\n" + //
            "#include <stdbool.h>\n" + //
            "#include <memoryManager.h>\n" + //
            "#include <assert.h>\n\n";

    public static String GenerateCCode(ProgramBase rules, Type programOutputType) {
        return (new LambdaNetToC(rules, programOutputType)).generateCCode();
    }

    private LambdaNetToC(ProgramBase rules, Type programType) {
        this.Program = rules;
        typeChain = programType.typeChain();
        programOutputType = typeChain.get(typeChain.size() - 1);
        assert (programOutputType != null);
    }

    private int getListDepthOfType(Type type) {
        int count = 0;
        while (type instanceof ListType lType) {
            count++;
            type = lType.elementType;
        }
        return count;
    }

    private Type getElementType(Type type) {

        if (type instanceof ListType lType) {
            return getElementType(lType.elementType);
        }
        return type;
    }

    private String printElementType(Type elementType) {
        if (elementType == Type.Int) {
            return "printf(\"%d\", ((intDataAgent *)(agentType)) -> data);\n";
        } else if (elementType == Type.Float) {
            return "printf(\"%f\", ((floatDataAgent *)(agentType)) -> data);\n";}
        else if (elementType == Type.Boolean)
        {
            return "bool result = *agentType == %s;".formatted(agentTypeToProgramVar.get(constructionProgram.getTrueAgentType())) +  "\n printf(result ? \"true\" : \"false\");\n";
        } else {
            throw new IllegalArgumentException("Cannot print element of type %s".formatted(elementType));
        }
    }

    private String generateCCode() {

        StringBuilder cProgram = new StringBuilder();
        Set<? extends ConditionalRewriteRule> ConditionalRewriteRules = Program.getRules();
        cProgram.append(includes);
        cProgram.append(defineAgentIds(ConditionalRewriteRules));
        cProgram.append("extern void IncreaseHotness(Queue *q, Wire *w);\n" +
                "extern void HandleLackOfRule(void);\n" +
                "extern Port *GetOtherPort(Port *p);\n" +
                "extern bool isFirst(Port *p);\n\n");

        cProgram.append("Port* OUTPUT_PORT = NULL;\n");
        cProgram.append("const int NESTED_LIST_OUTPUT_DEPTH = %d;\n".formatted(getListDepthOfType(programOutputType)));
        cProgram.append(createInputVariables());
        cProgram.append(makeReadInputsFunction());
        cProgram.append("void OutputElement(int *agentType){\n%s\n}\n"
                .formatted(printElementType(getElementType(programOutputType))));
        cProgram.append(initialisationFunctionBody());

        int count = 0;
        for (ConditionalRewriteRule r : ConditionalRewriteRules) {
            ruleNames.put(r, "Rule%d".formatted(count++));
            String FirstAgentName = r.getAgent1().getHumanReadableId() + "_PARAM_1";

            String SecondAgentName = r.getAgent2().getHumanReadableId() + "_PARAM_2";

            String Agent1Type = r.getAgent1().getCProgramStructType();
            String Agent2Type = r.getAgent2().getCProgramStructType();

            String t = outputRule(r, FirstAgentName, SecondAgentName, Agent1Type, Agent2Type);
            cProgram.append(t);
        }
        cProgram.append("void ApplyRule(Queue *queue, int *agent1Type, int  *agent2Type)\n{\n%s\n}"
                .formatted(generateApplyRuleFunctionBody(Program.getRules())));
        return cProgram.toString();
    }

    private StringBuilder createInputVariables() {
        StringBuilder sb = new StringBuilder();
        int numberOfInputs = typeChain.size() - 1;
        for (int i = 0; i < numberOfInputs; i++) {
            Type t = typeChain.get(i);
            String typeName;
            if (t == Type.Int) {
                typeName = "int";
            } else if (t == Type.Float) {
                typeName = "float";
            } else {
                throw new RuntimeException(
                        "shouldn't be reachable - exception should be thrown when creating input agents");
            }
            sb.append("%s USER_INPUT_%d;".formatted(typeName, i));

        }
        return sb;
    }

    private StringBuilder makeReadInputsFunction() {
        StringBuilder sb = new StringBuilder();
        sb.append("void ReadInputs(void){\n");

        int numberOfInputs = typeChain.size() - 1;
        if (numberOfInputs == 0) {
            sb.append("return;\n");
            sb.append("}\n");
            return sb;
        }
        sb.append("int s = scanf(\"");
        String prefix = "";
        for (int i = 0; i < numberOfInputs; i++) {
            sb.append(prefix);
            prefix = " ";
            Type t = typeChain.get(i);

            if (t == Type.Int) {
                sb.append("%d");
            } else if (t == Type.Float) {
                sb.append("%f");
            } else {
                throw new RuntimeException(
                        "shouldn't be reachable - exception should be thrown when creating input agents");
            }
        }
        sb.append("\"");
        for (int i = 0; i < numberOfInputs; i++) {
            sb.append(", &USER_INPUT_%d".formatted(i));
        }

        sb.append(");\n");
        sb.append("if (s < %d) {\n".formatted(numberOfInputs)
                + "    fprintf(stderr, \"Error reading inputs\\n\");\n"
                + "    exit(1);\n"
                + "}\n}");
        return sb;
    }

    private String outputRule(ConditionalRewriteRule rule, String firstAgentParameterName,
            String secondAgentParameterName,
            String Agent1Type,
            String Agent2Type) {

        StringBuilder ruleBodySB = new StringBuilder();

        ruleBodySB.append("void %s(Queue *queue, %s *%s, %s *%s) {\n".formatted(ruleNames.get(rule),
                Agent1Type,
                firstAgentParameterName, Agent2Type, secondAgentParameterName));
        ruleBodySB.append(rule.customInstructionsAtBeginning(firstAgentParameterName, secondAgentParameterName));
        ruleBodySB.append("\n");

       

        BiFunction<RewriteRuleResult, HashPMap<DataSource, String>, String> writeCodeForResult = (result,
                alreadyMapped) -> {
            return createOutcome(firstAgentParameterName, secondAgentParameterName, result.net(), result.agent1Index(),
                    result.agent2Index(), alreadyMapped).toString();
        };
        ruleBodySB.append(rule.generateCCode(writeCodeForResult, firstAgentParameterName, secondAgentParameterName));

        // String t = ("%s \nvoid %s(Queue *queue, %s *%s, %s *%s)
        // {\n%s\n}".formatted(OutcomesToBodyMap, ruleNames.get(rule),
        // Agent1Type,
        // firstAgentParameterName, Agent2Type, secondAgentParameterName,
        // ruleBodySB));
        ruleBodySB.append("}");
        return ruleBodySB.toString();
    }

    public StringBuilder defineAgentIds(Set<? extends ConditionalRewriteRule> allRules) {
        StringBuilder sb = new StringBuilder();
        String emptyListAgentType = "-1";// if we never use lists this can be whatever

        Set<AgentType> agentTypes = Program.getAgents();
        if(programOutputType == Type.Boolean)
        {
            agentTypes.add(constructionProgram.getTrueAgentType());
            agentTypes.add(constructionProgram.getFalseAgentType());//Otherwise it is possible we never assign them indexes
        }
        final String agentIdInitialiseString = "const static int %s = %s;\n";
        for (AgentType agentType : agentTypes) {
            String id = "%d".formatted(agentType.getId());
            String CVar = "%s_AGENT_TYPE_".formatted(agentType.getHumanReadableId());
            sb.append(String.format(agentIdInitialiseString, CVar, id));
            agentTypeToProgramVar.put(agentType, CVar);
            if (agentType == ProgramBase.EmptyListAgentType) {
                emptyListAgentType = CVar;
            }
        }

        sb.append("\nconst int EMPTY_LIST_AGENT_TYPE = %s;\n".formatted(emptyListAgentType));
        return sb;
    }

    public StringBuilder initialisationFunctionBody() {
        StringBuilder sb = new StringBuilder();
        ruleNames.put(Program.getStartingRule(), "INITIALISATION_RULE");
        sb.append(outputRule(Program.getStartingRule(), "SHOULD_NEVER_GET_USED1", "SHOULD_NEVER_GET_USED2",
                "Agent/*always null*/", "Agent/*always null*/"));
        return sb;
    }

    private StringBuilder createOutcome(String firstAgentParameterName,
            String secondAgentParameterName,
            NetBase result, Map<Integer, Port> agent1Index,
            Map<Integer, Port> agent2Index,
            HashPMap<DataSource, String> dataSourcesAlreadyAllocated) {

        Map<AgentImplementationBase, String> agentVarNames = new HashMap<>();
        Map<Port, String> portVarNames = new HashMap<>();

        agent1Index = new HashMap<>(agent1Index);// We modify these so make copies
        agent2Index = new HashMap<>(agent2Index);
        StringBuilder sb = new StringBuilder();
        int agentCount = 0;
        for (AgentImplementationBase agent : result.getAgents()) {
            String agentCVarName;
            if (!agentVarNames.containsKey(agent)) {
                agentCVarName = "%s_%d".formatted(agent.getHumanReadableIdentifier(), agentCount);
                // sb.append("Agent *%s = allocateAgent(%d);\n".formatted(agentCVarName,
                // agent.getAuxillaryPorts().size()));
                sb.append(agent.cCodeToAllocate(agentCVarName, firstAgentParameterName, secondAgentParameterName,
                        dataSourcesAlreadyAllocated));
                agentVarNames.put(agent, agentCVarName);

            } else {
                agentCVarName = agentVarNames.get(agent);
            }
            sb.append("%s -> type = %s;\n".formatted(agentCVarName, agentTypeToProgramVar.get(agent.getAgentType())));
            String principlePortName = "%sPrinciplePort".formatted(agentCVarName);
            portVarNames.put(agent.getPrinciplePort(), principlePortName);
            sb.append("Port *%s = &(%s -> principlePort);\n".formatted(principlePortName, agentCVarName));
            for (int i = 0; i < agent.getAuxillaryPorts().size(); i++) {
                String portName = "%sAuxPort%d".formatted(agentCVarName, i);
                sb.append("Port *%s = &(%s -> auxPorts[%d]);".formatted(portName, agentCVarName, i));
                portVarNames.put(agent.getAuxillaryPorts().get(i), portName);
            }
            agentCount++;

        }
        Map<Port, ? extends AgentImplementationBase> portToAgent = result.getPortToAgentMap();
        AllocateWires(firstAgentParameterName, secondAgentParameterName, portVarNames, result, agent1Index,
                agent2Index, sb, portToAgent);

        if (result.getSTDOUTPort().isPresent()) {
            sb.append("OUTPUT_PORT = %s;//Setting output Port (Should be in initialisation rule)\n"
                    .formatted(portVarNames.get(result.getSTDOUTPort().get())));

        }
        // Map External Ports in the result to the aux ports of the agents being
        // rewritten

        // Iterate over Agent1Map and Agent2Map;
        List<Set<Entry<Integer, Port>>> agentIndexes = new ArrayList<>();
        agentIndexes.add(agent1Index.entrySet());
        agentIndexes.add(agent2Index.entrySet());
        StringBuilder increaseHotnessSB = new StringBuilder();

        String agentName = firstAgentParameterName;
        for (Set<Entry<Integer, Port>> entrySet : agentIndexes) {
            for (Entry<Integer, Port> entry : entrySet) {
                Port portInDefinition = entry.getValue();
                AgentImplementationBase agentOfPort = portToAgent.get(portInDefinition);
                String portVarName;
                String agentVarName = agentVarNames.get(agentOfPort);
                if (portInDefinition == agentOfPort.getPrinciplePort()) {

                    portVarName = "%sPrinciplePort".formatted(agentVarName);
                } else {
                    portVarName = "%sAuxPort%d".formatted(agentVarName,
                            agentOfPort.getAuxillaryPorts().indexOf(portInDefinition));
                }
                String agentAuxPort = "(&(%s -> auxPorts[%d]))".formatted(agentName, entry.getKey());
                String wireVarName = "Agent%sAuxPort%dWire".formatted(agentName, entry.getKey());

                sb.append("Wire *%s = %s -> pointingAt;\n".formatted(wireVarName, agentAuxPort));
                sb.append("%s -> pointingAt = %s;\n".formatted(portVarName, wireVarName));
                // sb.append("%s -> isFirst = %s -> isFirst;\n".formatted(portVarName,
                // agentAuxPort));
                sb.append("if(isFirst(%s)) {\n".formatted(agentAuxPort) +
                        "%s -> wireEnd1 = %s; \n}\n else {".formatted(wireVarName, portVarName) +
                        "%s -> wireEnd2 = %s;\n }\n".formatted(wireVarName, portVarName));

                if (portInDefinition == agentOfPort.getPrinciplePort()) {
                    increaseHotnessSB.append("IncreaseHotness(queue, %s);\n".formatted(wireVarName));
                }

            }
            agentName = secondAgentParameterName;
        }

        sb.append("freeWrapper(%s -> principlePort.pointingAt);\n".formatted(firstAgentParameterName));
        sb.append("freeWrapper(%s);\n".formatted(firstAgentParameterName));
        sb.append("freeWrapper(%s);\n".formatted(secondAgentParameterName));
        sb.append(increaseHotnessSB);// Want this last as it involves atomics and limits GCC's ability to optimise
        sb.append("//inner Rule Returning\n");
        return sb;
    }

    private void AllocateWires(String firstAgentParameterName, String secondAgentParameterName,
            Map<Port, String> portVarNames, NetBase result, Map<Integer, Port> agent1Index,
            Map<Integer, Port> agent2Index,
            StringBuilder sb, Map<Port, ? extends AgentImplementationBase> portToAgent) {
        // Allocate Wires
        int count = 0;

        for (Wire wire : result.getWires()) {
            // TODO: I think the way free ports are handled can cause a concurrency bug - it
            // takes a fairly contrived program to expose it though so will fix if there is
            // time
            String wireCVarName = "wire%d".formatted(count++);
            int hotness = 0;
            Port port1 = wire.getPort1();
            Port port2 = wire.getPort2();
            // if Port1 or Port2 are external ports we need to instead have the runtime grab
            // the wire attached to that external port
            // and allocate half the wire to that
            // So is it as simple as replacing port1Name/port2Name with the code to grab the
            // other end of the wire?
            AgentImplementationBase agent1 = portToAgent.get(port1);
            String port1Name;
            String hotnessVarName = "hotness%d".formatted(count++);
            StringBuilder DynamicHotnessCalculatorString = new StringBuilder();
            sb.append("int %s = 0;\n".formatted(hotnessVarName));
            boolean EitherPortWasFree = false;
            if (agent1 != null) {
                port1Name = portVarNames.get(port1);
                if (port1 == agent1.getPrinciplePort()) {
                    hotness++;
                }
            } else {
                // Port1 is free
                EitherPortWasFree = true;
                boolean isInFirstIndex;
                int index;
                if (agent1Index.containsValue(port1)) {
                    index = agent1Index.entrySet().stream().filter(e -> e.getValue() == port1).findFirst().get()
                            .getKey();
                    agent1Index.remove(index);
                    isInFirstIndex = true;
                } else {
                    assert (agent2Index.containsValue(port1));
                    index = agent2Index.entrySet().stream().filter(e -> e.getValue() == port1).findFirst().get()
                            .getKey();
                    agent2Index.remove(index);
                    isInFirstIndex = false;
                }
                String externalPort = "(&(%s -> auxPorts[%d]))"
                        .formatted(isInFirstIndex ? firstAgentParameterName : secondAgentParameterName, index);
                port1Name = "FreePort%d".formatted(count++);
                sb.append("Port *%s = GetOtherPort(%s);\n".formatted(port1Name, externalPort));
                sb.append("Wire* WireToFree%d = %s -> pointingAt;\n".formatted(count, externalPort));
                DynamicHotnessCalculatorString
                        .append("%s += WireToFree%d -> hotness;\n".formatted(hotnessVarName, count));
                DynamicHotnessCalculatorString.append("freeWrapper(WireToFree%d);\n".formatted(count++));
            }
            AgentImplementationBase agent2 = portToAgent.get(port2);
            String port2Name;
            if (agent2 != null) {
                port2Name = portVarNames.get(port2);
                if (port2 == agent2.getPrinciplePort()) {
                    hotness++;
                }
            } else {
                // Port2 is free
                EitherPortWasFree = true;
                boolean isInFirstIndex;
                int index;
                if (agent1Index.containsValue(port2)) {
                    index = agent1Index.entrySet().stream().filter(e -> e.getValue() == port2).findFirst().get()
                            .getKey();
                    agent1Index.remove(index);
                    isInFirstIndex = true;
                } else {
                    assert (agent2Index.containsValue(port2));
                    index = agent2Index.entrySet().stream().filter(e -> e.getValue() == port2).findFirst().get()
                            .getKey();
                    agent2Index.remove(index);
                    isInFirstIndex = false;
                }
                String externalPort = "(&(%s -> auxPorts[%d]))"
                        .formatted(isInFirstIndex ? firstAgentParameterName : secondAgentParameterName, index);
                port2Name = "FreePort%d".formatted(count++);
                sb.append("Port *%s = GetOtherPort(%s);\n".formatted(port2Name, externalPort));
                sb.append("Wire* WireToFree%d = %s -> pointingAt;\n".formatted(count, externalPort));
                DynamicHotnessCalculatorString
                        .append("%s += WireToFree%d -> hotness;\n".formatted(hotnessVarName, count));
                DynamicHotnessCalculatorString.append("freeWrapper(WireToFree%d);\n".formatted(count++));
            }
            sb.append("Wire *%s = CreateWire(queue, %d,%s,%s);\n".formatted(wireCVarName, hotness,
                    port1Name, port2Name));
            sb.append(DynamicHotnessCalculatorString);
            if (EitherPortWasFree) {
                sb.append("%s -> hotness += %s;\n".formatted(wireCVarName, hotnessVarName));
                sb.append("if(%s -> hotness == 2)\n{Enqueue(queue, %s);}\n".formatted(wireCVarName, wireCVarName));
            }
        }
    }

    private StringBuilder generateApplyRuleFunctionBody(Set<? extends ConditionalRewriteRule> rules) {
        StringBuilder sb = new StringBuilder();
        sb.append("if(false){}\n");// Handles case where we have 0 rules

        for (ConditionalRewriteRule rule : rules) {
            for (boolean swapped : List.of(false, true)) {
                String ruleName = ruleNames.get(rule);
                String AGENT_1_TYPE = swapped ? "agent2Type" : "agent1Type";
                String AGENT_2_TYPE = swapped ? "agent1Type" : "agent2Type";
                AgentType agent1Type = rule.getAgent1();

                AgentType agent2Type = rule.getAgent2();
                assert (agentTypeToProgramVar.containsKey(agent1Type)
                        && agentTypeToProgramVar.get(agent1Type) != null);
                assert (agentTypeToProgramVar.containsKey(agent2Type)
                        && agentTypeToProgramVar.get(agent1Type) != null);
                sb.append("else if (*%s == %s && *%s == %s)\n{"
                        .formatted(AGENT_1_TYPE, agentTypeToProgramVar.get(agent1Type),
                                AGENT_2_TYPE,
                                agentTypeToProgramVar.get(agent2Type)));
                sb.append("%s(queue, (%s*)%s, (%s*)%s);\n}\n".formatted(ruleName,
                        agent1Type.getCProgramStructType(), AGENT_1_TYPE,
                        agent2Type.getCProgramStructType(), AGENT_2_TYPE));

            }
        }
        sb.append("else {\nHandleLackOfRule();\n}");
        return sb;
    }
}
