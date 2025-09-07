package com.carolang;

import java.util.function.Function;

import com.carolang.Inliner.Substitution;
import com.carolang.common.interaction_rules.ConditionalRewriteRule;
import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;

interface IinliningHeuristic
{
    Function<RewriteRuleResult, Function<Substitution, Float>> getHeuristic(ConditionalRewriteRule ruleBeingInlined);


    public abstract int MaxInterations();
}