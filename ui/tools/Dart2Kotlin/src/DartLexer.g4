lexer grammar DartLexer;

NUMBER
    : DIGIT+ ('.' DIGIT+)? EXPONENT?
    | '.' DIGIT+ EXPONENT?
    ;

fragment
EXPONENT
    : ('e' | 'E') ('+' | '-')? DIGIT+
    ;

HEX_NUMBER
    : '0x' HEX_DIGIT+
    | '0X' HEX_DIGIT+
    ;

fragment
HEX_DIGIT
    : 'a'..'f'
    | 'A'..'F'
    | DIGIT
    ;

// 16.5 Strings

SINGLE_LINE_NO_ESCAPE_STRING
    : RSQ (~( '\'' | '\r' | '\n'))*? SQ
    | RDQ (~( '"' | '\r' | '\n'))*? DQ
    ;

SINGLE_LINE_SQ_STRING
    : SQ -> pushMode(SingleQuoteString)
    ;

SINGLE_LINE_DQ_STRING
    : DQ -> pushMode(DoubleQuoteString)
    ;

MULTI_LINE_NO_ESCAPE_STRING
    : RTDQ .*? TDQ
    | RTSQ .*? TSQ
    ;

MULTI_LINE_SQ_STRING
    : TSQ -> pushMode(SingleQuoteMultilineString)
    ;

MULTI_LINE_DQ_STRING
    : TDQ -> pushMode(DoubleQuoteMultilineString)
    ;

fragment
ESCAPE_SEQUENCE
    : '\\n'
    | '\\r'
    | '\\f'
    | '\\b'
    | '\\t'
    | '\\v'
    | '\\x' HEX_DIGIT HEX_DIGIT
    | '\\u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    | '\\u{' HEX_DIGIT_SEQUENCE '}'
    ;

fragment
HEX_DIGIT_SEQUENCE
    : HEX_DIGIT HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT? HEX_DIGIT?
    ;

// Tokens
SCRIPT_START
    : '#!' -> pushMode(ScriptMode)
    ;

TSQ: '\'\'\'';
TDQ: '"""';
RTSQ: 'r' TSQ;
RTDQ: 'r' TDQ;
SQ: '\'';
DQ: '"';
RSQ: 'r\'';
RDQ: 'r"';
DOT: '.';
COMMA: ',';
SEMI: ';';
LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';
CURLY_OPEN: '{';
CURLY_CLOSE: '}';
HASH: '#';
MUL_EQ: '*=';
DIV_EQ: '/=';
TRUNC_EQ: '~/=';
MOD_EQ: '%=';
PLU_EQ: '+=';
MIN_EQ: '-=';
LSH_EQ: '<<=';
RSH_EQ: '>>=';
AND_EQ: '&=';
XOR_EQ: '^=';
OR_EQ: '|=';
NUL_EQ: '??=';
QUEST: '?';
COLON: ':';
IFNULL: '??';
LOG_OR: '||';
LOG_AND: '&&';
CMP_EQ: '==';
CMP_NEQ: '!=';
CMP_GE: '>=';
RANGLE: '>';
CMP_LE: '<=';
LANGLE: '<';
BIT_OR: '|';
BIT_XOR: '^';
BIT_AND: '&';
BIT_NOT: '~';
SHL: '<<';
//SHR: '>>';
MUL: '*';
DIV: '/';
MOD: '%';
TRUNC: '~/';
PLUS: '+';
MINUS: '-';
NOT: '!';
ASSIGN: '=';
INC: '++';
DEC: '--';
FAT_ARROW: '=>';
AT_SIGN: '@';
DOT_DOT: '..';
QUESTION_DOT: '?.';

Operators
    : SCRIPT_START
    | DOT
    | COMMA
    | SEMI
    | LPAREN
    | RPAREN
    | LBRACKET
    | RBRACKET
    | CURLY_OPEN
    | CURLY_CLOSE
    | HASH
    | MUL_EQ
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
    | QUEST
    | COLON
    | IFNULL
    | LOG_OR
    | LOG_AND
    | CMP_EQ
    | CMP_NEQ
    | CMP_GE
    | RANGLE
    | CMP_LE
    | LANGLE
    | BIT_OR
    | BIT_XOR
    | BIT_AND
    | BIT_NOT
    | SHL
