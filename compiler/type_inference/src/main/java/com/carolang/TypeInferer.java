package com.carolang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.carolang.common.types.FunctionType;
import com.carolang.common.types.ListType;
import com.carolang.common.types.Type;
import com.carolang.common.ast_nodes.BooleanNode;
import com.carolang.common.ast_nodes.ConsNode;
import com.carolang.common.ast_nodes.EmptyListNode;
import com.carolang.common.ast_nodes.FloatNode;
import com.carolang.common.ast_nodes.FunctionArgumentNode;
import com.carolang.common.ast_nodes.FunctionNode;
import com.carolang.common.ast_nodes.IfNode;
import com.carolang.common.ast_nodes.IntegerNode;
import com.carolang.common.ast_nodes.LambdaNode;
import com.carolang.common.ast_nodes.MagicNode;
import com.carolang.common.ast_nodes.MatchNode;
import com.carolang.common.ast_nodes.MatchStatementVariableNode;
import com.carolang.common.ast_nodes.Node;
import com.carolang.common.ast_nodes.RecursiveReferenceNode;

public class TypeInferer {

    public static void InferTypes(Node root) throws TypeException {
        (new TypeInferer()).innerInferTypes(root);
    }

    TypeInferer() {
        equalTypes.AddItem(Type.Int);
        equalTypes.AddItem(Type.Float);
        equalTypes.AddItem(Type.Boolean);
    }

    private void innerInferTypes(Node root) throws TypeException {
        evaluateType(root);

        // Repeatedly iterate over our sets
        // If a set contains two different bound types terminate with error (1)
        // If a set contains alpha and a alpha ->beta or beta -> alpha terminate with
        // error (2)
        // If a set contains alpha -> beta and gamma -> delta merge alpha and betas set
        // as well as gammas and deltas set (3)
        // We terminate if we do an iteration with no change
        // We then check that every set contains precisely one ground type
        // We then assign each node the ground type that is in the same set as its
        // assigned type in nodeTypes
        boolean changesMade = true;
        while (changesMade) {
            changesMade = false;
            Set<Set<Type>> equalSets = equalTypes.getSets();// Refreshes with all the updates of last iteration
            for (Set<Type> equalSet : equalSets) {
                Set<Type> boundTypes = new HashSet<>(); // automatically removes duplicates - since Types have correct
                                                        // equals method
                Set<Type> allArgumentTypes = new HashSet<>();
                Set<Type> allOutputTypes = new HashSet<>();
                Set<Type> allListElementTypes = new HashSet<>();
                for (Type t : equalSet) {
                    if (Type.isBound(t)) {
                        boundTypes.add(t);
                        if (boundTypes.size() > 1) {                            
                            throw new TypeException("Inconsistent types, two expressions should have the same type but don't"); // Consistency Check
                            //TODO: output which expressions and their SourceFilePosition
                        }
                    }
                    if (t instanceof FunctionType fType) {
                        allArgumentTypes.add(fType.argumentType);
                        allOutputTypes.add(fType.outputType);
                    }
                    if (t instanceof ListType lType) {
                        allListElementTypes.add(lType.elementType);
                    }

                }
                for (Type t : (Stream.concat(Stream.concat(allArgumentTypes.stream(), allOutputTypes.stream()),
                        allListElementTypes.stream())).toList()) {
                    if (equalSet.contains(t)) {
                        throw new TypeException("A function or list type contains itself.");// Occurs Check
                    }
                }
                // If no merges occur we must terminate while loop
                Predicate<Set<Type>> allTypesEqual = s -> {
                    Type initial = null;
                    for (Type t : s) {
                        if (initial == null) {
                            initial = t;
                        } else if (!equalTypes.areInSameSet(initial, t)) {
                            return false;
                        }
                    }
                    return true;
                };
                changesMade = changesMade || !allTypesEqual.test(allArgumentTypes)
                        || !allTypesEqual.test(allOutputTypes) || !allTypesEqual.test(allListElementTypes);

                Consumer<Set<Type>> mergeAll = s -> {

                    Stream<Type> sStream = s.stream();
                    sStream.reduce((t1, t2) -> {

                        equalTypes.mergeSets(t1, t2);
                        return t2;
                    });
                };
                mergeAll.accept(allListElementTypes);
                mergeAll.accept(allArgumentTypes);
                mergeAll.accept(allOutputTypes);
            }
        }

        // Now need to map each type to its bound type
        final Map<Type, Type> typeToBoundType = new HashMap<>();
        // This bit marks each type if it is in a set with a bound type
        // Still not quite enough as we might have alpha -> beta
        // Where we have found a bound type for both alpha and beta (or this can recurse
        // further)
        Set<Set<Type>> equalSets = equalTypes.getSets();
        for (Set<Type> equalSet : equalSets) {
            Type boundType = null;
            for (Type t : equalSet) {
                if (Type.isBound(t)) {
                    boundType = t;
                    break;
                }
            }
            if (boundType != null) {
                for (Type t : equalSet) {
                    typeToBoundType.put(t, boundType);
                }
            }
        }

        // We use a dynamic programming approach with typeToBoundType as our memoization
        // table
        for (Entry<Node, Type> entry : nodeTypes.entrySet()) {
            Node node = entry.getKey();
            Type type = entry.getValue();
            node.setType(getBoundType(typeToBoundType, type));
        }

    }

