package com.carolang;

import java.util.HashMap;
import java.util.Map;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.agent_types.DataAgentType;
import com.carolang.common.agent_types.NonDataAgentType;
import com.carolang.common.ast_nodes.MagicNode;
import com.carolang.common.ast_nodes.MagicNodeTag;

public class MagicAgentTypeFactory {
    private static Map<MagicNodeTag, AgentType> magicNodes = new HashMap<>();

    // This should move to magic node class
    public static AgentType getMagicAgentType(MagicNodeTag type) {
        if (magicNodes.containsKey(type)) {
            return magicNodes.get(type);
        } else {
            MagicNodeDataAgentInfo info = getInfo(type);
            AgentType magicType ;
            if(info == null)
            {
            magicType = new NonDataAgentType(MagicNode.getAuxiliaryPortCount(type),
                    "Magic_%s".formatted(type.toString()), null, isDuplicator(type));
            }
            else
            {
                magicType = new DataAgentType(MagicNode.getAuxiliaryPortCount(type),
                        "Magic_%s".formatted(type.toString()), null, info.CStructType(), info.CStructAllocationFunction(), info.CTypeForData());
            }

            magicType.setDoNotInline(doNotInline(type));
            // agentTypes.add(magicType);
            magicNodes.put(type, magicType);
            return magicType;
        }
    }

    // So if not Data agent super simple
    // If data agent need to know CStructType and Allocation Function


private static boolean isDuplicator(MagicNodeTag tag)
{
   return tag == MagicNodeTag.OUTPUT; 
}

private static boolean doNotInline(MagicNodeTag tag)
{
   switch (tag) {
      case OUTPUT:
         return true;
      default:
         return false;
   }
}

private static MagicNodeDataAgentInfo getInfo(MagicNodeTag tag)
{
      switch(tag)
      {
         case CURRIED_PLUS_INT:
         case CURRIED_MINUS_INT:
         case CURRIED_MULTIPLY_INT:
         case CURRIED_DIVIDE_INT:
         case CURRIED_GREATER_INT:
         case CURRIED_GREATER_EQUALS_INT:
         case CURRIED_LESS_INT:
         case CURRIED_LESS_EQUALS_INT:
         case CURRIED_EQUALS_INT:
         case CURRIED_NOT_EQUALS_INT:
         case CURRIED_MOD_INT:
          return new MagicNodeDataAgentInfo("intDataAgent", "allocateIntAgent", "int");
         case PLUS_INT:
         case MINUS_INT:
         case MULTIPLY_INT:
         case DIVIDE_INT:
         case MOD_INT:
         case GREATER_INT:
         case GREATER_EQUALS_INT:
         case LESS_INT:
         case LESS_EQUALS_INT:
         case EQUALS_INT:
         case NOT_EQUALS_INT:
         case OUTPUT:
         case INPUT_INT:
          return null;
         case CURRIED_PLUS_FLOAT:
         case CURRIED_MINUS_FLOAT:
         case CURRIED_MULTIPLY_FLOAT:
         case CURRIED_DIVIDE_FLOAT:
         case CURRIED_GREATER_FLOAT:
         case CURRIED_GREATER_EQUALS_FLOAT:
         case CURRIED_LESS_FLOAT:
         case CURRIED_LESS_EQUALS_FLOAT:
         case CURRIED_EQUALS_FLOAT:
         case CURRIED_NOT_EQUALS_FLOAT:
          return new MagicNodeDataAgentInfo("floatDataAgent", "allocateFloatAgent", "float");
         case PLUS_FLOAT:
         case MINUS_FLOAT:
         case MULTIPLY_FLOAT:
         case DIVIDE_FLOAT:
         case GREATER_FLOAT:
         case GREATER_EQUALS_FLOAT:
         case LESS_FLOAT:
         case LESS_EQUALS_FLOAT:
         case EQUALS_FLOAT:
         case NOT_EQUALS_FLOAT:
          return null;
         default:
          throw new UnsupportedOperationException("Unsupported MagicNodeTag: " + tag);
      }
}

 record MagicNodeDataAgentInfo(String CStructType, String CStructAllocationFunction, String CTypeForData) {
 }

 

}
