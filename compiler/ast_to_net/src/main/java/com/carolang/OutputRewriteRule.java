package com.carolang;

import java.util.Map;

import com.carolang.common.agent_types.AgentType;
import com.carolang.common.interaction_rules.NetBase;
import com.carolang.common.interaction_rules.Port;
import com.carolang.common.interaction_rules.RewriteRule;

public class OutputRewriteRule extends RewriteRule {

    private final String CStringFormat;
    private final String CdataType;

    public OutputRewriteRule(AgentType outputAgent, AgentType agentWithData, NetBase result,
            Map<Integer, Port> agent1Index, Map<Integer, Port> agent2Index, String CFormatSpecifier, String CdataType) {
        
        super(outputAgent, agentWithData, result, agent1Index, agent2Index);
        this.CStringFormat = CFormatSpecifier;
        this.CdataType = CdataType;
    }

    @Override
    public String customInstructionsAtBeginning(String OutputtingAgentVariableName, String AgentWithDataVariableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("%s data = %s -> data;\n".formatted(CdataType, AgentWithDataVariableName));
        sb.append("printf(\"%s\", data);\n".formatted(CStringFormat));
        return sb.toString();
    }

    
    
}
