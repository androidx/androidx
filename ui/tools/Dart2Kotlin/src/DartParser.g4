parser grammar DartParser;

options { tokenVocab = DartLexer; }

// 18 Libraries and Scripts

libraryDefinition
    : scriptTag? libraryName? importOrExport+ partDirective* topLevelDefinition*
    ;

scriptTag
    : SCRIPT_START (~NL)* NL
    ;

libraryName
    : metadata LIBRARY identifier (DOT identifier)* SEMI
    ;

topLevelDefinition
    : classDefinition 
    | enumType 
    | typeAlias 
    | functionSignatureDefinition
    | getterSignatureDefinition
    | setterSignatureDefinition
    | functionDefinition
    | getterDefinition
    | setterDefinition
    | staticFinalDeclarations
    | variableDeclaration SEMI
    ;

functionSignatureDefinition
    : EXTERNAL? functionSignature SEMI
    ;
getterSignatureDefinition
    : EXTERNAL? getterSignature SEMI
    ;
setterSignatureDefinition
    : EXTERNAL? setterSignature SEMI
    ;
functionDefinition
    : functionSignature functionBody
    ;
getterDefinition
    : returnType? GET identifier functionBody
    ;
setterDefinition
    : returnType? SET identifier formalParameterList functionBody
    ;
staticFinalDeclarations
    : (FINAL | CONST) type? staticFinalDeclarationList SEMI
    ;

getOrSet
    : GET
    | SET
    ;

importOrExport
    : libraryImport 
    | libraryExport
    ;

// 18.1 Imports

libraryImport
    : metadata importSpecification
    ;

importSpecification
    : IMPORT uri (AS identifier)? combinator* SEMI
    | IMPORT uri DEFERRED AS identifier combinator* SEMI
    ;

combinator
    : SHOW identifierList
    | HIDE identifierList
    ;

identifierList
    : identifier (COMMA identifier)*
    ;

// 18.2 Exports
libraryExport
    : metadata EXPORT uri combinator* SEMI
    ;

// 18.3 Parts

partDirective
    : metadata PART uri SEMI
    ;

partHeader
    : metadata PART OF identifier (DOT identifier)* SEMI
    ;

partDeclaration
    : partHeader topLevelDefinition* EOF
    ;

// 18.5 URIs
uri
    : stringLiteral
    ;

// 8 Variables
variableDeclaration
    : declaredIdentifier (COMMA identifier)*
    ;

declaredIdentifier
    : metadata finalConstVarOrType identifier
    ;

finalConstVarOrType
    : FINAL type?
    | CONST type?
    | COVARIANT type
    | varOrType
    ;

varOrType
    : VAR
    | type
    ;

initializedVariableDeclaration
    : declaredIdentifier (ASSIGN expression)? (COMMA initializedIdentifier)*
    ;

initializedIdentifier
    : identifier (ASSIGN expression)?
    ;

initializedIdentifierList
    : initializedIdentifier (COMMA initializedIdentifier)*
    ;

// 9 Functions

functionSignature
    : metadata returnType? identifier formalParameterList
    ;

returnType
    : VOID
    | type
    ;

functionBody
    : ASYNC? FAT_ARROW expression SEMI
    | (ASYNC | ASYNC_STAR | SYNC_STAR) block
    ;

block
    : CURLY_OPEN statements CURLY_CLOSE
    ;

// 9.2 Formal Parameters

formalParameterList
    : LPAREN RPAREN
    | LPAREN normalFormalParameters ( COMMA optionalFormalParameters)? RPAREN

    | LPAREN optionalFormalParameters RPAREN
    ;

normalFormalParameters
    : normalFormalParameter (COMMA normalFormalParameter)*
    ;

optionalFormalParameters
    : optionalPositionalFormalParameters
    | namedFormalParameters
    ;

optionalPositionalFormalParameters
    : LBRACKET defaultFormalParameter (COMMA defaultFormalParameter)* RBRACKET
    ;

namedFormalParameters
    : CURLY_OPEN defaultNamedParameter (COMMA defaultNamedParameter)* CURLY_CLOSE
    ;

// 9.2.1 Required Formals

