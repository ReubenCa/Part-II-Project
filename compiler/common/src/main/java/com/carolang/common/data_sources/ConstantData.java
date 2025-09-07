package com.carolang.common.data_sources;

import java.util.Set;

import org.pcollections.HashPMap;

public class ConstantData<T> extends DataSource {
    private T data;

    public ConstantData(T data) {
        super(typeToCType(data));
        this.data = data;
    }
    public T getData() {
        return data;
    }

    @Override
    protected ConstantData<T> innerClone() {
        return new ConstantData<>(this.data);
    }
    @Override
    protected String innerCreateCProgramForData(
            String resultVariableName, String param1, String param2, HashPMap<DataSource, String> alreadyAllocatedDatasources) {
                if (data instanceof Integer || data instanceof Float) {
                    return "%s %s = %s;//Constant Data \n".formatted(typeToCType(data), resultVariableName, data.toString());
                } else {
                    throw new RuntimeException("Unsupported type for constant data in C program generation");
                }
    }

    private static String  typeToCType(Object data)
    {
        if(data instanceof Integer)
        {
            return "int";
        }
        if(data instanceof Float)
        {
            return "float";
        }
        if(data instanceof Boolean)
        {
            return "bool";
        }
        else
        {
            throw new RuntimeException("Unsupported type for constant data");
        }
        
    }

    @Override
    protected DataSource innerSwitchSides() {
        return innerClone();
    }
	@Override
	DataSource innerInline(DataSource reducedAgent1DataSource, DataSource reducedAgent2DataSource) {
		return this;
	}
    @Override
    int innerGetSize() {
        return 1;
    }

    @Override
    public String toString() {
        return "\"ConstantData: " + data + "\"";
    }
    @Override
    boolean innerEquals(DataSource other) {
        if (other instanceof ConstantData) {
            ConstantData<?> otherConstantData = (ConstantData<?>) other;
            return this.data.equals(otherConstantData.data);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
    @Override
    public Set<DataSource> allDataSourcesNeeded() {
        return Set.of(this);
    }
}
