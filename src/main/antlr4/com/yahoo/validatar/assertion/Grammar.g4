/*
 * Copyright 2014-2015 Yahoo! Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

grammar Grammar;

@parser::header {
import java.util.Map;
import java.util.HashMap;
}

@parser::members {
    private Map<String, String> row = null;
    private Map<String, String> lookedUpValues = new HashMap<String, String>();;

    public void setCurrentRow(Map<String, String> row) {
        this.row = row;
    }

    public Map<String, String> getLookedUpValues() {
        return lookedUpValues;
    }

    private String getColumnValue(String name) {
        String result = row.get(name);
        lookedUpValues.put(name, result);
        return result;
    }

    private String stripQuotes(String literal) {
        return literal.substring(1, literal.length() - 1);
    }
}

functionalExpression returns [String value]
    :   APPROX LEFTPAREN l=base COMMA r=base COMMA p=Number RIGHTPAREN
                  {
                      Double percent = Double.parseDouble($p.text);
                      if (percent > 1 || percent < 0) {
                          throw new RuntimeException("Expected percentage for the function to be between 0 and 1. Got " + percent);
                      }
                      Double left = Double.parseDouble($l.value);
                      Double right = Double.parseDouble($r.value);
                      if (left > (right * (1 + percent))) {
                          $value = String.valueOf(false);
                      } else if (left < (right * (1 - percent))) {
                          $value = String.valueOf(false);
                      } else {
                          $value = String.valueOf(true);
                      }
                  }
    ;

base returns [String value]
    :   i=Identifier                               {$value = getColumnValue($i.text);}
    |   s=StringLiteral                            {$value = stripQuotes($s.text);}
    |   n=Number                                   {$value = $n.text;}
    |   LEFTPAREN o=orExpression RIGHTPAREN        {$value = String.valueOf($o.value);}
    |   f=functionalExpression                     {$value = String.valueOf($f.value);}
    ;

unaryExpression returns [String value]
    :   m=MINUS? b=base
                        {
                            if ($m == null) {
                                $value = $b.value;
                            } else {
                                $value = String.valueOf(-1.0 * Double.parseDouble($b.value));
                            };
                        }
    |   n=NOT? b=base
                        {
                            if ($n == null) {
                                $value = $b.value;
                            } else {
                                $value = String.valueOf(!Boolean.parseBoolean($b.value));
                            };
                        }
    ;

multiplicativeExpression returns [String value]
    :   u=unaryExpression                                   {$value = $u.value;}
    |   m=multiplicativeExpression TIMES u=unaryExpression  {$value = String.valueOf(Double.parseDouble($m.value) * Double.parseDouble($u.value));}
    |   m=multiplicativeExpression DIVIDE u=unaryExpression {$value = String.valueOf(Double.parseDouble($m.value) / Double.parseDouble($u.value));}
    ;

additiveExpression returns [String value]
    :   m=multiplicativeExpression                              {$value = $m.value;}
    |   a=additiveExpression PLUS m=multiplicativeExpression    {$value = String.valueOf(Double.parseDouble($a.value) + Double.parseDouble($m.value));}
    |   a=additiveExpression MINUS m=multiplicativeExpression   {$value = String.valueOf(Double.parseDouble($a.value) - Double.parseDouble($m.value));}
    ;

relationalExpression returns [String value]
    :   a=additiveExpression                                     {$value = $a.value;}
    |   r=relationalExpression GREATER a=additiveExpression      {$value = String.valueOf(Double.parseDouble($r.value) >  Double.parseDouble($a.value));}
    |   r=relationalExpression LESS a=additiveExpression         {$value = String.valueOf(Double.parseDouble($r.value) <  Double.parseDouble($a.value));}
    |   r=relationalExpression LESSEQUAL a=additiveExpression    {$value = String.valueOf(Double.parseDouble($r.value) <= Double.parseDouble($a.value));}
    |   r=relationalExpression GREATEREQUAL a=additiveExpression {$value = String.valueOf(Double.parseDouble($r.value) >= Double.parseDouble($a.value));}
    ;

equalityExpression returns [String value]
    :   r=relationalExpression                               {$value = $r.value;}
    |   e=equalityExpression EQUAL r=relationalExpression    {$value = String.valueOf($e.value.equals($r.value));}
    |   e=equalityExpression NOTEQUAL r=relationalExpression {$value = String.valueOf(!$e.value.equals($r.value));}
    ;

andExpression returns [String value]
    :   e=equalityExpression                     {$value = $e.value;}
    |   a=andExpression AND e=equalityExpression {$value = String.valueOf(Boolean.valueOf($a.value) && Boolean.valueOf($e.value));}
    ;

orExpression returns [String value]
    :   a=andExpression                   {$value = $a.value;}
    |   o=orExpression OR a=andExpression {$value = String.valueOf(Boolean.valueOf($o.value) || Boolean.valueOf($a.value));}
    ;

expression returns [Boolean value]
    :   o=orExpression                    {$value = Boolean.valueOf($o.value);}
    ;

QUOTE                : '"';
PERIOD               : '.';
COMMA                : ',';
LEFTPAREN            : '(';
RIGHTPAREN           : ')';
PLUS                 : '+';
MINUS                : '-';
TIMES                : '*';
DIVIDE               : '/';
GREATER              : '>';
LESS                 : '<';
GREATEREQUAL         : '>=';
LESSEQUAL            : '<=';
NOTEQUAL             : '!=';
EQUAL                : '==';
NOT                  : '!';
AND                  : '&&';
OR                   : '||';

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
NonDigit
    :  [a-zA-Z_]
    ;

fragment
Digit
    :  [0-9]
    ;

Number
    :  Digit+ (PERIOD Digit+)?
    ;

WhitespaceCharacter
    : [ \t\n\r]
    ;

Character
    : (NonDigit | Digit)
    ;

StringLiteral
    :   QUOTE (Character | WhitespaceCharacter)* QUOTE
    ;

Identifier
    :  (Character)+ PERIOD ? (Character)+
    ;