normalFormalParameter
    : functionSignature
    | fieldFormalParameter
    | simpleFormalParameter
    ;

simpleFormalParameter
    : declaredIdentifier
    | metadata identifier
    ;

fieldFormalParameter
    : metadata finalConstVarOrType? THIS DOT identifier formalParameterList?
    ;

// 9.2.2 Optional Formals

defaultFormalParameter
    : normalFormalParameter (ASSIGN expression)?
    ;

defaultNamedParameter
    : normalFormalParameter ( COLON expression)?
    ;

// 10 Classes
classDefinition
    : metadata ABSTRACT? CLASS identifier typeParameters? (superclass mixins?)? interfaces? CURLY_OPEN classMemberDefinition* CURLY_CLOSE
    | metadata ABSTRACT? CLASS mixinApplicationClass
    ;

mixins
    : WITH typeList
    ;

classMemberDefinition
    : metadata declaration SEMI
    | metadata methodSignature functionBody
    ;

methodSignature
    : constructorSignature initializers?
    | factoryConstructorSignature
    | redirectingFactoryConstructorSignature
    | STATIC? functionSignature
    | STATIC? getterSignature
    | STATIC? setterSignature
    | operatorSignature
    ;

declaration
    : constantConstructorSignature (redirection | initializers)?
    | constructorSignature (redirection | initializers)?
    | EXTERNAL constantConstructorSignature
    | EXTERNAL constructorSignature
    | ((EXTERNAL STATIC?))? getterSignature
    | ((EXTERNAL STATIC?))? setterSignature
    | EXTERNAL? operatorSignature
    | ((EXTERNAL STATIC?))? functionSignature
    | STATIC (FINAL | CONST) type? staticFinalDeclarationList
    | FINAL type? initializedIdentifierList
    | STATIC? (VAR | type) initializedIdentifierList
    ;
staticFinalDeclarationList
    : staticFinalDeclaration (COMMA staticFinalDeclaration)*
    ;
staticFinalDeclaration
    : identifier ASSIGN expression
    ;

// 10.1.1 Operators

operatorSignature
    : returnType? OPERATOR operator formalParameterList
    ;

operator
    : BIT_NOT
    | binaryOperator
    | LBRACKET RBRACKET
    | LBRACKET RBRACKET ASSIGN
    ;

binaryOperator
    : multiplicativeOperator
    | additiveOperator
    | shiftOperator
    | relationalOperator
    | CMP_EQ
    | bitwiseOperator
    ;

// 10.2 Getters

getterSignature
    : returnType? GET identifier
    ;

// 10.3 Setters

setterSignature
    : returnType? SET identifier formalParameterList
    ;

// 10.6.1 Generative Constructors

constructorSignature
    : identifier (DOT identifier)? formalParameterList
    ;

redirection
    : COLON THIS (DOT identifier)? arguments
    ;

initializers
    : COLON superCallOrFieldInitializer (COMMA superCallOrFieldInitializer)*
    ;

superCallOrFieldInitializer
    : SUPER arguments
    | SUPER DOT identifier arguments
    | fieldInitializer
    | assertInitializer
    ;

assertInitializer
    : ASSERT LPAREN expression RPAREN
    ;

fieldInitializer
    : (THIS DOT)? identifier ASSIGN conditionalExpression cascadeSection*
    ;

// 10.6.2 Factories

factoryConstructorSignature
    : FACTORY identifier (DOT identifier)? formalParameterList
    ;

redirectingFactoryConstructorSignature
    : CONST? FACTORY identifier (DOT identifier)? formalParameterList ASSIGN type (DOT identifier)?
    ;

// 10.6.3 Constant Constructors

constantConstructorSignature
    : CONST qualified formalParameterList
    ;

// 10.9 Superclasses

superclass
    : EXTENDS type
    ;

// 10.10 Superinterfaces

interfaces
    : IMPLEMENTS typeList
    ;

// 12 Mixin Application

mixinApplicationClass
    : identifier typeParameters? ASSIGN mixinApplication SEMI
    ;

mixinApplication
    : type mixins interfaces?
    ;

// 13 Enums

