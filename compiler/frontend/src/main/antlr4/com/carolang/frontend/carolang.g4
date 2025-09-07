grammar carolang;

carolang
    : expression EOF #carolangEOF
    ;
 
expression
    :  expression expression #functionApp
    | IF expression THEN expression ELSE expression  #ifStatement
    | LET variable EQUALS expression IN expression  #letIn
    | LETREC variable EQUALS expression IN expression #letRecIn
    | baseexpression #base
    | list #listBeingDefined
    | MATCH expression WITH (VERTICAL_BAR matchOutcome)* #matchExpression
    | MATCH expression WITH matchOutcome (VERTICAL_BAR matchOutcome)* #matchExpressionNoBar
    ;


matchOutcome
    : EMPTYLIST ARROW expression #emptyListMatch
    | variable LIST_JOIN variable ARROW expression #listMatch
    ;

baseexpression
    : INT #intValue
    | BOOL #boolValue
    | FLOAT #FloatValue
    | variable #variableValue
    | FUN variable ARROW expression #functionDeclaration
    | FUN variable (TYPE_ANNOTATION type)? ARROW expression #annotatedFunctionDeclaration
    | LPAREN expression RPAREN  #parenExpr
    ;


list 
    : LPAREN expression RPAREN LIST_JOIN expression #listConsExpression
    | baseexpression LIST_JOIN list #listConsBaseExpression
    | LIST_LPAREN (expression LIST_SEMICOLON)* expression LIST_RPAREN #listRawDefinition
    | baseexpression #variableList
    | EMPTYLIST #emptyList
    ;

type
    : GROUND_TYPE
    | LPAREN type ARROW type RPAREN
    | LPAREN type RPAREN
    ;

//Lexing Rules

fragment SPECIAL_VAR
    : '+'
    | '+.'
    | '-'
    | '-.'
    | '*'
    | '*.'
    | '/'
    | '/.'
    | '=='
    | '==.'
    | '>='
    | '>=.'
    | '>'
    | '>.'
    | '<='
    | '<=.'
    | '<'
    | '<.'
    | '!='
    | '!=.'
    ;

MATCH
    : 'match'
    ;

WITH
    : 'with'
    ;

EMPTYLIST 
    : '[]'
    ;

LIST_LPAREN
    : '['
    ;

LIST_RPAREN
    : ']'
    ;

LIST_SEMICOLON
    : ';'
    ;

LIST_JOIN
    : '::' 
    ;

LPAREN
    : '(' 
    ;

RPAREN 
    : ')' 
    ;

EQUALS
    : '='
    ;

TYPE_ANNOTATION
    : ':'
    ;
    
INT
    : '0'                        
    | '-'? [1-9] [0-9]*     
    ;

FLOAT
    : '-'? [0-9]+ '.' [0-9]+ 'f'?
    | '-'? [0-9]+ 'f'
    ;

BOOL
    : 'true'
    | 'false'
    ;

LETREC
    : 'let rec'
    ;

LET 
    : 'let'
    ;

IN 
    : 'in'
    ;

FUN
    : 'fun'
    ;

ARROW 
    : '->'
    ;

IF 
    : 'if'
    ;

THEN 
    : 'then'
    ;

ELSE 
    : 'else'
    ;

variable
    : VARIABLE
    ;

GROUND_TYPE
    : 'int'
    | 'bool'
    | 'float'
    ;

VERTICAL_BAR
    : '|'
    ;

//Want variable rule last to avoid lexing keywords as variables
VARIABLE
    : [a-z][a-zA-Z]*
    | SPECIAL_VAR
    ;

//Skips whitespace
WS
    : [ \t\n\r]+ -> skip
    ;