// package com.carolang.common.data_sources;

// import java.util.HashMap;
// import java.util.Map;

// public class DuplicatedDataSource extends DataSource {

//     private DataSource duplicatedFrom;
//     private String CTypeforData;

//     public DuplicatedDataSource(DataSource duplicatedFrom, String CTypeForData)
//     {
//         this.duplicatedFrom = duplicatedFrom;
//         this.CTypeforData = CTypeForData;
//     }

//     private static Map<DataSource, DataSource> cloneMap = null;

//     static void clearCloneMap()
//     {
//         cloneMap = new HashMap<>();
//     }
//     @Override
//     protected DataSource innerClone() {
//         if(cloneMap.containsKey(this))
//         {
//             return cloneMap.get(this);
//         }
//         else
//         {
//             DataSource clone = new DuplicatedDataSource(duplicatedFrom.innerClone(), CTypeforData);
//             cloneMap.put(this, clone);
//             return clone;
//         }
//     }

//     private static Map<DataSource, String> dataSourceToVarName = null;
//     static void clearDataSourceToVarName()
//     {
//         dataSourceToVarName = new HashMap<>();
//     }

//     @Override
//     protected String innerCreateCProgramForData(String resultVariableName, String agent1ParameterName,
//             String agent2ParameterName) {
//         if(dataSourceToVarName.containsKey(duplicatedFrom))
//         {
//             String variableName = dataSourceToVarName.get(duplicatedFrom);
//             return "%s %s = %s;//Reuse duplicated value\n".formatted(CTypeforData, resultVariableName, variableName);
//         }
//         else
//         {
//             String variableName = "SOURCE_DUP_" + getUniqueTag();
//             dataSourceToVarName.put(duplicatedFrom, variableName);
//             String programForInnerData = duplicatedFrom.innerCreateCProgramForData(variableName, agent1ParameterName, agent2ParameterName);
//             String result = "%s %s = %s;\n".formatted(CTypeforData, resultVariableName, variableName);
//             return programForInnerData + result;
//         }
//     }


//     private static Map<DataSource, DataSource> dataSourceToSwitchedSidesMap = null;

//     static void clearDataSourceToSwitchedSidesMap()
//     {
//         dataSourceToSwitchedSidesMap = new HashMap<>();
//     }
//     @Override
//     protected DataSource innerSwitchSides() {
//         if(dataSourceToSwitchedSidesMap.containsKey(duplicatedFrom))
//         {
//             return dataSourceToSwitchedSidesMap.get(duplicatedFrom);
//         }
//         else
//         {
//             DataSource switchedSides = duplicatedFrom.innerSwitchSides();
//             dataSourceToSwitchedSidesMap.put(duplicatedFrom, switchedSides);
//             return new DuplicatedDataSource(switchedSides, CTypeforData);
//         }
//     }

//     static Map<DataSource, DataSource> inlineMap = null;
//     static void clearInlineMap()
//     {
//         inlineMap = new HashMap<>();
//     }
// 	@Override
// 	public DataSource innerInline(DataSource reducedAgent1DataSource, DataSource reducedAgent2DataSource) {
//         if(inlineMap.containsKey(duplicatedFrom))
//         {
//             return inlineMap.get(duplicatedFrom);
//         }
//         else
//         {
//             DataSource inlined = duplicatedFrom.innerInline(reducedAgent1DataSource, reducedAgent2DataSource);
//             inlineMap.put(duplicatedFrom, inlined);
//             return new DuplicatedDataSource(inlined, CTypeforData);
//         }
//     }

//     @Override 
//     public String toString() {
//         return "\"DuplicatedDataSource\"";
// 	}
    
//     private static Map<DataSource, Integer> sizesMap = null;
    
//     static void clearSizesMap()
//     {
//         sizesMap = new HashMap<>();
//     }
//     @Override
//     int innerGetSize() {
//         if(sizesMap.containsKey(duplicatedFrom))
//         {
//             return 1;
//         }
//         else
//         {
//             int size = duplicatedFrom.innerGetSize();
//             sizesMap.put(duplicatedFrom, size);
//             return size;
//         }
//     }
        

//     @Override
//     boolean innerEquals(DataSource other) {
//         return other instanceof DuplicatedDataSource && this.duplicatedFrom.equals(((DuplicatedDataSource) other).duplicatedFrom);
//     }
    
// }
