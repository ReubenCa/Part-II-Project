package com.carolang.common.data_sources;

import java.util.Set;

import org.pcollections.HashPMap;

public abstract class DataSource {

    final String resultCType;
    public DataSource(String resultCType)
    {
        this.resultCType = resultCType;
    }


    //Things like input from IO
    protected abstract DataSource innerClone();
    public DataSource Clone()
    {
      //  DuplicatedDataSource.clearCloneMap();
        return innerClone();
    }

    private static long VarUniqueTag = 0;
    protected String getUniqueTag()
    {
        return "TAG_%d_".formatted(VarUniqueTag++);
    }
    protected abstract String innerCreateCProgramForData(String resultVariableName, String agent1ParameterName, String agent2ParameterName, HashPMap<DataSource, String> alreadyAllocatedDatasources);



    public String CreateCProgramForData(String resultVariableName, String agent1ParameterName, String agent2ParameterName, HashPMap<DataSource, String> alreadyAllocatedDatasources)
    {

        //How datasources can be optimised
        //We have tree of results for rule
        //We go bottom up on the tree
        //Maintaining at each node a set of all datasources where EVERY path below it needs this source and that its parent doesn't have this property
        //So pass looks like recursively call on both children - take intersection of their sets AND REMOVE INTERSECTION FROM THEIR SETS.
        //Or actually in the top down pass we just keep a Map of all the DataSources already initialised 
        //That is the node this source gets allocated at
        //Then once we have assigned to each node in tree which sources get allocated at it we do a top down pass that actually allocates each node

        //Data source has a global static map of all sources that have already been allocated
        //We have a memoizedCreateCProgramForData that checks this map and then defaults to a  overridden method if not in the map
        //The methods all make call to memoizedCreateCProgramForData

        if(alreadyAllocatedDatasources.containsKey(this))
        {
            return "%s %s = %s;\n".formatted(resultCType ,resultVariableName, alreadyAllocatedDatasources.get(this));
        }
        else
        {
            //Thanks to the first bottom up pass we know that alreadyAllocatedDatasources contains precisely the datasources we want here
            return innerCreateCProgramForData(resultVariableName, agent1ParameterName, agent2ParameterName, alreadyAllocatedDatasources);
        }
    }

    public boolean dependsOn(DataSource other)
    {
        return false;
    }

    /**
     * Switches Agent1 and Agent2
     */
    protected abstract DataSource innerSwitchSides();
    public DataSource SwitchSides()
    {
      //  DuplicatedDataSource.clearDataSourceToSwitchedSidesMap();
        return innerSwitchSides();
    }

    abstract DataSource innerInline(DataSource reducedAgent1DataSource, DataSource reducedAgent2DataSource);
    
    public DataSource Inline(DataSource reducedAgent1DataSource, DataSource reducedAgent2DataSource)
    {
   //     DuplicatedDataSource.clearInlineMap();
        return innerInline(reducedAgent1DataSource, reducedAgent2DataSource);
    }

    abstract int innerGetSize();

    public int getSize()
    {
     //   DuplicatedDataSource.clearSizesMap();
        return innerGetSize();
    }

    @Override 
    public String toString() {
        return "\"DataSource\"";
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof DataSource)) {
            return false;
        }
        DataSource other = (DataSource) obj;
        return innerEquals(other);
    }

    abstract boolean innerEquals(DataSource other);

    @Override
    public abstract int hashCode();

    //Returns the set of all datasources this node needs - including this one
    public abstract Set<DataSource> allDataSourcesNeeded();
}