enumType
    : metadata ENUM identifier CURLY_OPEN identifier (COMMA identifier)* COMMA? CURLY_CLOSE
    ;

// 14 Generics

typeParameter
    : metadata identifier (EXTENDS type)?
    ;

typeParameters
    : LANGLE typeParameter (COMMA typeParameter)* RANGLE
    ;

// 15 Metadata

metadata
    : (AT_SIGN qualified (DOT identifier)? (arguments)?)*
    ;

// 16 Expressions
expression
    : assignableExpression assignmentOperator expression
    | conditionalExpression cascadeSection*
    | throwExpression
    ;

expressionWithoutCascade
    : assignableExpression assignmentOperator expressionWithoutCascade
    | conditionalExpression
    | throwExpressionWithoutCascade
    ;

expressionList
    : expression (COMMA expression)*
    ;

primary
    : thisExpression
    | SUPER unconditionalAssignableSelector
    | functionExpression
    | literal
    | identifier
    | newExpression
    | NEW type HASH (DOT identifier)?
    | constObjectExpression
    | LPAREN expression RPAREN
    ;

// 16.1 Constants

literal
    : nullLiteral
    | booleanLiteral
    | numericLiteral
    | stringLiteral
    | symbolLiteral
    | mapLiteral
    | listLiteral
    ;

// 16.2 Null

nullLiteral
    : NULL
    ;

// 16.3 Numbers

numericLiteral
    : NUMBER
    | HEX_NUMBER
    ;

// 16.4 Booleans

booleanLiteral
    : TRUE
    | FALSE
    ;

// 16.5 Strings

stringLiteral
    : (singleLineString | multilineString)+
    ;

singleLineString
    : SINGLE_LINE_NO_ESCAPE_STRING
    | SINGLE_LINE_SQ_STRING (~SQS_END)*? SQS_END
    | SINGLE_LINE_DQ_STRING (~DQS_END)*? DQS_END
    ;

multilineString
    : MULTI_LINE_NO_ESCAPE_STRING
    | MULTI_LINE_SQ_STRING (~SQMLS_END)*? SQMLS_END
    | MULTI_LINE_DQ_STRING (~DQMLS_END)*? DQMLS_END
    ;

// 16.6 Symbols

symbolLiteral
    : HASH (operator | (identifier (DOT identifier)*))
    ;

// 16.7 Lists

listLiteral
    : CONST? typeArguments? LBRACKET (expressionList COMMA?)? RBRACKET
    ;

// 16.8 Maps

mapLiteral
    : CONST? typeArguments? CURLY_OPEN (mapLiteralEntry (COMMA mapLiteralEntry)* COMMA?)? CURLY_CLOSE
    ;

mapLiteralEntry
    : expression COLON expression
    ;

// 16.9 Throw

throwExpression
    : THROW expression
    ;

throwExpressionWithoutCascade
    : THROW expressionWithoutCascade
    ;

// 16.10 Function Expression

functionExpression
    : formalParameterList functionExpressionBody
    ;

functionExpressionBody
    : ASYNC? FAT_ARROW expression
    | (ASYNC | ASYNC_STAR | SYNC_STAR) block
    ;


// 16.11 This
thisExpression
    : THIS
    ;

// 16.12.1 New

newExpression
    : NEW type (DOT identifier)? arguments
    ;

// 16.12.2 Const

constObjectExpression
    : CONST type (DOT identifier)? arguments
    ;

// 16.14.1 Actual Argument List Evaluation

arguments
    : (LANGLE type (COMMA type)* RANGLE)? LPAREN argumentList? RPAREN
    ;

argumentList
    : namedArgument (COMMA namedArgument)* COMMA?
    | expressionList (COMMA namedArgument)* COMMA?
    ;

namedArgument
    : label expression
    ;

// 16.17.2 Cascaded Invocations

cascadeSection
    : DOT_DOT (cascadeSelector arguments*) (assignableSelector arguments*)* (assignmentOperator expressionWithoutCascade)?
    ;

cascadeSelector
    : LBRACKET expression RBRACKET
    | identifier
    ;

// 16.19 Assignment
assignmentOperator
    : ASSIGN
    | compoundAssignmentOperator
    ;

