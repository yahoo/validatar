/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */

grammar Grammar;

truthy
    :   TRUE
    |   FALSE
    ;

numeric
    :   w=WholeNumber     # wholeNumber
    |   d=DecimalNumber   # decimalNumber
    ;

base
    :   t=truthy          # truthValue
    |   n=numeric         # numericValue
    |   s=StringLiteral   # stringValue
    |   i=Identifier      # identifier
    ;

functionalExpression
    :   APPROX LEFTPAREN l=base COMMA r=base COMMA p=base RIGHTPAREN   # approxValue
    ;

baseExpression
    : b=base                                # baseValue
    | f=functionalExpression                # functionalValue
    | LEFTPAREN o=orExpression RIGHTPAREN   # parenthesizedValue
    ;

unaryExpression
    :   m=MINUS? b=baseExpression # negateValue
    |   n=NOT? b=baseExpression   # logicalNegateValue
    ;

multiplicativeExpression
    :   u=unaryExpression                                      # baseUnaryValue
    |   m=multiplicativeExpression TIMES u=unaryExpression     # multiplyValue
    |   m=multiplicativeExpression DIVIDE u=unaryExpression    # divideValue
    |   m=multiplicativeExpression MODULUS u=unaryExpression   # modValue
    ;

additiveExpression
    :   m=multiplicativeExpression                            # baseMultiplicativeValue
    |   a=additiveExpression PLUS m=multiplicativeExpression  # addValue
    |   a=additiveExpression MINUS m=multiplicativeExpression # subtractValue
    ;

relationalExpression
    :   a=additiveExpression                                       # baseAdditiveValue
    |   r=relationalExpression GREATER a=additiveExpression        # greaterValue
    |   r=relationalExpression LESS a=additiveExpression           # lessValue
    |   r=relationalExpression LESSEQUAL a=additiveExpression      # lessEqualValue
    |   r=relationalExpression GREATEREQUAL a=additiveExpression   # greaterEqualValue
    ;

equalityExpression
    :   r=relationalExpression                                 # baseRelativeValue
    |   e=equalityExpression EQUAL r=relationalExpression      # equalityValue
    |   e=equalityExpression NOTEQUAL r=relationalExpression   # notEqualityValue
    ;

andExpression
    :   e=equalityExpression                       # baseEqualityValue
    |   a=andExpression AND e=equalityExpression   # andValue
    ;

orExpression
    :   a=andExpression                    # baseAndValue
    |   o=orExpression OR a=andExpression  # orValue
    ;

statement
    :   o=orExpression                        # baseOrValue
    |   o=orExpression WHERE j=orExpression   # joinValue
    ;

DOUBLEQUOTE          : '"';
QUOTE                : '\'';
PERIOD               : '.';
COMMA                : ',';
LEFTPAREN            : '(';
RIGHTPAREN           : ')';
PLUS                 : '+';
MINUS                : '-';
TIMES                : '*';
DIVIDE               : '/';
MODULUS              : '%';
GREATER              : '>';
LESS                 : '<';
GREATEREQUAL         : '>=';
LESSEQUAL            : '<=';
NOTEQUAL             : '!=';
EQUAL                : '==';
NOT                  : '!';
AND                  : '&&';
OR                   : '||';
TRUE                 : 'true';
FALSE                : 'false';
WHERE                : 'where';
APPROX               : 'approx';

Whitespace
    :   [ \t]+
        -> skip
    ;

Newline
    :   [\r\n]
        -> skip
    ;

fragment
NoDoubleQuoteCharacter
    : ~["]
    ;

fragment
NoQuoteCharacter
    : ~[']
    ;

fragment
NonDigit
    :  [a-zA-Z_]
    ;

fragment
Digit
    :  [0-9]
    ;

fragment
IdentifierCharacter
    : NonDigit
    | Digit
    ;

WholeNumber
    :  Digit+
    ;

DecimalNumber
    :  Digit+ PERIOD Digit+
    ;

StringLiteral
    : DOUBLEQUOTE NoDoubleQuoteCharacter* DOUBLEQUOTE
    | QUOTE NoQuoteCharacter* QUOTE
    ;

Identifier
    :  IdentifierCharacter+ PERIOD ? IdentifierCharacter+
    ;
