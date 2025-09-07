package com.carolang.common.interaction_rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.pcollections.HashPMap;
import org.pcollections.HashTreePMap;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.data_sources.DataSource;

public class ConditionalRewriteRule {
    public record RewriteRuleResult(NetBase net, Map<Integer, Port> agent1Index, Map<Integer, Port> agent2Index) {

        public RewriteRuleResult Clone() {
            int oldWiresCount = net.getWires().size();
            Map<Port, Port> cloneMap = new HashMap<>();
            NetBase result1 = net.Clone(cloneMap);

            Map<Integer, Port> newAgent1Index = agent1Index.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> cloneMap.get(e.getValue())));
            Map<Integer, Port> newAgent2Index = agent2Index.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> cloneMap.get(e.getValue())));
            assert (result1.getWires().size() == oldWiresCount);
            return new RewriteRuleResult(result1, newAgent1Index, newAgent2Index);

        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"Net\": ").append(net).append(",\n");
            sb.append("\"Agent1Index\": ").append(mapToJson(agent1Index)).append("\n");
            if(agent2Index != null)
            {
                sb.append(",\n");
                sb.append("\"Agent2Index\": ").append(mapToJson(agent2Index)).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }

        private StringBuilder mapToJson(Map<Integer, Port> map)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            String prefix = "";
            for (Map.Entry<Integer, Port> entry :  map.entrySet())
            {
                sb.append(prefix);
                sb.append("\"%d\" : %s".formatted(entry.getKey(), entry.getValue()));
    
                prefix = ",";
            }
            sb.append("}");
            return sb;
        }
    }

    AgentType agent1;
    AgentType agent2;

    public AgentType getAgent1() {
        return agent1;
    }

    public AgentType getAgent2() {
        return agent2;
    }

    private ConditionalRewriteRule(AgentType agent1, AgentType agent2, ConditionTreeNode conditionTree) {
        this.agent1 = agent1;
        this.agent2 = agent2;
        this.conditionTree = conditionTree;
    }

    /**
     * Used for when we create a rule without a condition
     * 
     * @param agent1
     * @param agent2
     * @param conditionTree
     */
    protected ConditionalRewriteRule(AgentType agent1, AgentType agent2, NetBase result, Map<Integer, Port> agent1Index,
            Map<Integer, Port> agent2Index) {
        this.agent1 = agent1;
        this.agent2 = agent2;
        this.conditionTree = new ConditionTreeLeafNode(new RewriteRuleResult(result, agent1Index, agent2Index));
    }

    private ConditionTreeNode conditionTree;

    public ConditionTreeNode getConditionTree() {
        return conditionTree;
    }

    public ConditionalRewriteRule ReplaceResultWithConditionalResult(RewriteRuleResult oldResult,
            ConditionTreeNode newResult) {

        ConditionTreeNode newCondition = ReplaceResultConditionalHelper(conditionTree, oldResult, newResult);
        return new ConditionalRewriteRule(
                agent1,
                agent2,
                newCondition);

    }

    private ConditionTreeNode ReplaceResultConditionalHelper(ConditionTreeNode node, RewriteRuleResult resultToReplace,
            ConditionTreeNode newNode) {
        if (node instanceof ConditionTreeLeafNode leaf) {
            if (leaf.result == resultToReplace) {
                return newNode;
            } else {
                return node;
            }
        } else if (node instanceof ConditionTreeNonLeafNode nonLeaf) {
            return new ConditionTreeNonLeafNode(
                    ReplaceResultConditionalHelper(nonLeaf.ifTrue, resultToReplace, newNode),
                    ReplaceResultConditionalHelper(nonLeaf.ifFalse, resultToReplace, newNode),
                    nonLeaf.condition);
        } else {
            throw new RuntimeException("Unknown node type");
        }

    
       

    }

    // public ConditionalRewriteRule ReplaceResult(RewriteRuleResult oldResult,
    // RewriteRuleResult newResult) {
    // return new ConditionalRewriteRule(
    // agent1,
    // agent2,
    // ReplaceResultHelper(conditionTree, oldResult, newResult));
    // }

    // private ConditionTreeNode ReplaceResultHelper(ConditionTreeNode node,
    // RewriteRuleResult oldResult,
    // RewriteRuleResult newResult) {
    // if (node instanceof ConditionTreeLeafNode leaf) {
    // if (leaf.result == oldResult) {
    // return new ConditionTreeLeafNode(newResult);
    // } else {
    // return node;
    // }
    // } else if (node instanceof ConditionTreeNonLeafNode nonLeaf) {
    // return new ConditionTreeNonLeafNode(
    // ReplaceResultHelper(nonLeaf.ifTrue, oldResult, newResult),
    // ReplaceResultHelper(nonLeaf.ifFalse, oldResult, newResult),
    // nonLeaf.condition);
    // } else {
    // throw new RuntimeException("Unknown node type");
    // }
    // }

    public static ConditionalRewriteRule CreateIfStatementConditionalRewriteRule(AgentType agent1, AgentType agent2,
            DataSource condition, NetBase resultIfTrue, Map<Integer, Port> agent1IndexIfTrue,
            Map<Integer, Port> agent2IndexIfTrue, NetBase resultIfFalse, Map<Integer, Port> agent1IndexIfFalse,
            Map<Integer, Port> agent2IndexIfFalse) {

        ConditionTreeNode tree = new ConditionTreeNonLeafNode(
                new ConditionTreeLeafNode(new RewriteRuleResult(resultIfTrue, agent1IndexIfTrue, agent2IndexIfTrue)),
                new ConditionTreeLeafNode(new RewriteRuleResult(resultIfFalse, agent1IndexIfFalse, agent2IndexIfFalse)),
                condition);
        return new ConditionalRewriteRule(agent1, agent2, tree);

    }

    // TODO: if we allocate in a BFS order we can reuse the same variables for
    // shared conditions and datasources
    public String generateCCode(BiFunction<RewriteRuleResult, HashPMap<DataSource, String>, String> functionToWriteCCodeForResults, String agent1ParamName,
            String agent2ParamName) {
        //So at each conditional node we allocate all DataSources that are needed by everything below it
        
        return conditionTree.Allocate(functionToWriteCCodeForResults, agent1ParamName, agent2ParamName, HashTreePMap.empty());
    }

    public Set<RewriteRuleResult> getResults() {
        return conditionTree.getResults();
    }


    public Set<AgentType> getAllAgentTypes() {
        Set<AgentType> allAgentTypes = new HashSet<AgentType>();
        if (agent1 != null) {// null if starting rule
            allAgentTypes.add(agent1);
        }
        if (agent2 != null)// Might be single rule
        {
            allAgentTypes.add(agent2);
        }
        for (RewriteRuleResult result : getResults()) {
            allAgentTypes.addAll(result.net().getAgents().stream().map(a -> a.getAgentType()).toList());
        }
        return allAgentTypes;

    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if(agent1 != null)
        {
            sb.append("\"agent1\": ").append(agent1).append(",\n");
        }
        if(agent2 != null)
        {
            sb.append("\"agent2\": ").append(agent2).append(",\n");
        }
   
        sb.append("\"conditionTree\": ").append(conditionTree).append("\n");
        sb.append("}");
        return sb.toString();
        }


    public String customInstructionsAtBeginning(String agent1ParamName, String agent2ParamName) {
        return "";
    }

}
