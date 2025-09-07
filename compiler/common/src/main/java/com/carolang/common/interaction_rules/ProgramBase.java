package com.carolang.common.interaction_rules;

import java.util.HashSet;
import java.util.Set;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.agent_types.NonDataAgentType;
import com.google.common.collect.Sets;



public abstract class ProgramBase {

   public abstract Set<? extends ConditionalRewriteRule> getRules();

   public abstract ConditionalRewriteRule getStartingRule();

   public Set<AgentType> getAgents()
   {
        Set<AgentType> allAgentTypes = new HashSet<>();
        for(ConditionalRewriteRule rule : Sets.union(getRules(), Set.of(getStartingRule())))
        {
            allAgentTypes.addAll(rule.getAllAgentTypes());
        }
        return allAgentTypes;
   }

   @Override
   public String toString()
   {
         StringBuilder sb = new StringBuilder();
         sb.append("{");
         sb.append("\"rules\" : [\n");
         String prefix = "";
         for(ConditionalRewriteRule rule : getRules())
         {
            sb.append(prefix);
            prefix = ",";
            sb.append(rule.toString());
         }
         sb.append("]\n,\" startingRule\" :").append(getStartingRule());
         sb.append('}');
         return sb.toString();
   }

   //To output lists we need to know this agent type
   public final static NonDataAgentType EmptyListAgentType = new NonDataAgentType(0, "EMPTY_LIST", null);
}