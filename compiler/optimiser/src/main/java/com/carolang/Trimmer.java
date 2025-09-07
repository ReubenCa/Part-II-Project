package com.carolang;

import java.util.Set;
import java.util.stream.Collectors;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.interaction_rules.ConditionalRewriteRule;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;
import com.carolang.common.interaction_rules.Program;
import com.carolang.common.interaction_rules.ProgramBase;

public class Trimmer {
    /**
     * Over approximates which rules can be reached and then removes those that
     * can't
     */
    public static ProgramBase TrimProgram(ProgramBase program)
    {
        return new Program(getAllReachableRules(program), program.getStartingRule());
    }


    private static Set<ConditionalRewriteRule> getAllReachableRules(ProgramBase program) {
        // Start with rules immediately available from starting net
        // Set<ConditionalRewriteRule> reachableRules = new HashSet<>();
        Set<? extends ConditionalRewriteRule> allRules = program.getRules();
        Set<AgentType> reachableAgentTypes = program.getStartingRule().getAllAgentTypes();
                

        while (true) {
            int reachableSize = reachableAgentTypes.size();
            Set<ConditionalRewriteRule> reachableRules = allRules.stream()
                    .filter(rule -> reachableAgentTypes.contains(rule.getAgent1())
                            && reachableAgentTypes.contains(rule.getAgent2()))
                    .collect(Collectors.toSet());
            // Add all the agent types that are reachable from the rules
            reachableRules.forEach(rule -> {
                reachableAgentTypes.addAll(getDirectlyPossibleAgentTypes(rule));
            });
            if(reachableAgentTypes.size() == reachableSize) {
                return reachableRules;
            }
            assert(reachableAgentTypes.size() > reachableSize);//shouldn't be able to decrease - we know this terminates as it can only increase and can't go above the size of the original ruleset.
        }

    }

    private static Set<AgentType> getDirectlyPossibleAgentTypes(ConditionalRewriteRule rule) {
        Set<RewriteRuleResult> results = rule.getResults();
        return results.stream().flatMap(result -> result.net().getAgents().stream().map(a -> a.getAgentType()))
                .collect(Collectors.toSet());
    }


}
