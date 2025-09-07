package com.carolang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.agent_types.DataAgentType;
import com.carolang.common.agent_types.NonDataAgentType;
import com.carolang.common.data_sources.DataAgentImplementation;
import com.carolang.common.data_sources.DataFromReducedAgents;
import com.carolang.common.data_sources.DataFromReducedAgents.AgentOneOrTwo;
import com.carolang.common.data_sources.DataOperator;
import com.carolang.common.data_sources.DataOperator.OperatorType;
import com.carolang.common.data_sources.DataProducer;
import com.carolang.common.data_sources.DataProducer.DataProducerType;
import com.carolang.common.data_sources.DataSource;
import com.carolang.common.types.Type;
import com.carolang.common.interaction_rules.AgentImplementationBase;
import com.carolang.common.interaction_rules.ConditionalRewriteRule;
import com.carolang.common.interaction_rules.Net;
import com.carolang.common.interaction_rules.NetBase;
import com.carolang.common.interaction_rules.Port;
import com.carolang.common.interaction_rules.Program;
import com.carolang.common.interaction_rules.ProgramBase;
import com.carolang.common.interaction_rules.RewriteRule;
import com.carolang.common.interaction_rules.Wire;
import com.carolang.common.ast_nodes.MagicNodeTag;
import com.carolang.common.ast_nodes.Node;
import com.google.common.collect.Sets;

public class ASTtoNetRules {
        public static ProgramBase ASTtoNetRules(Node lambdaTree, Type programType, String outputDebugNetFile)
                        throws IOException, InvalidInputTypeException {
                constructionProgram rogram = new constructionProgram(lambdaTree);
                if (outputDebugNetFile != null) {
                        Files.writeString(Path.of(outputDebugNetFile), rogram.toString());
                }
                List<Type> typeChain = programType.typeChain();
                ProgramBase program = addInputs(rogram, typeChain);
                program = addOutput(program, typeChain.get(typeChain.size() - 1));
                program = addStaticRules(program);
                program = SingleRuleRemover.removeSingleRules(program);
                program = addDuplicatorRules(program);

                // TODO: trim rules where the types dont line up
                // TODO: once we have inlined might be able to actually remove agenttypes and
                // trim again
                return program;
        }

        public static ProgramBase addInputs(constructionProgram program, List<Type> typeChain)
                        throws InvalidInputTypeException {
                RewriteRule startingRule = program.getStartingRule();
                constructionNet startingNet = (constructionNet) startingRule.getResult();
                // TODO: horrifically dangerous cast
                Set<Wire> newWires = new HashSet<>();
                Set<AgentImplementationBase> newAgents = new HashSet<>();
                for (int i = 0; i < typeChain.size() - 1; i++) {
                        Port portToConnectInputTo = startingNet.getExternalFacingPorts().get(i);
                        Type inputType = typeChain.get(i);
                        DataAgentType inputAgentType;
                        DataProducerType producerType;
                        if (inputType == Type.Int) {
                                producerType = DataProducerType.STD_IN_INT;
                                inputAgentType = constructionProgram.getIntegerAgentType();
                        } else if (inputType == Type.Float) {
                                producerType = DataProducerType.STD_IN_FLOAT;
                                inputAgentType = constructionProgram.getFloatAgentType();
                        } else {
                                throw new InvalidInputTypeException(inputType);
                        }

                        DataAgentImplementation inputAgent = new DataAgentImplementation(List.of(), new Port(),
                                        inputAgentType, new DataProducer(producerType, i));
                        newAgents.add(inputAgent);
                        newWires.add(new Wire(inputAgent.getPrinciplePort(), portToConnectInputTo));
                }
                // Important we set output port to the last external port
                NetBase newStartingNet = new Net(Sets.union(startingNet.getAgents(), newAgents),
                                Sets.union(startingNet.getWires(), newWires), startingNet.getExternalFacingPorts()
                                                .get(startingNet.getExternalFacingPorts().size() - 1));
                return new Program(program.getRules(), newStartingNet);
        }

        public static ProgramBase ASTtoNetRules(Node lambdaTree, Type programOutputType)
                        throws InvalidInputTypeException {
                try {
                        return ASTtoNetRules(lambdaTree, programOutputType, null);
                } catch (IOException e) {
                        throw new RuntimeException(e);// should never happen
                }
        }

