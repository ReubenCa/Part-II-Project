package com.carolang;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.carolang.Inliner.Substitution;
import com.carolang.common.data_sources.DataAgentImplementation;
import com.carolang.common.data_sources.DataSource;
import com.carolang.common.interaction_rules.ConditionalRewriteRule;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;

class InliningHeuristic implements IinliningHeuristic
{
    private boolean inlineConditionals;
    private boolean inlineRecursives;
    private float aggresiveness;

    public InliningHeuristic(boolean inlineConditionals, boolean inlineRecursives, float aggresiveness)
    {
        this.inlineConditionals = inlineConditionals;
        this.inlineRecursives = inlineRecursives;
        this.aggresiveness = aggresiveness;
    }

    @Override
    public  Function<RewriteRuleResult, Function<Substitution, Float>> getHeuristic(ConditionalRewriteRule ruleBeingInlined)
    {
        return resultBeingInlined -> substitutionToBePerformed -> {
            
            Set<RewriteRuleResult> resultsOfSubstitutionBeingApplied = substitutionToBePerformed.RuleToSubstituteIn().getResults();
            
            boolean ruleBeingSubstitutedInIsRecursive = resultsOfSubstitutionBeingApplied.stream().flatMap(result -> result.net().getAgents().stream()).anyMatch(agent -> 
            (agent.getAgentType() == substitutionToBePerformed.agent1().getAgentType() ||
            agent.getAgentType() == substitutionToBePerformed.agent2().getAgentType()));
            if(ruleBeingSubstitutedInIsRecursive && !inlineRecursives)
            {
                return -1f;
            }

            Set<RewriteRuleResult> resultsOfRuleBeingInlined = ruleBeingInlined.getResults();
            int ruleResultsSize = resultsOfRuleBeingInlined.size();//Amount of different results the rule currently has

            int newResultsAdded = resultsOfSubstitutionBeingApplied.size();//Amount of new results being added
            if(newResultsAdded > 1 && !inlineConditionals)
            {
                return -1f;
            }
            List<Integer> amountOfAgentsBeingAdded = substitutionToBePerformed.RuleToSubstituteIn().getResults().stream().map(r -> r.net().getAgents().size()).toList();
            int amountOfAgentsAlreadyInResult = resultBeingInlined.net().getAgents().size();


            Set<DataSource> dataSources = resultBeingInlined.net().getAgents().stream().filter(a -> a instanceof DataAgentImplementation).map(a -> ((DataAgentImplementation)a).getDataSource()).collect(Collectors.toSet());

            int dataSourceSizes = dataSources.stream().mapToInt(DataSource::getSize).sum();
    
            return aggresiveness - (float)ruleResultsSize * ((float)amountOfAgentsAlreadyInResult - 2f*(float)newResultsAdded - 0.125f * (float)dataSourceSizes); 
        };
    }

    @Override
    public int MaxInterations() {
        return (int)Math.pow(2, 15);
    }
}