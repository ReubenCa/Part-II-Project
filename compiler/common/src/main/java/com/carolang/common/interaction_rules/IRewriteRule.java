// package com.carolang.common.interaction_rules;

// import java.util.Map;
// import java.util.Optional;
// import java.util.Set;

// import com.carolang.common.agent_types.AgentType;
// import com.carolang.common.interaction_rules.ConditionalRewriteRule.RewriteRuleResult;

// interface IRewriteRule {

//     AgentType getAgent1();

//     Optional<AgentType> getAgentOption2();

//     NetBase getResult();


//     Map<Integer, Port> getAgent1Index();

//     Optional<Map<Integer, Port>> getAgent2Index();
//     // And probably a MAP of agents to where they go in the result
//     // If agent 2 is not specified it can be any agent

//     public String customInstructionsAtBeginning(String agent1ParamName, String agent2ParamName);

//     public Set<AgentType> getAllAgentTypes();

//     public RewriteRuleResult getRewriteRuleResult();

//    // public IRewriteRule Clone();

// }