package com.carolang.asttolambdatree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.pcollections.HashPMap;
import org.pcollections.HashTreePMap;

import com.carolang.asttolambdatree.exceptions.InvalidMatchStatementException;
import com.carolang.asttolambdatree.exceptions.NonFunctionRecursiveDefinitionException;
import com.carolang.asttolambdatree.exceptions.UnknownTypeAnnotation;
import com.carolang.asttolambdatree.exceptions.UnrecognisedVariableException;
import com.carolang.asttolambdatree.exceptions.VisitorException;
import com.carolang.common.SourceFilePosition;
import com.carolang.common.types.FunctionType;
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
import com.carolang.common.ast_nodes.MagicNodeTag;
import com.carolang.common.ast_nodes.MatchNode;
import com.carolang.common.ast_nodes.MatchStatementVariableNode;
import com.carolang.common.ast_nodes.Node;
import com.carolang.common.ast_nodes.RecursiveReferenceNode;
import com.carolang.frontend.carolangBaseVisitor;
import com.carolang.frontend.carolangParser;
import com.carolang.frontend.carolangParser.AnnotatedFunctionDeclarationContext;
import com.carolang.frontend.carolangParser.CarolangEOFContext;
import com.carolang.frontend.carolangParser.EmptyListContext;
import com.carolang.frontend.carolangParser.FunctionDeclarationContext;
import com.carolang.frontend.carolangParser.IfStatementContext;
import com.carolang.frontend.carolangParser.LetInContext;
import com.carolang.frontend.carolangParser.LetRecInContext;
import com.carolang.frontend.carolangParser.ListConsBaseExpressionContext;
import com.carolang.frontend.carolangParser.ListConsExpressionContext;
import com.carolang.frontend.carolangParser.MatchExpressionContext;
import com.carolang.frontend.carolangParser.MatchExpressionNoBarContext;
import com.carolang.frontend.carolangParser.VariableListContext;

public class lambdaTreeVisitor extends carolangBaseVisitor<Node> {

  // Needed as I want to be able to throw checked exceptions and can't as the base
  // type doesn't declare them in method signature
  public class visitorExceptionWrapper extends RuntimeException {
    private VisitorException innerException;

    visitorExceptionWrapper(VisitorException innerException) {
      this.innerException = innerException;
    }

    public VisitorException getInner() throws VisitorException {
      return innerException;
    }
  }

