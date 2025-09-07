package com.carolang.common.interaction_rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.pcollections.HashPMap;

import com.carolang.common.data_sources.DataSource;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;

public abstract class ConditionTreeNode {
    /**
     * 
     * @param functionsToAllocateResults Functions to call depending on which
     *                                   outcome - This includes the arguments
     *                                   already so just do "%s;"
     * @return
     */
    abstract String innerAllocate(BiFunction<RewriteRuleResult, HashPMap<DataSource, String>, String> functionToWriteCCodeForResults, String agent1ParamName,
            String agent2ParamName, HashPMap<DataSource, String> dataSourcesAlreadyAllocated);
    static int counter = 0;


    public String Allocate(BiFunction<RewriteRuleResult, HashPMap<DataSource, String>, String> functionToWriteCCodeForResults, String agent1ParamName,
    String agent2ParamName, HashPMap<DataSource, String> dataSourcesAlreadyAllocated)
    {
        //Allocates all Datasources that have to be done here in topological order
        //Then calls allocation function on the inner node which either spits out the function to the result
        //Or if non leaf node adds the if statement and then does this again in each branch
        Set<DataSource> sourcesThatNeedToBeAllocatedHere = new HashSet<>();
        for(DataSource dataSource : memoizedGetDataSourcesNeededByEveryChildOfThisNode())
        {
            if(!dataSourcesAlreadyAllocated.containsKey(dataSource))
            {
                sourcesThatNeedToBeAllocatedHere.add(dataSource);
            }
        }
        List<DataSource> sortedDataSourcesToAllocate = topologicalSort(sourcesThatNeedToBeAllocatedHere);
        StringBuilder sb = new StringBuilder();
        for(DataSource dataSource : sortedDataSourcesToAllocate)
        {
            String dataSourceName = "dataSource%d".formatted(counter++);
            String dataSourceAllocation = dataSource.CreateCProgramForData(dataSourceName, agent1ParamName, agent2ParamName, dataSourcesAlreadyAllocated);
            sb.append(dataSourceAllocation);
            dataSourcesAlreadyAllocated = dataSourcesAlreadyAllocated.plus(dataSource, dataSourceName);
        }
        sb.append(innerAllocate(functionToWriteCCodeForResults, agent1ParamName, agent2ParamName, dataSourcesAlreadyAllocated));
        return sb.toString();
    }

    private Set<DataSource> dataSourcesNeededByThisNode = null;
    public Set<DataSource> memoizedGetDataSourcesNeededByEveryChildOfThisNode()
    {
        if(dataSourcesNeededByThisNode == null)
        {
            dataSourcesNeededByThisNode = unMemoizedDataSourcesNeededByEveryChildOfThisNode();
        }
        return dataSourcesNeededByThisNode;
    }

    //returns an order we can allocate the DataSources in
    static List<DataSource> topologicalSort(Set<DataSource> sources)
    {
        List<DataSource> sorted = new ArrayList<>();
        sources = new HashSet<>(sources);
        while(sources.size() > 0)
        {
            topologicalVisit(sorted, sources, sources.iterator().next());
        }
        return sorted;
    }

    private static void topologicalVisit(List<DataSource> sorted, Set<DataSource> unvisitedNodes, DataSource nodeToVisit)
    {
        unvisitedNodes.remove(nodeToVisit);
        Set<DataSource> dependencies = nodeToVisit.allDataSourcesNeeded();
        for(DataSource dependency : dependencies)
        {
            if(unvisitedNodes.contains(dependency))
            {
                topologicalVisit(sorted, unvisitedNodes, dependency);
            }
        }
        sorted.add(nodeToVisit);

        return;
    }

    abstract Set<DataSource> unMemoizedDataSourcesNeededByEveryChildOfThisNode();

    public abstract ConditionTreeNode MapResults(Map<RewriteRuleResult, RewriteRuleResult> map);

    public abstract ConditionTreeNode InlineConditions(DataSource agent1DataSource, DataSource agent2DataSource);

    public abstract Set<RewriteRuleResult> getResults();

    @Override
    public abstract String toString();
}