//    | SHR
    | MUL
    | DIV
    | MOD
    | TRUNC
    | PLUS
    | MINUS
    | NOT
    | ASSIGN
    | INC
    | DEC
    | FAT_ARROW
    | AT_SIGN
    | DOT_DOT
    | QUESTION_DOT
    ;

// KEYWORDS

ABSTRACT
    : 'abstract'
    ;
AS
    : 'as'
    ;
ASSERT
    : 'assert'
    ;
ASYNC
    : 'async'
    ;
ASYNC_STAR
    : 'async*'
    ;
AWAIT
    : 'await'
    ;
BREAK
    : 'break'
    ;
CASE
    : 'case'
    ;
CATCH
    : 'catch'
    ;
CLASS
    : 'class'
    ;
CONST
    : 'const'
    ;
CONTINUE
    : 'continue'
    ;
COVARIANT
    : 'covariant'
    ;
DEFERRED
    : 'deferred'
    ;
DEFAULT
    : 'default'
    ;
DO
    : 'do'
    ;
DYNAMIC
    : 'dynamic'
    ;
ELSE
    : 'else'
    ;
ENUM
    : 'enum'
    ;
EXPORT
    : 'export'
    ;
EXTENDS
    : 'extends'
    ;
EXTERNAL
    : 'external'
    ;
FACTORY
    : 'factory'
    ;
FALSE
    : 'false'
    ;
FINAL
    : 'final'
    ;
FINALLY
    : 'finally'
    ;
FOR
    : 'for'
    ;
GET
    : 'get'
    ;
HIDE
    : 'hide'
    ;
IF
    : 'if'
    ;
IMPLEMENTS
    : 'implements'
    ;
IMPORT
    : 'import'
    ;
IN
    : 'in'
    ;
IS
    : 'is'
    ;
LIBRARY
    : 'library'
    ;
NEW
    : 'new'
    ;
NULL
    : 'null'
    ;
OF
    : 'of'
    ;
ON
    : 'on'
    ;
OPERATOR
    : 'operator'
    ;
PART
    : 'part'
    ;
RETHROW
    : 'rethrow'
    ;
RETURN
    : 'return'
    ;
SET
    : 'set'
    ;
SHOW
    : 'show'
    ;
STATIC
    : 'static'
    ;
SUPER
    : 'super'
    ;
SWITCH
    : 'switch'
    ;
SYNC_STAR
    : 'sync*'
    ;
THIS
    : 'this'
    ;
THROW
    : 'throw'
    ;
TRUE
    : 'true'
    ;
TRY
    : 'try'
    ;
TYPEDEF
    : 'typedef'
    ;
VAR
    : 'var'
    ;
VOID
    : 'void'
    ;
WHILE
    : 'while'
    ;
WITH
    : 'with'
    ;
YIELD
    : 'yield'
    ;

Keywords
    : ABSTRACT
    | AS
    | ASSERT
    | ASYNC
    | ASYNC_STAR
    | AWAIT
    | BREAK
    | CASE
    | CATCH
    | CLASS
    | CONST
    | CONTINUE
    | COVARIANT
    | DEFERRED
    | DEFAULT
    | DO
    | DYNAMIC
    | ELSE
    | ENUM
    | EXPORT
    | EXTENDS
    | EXTERNAL
    | FACTORY
    | FALSE
    | FINAL
    | FINALLY
    | FOR
    | GET
    | HIDE
    | IF
    | IMPLEMENTS
    | IMPORT
    | IN
    | IS
    | LIBRARY
    | NEW
    | NULL
    | OF
    | ON
    | OPERATOR
    | PART
    | RETHROW
    | RETURN
    | SET
    | SHOW
    | STATIC
    | SUPER
    | SWITCH
    | SYNC_STAR
    | THIS
    | THROW
    | TRUE
    | TRY
    | TYPEDEF
    | VAR
    | VOID
    | WHILE
    | WITH
    | YIELD
    ;

// 16.33 Identifier Reference

fragment
IDENTIFIER_NO_DOLLAR
    : IDENTIFIER_START_NO_DOLLAR IDENTIFIER_PART_NO_DOLLAR*
    ;