    private Type getBoundType(final Map<Type, Type> typeToBoundType, Type t) throws TypeException {
        if (typeToBoundType.containsKey(t)) {
            return typeToBoundType.get(t);
        }
        // if (t instanceof FunctionType fType) {//No? just need to find if it has an
        // equivalent function type
        // Type argumentType = getBoundType(typeToBoundType, fType.argumentType);
        // Type outputType = getBoundType(typeToBoundType, fType.outputType);
        // Type boundType = new FunctionType(argumentType, outputType);

        // Set<Type> typesEqualToT = equalTypes.getSet(t);
        // for (Type eqT : typesEqualToT) {
        // typeToBoundType.put(eqT, boundType);
        // }
        // return boundType;
        // }
        List<FunctionType> functionTypesEqualToT = equalTypes.getSet(t).stream().filter(FunctionType.class::isInstance)
                .map(FunctionType.class::cast).toList();
        List<ListType> listTypesEqualToT = equalTypes.getSet(t).stream().filter(ListType.class::isInstance)
                .map(ListType.class::cast).toList();

                // Either T is a function/list type (should have something here) OR is a ground type
                                      // in which it should already be in typeToBoundType
                                      Type boundType;
        if(functionTypesEqualToT.size() > 0 && listTypesEqualToT.size() > 0) {
            throw new TypeException("Inconsistent types, two expressions should have the same type but don't"); // Can't be List and Function Type
        }
        if(listTypesEqualToT.size() > 0) {
            Set<Type> allElementTypes = new HashSet<>();
            for (ListType lType : listTypesEqualToT) {
                allElementTypes.add(getBoundType(typeToBoundType, lType.elementType));
            }
            if(allElementTypes.size() > 1) {
                throw new TypeException("A list contain only contain elements of one type.");
            }
            Type elementType = allElementTypes.iterator().next();
            boundType = new ListType(elementType);
        }
        else if (functionTypesEqualToT.size() > 0) {

        
        Set<Type> allArgumentTypes = new HashSet<>();
        Set<Type> allOutputTypes = new HashSet<>();
        for (FunctionType fType : functionTypesEqualToT) {
            allArgumentTypes.add(getBoundType(typeToBoundType, fType.argumentType));
            allOutputTypes.add(getBoundType(typeToBoundType, fType.outputType));
        }
        if (allArgumentTypes.size() > 1 || allOutputTypes.size() > 1) {
            throw new TypeException("A function cannot have different input and output types in different calls");
        }
        Type argumentType = allArgumentTypes.iterator().next();
        Type outputType = allOutputTypes.iterator().next();
        boundType = new FunctionType(argumentType, outputType);
        }   
        // equalTypes.mergeSets(boundType, t);
        else 
        {
            //(x -> x) goes here
            throw new TypeException("Not enough information to determine type of all expressions, add type annotations to ambiguous function arguments");
        }
        typeToBoundType.put(t, boundType);
        return boundType;
    }

    private DisjointSet<Type> equalTypes = new DisjointSet<>();

    private Map<Node, Type> nodeTypes = new HashMap<>();

    Map<Type, Node> types;

    Map<FunctionNode, FunctionType> functionTypes = new HashMap<>();
    Map<MatchNode, Type> matchExpressionElementTypes = new HashMap<>();//The type that of the elements of the list being matched