  private SourceFilePosition getPosition(ParserRuleContext ctx) {
    return new SourceFilePosition(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
  }

  private Optional<MagicNode> getMagicNode(String identifier, SourceFilePosition pos) {
    MagicNodeTag tag;
    switch (identifier) {
      case "+":
      tag = MagicNodeTag.PLUS_INT;
      break;
      case "+.":
      tag = MagicNodeTag.PLUS_FLOAT;
      break;
      case "-":
      tag = MagicNodeTag.MINUS_INT;
      break;
      case "-.":
      tag = MagicNodeTag.MINUS_FLOAT;
      break;
      case "*":
      tag = MagicNodeTag.MULTIPLY_INT;
      break;
      case "*.":
      tag = MagicNodeTag.MULTIPLY_FLOAT;
      break;
      case "/":
      tag = MagicNodeTag.DIVIDE_INT;
      break;
      case "/.":
      tag = MagicNodeTag.DIVIDE_FLOAT;
      break;
      case ">":
      tag = MagicNodeTag.GREATER_INT;
      break;
      case ">.":
      tag = MagicNodeTag.GREATER_FLOAT;
      break;
      case ">=":
      tag = MagicNodeTag.GREATER_EQUALS_INT;
      break;
      case ">=.":
      tag = MagicNodeTag.GREATER_EQUALS_FLOAT;
      break;
      case "<":
      tag = MagicNodeTag.LESS_INT;
      break;
      case "<.":
      tag = MagicNodeTag.LESS_FLOAT;
      break;
      case "<=":
      tag = MagicNodeTag.LESS_EQUALS_INT;
      break;
      case "<=.":
      tag = MagicNodeTag.LESS_EQUALS_FLOAT;
      break;
      case "==":
      tag = MagicNodeTag.EQUALS_INT;
      break;
      case "==.":
      tag = MagicNodeTag.EQUALS_FLOAT;
      break;
      case "!=":
      tag = MagicNodeTag.NOT_EQUALS_INT;
      break;
      case "!=.":
      tag = MagicNodeTag.NOT_EQUALS_FLOAT;
      break;
      case "mod":
      tag = MagicNodeTag.MOD_INT;
      break;
      default:
      return Optional.empty();
    }
    return Optional.of(new MagicNode(tag, pos));
  }

  private abstract class VariableDefinition {

  }

  private class MatchStatementVariableDefinition extends VariableDefinition {
    private MatchNode matchNode;
    private boolean headOrTail;

    public MatchStatementVariableDefinition(MatchNode node, boolean isHead) {
      this.headOrTail = isHead;
      this.matchNode = node;
    }

    public boolean isHead() {
      return headOrTail;
    }

    public boolean isTail() {
      return !headOrTail;
    }

    public MatchNode getMatchNode() {
      return matchNode;
    }
  }

  private class FunctionVaraibleDefinition extends VariableDefinition {
    // We translate let statements into functions so need a map of in scope
    // variables to the function that defines them as well as a bool to indicate if
    // they are the ARGUMENT
    // of the function or if this is a recursive call to the function as arguments
    // and recursive calls get different node types but both are using our namespace
    // DO need to only use one immutable dict so that they hide each other in the
    // correct order.
    FunctionNode function;
    boolean isArgument;
    Optional<Type> type = Optional.empty();

    public FunctionVaraibleDefinition(FunctionNode function, boolean isArgument) {
      this.function = function;
      this.isArgument = isArgument;
    }

    public FunctionVaraibleDefinition(FunctionNode function, boolean isArgument, Type type) {
      this.function = function;
      this.isArgument = isArgument;
      this.type = Optional.of(type);
    }

    public FunctionVaraibleDefinition(boolean isArgument) {
      this.function = null;
      {

      }
      ;
      this.isArgument = isArgument;
    }

    public FunctionNode getFunction() {
      return function;
    }

    public void setFunction(FunctionNode function) {
      assert (this.function == null);// Could be done better
      this.function = function;
      for (RecursiveReferenceNode node : recursiveReferences) {
        node.setDefinition(function);
      }
    }

    public boolean isArgument() {
      return isArgument;
    }

    private Set<RecursiveReferenceNode> recursiveReferences = new HashSet<RecursiveReferenceNode>();

    public void assignOnceAssigned(RecursiveReferenceNode node) {
      recursiveReferences.add(node);
    }
  }

  private HashPMap<String, VariableDefinition> functionArguments = HashTreePMap.empty();

  @Override
  public Node visitCarolangEOF(CarolangEOFContext ctx) {
    return visit(ctx.getChild(0));
  }

  @Override
  public Node visitFunctionApp(carolangParser.FunctionAppContext ctx) {
    Node functionNode = visit(ctx.getChild(0));
    Node ArgumentNode = visit(ctx.getChild(1));
    Node result = new LambdaNode(functionNode, ArgumentNode, getPosition(ctx));
    return result;
  }

  @Override
  public Node visitParenExpr(carolangParser.ParenExprContext ctx) {
    return visit(ctx.getChild(1));
  }

  @Override
  public Node visitIntValue(carolangParser.IntValueContext ctx) {
    return new IntegerNode(Integer.parseInt(ctx.getText()), getPosition(ctx));
  }

  @Override
  public Node visitFloatValue(carolangParser.FloatValueContext ctx) {
    return new FloatNode((float) Float.parseFloat(ctx.getText()), getPosition(ctx));
  }

  @Override
  public Node visitBoolValue(carolangParser.BoolValueContext ctx) {
    return new BooleanNode(Boolean.parseBoolean(ctx.getText()), getPosition(ctx));
  }

  @Override
  public Node visitBase(carolangParser.BaseContext ctx) {
    return visit(ctx.getChild(0));
  }

  @Override
  public Node visitVariableValue(carolangParser.VariableValueContext ctx) {
    String identifier = ctx.getText();

    VariableDefinition identiferDefinition = functionArguments.get(identifier);
    if (identiferDefinition instanceof FunctionVaraibleDefinition def) {

      if (def != null) {
        if (def.isArgument()) {
          FunctionArgumentNode faNode = new FunctionArgumentNode(def.getFunction(), getPosition(ctx));
          if (def.type.isPresent()) {
            faNode.setType(def.type.get());
          }
          return faNode;
        } else {

          RecursiveReferenceNode node = new RecursiveReferenceNode(getPosition(ctx));
          def.assignOnceAssigned(node);
          return node;
        }
      }

    } else if (identiferDefinition instanceof MatchStatementVariableDefinition def) {
      return new MatchStatementVariableNode(getPosition(ctx), def.getMatchNode(), def.isHead());
    } else {
      Optional<MagicNode> magicNode = getMagicNode(identifier, getPosition(ctx));
      if (magicNode.isPresent()) {
        return magicNode.get();
      }
    }
    UnrecognisedVariableException error = new UnrecognisedVariableException(identifier, getPosition(ctx));
    throw new visitorExceptionWrapper(error);
  }

  @Override
  public Node visitIfStatement(IfStatementContext ctx) {
    Node Condition = visit(ctx.getChild(1));
    Node Then = visit(ctx.getChild(3));
    Node Else = visit(ctx.getChild(5));

    return new IfNode(Condition, Then, Else, getPosition(ctx));
  }

  // At this phase can get rid of Named variables anyway
  // ie if we have let x = 5 in e1 occurrences of x in e1 will just be pointers to
  // the let node
  // Another way of doing this is to have a complete copy of the tree copied in
  // for each let - this wont work recursively and also means we evaluate every
  // time an argument is used
  // So a let is going to create its own lambda tree that is completely separate
  // from this one and in this stage we bind all variable nodes to the lambda tree
  // they represent i.e. here is where we manage binding contexts.
  @Override
  public Node visitLetIn(LetInContext ctx) {
    String variableName = ctx.getChild(1).getText();
    Node variableValue = visit(ctx.getChild(3));

    FunctionNode function = new FunctionNode(getPosition(ctx), variableName);
    HashPMap<String, VariableDefinition> oldFunctionArguments = functionArguments;
    functionArguments = functionArguments.plus(variableName, new FunctionVaraibleDefinition(function, true));
    Node expressionToSubInto = visit(ctx.getChild(5));
    function.setDefinition(expressionToSubInto);
    functionArguments = oldFunctionArguments;

    LambdaNode functionApp = new LambdaNode(function, variableValue, getPosition(ctx));
    return functionApp;

  }

  public Node visitLetRecIn(LetRecInContext ctx) {
    String variableName = ctx.getChild(1).getText();

    HashPMap<String, VariableDefinition> oldFunctionArguments = functionArguments;

    FunctionVaraibleDefinition def = new FunctionVaraibleDefinition(false);
    functionArguments = functionArguments.plus(variableName, def);
    // So references to the variableName are recursive uses of the variable within
    // its own definition rather than uses of the variable in a normal sense
    Node variableValue = visit(ctx.getChild(3));
    if (variableValue instanceof FunctionNode fNode) {
      def.setFunction(fNode);
    } else {
      NonFunctionRecursiveDefinitionException error = new NonFunctionRecursiveDefinitionException(variableName,
          getPosition(ctx));
      throw new visitorExceptionWrapper(error);
    }
    functionArguments = oldFunctionArguments; // Technically unnecessary I think

    FunctionNode function = new FunctionNode(getPosition(ctx), variableName);
    functionArguments = functionArguments.plus(variableName, new FunctionVaraibleDefinition(function, true));
    Node expressionToSubInto = visit(ctx.getChild(5));
    function.setDefinition(expressionToSubInto);
    functionArguments = oldFunctionArguments;

    LambdaNode functionApp = new LambdaNode(function, variableValue, getPosition(ctx));
    return functionApp;
  }

  @Override
  public Node visitFunctionDeclaration(FunctionDeclarationContext ctx) {
    String ArgumentName = ctx.getChild(1).getText();
    ParseTree functionBodyTree = ctx.getChild(3);

    return createFunctionNode(getPosition(ctx), ArgumentName, functionBodyTree, Optional.empty());
  }

  private Node createFunctionNode(SourceFilePosition pos, String ArgumentName, ParseTree functionBodyTree,
      Optional<Type> type) {
    FunctionNode function = new FunctionNode(pos, ArgumentName);

    HashPMap<String, VariableDefinition> oldFunctionArguments = functionArguments;
    if (type.isPresent()) {
      functionArguments = functionArguments.plus(ArgumentName,
          new FunctionVaraibleDefinition(function, true, type.get()));
    } else {
      functionArguments = functionArguments.plus(ArgumentName, new FunctionVaraibleDefinition(function, true));
    }

    Node result = visit(functionBodyTree);
    function.setDefinition(result);

    functionArguments = oldFunctionArguments;

    return function;
  }

  @Override
  public Node visitAnnotatedFunctionDeclaration(AnnotatedFunctionDeclarationContext ctx) {
    String ArgumentName = ctx.getChild(1).getText();
    ParseTree functionBodyTree = ctx.getChild(5);
    Type type = annotationToType(ctx.getChild(3));

    Node node = createFunctionNode(getPosition(ctx), ArgumentName, functionBodyTree, Optional.of(type));

    if (type == null) {
      throw new visitorExceptionWrapper(new UnknownTypeAnnotation(getPosition(ctx)));

    }

    return node;

  }

  private Type annotationToType(ParseTree annotation) {
    if (annotation.getChildCount() == 5) {
      assert (annotation.getChild(2).getText().equals("->"));
      Type argumentType = annotationToType(annotation.getChild(1));
      Type outputType = annotationToType(annotation.getChild(3));
      return new FunctionType(argumentType, outputType);
    }
    assert (annotation.getChildCount() == 1);
    switch (annotation.getChild(0).getText()) {
      case "int":
        return Type.Int;
      case "float":
        return Type.Float;
      default:
        return null;
    }
  }

  @Override
  public Node visitListConsExpression(ListConsExpressionContext ctx) {
    Node head = visit(ctx.getChild(1));
    Node tail = visit(ctx.getChild(4));
    Node result = new ConsNode(getPosition(ctx), head, tail);
    return result;
  }

  @Override
  public Node visitListConsBaseExpression(ListConsBaseExpressionContext ctx) {
    Node head = visit(ctx.getChild(0));
    Node tail = visit(ctx.getChild(2));
    Node result = new ConsNode(getPosition(ctx), head, tail);
    return result;
  }

  @Override
  public Node visitListRawDefinition(carolangParser.ListRawDefinitionContext ctx) {
    List<Node> elements = new ArrayList<>();
    for (int i = 1; i < ctx.getChildCount(); i += 2) {
      elements.add(visit(ctx.getChild(i)));
    }
    Node tail = new EmptyListNode(getPosition(ctx));
    Collections.reverse(elements);
    for (Node element : elements) {
      tail = new ConsNode(getPosition(ctx), element, tail);
    }
    return tail;
  }

  @Override
  public Node visitVariableList(VariableListContext ctx) {
    return visit(ctx.getChild(0));
  }

  @Override
  public Node visitEmptyList(EmptyListContext ctx) {
    return new EmptyListNode(getPosition(ctx));
  }

  @Override
  public Node visitMatchExpression(MatchExpressionContext ctx) {
    Node expression = visit(ctx.getChild(1));
    List<ParseTree> cases = new ArrayList<>();
    for (int i = 4; i < ctx.getChildCount(); i += 2) {
      cases.add(ctx.getChild(i));
    }
    return handleMatchExpression(expression, cases, getPosition(ctx));
  }

  @Override
  public Node visitMatchExpressionNoBar(MatchExpressionNoBarContext ctx) {
    Node expression = visit(ctx.getChild(1));
    List<ParseTree> cases = new ArrayList<>();
    for (int i = 3; i < ctx.getChildCount(); i += 2) {
      cases.add(ctx.getChild(i));
    }
    return handleMatchExpression(expression, cases, getPosition(ctx));
  }

  public Node handleMatchExpression(Node expression, List<ParseTree> cases, SourceFilePosition pos) {
    if (cases.size() != 2) {
      throw new visitorExceptionWrapper(new InvalidMatchStatementException(pos, "Match statement must have 2 cases"));
    }
    ParseTree emptyListCaseTree;
    ParseTree nonEmptyListCaseTree;

    Predicate<ParseTree> isEmptyListCase = (ParseTree caseNode) -> {
      return caseNode.getChild(0).getText().equals("[]");
    };

    if (isEmptyListCase.test(cases.get(0))) {
      emptyListCaseTree = cases.get(0);
      if (isEmptyListCase.test(cases.get(1))) {
        throw new visitorExceptionWrapper(
            new InvalidMatchStatementException(pos, "Match statement can't have two cases for empty list"));
      }
      nonEmptyListCaseTree = cases.get(1);
    } else {
      if (!isEmptyListCase.test(cases.get(1))) {
        throw new visitorExceptionWrapper(
            new InvalidMatchStatementException(pos, "Match statement must have case for empty list"));
      }
      emptyListCaseTree = cases.get(1);
      nonEmptyListCaseTree = cases.get(0);
    }

    MatchNode matchNode = new MatchNode(pos, expression);
    Node emptyListCase = visit(emptyListCaseTree);
    VariableDefinition headBinding = new MatchStatementVariableDefinition(matchNode, true);
    VariableDefinition tailBinding = new MatchStatementVariableDefinition(matchNode, false);
    HashPMap<String, VariableDefinition> oldFunctionArguments = functionArguments;
    functionArguments = functionArguments.plus(nonEmptyListCaseTree.getChild(0).getText(), headBinding);
    functionArguments = functionArguments.plus(nonEmptyListCaseTree.getChild(2).getText(), tailBinding);
    Node nonEmptyListCase = visit(nonEmptyListCaseTree);
    functionArguments = oldFunctionArguments;

    matchNode.setDefinitions(emptyListCase, nonEmptyListCase);
    return matchNode;

  }

}