IDENTIFIER
    : IDENTIFIER_START IDENTIFIER_PART*
    ;

fragment
IDENTIFIER_START
    : IDENTIFIER_START_NO_DOLLAR
    | '$'
    ;

fragment
IDENTIFIER_START_NO_DOLLAR
    : LETTER
    | '_'
    ;

fragment
IDENTIFIER_PART_NO_DOLLAR
    : IDENTIFIER_START_NO_DOLLAR
    | DIGIT
    ;

fragment
IDENTIFIER_PART
    : IDENTIFIER_START
    | DIGIT
    ;

// 20.1.1 Reserved Words

fragment
LETTER
    : 'a' .. 'z'
    | 'A' ..'Z'
    ;

fragment
DIGIT
    : '0' .. '9'
    ;

fragment
NEWLINE
    : '\n'
    | '\r'
    ;

WHITESPACE
    : ('\t' | ' ' | NEWLINE)+ -> channel(1)
    ;

// 20.1.2 Comments

COMMENT
    :   '/*' .*? '*/'  -> channel(2)
    ;

// Single-line comments
SL_COMMENT
    :   '//'
        (~('\n'|'\r'))* ('\n'|'\r'('\n')?)
        -> channel(2)
    ;

mode SingleQuoteString;

SQS_END
    : SQ -> popMode
    ;

SQS_ESCAPE_SEQUENCE
    : ESCAPE_SEQUENCE
    | '\\\''
    ;

SQS_EXPRESSION_IDENTIFIER
    : '$' IDENTIFIER_NO_DOLLAR
    ;

SQS_EXPRESSION_START
    : '${' -> pushMode(StringExpression)
    ;

SQS_TEXT
    : ~('\\' | '\'' | '$')+
    ;

mode DoubleQuoteString;

DQS_END
    : DQ -> popMode
    ;

DQS_ESCAPE_SEQUENCE
    : ESCAPE_SEQUENCE
    | '\\"'
    ;

DQS_EXPRESSION_IDENTIFIER
    : '$' IDENTIFIER_NO_DOLLAR
    ;

DQS_EXPRESSION_START
    : '${' -> pushMode(StringExpression)
    ;

DQS_TEXT
    : ~('\\' | '"' | '$')+
    ;

mode DoubleQuoteMultilineString;

DQMLS_END
    : TDQ -> popMode
    ;

DQMLS_ESCAPE_SEQUENCE
    : ESCAPE_SEQUENCE
    | '\\"'
    ;

DQMLS_EXPRESSION_IDENTIFIER
    : '$' IDENTIFIER_NO_DOLLAR
    ;

DQMLS_EXPRESSION_START
    : '${' -> pushMode(StringExpression)
    ;

DQMLS_TEXT
    : ~('\\' | '"' | '$')+
    | .
    ;

mode SingleQuoteMultilineString;

SQMLS_END
    : TSQ -> popMode
    ;

SQMLS_ESCAPE_SEQUENCE
    : ESCAPE_SEQUENCE
    | '\\\''
    ;

SQMLS_EXPRESSION_IDENTIFIER
    : '$' IDENTIFIER_NO_DOLLAR
    ;

SQMLS_EXPRESSION_START
    : '${' -> pushMode(StringExpression)
    ;

SQMLS_TEXT
    : ~('\\' | '\'' | '$')+
    | .
    ;

mode StringExpression;

SE_Keywords: Keywords;

SE_WHITESPACE: WHITESPACE;

SE_COMMENT: COMMENT;

SE_SINGLE_LINE_SQ_STRING: SINGLE_LINE_SQ_STRING -> pushMode(SingleQuoteString);

SE_SINGLE_LINE_DQ_STRING: SINGLE_LINE_DQ_STRING -> pushMode(DoubleQuoteString);

SE_SINGLE_LINE_NO_ESCAPE_STRING: SINGLE_LINE_NO_ESCAPE_STRING;

SE_IDENTIFIER: IDENTIFIER;

SE_NUMBER: NUMBER;

SE_HEX_NUMBER: HEX_NUMBER;

SE_END
    : '}' -> popMode
    ;

SE_Operators: Operators;

mode ScriptMode;
NL
    : NEWLINE -> popMode;