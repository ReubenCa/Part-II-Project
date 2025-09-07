// package com.carolang.common.interaction_rules;

// import com.carolang.common.data_sources.DataSource;

// public class RuleCondition {

//     private final DataSource operand1;
//     private final DataSource operand2;
//     private final ConditionType conditionType;

//     public enum ConditionType
//     {
//         EQUALS_INT,
//         LESS_EQUALS_INT,
//         LESS_INT,
//         GREATER_EQUALS_INT,
//         GREATER_INT,
//         NOT_EQUALS_INT
        
//     }

//     public RuleCondition(DataSource operand1, DataSource operand2, ConditionType conditionType) {
//         this.operand1 = operand1;
//         this.operand2 = operand2;
//         this.conditionType = conditionType;
//     }
//     public DataSource getOperand1() {
//         return operand1;
//     }
//     public DataSource getOperand2() {
//         return operand2;
//     }
//     public ConditionType getConditionType() {
//         return conditionType;
//     }

//     private static long VarUniqueTag = 0;
//     String getUniqueTag() {
//         return "_TAG_%d".formatted(VarUniqueTag++);
//     }
//     /**
//      * Returns a string that allocates a boolean variable called CVarName to the value of the condition
//      * @param CVarName
//      * @param agent1ParameterName
//      * @param agent2ParameterName
//      * @return
//      */
//     public String allocateCConditionVariable(String CVarName, String agent1ParameterName, String agent2ParameterName)
//     {
//         String operand1VarName = "operand1%s".formatted(getUniqueTag());
//         String operand2VarName = "operand2%s".formatted(getUniqueTag());
//         String operand1Code = operand1.CreateCProgramForData(operand1VarName, agent1ParameterName, agent2ParameterName);
//         String operand2Code = operand2.CreateCProgramForData(operand2VarName, agent1ParameterName, agent2ParameterName);
//         return  operand1Code + operand2Code + "bool %s = %s %s %s;\n".formatted(CVarName, operand1VarName, getOperatorString(), operand2VarName);
               
//     }

//     public RuleCondition Inline(DataSource agent1DataSource, DataSource agent2DataSource)
//     {
//         //TODO: could be innerInlines?
//         DataSource newOperand1 = operand1.Inline(agent1DataSource, agent2DataSource);
//         DataSource newOperand2 = operand2.Inline(agent1DataSource, agent2DataSource);
//         return new RuleCondition(newOperand1, newOperand2, conditionType);
//     }

//     @Override
//     public String toString() {
//         return "{" +
//                 "\"operand1\": " + operand1 + "," +
//                 "\"operand2\": " + operand2 + "," +
//                 "\"conditionType\": \"" + conditionType + "\"" +
//                 "}";
//     }


    
//     private String getOperatorString()
//     {
//         switch(conditionType)
//         {
//             case EQUALS_INT:
//                 return "==";
//             case LESS_EQUALS_INT:
//                 return "<=";
//             case LESS_INT:
//                 return "<";
//             case GREATER_EQUALS_INT:
//                 return ">=";
//             case GREATER_INT:
//                 return ">";
//             case NOT_EQUALS_INT:
//                 return "!=";
//             default:
//                 throw new IllegalArgumentException("Unknown condition type: " + conditionType);
//         }
//     }
// }