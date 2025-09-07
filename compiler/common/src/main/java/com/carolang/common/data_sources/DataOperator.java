package com.carolang.common.data_sources;

import java.util.Set;

import org.pcollections.HashPMap;

import com.google.common.collect.Sets;

public class DataOperator extends DataSource {

    final OperatorType operatorType;
    final DataSource operand1;
    final DataSource operand2;

    private final String COperator;
    private final boolean isInfix;

    private static record OperatorInfo(String cOperator, boolean isInfix, String resultCType) {
    }

    private static OperatorInfo getOperatorInfo(OperatorType operatorType)
    {
        return switch (operatorType) {
            case PLUS_INT -> new OperatorInfo("+", true, "int");
            case MINUS_INT -> new OperatorInfo("-", true, "int");
            case MULTIPLY_INT -> new OperatorInfo("*", true, "int");
            case DIVIDE_INT -> new OperatorInfo("/", true, "int");
            case GREATER_INT -> new OperatorInfo(">", true, "bool");
            case GREATER_EQUALS_INT -> new OperatorInfo(">=", true, "bool");
            case LESS_INT -> new OperatorInfo("<", true, "bool");
            case LESS_EQUALS_INT -> new OperatorInfo("<=", true, "bool");
            case EQUALS_INT -> new OperatorInfo("==", true, "bool");
            case NOT_EQUALS_INT -> new OperatorInfo("!=", true, "bool");
            case MOD_INT -> new OperatorInfo("%", true, "int");
            case PLUS_FLOAT -> new OperatorInfo("+", true, "float");
            case MINUS_FLOAT -> new OperatorInfo("-", true, "float");
            case MULTIPLY_FLOAT -> new OperatorInfo("*", true, "float");
            case DIVIDE_FLOAT -> new OperatorInfo("/", true, "float");
            case GREATER_FLOAT -> new OperatorInfo(">", true, "bool");
            case GREATER_EQUALS_FLOAT -> new OperatorInfo(">=", true, "bool");
            case LESS_FLOAT -> new OperatorInfo("<", true, "bool");
            case LESS_EQUALS_FLOAT -> new OperatorInfo("<=", true, "bool");
            case EQUALS_FLOAT -> new OperatorInfo("==", true, "bool");
            case NOT_EQUALS_FLOAT -> new OperatorInfo("!=", true, "bool");
            default -> throw new RuntimeException("Unsupported operator type");
        };
    }

    public DataOperator(OperatorType operatorType, DataSource operand1, DataSource operand2) {
        super(getOperatorInfo(operatorType).resultCType);
        this.operatorType = operatorType;
        this.COperator = getOperatorInfo(operatorType).cOperator;
        this.isInfix = getOperatorInfo(operatorType).isInfix;
        this.operand1 = operand1;
        this.operand2 = operand2;
       
    }

    public OperatorType getOperatorType() {
        return operatorType;
    }

    public DataSource getOperand1() {
        return operand1;
    }

    public DataSource getOperand2() {
        return operand2;
    }

    public enum OperatorType {
        PLUS_INT,
        MINUS_INT,
        EQUALITY_INT, MULTIPLY_INT, DIVIDE_INT, GREATER_INT, GREATER_EQUALS_INT, LESS_INT, LESS_EQUALS_INT, EQUALS_INT, MOD_INT, DIVIDE_FLOAT, MULTIPLY_FLOAT, MINUS_FLOAT, PLUS_FLOAT, GREATER_FLOAT, GREATER_EQUALS_FLOAT, LESS_FLOAT, LESS_EQUALS_FLOAT, EQUALS_FLOAT, NOT_EQUALS_FLOAT, NOT_EQUALS_INT
    }

    @Override
    public DataSource innerClone() {
        return new DataOperator(this.operatorType, this.operand1.innerClone(), this.operand2.innerClone());
    }

    @Override
    protected String innerCreateCProgramForData(
            String resultVariableName, String agent1ParameterName, String agent2ParameterName, HashPMap<DataSource, String> alreadyAllocatedDatasources) {
        String operand1VarName = alreadyAllocatedDatasources.get(operand1);//We guarantee that everything this depends on has already been allocated
        String operand2VarName = alreadyAllocatedDatasources.get(operand2);
        assert(operand1VarName != null);
        assert(operand2VarName != null);
        String calculateResult;
        if (isInfix) {
            calculateResult = "%s %s = %s %s %s;//Data Operator\n".formatted(resultCType, resultVariableName, operand1VarName,
                    COperator, operand2VarName);
        } else {
            throw new RuntimeException("Unsupported operator type");
        }
        return calculateResult;

    }

    @Override
    protected DataSource innerSwitchSides() {
        return new DataOperator(this.operatorType, this.operand1.innerSwitchSides(), this.operand2.innerSwitchSides());
    }

	@Override
	DataSource innerInline(DataSource reducedAgent1DataSource, DataSource reducedAgent2DataSource) {
		return new DataOperator(this.operatorType, this.operand1.innerInline(reducedAgent1DataSource, reducedAgent2DataSource), this.operand2.innerInline(reducedAgent1DataSource, reducedAgent2DataSource));
	}

    @Override
    int innerGetSize() {
        return 1 + operand1.innerGetSize() + operand2.innerGetSize();
    }

    @Override
    boolean innerEquals(DataSource other) {
        if (other instanceof DataOperator) {
            DataOperator otherData = (DataOperator) other;
            return this.operatorType == otherData.operatorType && this.operand1.equals(otherData.operand1)
                    && this.operand2.equals(otherData.operand2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return operatorType.hashCode() ^ operand1.hashCode() ^ operand2.hashCode();
    }

    @Override
    public Set<DataSource> allDataSourcesNeeded() {
        return Sets.union(Set.of(this), Sets.union(operand1.allDataSourcesNeeded(), operand2.allDataSourcesNeeded()));
      
    }

    @Override
    public boolean dependsOn(DataSource other) {
        return operand1.dependsOn(other) || operand2.dependsOn(other);
    }

}