// 16.19.1 Compound Assignment

compoundAssignmentOperator
    : MUL_EQ
    | DIV_EQ
    | TRUNC_EQ
    | MOD_EQ
    | PLU_EQ
    | MIN_EQ
    | LSH_EQ
    | RSH_EQ
    | AND_EQ
    | XOR_EQ
    | OR_EQ
    | NUL_EQ
    ;

// 16.20 Conditional
conditionalExpression
    : ifNullExpression (QUEST expressionWithoutCascade COLON expressionWithoutCascade)?
    ;

// 16.21 If-null Expressions

ifNullExpression
    : logicalOrExpression (IFNULL logicalOrExpression)*
    ;

// 16.22 Logical Boolean Expressions

logicalOrExpression
    : logicalAndExpression (LOG_OR logicalAndExpression)*
    ;

logicalAndExpression
    : equalityExpression (LOG_AND equalityExpression)*
    ;

// 16.23 Equality

equalityExpression
    : relationalExpression (equalityOperator relationalExpression)?
    | SUPER equalityOperator relationalExpression
    ;

equalityOperator
    : CMP_EQ
    | CMP_NEQ
    ;

// 16.24 Relational Expressions

relationalExpression
    : bitwiseOrExpression (typeTest | typeCast | relationalOperator bitwiseOrExpression)?
    | SUPER relationalOperator bitwiseOrExpression
    ;

relationalOperator
    : CMP_GE
    | RANGLE
    | CMP_LE
    | LANGLE
    ;

// 16.25 Bitwise Expressions

bitwiseOrExpression
    : bitwiseXorExpression (BIT_OR bitwiseXorExpression)*
    | SUPER (BIT_OR bitwiseXorExpression)+
    ;

bitwiseXorExpression
    : bitwiseAndExpression (BIT_XOR bitwiseAndExpression)*
    | SUPER (BIT_XOR bitwiseAndExpression)+
    ;

bitwiseAndExpression
    : shiftExpression (BIT_AND shiftExpression)*
    | SUPER (BIT_AND shiftExpression)+
    ;

bitwiseOperator
    : BIT_AND
    | BIT_XOR
    | BIT_OR
    ;

// 16.26 Shift

shiftExpression
    : additiveExpression (shiftOperator additiveExpression)*
    | SUPER (shiftOperator additiveExpression)+
    ;

shiftOperator
    : SHL
    | RANGLE RANGLE
    ;

// 16.27 Additive Expressions

additiveExpression
    : multiplicativeExpression (additiveOperator multiplicativeExpression)*
    | SUPER (additiveOperator multiplicativeExpression)+
    ;

additiveOperator
    : PLUS
    | MINUS
    ;

// 16.28 Multiplicative Expressions

multiplicativeExpression
    : unaryExpression (multiplicativeOperator unaryExpression)*
    | SUPER (multiplicativeOperator unaryExpression)+
    ;

multiplicativeOperator
    : MUL
    | DIV
    | MOD
    | TRUNC
    ;

// 16.29 Unary Expressions

unaryExpression
    : prefixOperator otherUnaryExpression
    | otherUnaryExpression
    ;

otherUnaryExpression
    : awaitExpression
    | postfixExpression
    | (minusOperator | tildeOperator) SUPER
    | incrementOperator assignableExpression
    ;

prefixOperator
    : minusOperator
    | negationOperator
    | tildeOperator
    ;

minusOperator
    : MINUS
    ;

negationOperator
    : NOT
    ;

tildeOperator
    : '~'
    ;

// 16.30 Await Expressions

awaitExpression
    : AWAIT unaryExpression
    ;

// 16.31 Postfix Expressions

postfixExpression
    : assignableExpression postfixOperator
    | primary (selector* | ( HASH ( (identifier ASSIGN?) | operator)))
    ;

postfixOperator
    : incrementOperator
    ;

selector
    : assignableSelector
    | arguments
    ;

incrementOperator
    : INC
    | DEC
    ;

// 16.32 Assignable Expressions
assignableExpression
    : primary (arguments* assignableSelector)+
    | SUPER unconditionalAssignableSelector
    | identifier
    ;