        private static ProgramBase addOutput(ProgramBase program, Type programOutputType) {
                NetBase oldStartingNet = ((RewriteRule) program.getStartingRule()).getResult();
                NonDataAgentType outputAgentType = (NonDataAgentType) MagicAgentTypeFactory
                                .getMagicAgentType(MagicNodeTag.OUTPUT);
                // By making our outputType a duplicator we magically have the duplicator rules
                // do all the heavy lifting of making it propagate.

                // Set<ConditionalRewriteRule> outputRules = getOutputRules();
                // Our nice duplicator agent trick now takes care of output rules - and we now
                // don't print we do a seperate pass in the runtime
                // So no more need for custom compiler instructions

                Port outputPrinciplePort = new Port();
                AgentImplementation outputAgent = new AgentImplementation(List.of(new Port()), outputPrinciplePort,
                                outputAgentType);
                Wire outputWire = new Wire(outputAgent.getPrinciplePort(), oldStartingNet.getOutputPort());

                Net newStartingNet = new Net(Sets.union(oldStartingNet.getAgents(), Set.of(outputAgent)),
                                Sets.union(oldStartingNet.getWires(), Set.of(outputWire)), null);
                newStartingNet.setSTDOUTPort(outputAgent.getAuxillaryPorts().get(0));
                return new Program(program.getRules(), newStartingNet);
        }

        private static ProgramBase addStaticRules(ProgramBase program) {
                Set<ConditionalRewriteRule> staticRules = getStaticRules();
                Set<ConditionalRewriteRule> allRules = Sets.union(program.getRules(), staticRules);
                return new Program(allRules, program.getStartingRule());
        }

