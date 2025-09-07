package com.carolang.common.interaction_rules;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.pcollections.HashPMap;

import com.carolang.common.data_sources.DataAgentImplementation;
import com.carolang.common.data_sources.DataSource;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;

public class ConditionTreeLeafNode extends ConditionTreeNode {
    RewriteRuleResult result;

    public ConditionTreeLeafNode(RewriteRuleResult result) {
        this.result = result;
    }

    String innerAllocate(BiFunction<RewriteRuleResult, HashPMap<DataSource, String>, String> functionToWriteCCodeForResults, String agent1ParamName,
            String agent2ParamName, HashPMap<DataSource, String> dataSourcesAlreadyAllocated) {
        return functionToWriteCCodeForResults.apply(result, dataSourcesAlreadyAllocated);
    }

    

    @Override
    public ConditionTreeNode MapResults(Map<RewriteRuleResult, RewriteRuleResult> map) {
        RewriteRuleResult newResult = map.get(result);
        assert (newResult != null);
        return new ConditionTreeLeafNode(newResult);
    }

    @Override
    public ConditionTreeNode InlineConditions(DataSource agent1DataSource, DataSource agent2DataSource) {
        return new ConditionTreeLeafNode(result);
    }

    @Override
    public Set<RewriteRuleResult> getResults() {
        return Set.of(result);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"result\": ").append(result).append("\n}");
        return sb.toString();
    }

    @Override
    Set<DataSource> unMemoizedDataSourcesNeededByEveryChildOfThisNode()
    {
        Set<DataSource> dataSources = result.net().getAgents().stream().filter(a -> a instanceof DataAgentImplementation).map(a -> ((DataAgentImplementation)a).getDataSource()).flatMap(ds -> ds.allDataSourcesNeeded().stream()).collect(Collectors.toSet());
        return dataSources;

    }


        
    
}