unconditionalAssignableSelector
    : LBRACKET expression RBRACKET
    | DOT identifier
    ;

assignableSelector
    : unconditionalAssignableSelector
    | QUESTION_DOT identifier
    ;

// 16.33 Identifier Reference

identifier
    : IDENTIFIER
    | DYNAMIC
    | LIBRARY
    ;

qualified
    : identifier (DOT identifier)?
    ;

// 16.34 Type Test

typeTest
    : isOperator type
    ;

isOperator
    : IS NOT?
    ;

// 16.35 Type Cast

typeCast
    : asOperator type
    ;

asOperator
    : AS
    ;

// 17 Statements

statements
    : statement*
    ;

statement
    : label* nonLabelledStatement
    ;

nonLabelledStatement
    : block
    | localVariableDeclaration
    | forStatement
    | whileStatement
    | doStatement
    | switchStatement
    | ifStatement
    | rethrowStatement
    | tryStatement
    | breakStatement
    | continueStatement
    | returnStatement
    | yieldStatement
    | yieldEachStatement
    | expressionStatement
    | assertStatement
    | localFunctionDeclaration
    ;

// 17.2 Expression Satements
expressionStatement
    : expression? SEMI
    ;

// 17.3 Local Variable Declaration

localVariableDeclaration
    : initializedVariableDeclaration SEMI
    ;

// 17.4 Local Function Declaration

localFunctionDeclaration
    : functionSignature functionBody
    ;

// 17.5 If

ifStatement
    : IF LPAREN expression RPAREN statement ( ELSE statement)?
    ;

// 17.6 For

forStatement
    : AWAIT? FOR LPAREN forLoopParts RPAREN statement
    ;

forLoopParts
    : forInitializerStatement expression? SEMI expressionList?
    | declaredIdentifier IN expression
    | identifier IN expression
    ;

forInitializerStatement
    : localVariableDeclaration
    | expression? SEMI
    ;

// 17.7 While

whileStatement
    : WHILE LPAREN expression RPAREN statement
    ;

// 17.8 Do
doStatement
    : DO statement WHILE LPAREN expression RPAREN SEMI
    ;

// 17.9 Switch

switchStatement
    : SWITCH LPAREN expression RPAREN CURLY_OPEN switchCase* defaultCase? CURLY_CLOSE
    ;

switchCase
    : label* CASE expression COLON statements
    ;

defaultCase
    : label* DEFAULT COLON statements
    ;

// 17.10 Rethrow

rethrowStatement
    : RETHROW SEMI
    ;

// 17.11 Try

tryStatement
    : TRY block (onPart+ finallyPart? | finallyPart)
    ;

onPart
    : catchPart block
    | ON type catchPart? block
    ;

catchPart
    : CATCH LPAREN identifier (COMMA identifier)? RPAREN
    ;

finallyPart
    : FINALLY block
    ;

// 17.12 Return

returnStatement
    : RETURN expression? SEMI
    ;

// 17.13 Labels

label
    : identifier COLON
    ;

// 17.14 Break

breakStatement
    : BREAK identifier? SEMI
    ;

// 17.15 Continue

continueStatement
    : CONTINUE identifier? SEMI
    ;

// 17.16.1 Yield

yieldStatement
    : YIELD expression SEMI
    ;

// 17.16.2 Yield Each

yieldEachStatement
    : YIELD* expression SEMI
    ;

// 17.17 Assert

assertStatement
    : ASSERT LPAREN expression (COMMA stringLiteral)? RPAREN SEMI
//    : ASSERT LPAREN conditionalExpression RPAREN SEMI
    ;

// 19.1 Static Types

type
    : typeName typeArguments?
    | DYNAMIC
    ;

typeName
    : qualified
    ;

typeArguments
    : LANGLE typeList RANGLE
    ;

typeList
    : type (COMMA type)*
    ;

// 19.3.1 Type Alias

typeAlias
    : metadata TYPEDEF typeAliasBody
    ;

typeAliasBody
    : functionTypeAlias
    ;

functionTypeAlias
    : functionPrefix typeParameters? formalParameterList SEMI
    ;

functionPrefix
    : returnType? identifier
    ;