        private static Set<ConditionalRewriteRule> getStaticRules() {
                Set<ConditionalRewriteRule> rules = new HashSet<>();
                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.PLUS_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_PLUS_INT),
                                Type.Int,
                                "int"));
                rules.add(GenerateCurriedOperatorRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_PLUS_INT),
                                constructionProgram.getIntegerAgentType(), constructionProgram.getIntegerAgentType(),
                                OperatorType.PLUS_INT, "int", "int", Type.Int));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.MINUS_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_MINUS_INT),
                                Type.Int,
                                "int"));
                rules.add(GenerateCurriedOperatorRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_MINUS_INT),
                                constructionProgram.getIntegerAgentType(), constructionProgram.getIntegerAgentType(),
                                OperatorType.MINUS_INT, "int", "int", Type.Int));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.MULTIPLY_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_MULTIPLY_INT),
                                Type.Int,
                                "int"));
                rules.add(
                                GenerateCurriedOperatorRule(
                                                MagicAgentTypeFactory
                                                                .getMagicAgentType(MagicNodeTag.CURRIED_MULTIPLY_INT),
                                                constructionProgram.getIntegerAgentType(),
                                                constructionProgram.getIntegerAgentType(),
                                                OperatorType.MULTIPLY_INT, "int", "int", Type.Int));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.DIVIDE_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_DIVIDE_INT),
                                Type.Int,
                                "int"));
                rules.add(GenerateCurriedOperatorRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_DIVIDE_INT),
                                constructionProgram.getIntegerAgentType(), constructionProgram.getIntegerAgentType(),
                                OperatorType.DIVIDE_INT, "int", "int", Type.Int));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.MOD_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_MOD_INT),
                                Type.Int,
                                "int"));

                rules.add(GenerateCurriedOperatorRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_MOD_INT),
                                constructionProgram.getIntegerAgentType(), constructionProgram.getIntegerAgentType(),
                                OperatorType.MOD_INT, "int", "int", Type.Int));

                // Float operations
                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.PLUS_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_PLUS_FLOAT),
                                Type.Float,
                                "float"));
                rules.add(GenerateCurriedOperatorRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_PLUS_FLOAT),
                                constructionProgram.getFloatAgentType(), constructionProgram.getFloatAgentType(),
                                OperatorType.PLUS_FLOAT, "float", "float", Type.Float));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.MINUS_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_MINUS_FLOAT),
                                Type.Float,
                                "float"));
                rules.add(GenerateCurriedOperatorRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_MINUS_FLOAT),
                                constructionProgram.getFloatAgentType(), constructionProgram.getFloatAgentType(),
                                OperatorType.MINUS_FLOAT, "float", "float", Type.Float));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.MULTIPLY_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_MULTIPLY_FLOAT),
                                Type.Float,
                                "float"));
                rules.add(GenerateCurriedOperatorRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_MULTIPLY_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                constructionProgram.getFloatAgentType(),
                                OperatorType.MULTIPLY_FLOAT, "float", "float", Type.Float));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.DIVIDE_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_DIVIDE_FLOAT),
                                Type.Float,
                                "float"));
                rules.add(GenerateCurriedOperatorRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_DIVIDE_FLOAT),
                                constructionProgram.getFloatAgentType(), constructionProgram.getFloatAgentType(),
                                OperatorType.DIVIDE_FLOAT, "float", "float", Type.Float));

                // EQUALITY is going to need something more
                // Since booleans aren't data agents we need to produce a different result net
                // based on the data
                // Want to design this in a way that allows inlining of boolean agents to if
                // statements
                // I suspect the best way to do it is to add an undetermined boolean
                // IAgentImplementation and Type and this comes with a String (or function that
                // gens a string) for the C boolean condition

                // So Boolean Agent is like data agent with a condition DataSource
                // So we have our equality rules that generate a a boolean agent as a result

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.GREATER_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_GREATER_INT),
                                Type.Int,
                                "int"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.GREATER_INT, "int",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_GREATER_INT),
                                constructionProgram.getIntegerAgentType()));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.GREATER_EQUALS_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_GREATER_EQUALS_INT),
                                Type.Int, "int"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.GREATER_EQUALS_INT, "int",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_GREATER_EQUALS_INT),
                                constructionProgram.getIntegerAgentType()));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.LESS_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_LESS_INT),
                                Type.Int,
                                "int"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.LESS_INT, "int",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_LESS_INT),
                                constructionProgram.getIntegerAgentType()));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.LESS_EQUALS_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_LESS_EQUALS_INT),
                                Type.Int,
                                "int"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.LESS_EQUALS_INT, "int",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_LESS_EQUALS_INT),
                                constructionProgram.getIntegerAgentType()));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.EQUALS_INT),
                                constructionProgram.getIntegerAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_EQUALS_INT),
                                Type.Int,
                                "int"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.EQUALS_INT, "int",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_EQUALS_INT),
                                constructionProgram.getIntegerAgentType()));

                rules.add(GenerateTrueFalseConditionalRule(OperatorType.NOT_EQUALS_INT, "int",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_NOT_EQUALS_INT),
                                constructionProgram.getIntegerAgentType()));


                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.NOT_EQUALS_INT),
                constructionProgram.getIntegerAgentType(),
                (DataAgentType) MagicAgentTypeFactory
                                .getMagicAgentType(MagicNodeTag.CURRIED_NOT_EQUALS_INT),
                Type.Int,
                "int"));

                // Float versions
                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.GREATER_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_GREATER_FLOAT),
                                Type.Float,
                                "float"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.GREATER_FLOAT, "float",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_GREATER_FLOAT),
                                constructionProgram.getFloatAgentType()));

                rules.add(GenerateCurryingRule(
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.GREATER_EQUALS_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_GREATER_EQUALS_FLOAT),
                                Type.Float, "float"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.GREATER_EQUALS_FLOAT, "float",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_GREATER_EQUALS_FLOAT),
                                constructionProgram.getFloatAgentType()));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.LESS_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_LESS_FLOAT),
                                Type.Float,
                                "float"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.LESS_FLOAT, "float",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_LESS_FLOAT),
                                constructionProgram.getFloatAgentType()));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.LESS_EQUALS_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_LESS_EQUALS_FLOAT),
                                Type.Float,
                                "float"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.LESS_EQUALS_FLOAT, "float",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_LESS_EQUALS_FLOAT),
                                constructionProgram.getFloatAgentType()));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.EQUALS_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_EQUALS_FLOAT),
                                Type.Float,
                                "float"));
                rules.add(GenerateTrueFalseConditionalRule(OperatorType.EQUALS_FLOAT, "float",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_EQUALS_FLOAT),
                                constructionProgram.getFloatAgentType()));

                rules.add(GenerateCurryingRule(MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.NOT_EQUALS_FLOAT),
                                constructionProgram.getFloatAgentType(),
                                (DataAgentType) MagicAgentTypeFactory
                                                .getMagicAgentType(MagicNodeTag.CURRIED_NOT_EQUALS_FLOAT),
                                Type.Float,
                                "float"));

                rules.add(GenerateTrueFalseConditionalRule(OperatorType.NOT_EQUALS_FLOAT, "float",
                                MagicAgentTypeFactory.getMagicAgentType(MagicNodeTag.CURRIED_NOT_EQUALS_FLOAT),
                                constructionProgram.getFloatAgentType()));

                return rules;
        }

        /**
         * So this generates the rule that allows (+) 1 2 to go to (+_1) 2
         */
        private static ConditionalRewriteRule GenerateCurryingRule(AgentType operatorAgentType,
                        AgentType operandAgentType,
                        DataAgentType resultAgentType, Type resultDataType, String CResultType) {
                // This is missing the fact that operator has an aux port
                Port resultPrinciplePort = new Port();
                DataSource resultDataSource = new DataFromReducedAgents(AgentOneOrTwo.AGENT_TWO, CResultType);
                DataAgentImplementation resultAgentImplementation = new DataAgentImplementation(
                                List.of(new Port()),
                                resultPrinciplePort, resultAgentType, resultDataSource);
                NetBase resultNet = new Net(Set.of(resultAgentImplementation), Set.of(), resultPrinciplePort);
                ConditionalRewriteRule rule = new RewriteRule(operatorAgentType, operandAgentType, resultNet,
                                Map.of(0, resultPrinciplePort, 1, resultAgentImplementation.getAuxillaryPorts().get(0)),
                                Map.of());
                return rule;
        }

        private static ConditionalRewriteRule GenerateCurriedOperatorRule(AgentType curriedOperatorAgentType,
                        AgentType operandAgentType, DataAgentType resultAgentType, OperatorType operatorType,
                        String CTypeForDataFromCurriedOperator, String CTypeForOperand, Type resultType) {
                Port resultPrinciplePort = new Port();
                DataSource resultDataSource = new DataOperator(operatorType,
                                new DataFromReducedAgents(AgentOneOrTwo.AGENT_ONE, CTypeForDataFromCurriedOperator),
                                new DataFromReducedAgents(AgentOneOrTwo.AGENT_TWO, CTypeForDataFromCurriedOperator));
                DataAgentImplementation resultAgentImplementation = new DataAgentImplementation(List.of(),
                                resultPrinciplePort,
                                resultAgentType, resultDataSource);
                NetBase resultNet = new Net(Set.of(resultAgentImplementation), Set.of(), resultPrinciplePort);
                ConditionalRewriteRule rule = new RewriteRule(curriedOperatorAgentType, operandAgentType, resultNet,
                                Map.of(0, resultPrinciplePort), Map.of());
                return rule;
        }

        /**
         * Rule where we get a net of a True agent or a false agent depending on
         * condition
         */
        private static ConditionalRewriteRule GenerateTrueFalseConditionalRule(
                        OperatorType conditionType,
                        String CTypeForOperands, AgentType curriedOperatorAgentType, AgentType secondOperandAgentType) {
                AgentImplementation trueAgent = new AgentImplementation(List.of(), new Port(),
                                constructionProgram.getTrueAgentType());
                AgentImplementation falseAgent = new AgentImplementation(List.of(), new Port(),
                                constructionProgram.getFalseAgentType());
                DataSource source1 = new DataFromReducedAgents(AgentOneOrTwo.AGENT_ONE, CTypeForOperands);
                DataSource source2 = new DataFromReducedAgents(AgentOneOrTwo.AGENT_TWO, CTypeForOperands);
                DataSource condition = new DataOperator(conditionType, source1, source2);
                NetBase resultIfTrue = new Net(Set.of(trueAgent), Set.of(),
                                trueAgent.getPrinciplePort());
                NetBase resultIfFalse = new Net(Set.of(falseAgent), Set.of(),
                                falseAgent.getPrinciplePort());

                // return new IfStatementConditionalRewriteRule(curriedOperatorAgentType,
                // secondOperandAgentType, condition,
                // resultIfTrue, Map.of(0, trueAgent.getPrinciplePort()), Map.of(),
                // resultIfFalse,
                // Map.of(0, falseAgent.getPrinciplePort()), Map.of());

                return ConditionalRewriteRule.CreateIfStatementConditionalRewriteRule(curriedOperatorAgentType,
                                secondOperandAgentType, condition, resultIfTrue,
                                Map.of(0, trueAgent.getPrinciplePort()), Map.of(), resultIfFalse,
                                Map.of(0, falseAgent.getPrinciplePort()), Map.of());

        }

        /**
         * Done before we remove single rules as adding duplicator rules with sig
         * 
         * @param progam
         * @return
         */
        private static ProgramBase addDuplicatorRules(ProgramBase program) {
                Set<AgentType> duplicatorAgents = program.getAgents().stream().filter(a -> a.isDuplicatorAgent())
                                .collect(Collectors.toSet());
                Set<AgentType> allAgents = program.getAgents();
                HashSet<ConditionalRewriteRule> newRules = new HashSet<>();
                // Need to create a rewrite rule between all DupAgents and all Agents
                for (AgentType duplicatorAgentType : duplicatorAgents) {
                        for (AgentType otherAgentType : allAgents) {
                                newRules.add(createDuplicatorRule((NonDataAgentType) duplicatorAgentType,
                                                otherAgentType));
                        }
                }
                return new Program(Sets.union(newRules, program.getRules()), program.getStartingRule());
        }

        private static ConditionalRewriteRule createDuplicatorRule(NonDataAgentType duplicatorAgentType,
                        AgentType otherAgentType) {
                int nAryOfDuplicator = duplicatorAgentType.getAuxiliaryPortCount();
                int nAryOfOther = otherAgentType.getAuxiliaryPortCount();
                // So need to create nAryOFDuplicator copies of other AgentType
                // And nAryOfOther copies of the duplicator
                // Each aux port in each new otherAgent connected to one of the auxPorts of the
                // corresponding duplicator
                // Agent1 Index maps to the principle ports of the new OtherAgents
                // Agent2 index maps to the principle ports of the new DuplicatorAgents

                List<AgentImplementationBase> otherAgents = new ArrayList<>();
                DataSource dataSource = null;
                if (otherAgentType instanceof DataAgentType dType) {
                        dataSource = new DataFromReducedAgents(AgentOneOrTwo.AGENT_TWO,
                                        dType.getCTypeForData());
                }
                for (int i = 0; i < nAryOfDuplicator; i++) {
                        List<Port> auxPorts = new ArrayList<>();
                        for (int j = 0; j < nAryOfOther; j++) {
                                auxPorts.add(new Port());
                        }
                        if (otherAgentType instanceof NonDataAgentType nType) {
                                otherAgents.add(new AgentImplementation(auxPorts, new Port(), nType));
                        } else if (otherAgentType instanceof DataAgentType dType) {
                                otherAgents.add(new DataAgentImplementation(auxPorts, new Port(), dType,
                                                dataSource));
                        } else {
                                throw new RuntimeException("Unknown agent type");
                        }

                }

                Set<Wire> wires = new HashSet<>();

                List<AgentImplementationBase> duplicatorAgents = new ArrayList<>();
                for (int i = 0; i < nAryOfOther; i++) {
                        List<Port> auxPorts = new ArrayList<>();
                        for (int j = 0; j < nAryOfDuplicator; j++) {
                                auxPorts.add(new Port());
                        }
                        duplicatorAgents.add(new AgentImplementation(auxPorts, new Port(),
                                        duplicatorAgentType));
                }

                for (int otherAgentIndex = 0; otherAgentIndex < nAryOfDuplicator; otherAgentIndex++) {
                        for (int otherAgentAuxPortIndex = 0; otherAgentAuxPortIndex < nAryOfOther; otherAgentAuxPortIndex++) {
                                Port otherAgentPort = otherAgents.get(otherAgentIndex).getAuxillaryPorts()
                                                .get(otherAgentAuxPortIndex);
                                Port DuplicatorPort = duplicatorAgents.get(otherAgentAuxPortIndex).getAuxillaryPorts()
                                                .get(otherAgentIndex);
                                wires.add(new Wire(otherAgentPort, DuplicatorPort));
                        }
                }

                Map<Integer, Port> agent1Index = new HashMap<>();
                Map<Integer, Port> agent2Index = new HashMap<>();

                for (int i = 0; i < nAryOfDuplicator; i++) {
                        agent1Index.put(i, otherAgents.get(i).getPrinciplePort());
                }
                for (int i = 0; i < nAryOfOther; i++) {
                        agent2Index.put(i, duplicatorAgents.get(i).getPrinciplePort());
                }

                Set<AgentImplementationBase> allAgents = Stream.concat(duplicatorAgents.stream(), otherAgents.stream())
                                .collect(Collectors.toSet());
                NetBase result = new Net(allAgents, wires, null);
                return new RewriteRule(duplicatorAgentType, otherAgentType, result, agent1Index, agent2Index);

        }
}
