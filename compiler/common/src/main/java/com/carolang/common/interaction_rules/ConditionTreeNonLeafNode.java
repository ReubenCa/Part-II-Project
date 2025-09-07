package com.carolang.common.interaction_rules;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.pcollections.HashPMap;

import com.carolang.common.data_sources.DataSource;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;
import com.google.common.collect.Sets;

public class ConditionTreeNonLeafNode extends ConditionTreeNode {
    static long counter;
    ConditionTreeNode ifTrue;
    ConditionTreeNode ifFalse;
    DataSource condition;

    public ConditionTreeNonLeafNode(ConditionTreeNode ifTrue, ConditionTreeNode ifFalse, DataSource condition) {
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
        this.condition = condition;
    }

    @Override
    String innerAllocate(BiFunction<RewriteRuleResult, HashPMap<DataSource, String>, String> functionsToAllocateResults, String agent1ParamName,
            String agent2ParamName, HashPMap<DataSource, String> dataSourcesAlreadyAllocated) {
        String conditionVarName = "condition%s".formatted(counter++);
        String conditionCode = condition.CreateCProgramForData(conditionVarName, agent1ParamName,
                agent2ParamName, dataSourcesAlreadyAllocated);
        return "%s\nif(%s)\n {\n%s\n}\n else\n {\n%s\n}".formatted(
                conditionCode,
                conditionVarName,
                ifTrue.Allocate(functionsToAllocateResults, agent1ParamName, agent2ParamName, dataSourcesAlreadyAllocated),
                ifFalse.Allocate(functionsToAllocateResults, agent1ParamName, agent2ParamName, dataSourcesAlreadyAllocated));
    }

    @Override
    public ConditionTreeNode MapResults(Map<RewriteRuleResult, RewriteRuleResult> map) {
        ConditionTreeNode newIfTrue = ifTrue.MapResults(map);
        ConditionTreeNode newIfFalse = ifFalse.MapResults(map);
        return new ConditionTreeNonLeafNode(newIfTrue, newIfFalse, condition);
    }

    @Override
    public ConditionTreeNode InlineConditions(DataSource agent1DataSource, DataSource agent2DataSource) {
        return new ConditionTreeNonLeafNode(
                ifTrue.InlineConditions(agent1DataSource, agent2DataSource),
                ifFalse.InlineConditions(agent1DataSource, agent2DataSource),
                condition.Inline(agent1DataSource, agent2DataSource));
    }

    @Override
    public Set<RewriteRuleResult> getResults() {
        return Sets.union(ifTrue.getResults(), ifFalse.getResults());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"ifTrue\": ").append(ifTrue).append(",\n");
        sb.append("\"ifFalse\": ").append(ifFalse).append(",\n");
        sb.append("\"condition\": ").append(condition).append("\n}");
        return sb.toString();
    }

    @Override
    Set<DataSource> unMemoizedDataSourcesNeededByEveryChildOfThisNode() {
        Set<DataSource> dataSources = Sets.union(Sets.intersection(ifTrue.unMemoizedDataSourcesNeededByEveryChildOfThisNode(),
                ifFalse.unMemoizedDataSourcesNeededByEveryChildOfThisNode()),
                condition.allDataSourcesNeeded());
        return dataSources;
    }
}