    private Type evaluateType(Node node) throws TypeException {

        Type t;

        if (node instanceof FunctionNode fNode) {
            Type argumentType = Type.getUnboundType();
            Type unboundDefinitionType = Type.getUnboundType();
            equalTypes.AddItem(unboundDefinitionType);
            equalTypes.AddItem(argumentType);

            FunctionType unboundFunctionType = new FunctionType(argumentType, unboundDefinitionType);
            equalTypes.AddItem(unboundFunctionType);
            functionTypes.put(fNode, unboundFunctionType);

            Type definitionType = evaluateType(fNode.getDefinition());

            t = new FunctionType(argumentType, definitionType);
            equalTypes.AddItem(t);
            equalTypes.mergeSets(t, unboundFunctionType);

            // Lambda node will infer that this argument must be of the same type as its
            // right child
        } else if (node instanceof IntegerNode) {
            t = Type.Int;
        } else if (node instanceof FloatNode) {
            t = Type.Float;
        } else if (node instanceof BooleanNode) {
            t = Type.Boolean;
        } else if (node instanceof LambdaNode lNode) {
            Type functionType = evaluateType(lNode.getFunction());
            Type argumentType = evaluateType(lNode.getArgument());
            // if (functionType instanceof FunctionType fType) {
            // equalTypes.mergeSets(argumentType, fType.argumentType);
            // t = fType.outputType;
            // } else {
            // //Create new Function Type

            // }
            // Instead create new type fo function type and infer it is equal to existing
            // ones
            // Also Infer the argument of new function type is the same type as the thing
            // lambda node being applied to

            Type unboundOutputType = Type.getUnboundType();
            Type unboundFunctionType = new FunctionType(argumentType, unboundOutputType);
            equalTypes.AddItem(unboundFunctionType);
            equalTypes.AddItem(unboundOutputType);
            equalTypes.mergeSets(unboundFunctionType, functionType);

            t = unboundOutputType;

        } else if (node instanceof FunctionArgumentNode faNode) {
            t = functionTypes.get(faNode.getDefiningFunction()).argumentType;
        } else if (node instanceof RecursiveReferenceNode rrNode) {
            t = functionTypes.get(rrNode.getDefinition());
        } else if (node instanceof MagicNode mNode) {

            t = mNode.getType();
            if (t instanceof FunctionType fType) {
                Type resultType = fType.outputType;
                equalTypes.AddItem(resultType);
            }


        } else if (node instanceof IfNode ifNode) {
            Type trueType = evaluateType(ifNode.getThen());
            Type falseType = evaluateType(ifNode.getElse());
            Type ConditionType = evaluateType(ifNode.getCondition());
            equalTypes.AddItem(ConditionType);
            equalTypes.mergeSets(ConditionType, Type.Boolean);
            equalTypes.AddItem(falseType);
            equalTypes.mergeSets(trueType, falseType);
            t = trueType;
        } else if (node instanceof ConsNode consNode) {
            Type headType = evaluateType(consNode.getHead());
            Type tailType = evaluateType(consNode.getTail());
            t = new ListType(headType);
            equalTypes.AddItem(t);
            equalTypes.AddItem(tailType);
            equalTypes.AddItem(headType);
            equalTypes.mergeSets(t, tailType);
        } else if (node instanceof EmptyListNode) {
            Type elementType = Type.getUnboundType();
            t = new ListType(elementType);
            equalTypes.AddItem(elementType);
        } else if (node instanceof MatchNode mNode)
        {


            Type expressionType = evaluateType(mNode.getExpressionBeingMatched());
            Type expressionElementsType = Type.getUnboundType();
            equalTypes.AddItem(expressionElementsType);
            Type unboundExpressionType = new ListType(expressionElementsType);
            equalTypes.AddItem(unboundExpressionType);
            equalTypes.mergeSets(expressionType, unboundExpressionType);
            matchExpressionElementTypes.put(mNode, expressionElementsType);
          

            Node emptyCase = mNode.getEmptyCase();
            Node nonEmptyCase = mNode.getNonEmptyCase();
            Type emptyCaseType = evaluateType(emptyCase);
            Type nonEmptyCaseType = evaluateType(nonEmptyCase);
            equalTypes.mergeSets(emptyCaseType, nonEmptyCaseType);
            t = emptyCaseType;
        } 
        else if (node instanceof MatchStatementVariableNode msvNode) {
            t = Type.getUnboundType();
            equalTypes.AddItem(t);
            Type matchExpressionElementsType = matchExpressionElementTypes.get(msvNode.getMatchStatement());
            if(msvNode.isHead())
            {
                equalTypes.mergeSets(t, matchExpressionElementsType);
            }
            else
            {
                Type tailType = new ListType(matchExpressionElementsType);
                equalTypes.AddItem(tailType);
                equalTypes.mergeSets(t, tailType);
            }
        }     
        else {
            throw new UnsupportedOperationException("Invalid node passed in to setType: " + node.getClass().getName(), null);
        }

        if (node.getType() != null) {
            equalTypes.AddItem(node.getType());// In case of type annotations
            equalTypes.mergeSets(t, node.getType());
        }
        nodeTypes.put(node, t);
        equalTypes.AddItem(t);
        return t;
    }
}