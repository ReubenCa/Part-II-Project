// package com.carolang.common.interaction_rules;

// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;

// import com.carolang.common.agent_types.AgentType;
// import com.google.common.collect.Sets;

// public class IfStatementConditionalRewriteRule extends ConditionalRewriteRule {

//     private final RuleCondition condition;


//     public IfStatementConditionalRewriteRule(AgentType agent1, AgentType agent2,  RuleCondition condition, NetBase resultIfTrue, Map<Integer, Port> agent1IndexIfTrue, Map<Integer, Port> agent2IndexIfTrue, NetBase resultIfFalse, Map<Integer, Port> agent1IndexIfFalse, Map<Integer, Port> agent2IndexIfFalse) {
//         super(agent1, agent2, List.of(new RewriteRuleResult(resultIfTrue, agent1IndexIfTrue, agent2IndexIfTrue), new RewriteRuleResult(resultIfFalse, agent1IndexIfFalse, agent2IndexIfFalse)));
//         this.condition = condition;
        
//     }


    
//     public RuleCondition getRuleCondition()
//     {
//         return condition;
//     }
//     public NetBase getResultIfTrue()
//     {
//         return results.get(0).net();
//     }
//     public NetBase getResultIfFalse()
//     {
//         return results.get(1).net();
//     }
//     public Map<Integer, Port> getAgent1IndexIfTrue()
//     {
//         return results.get(0).agent1Index();
//     }
//     public Map<Integer, Port> getAgent2IndexIfTrue()
//     {
//         return results.get(0).agent2Index();
//     }
//     public Map<Integer, Port> getAgent1IndexIfFalse()
//     {
//         return results.get(1).agent1Index();
//     }
//     public Map<Integer, Port> getAgent2IndexIfFalse()
//     {
//         return results.get(1).agent2Index();
//     }
    

//     @Override 
//     public IfStatementConditionalRewriteRule Clone()
//     {
//         return new IfStatementConditionalRewriteRule(agent1, agent2.get(), condition, getResultIfTrue(), getAgent1IndexIfTrue(), getAgent2IndexIfTrue(), getResultIfFalse(), getAgent1IndexIfFalse(), getAgent2IndexIfFalse());
//     }

//     @Override
//     public Set<AgentType> getAllAgentTypes() {
//         Set<AgentType> allAgentTypes = new HashSet<AgentType>();
//         allAgentTypes.add(getAgent1());
//         if(getAgent2().isPresent())
//         {
//             allAgentTypes.add(getAgent2().get());
//         }
//         for(AgentImplementationBase agent : Sets.union(getResultIfTrue().getAgents(), getResultIfFalse().getAgents()))
//         {
//             allAgentTypes.add(agent.getAgentType());
//         }
//         return allAgentTypes;
        
//     }
    
// }
