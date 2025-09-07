package com.carolang.common.data_sources;

import java.util.Set;

import org.pcollections.HashPMap;

public class DataProducer extends DataSource {
    private static String typeToCType(DataProducerType type) {
        return switch (type) {
            case STD_IN_INT -> "int";
            default -> throw new RuntimeException("Unsupported type for constant data");
        };
    }

    private final int whichInput;

    // Like IO
    public DataProducer(DataProducerType type, int whichInput) {
        super(typeToCType(type));
        this.type = type;
        this.whichInput = whichInput;
    }

    private DataProducerType type;

    public DataProducerType getType() {
        return type;
    }

    @Override
    protected DataProducer innerClone() {
        return new DataProducer(this.type, whichInput);

    }

    public enum DataProducerType {
        STD_IN_INT, STD_IN_FLOAT
    }

    @Override
    protected String innerCreateCProgramForData(
            String resultVariableName, String agent1ParameterName, String agent2ParameterName,
            HashPMap<DataSource, String> alreadyAllocatedDatasources) {
        return "%s %s = USER_INPUT_%d;\n".formatted( typeToCType(type),resultVariableName, whichInput);
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
    boolean innerEquals(DataSource other) {
        if (other instanceof DataProducer) {
            DataProducer otherData = (DataProducer) other;
            return this.type == otherData.type;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public Set<DataSource> allDataSourcesNeeded() {
        return Set.of(this);
    }

    @Override
    public String toString() {
        return "\"STD_IN %s %d\"".formatted(type, whichInput);
    }
}