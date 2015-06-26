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
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.common.TypeSystem.Type;
import static com.yahoo.validatar.common.TypeSystem.*;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
}

@parser::members {
    private Map<String, TypedObject> row = null;
    private Map<String, String> lookedUpValues = new HashMap<>();;

    public void setCurrentRow(Map<String, TypedObject> row) {
        this.row = row;
    }

    public Map<String, String> getLookedUpValues() {
        return lookedUpValues;
    }

    private TypedObject getColumnValue(String name) {
        TypedObject result = row.get(name);
        if (result == null) {
            throw new RuntimeException("Unable to find value for column: " + name + " in results");
        }
        lookedUpValues.put(name, result.data.toString());
        return result;
    }

    private String stripQuotes(String literal) {
        return literal.substring(1, literal.length() - 1);
    }

    private TypedObject parseCharacter(String text) {
        return new TypedObject(stripQuotes(text).charAt(0), Type.CHARACTER);
    }

    private TypedObject parseString(String text) {
        return new TypedObject(stripQuotes(text), Type.STRING);
    }

    private TypedObject parseWholeNumber(String text) {
        TypedObject object;
        try {
            object = new TypedObject(Long.parseLong(text), Type.LONG);
        } catch (NumberFormatException nfe) {
            object = new TypedObject(new BigDecimal(text), Type.DECIMAL);
        }
        return object;
    }

    private TypedObject parseDecimalNumber(String text) {
        TypedObject object;
        try {
            object = new TypedObject(Double.parseDouble(text), Type.DOUBLE);
        } catch (NumberFormatException nfe) {
            object = new TypedObject(new BigDecimal(text), Type.DECIMAL);
        }
        return object;
    }

    private TypedObject approx(TypedObject first, TypedObject second, TypedObject percent) {
        if ((Boolean) isGreaterThan(percent, asTypedObject(1L)).data || (Boolean) isLessThan(percent, asTypedObject(0L)).data) {
            throw new RuntimeException("Expected percentage for approx to be between 0 and 1. Got " + percent.data);
        }

        TypedObject max = multiply(second, add(asTypedObject(1L), percent));
        if ((Boolean) isGreaterThan(first, max).data) {
            return asTypedObject(false);
        }

        TypedObject min = multiply(second, subtract(asTypedObject(1L), percent));
        if ((Boolean) isLessThan(first, min).data) {
            return asTypedObject(false);
        }
        return asTypedObject(true);
    }
}

functionalExpression returns [TypedObject value]
    :   APPROX LEFTPAREN l=base COMMA r=base COMMA p=numeric RIGHTPAREN {$value = approx($l.value, $r.value, $p.value);}
    ;

base returns [TypedObject value]
    :   i=Identifier                               {$value = getColumnValue($i.text);}
    |   c=CharacterLiteral                         {$value = parseCharacter($c.text);}
    |   s=StringLiteral                            {$value = parseString($s.text);}
    |   n=numeric                                  {$value = $n.value;}
    |   LEFTPAREN o=orExpression RIGHTPAREN        {$value = $o.value;}
    |   f=functionalExpression                     {$value = $f.value;}
    ;

numeric returns [TypedObject value]
    :   w=WholeNumber                              {$value = parseWholeNumber($w.text);}
    |   d=DecimalNumber                            {$value = parseDecimalNumber($d.text);}
    ;

unaryExpression returns [TypedObject value]
    :   m=MINUS? b=base
                        {
                            if ($m == null) {
                                $value = $b.value;
                            } else {
                                $value = negate($b.value);
                            };
                        }
    |   n=NOT? b=base
                        {
                            if ($n == null) {
                                $value = $b.value;
                            } else {
                                $value = logicalNegate($b.value);
                            };
                        }
    ;

multiplicativeExpression returns [TypedObject value]
    :   u=unaryExpression                                   {$value = $u.value;}
    |   m=multiplicativeExpression TIMES u=unaryExpression  {$value = multiply($m.value, $u.value);}
    |   m=multiplicativeExpression DIVIDE u=unaryExpression {$value = divide($m.value, $u.value);}
    ;

additiveExpression returns [TypedObject value]
    :   m=multiplicativeExpression                              {$value = $m.value;}
    |   a=additiveExpression PLUS m=multiplicativeExpression    {$value = add($a.value, $m.value);}
    |   a=additiveExpression MINUS m=multiplicativeExpression   {$value = subtract($a.value, $m.value);}
    ;

relationalExpression returns [TypedObject value]
    :   a=additiveExpression                                     {$value = $a.value;}
    |   r=relationalExpression GREATER a=additiveExpression      {$value = isGreaterThan($r.value, $a.value);}
    |   r=relationalExpression LESS a=additiveExpression         {$value = isLessThan($r.value, $a.value);}
    |   r=relationalExpression LESSEQUAL a=additiveExpression    {$value = isLessThanOrEqual($r.value, $a.value);}
    |   r=relationalExpression GREATEREQUAL a=additiveExpression {$value = isGreaterThanOrEqual($r.value, $a.value);}
    ;

equalityExpression returns [TypedObject value]
    :   r=relationalExpression                               {$value = $r.value;}
    |   e=equalityExpression EQUAL r=relationalExpression    {$value = isEqualTo($e.value, $r.value);}
    |   e=equalityExpression NOTEQUAL r=relationalExpression {$value = isNotEqualTo($e.value, $r.value);}
    ;

andExpression returns [TypedObject value]
    :   e=equalityExpression                     {$value = $e.value;}
    |   a=andExpression AND e=equalityExpression {$value = logicalAnd($a.value, $e.value);}
    ;

orExpression returns [TypedObject value]
    :   a=andExpression                   {$value = $a.value;}
    |   o=orExpression OR a=andExpression {$value = logicalOr($o.value, $a.value);}
    ;

expression returns [Boolean value]
    :   o=orExpression                    {$value = (Boolean) $o.value.data;}
    ;

SINGLEQUOTE          : '\'';
DOUBLEQUOTE          : '"';
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

WholeNumber
    :  Digit+
    ;

DecimalNumber
    :  Digit+ PERIOD Digit+
    ;

WhitespaceCharacter
    : [ \t\n\r]
    ;

Character
    : (NonDigit | Digit)
    ;

CharacterLiteral
    : SINGLEQUOTE (Character | WhitespaceCharacter) SINGLEQUOTE
    ;

StringLiteral
    :   DOUBLEQUOTE (Character | WhitespaceCharacter)* DOUBLEQUOTE
    ;

Identifier
    :  (Character)+ PERIOD ? (Character)+
    ;

