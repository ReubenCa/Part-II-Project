// package com.carolang.common.interaction_rules;

// import java.util.Map;

// public class MagicRewriteRule extends RewriteRule {
//     String magicTag;/*TODO better tag type */

//     public String getMagicTag()
//     {
//         return magicTag;
//     }

//     public MagicRewriteRule(AgentType agent1, AgentType agent2, Net result, Map<Integer, Port> agent1Index,
//             Map<Integer, Port> agent2Index, String tagType) {
//         super(agent1, agent2, result, agent1Index, agent2Index);
//         magicTag = tagType;
//     }

//     public MagicRewriteRule(AgentType agent1, Map<Integer, Port> agent1Index, Net result, String tagType) {
//         super(agent1, agent1Index, result);

//         magicTag = tagType;
//     }



//     @Override
//     public String toString()
//     {
//         return "MAGIC RULE: " + magicTag;
//     }
// }
