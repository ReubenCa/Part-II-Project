package com.carolang.common.interaction_rules;

import java.util.Map;
import java.util.Set;

public class Program extends ProgramBase {


    private final Set<? extends ConditionalRewriteRule> rules;
    private final ConditionalRewriteRule startingRule;

    public Program(Set<? extends ConditionalRewriteRule> rules, ConditionalRewriteRule startingRule) {
        this.rules = rules;
        this.startingRule = startingRule;
    }

    public Program(Set<? extends ConditionalRewriteRule> rules, NetBase startingNet) {
        this.rules = rules;
        this.startingRule = new RewriteRule(null, null,  startingNet, Map.of(), Map.of());
    }

    @Override
    public Set<? extends ConditionalRewriteRule> getRules() {
        return rules;
    }

    @Override
    public ConditionalRewriteRule getStartingRule() {
        return startingRule;
    }

    
}
