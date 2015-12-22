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

package com.yahoo.validatar.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static com.yahoo.validatar.common.TypeSystem.Type;
import static com.yahoo.validatar.common.TypeSystem.add;
import static com.yahoo.validatar.common.TypeSystem.asTypedObject;
import static com.yahoo.validatar.common.TypeSystem.divide;
import static com.yahoo.validatar.common.TypeSystem.isEqualTo;
import static com.yahoo.validatar.common.TypeSystem.isGreaterThan;
import static com.yahoo.validatar.common.TypeSystem.isGreaterThanOrEqual;
import static com.yahoo.validatar.common.TypeSystem.isLessThan;
import static com.yahoo.validatar.common.TypeSystem.isLessThanOrEqual;
import static com.yahoo.validatar.common.TypeSystem.isNotEqualTo;
import static com.yahoo.validatar.common.TypeSystem.logicalAnd;
import static com.yahoo.validatar.common.TypeSystem.logicalNegate;
import static com.yahoo.validatar.common.TypeSystem.logicalOr;
import static com.yahoo.validatar.common.TypeSystem.modulus;
import static com.yahoo.validatar.common.TypeSystem.multiply;
import static com.yahoo.validatar.common.TypeSystem.subtract;

public class TypeSystemTest {
    private TypeSystem system = new TypeSystem();

    private class CustomOperations implements TypeSystem.Operations {
    }

    public static final double EPSILON = 0.00001;

    private boolean boolify(TypedObject type) {
        return (Boolean) type.data;
    }

    private boolean equals(Double first, Double second) {
        return Math.abs(first - second) < EPSILON;
    }

    private boolean equals(Timestamp first, Timestamp second) {
        return first.getTime() == second.getTime();
    }

    @Test
    public void testTypedObjectConversions() {
        Boolean booleanValue = true;
        Assert.assertEquals(asTypedObject(booleanValue).data, Boolean.valueOf(true));
        Assert.assertEquals(asTypedObject(booleanValue).type, Type.BOOLEAN);
        String stringValue = "foo";
        Assert.assertEquals((String) asTypedObject(stringValue).data, "foo");
        Assert.assertEquals(asTypedObject(stringValue).type, Type.STRING);

        Long longValue = 131412300000000000L;
        Assert.assertEquals(asTypedObject(longValue).data, Long.valueOf(131412300000000000L));
        Assert.assertEquals(asTypedObject(longValue).type, Type.LONG);

        Double doubleValue = 235242523.04;
        Assert.assertEquals(asTypedObject(doubleValue).data, Double.valueOf(235242523.04));
        Assert.assertEquals(asTypedObject(doubleValue).type, Type.DOUBLE);

        BigDecimal decimalValue = new BigDecimal("234235234234223425151231231151231.123141231231411231231");
        Assert.assertEquals(asTypedObject(decimalValue).data, new BigDecimal("234235234234223425151231231151231.123141231231411231231"));
        Assert.assertEquals(asTypedObject(decimalValue).type, Type.DECIMAL);

        long timeNow = System.currentTimeMillis();
        Timestamp timestampValue = new Timestamp(timeNow);
        Assert.assertEquals(asTypedObject(timestampValue).data, new Timestamp(timeNow));
        Assert.assertEquals(asTypedObject(timestampValue).type, Type.TIMESTAMP);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testFirstOperandNull() {
        TypedObject stringSample = asTypedObject("foo");
        TypeSystem.compare(null, stringSample);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testSecondOperandNull() {
        TypedObject stringSample = asTypedObject("foo");
        TypeSystem.compare(stringSample, null);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testBothOperandsNull() {
        TypeSystem.compare(null, null);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testNotUnifiableTypes() {
        TypedObject booleanSample = asTypedObject(false);
        TypedObject longSample = asTypedObject(123L);
        TypeSystem.compare(longSample, booleanSample);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testNonArithmeticOperableTypes() {
        TypedObject booleanSample = asTypedObject(false);
        TypeSystem.add(booleanSample, booleanSample);
    }

    /***
     * STRING
     ***/

    @Test
    public void testCastingToString() {
        TypedObject stringedObject = asTypedObject("");

        stringedObject.data = "123";
        TypedObject longSample = asTypedObject(123L);
        Assert.assertTrue(boolify(isEqualTo(stringedObject, longSample)));

        stringedObject.data = "1.23";
        TypedObject doubleSample = asTypedObject(1.23);
        Assert.assertTrue(boolify(isEqualTo(stringedObject, doubleSample)));

        stringedObject.data = "12312312.2312412431321314123123123124123123123";
        TypedObject decimalSample = asTypedObject(new BigDecimal("12312312.2312412431321314123123123124123123123"));
        Assert.assertTrue(boolify(isEqualTo(stringedObject, decimalSample)));

        stringedObject.data = "false";
        TypedObject booleanSample = asTypedObject(false);
        Assert.assertTrue(boolify(isEqualTo(stringedObject, booleanSample)));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingTimestampToString() {
        TypedObject stringedObject = asTypedObject("2015-06-28 21:57:56.0");
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(stringedObject, timestampSample);
    }

    @Test
    public void testStringTypeComparisons() {
        TypedObject stringSample = asTypedObject("sample");

        Assert.assertTrue(boolify(isEqualTo(stringSample, stringSample)));
        Assert.assertFalse(boolify(isNotEqualTo(stringSample, stringSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(stringSample, stringSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(stringSample, stringSample)));

        TypedObject anotherSample = asTypedObject("foo");

        Assert.assertFalse(boolify(isEqualTo(stringSample, anotherSample)));
        Assert.assertTrue(boolify(isNotEqualTo(stringSample, anotherSample)));
        Assert.assertFalse(boolify(isLessThan(stringSample, anotherSample)));
        Assert.assertTrue(boolify(isGreaterThan(stringSample, anotherSample)));
        Assert.assertFalse(boolify(isLessThanOrEqual(stringSample, anotherSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(stringSample, anotherSample)));
    }

    @Test
    public void testStringArithmetic() {
        Assert.assertEquals((String) (add(asTypedObject("sample"), asTypedObject("foo")).data), "samplefoo");
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testStringSubtraction() {
        subtract(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testStringMultiplication() {
        multiply(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testStringDivision() {
        divide(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testStringModulus() {
        modulus(asTypedObject("sample"), asTypedObject("foo"));
    }

    /***
     * LONG
     ***/

    @Test
    public void testCastingToLong() {
        TypedObject longedObject = asTypedObject(0L);

        longedObject.data = 123235L;
        TypedObject stringSample = asTypedObject("123235");
        Assert.assertTrue(boolify(isEqualTo(longedObject, stringSample)));

        longedObject.data = 1435553876000L;
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        Assert.assertTrue(boolify(isEqualTo(longedObject, timestampSample)));
    }

    @Test
    public void testFailCastingDoubleToLongButCastOtherWay() {
        TypedObject longedObject = asTypedObject(1L);
        TypedObject doubleSample = asTypedObject(1.0);
        Assert.assertTrue(boolify(isEqualTo(longedObject, doubleSample)));
    }

    @Test
    public void testFailCastingDecimalToLongButCastOtherWay() {
        TypedObject longedObject = asTypedObject(0L);
        TypedObject decimalSample = asTypedObject(new BigDecimal("12.23"));
        Assert.assertFalse(boolify(isEqualTo(longedObject, decimalSample)));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingBooleanToLong() {
        TypedObject longedObject = asTypedObject(0L);
        TypedObject booleanSample = asTypedObject(false);
        isEqualTo(longedObject, booleanSample);
    }

    @Test
    public void testLongTypeComparisons() {
        TypedObject longSample = asTypedObject(42L);

        Assert.assertTrue(boolify(isEqualTo(longSample, longSample)));
        Assert.assertFalse(boolify(isNotEqualTo(longSample, longSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(longSample, longSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(longSample, longSample)));

        TypedObject anotherSample = asTypedObject(51L);

        Assert.assertFalse(boolify(isEqualTo(longSample, anotherSample)));
        Assert.assertTrue(boolify(isNotEqualTo(longSample, anotherSample)));
        Assert.assertTrue(boolify(isLessThan(longSample, anotherSample)));
        Assert.assertFalse(boolify(isGreaterThan(longSample, anotherSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(longSample, anotherSample)));
        Assert.assertFalse(boolify(isGreaterThanOrEqual(longSample, anotherSample)));
    }

    @Test
    public void testLongArithmetic() {
        Assert.assertEquals(add(asTypedObject(14L), asTypedObject(4L)).data, Long.valueOf(18L));
        Assert.assertEquals(subtract(asTypedObject(4L), asTypedObject(14L)).data, Long.valueOf(-10L));
        Assert.assertEquals(multiply(asTypedObject(14L), asTypedObject(3L)).data, Long.valueOf(42L));
        Assert.assertEquals(divide(asTypedObject(14L), asTypedObject(4L)).data, Long.valueOf(3L));
        Assert.assertEquals(modulus(asTypedObject(14L), asTypedObject(4L)).data, Long.valueOf(2L));
    }

    /***
     * DOUBLE
     ***/

    @Test
    public void testCastingToDouble() {
        TypedObject doubledObject = new TypedObject(0.0, Type.DOUBLE);

        doubledObject.data = 123.0;
        TypedObject longSample = asTypedObject(123L);
        Assert.assertTrue(boolify(isEqualTo(doubledObject, longSample)));

        doubledObject.data = 3.14159265;
        TypedObject stringSample = asTypedObject("3.14159265");
        Assert.assertTrue(boolify(isEqualTo(doubledObject, stringSample)));
    }

    @Test
    public void testFailCastingDecimalToDoubleButCastOtherWay() {
        TypedObject doubledObject = asTypedObject(12.23);
        TypedObject decimalSample = asTypedObject(new BigDecimal("12.23"));
        Assert.assertTrue(boolify(isEqualTo(doubledObject, decimalSample)));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingTimestampToDouble() {
        TypedObject doubledObject = asTypedObject(12.23);
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(doubledObject, timestampSample);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingBooleanToDouble() {
        TypedObject doubledObject = asTypedObject(12.23);
        TypedObject booleanSample = asTypedObject(false);
        isEqualTo(doubledObject, booleanSample);
    }

    @Test
    public void testDoubleTypeComparisons() {
        TypedObject doubleSample = asTypedObject(42.4242);

        Assert.assertTrue(boolify(isEqualTo(doubleSample, doubleSample)));
        Assert.assertFalse(boolify(isNotEqualTo(doubleSample, doubleSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(doubleSample, doubleSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(doubleSample, doubleSample)));

        TypedObject anotherSample = asTypedObject(3.14159265);

        Assert.assertFalse(boolify(isEqualTo(doubleSample, anotherSample)));
        Assert.assertTrue(boolify(isNotEqualTo(doubleSample, anotherSample)));
        Assert.assertFalse(boolify(isLessThan(doubleSample, anotherSample)));
        Assert.assertTrue(boolify(isGreaterThan(doubleSample, anotherSample)));
        Assert.assertFalse(boolify(isLessThanOrEqual(doubleSample, anotherSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(doubleSample, anotherSample)));
    }

    @Test
    public void testDoubleArithmetic() {
        Assert.assertTrue(equals((Double) add(asTypedObject(14.25), asTypedObject(4.50)).data, Double.valueOf(18.75)));
        Assert.assertTrue(equals((Double) subtract(asTypedObject(14.34), asTypedObject(14.0)).data, Double.valueOf(0.34)));
        Assert.assertTrue(equals((Double) multiply(asTypedObject(4.25), asTypedObject(4.0)).data, Double.valueOf(17.0)));
        Assert.assertTrue(equals((Double) divide(asTypedObject(14.0), asTypedObject(4.0)).data, Double.valueOf(3.5)));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testDoubleModulus() {
        modulus(asTypedObject(14.0), asTypedObject(4.0));
    }

    /***
     * DECIMAL
     ***/

    @Test
    public void testCastingToDecimal() {
        TypedObject decimaledObject = asTypedObject(new BigDecimal("0"));

        decimaledObject.data = new BigDecimal("0.01");
        TypedObject stringSample = asTypedObject("0.01");
        Assert.assertTrue(boolify(isEqualTo(decimaledObject, stringSample)));

        decimaledObject.data = new BigDecimal("123");
        TypedObject longSample = asTypedObject(123L);
        Assert.assertTrue(boolify(isEqualTo(decimaledObject, longSample)));

        decimaledObject.data = new BigDecimal("1.23");
        TypedObject doubleSample = asTypedObject(1.23);
        Assert.assertTrue(boolify(isEqualTo(decimaledObject, doubleSample)));

        decimaledObject.data = new BigDecimal("1435553876000");
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(decimaledObject, timestampSample);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingBooleanToDecimal() {
        TypedObject decimaledObject = asTypedObject(new BigDecimal("0.1"));
        TypedObject booleanSample = asTypedObject(false);
        isEqualTo(decimaledObject, booleanSample);
    }

    @Test
    public void testDecimalTypeComparisons() {
        TypedObject decimalSample = asTypedObject(new BigDecimal("12345678912345.123456789123456789123456789"));

        Assert.assertTrue(boolify(isEqualTo(decimalSample, decimalSample)));
        Assert.assertFalse(boolify(isNotEqualTo(decimalSample, decimalSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(decimalSample, decimalSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(decimalSample, decimalSample)));

        TypedObject anotherSample = asTypedObject("3.1415926535897932384626433832795029");

        Assert.assertFalse(boolify(isEqualTo(decimalSample, anotherSample)));
        Assert.assertTrue(boolify(isNotEqualTo(decimalSample, anotherSample)));
        Assert.assertFalse(boolify(isLessThan(decimalSample, anotherSample)));
        Assert.assertTrue(boolify(isGreaterThan(decimalSample, anotherSample)));
        Assert.assertFalse(boolify(isLessThanOrEqual(decimalSample, anotherSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(decimalSample, anotherSample)));
    }

    @Test
    public void testDecimalArithmetic() {
        Assert.assertEquals(add(asTypedObject(new BigDecimal("0.01")), asTypedObject(new BigDecimal("10.0"))).data,
                            new BigDecimal("10.01"));
        Assert.assertEquals(subtract(asTypedObject(new BigDecimal("0.01")), asTypedObject(new BigDecimal("10.0"))).data,
                            new BigDecimal("-9.99"));
        Assert.assertEquals(multiply(asTypedObject(new BigDecimal("0.01")), asTypedObject(new BigDecimal("10.0"))).data,
                            new BigDecimal("0.100"));
        Assert.assertEquals(divide(asTypedObject(new BigDecimal("0.01")), asTypedObject(new BigDecimal("10.0"))).data,
                            new BigDecimal("0.001"));
        Assert.assertEquals(modulus(asTypedObject(new BigDecimal("101.2")), asTypedObject(new BigDecimal("10.0"))).data,
                            new BigDecimal("1.2"));
    }

    /***
     * BOOLEAN
     ***/

    @Test
    public void testCastingToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject stringSample = asTypedObject("false");
        Assert.assertTrue(boolify(isEqualTo(booleanedObject, stringSample)));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingLongToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject longSample = asTypedObject(1L);
        isEqualTo(booleanedObject, longSample);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingDoubleToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject doubleSample = asTypedObject(1.0);
        isEqualTo(booleanedObject, doubleSample);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingDecimalToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject decimalSample = asTypedObject(new BigDecimal("1.2"));
        isEqualTo(booleanedObject, decimalSample);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingTimestampToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(booleanedObject, timestampSample);
    }

    @Test
    public void testBooleanTypeComparisons() {
        TypedObject booleanSample = asTypedObject(true);

        Assert.assertTrue(boolify(isEqualTo(booleanSample, booleanSample)));
        Assert.assertFalse(boolify(isNotEqualTo(booleanSample, booleanSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(booleanSample, booleanSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(booleanSample, booleanSample)));

        TypedObject anotherSample = asTypedObject(false);

        Assert.assertFalse(boolify(isEqualTo(booleanSample, anotherSample)));
        Assert.assertTrue(boolify(isNotEqualTo(booleanSample, anotherSample)));
        Assert.assertFalse(boolify(isLessThan(booleanSample, anotherSample)));
        Assert.assertTrue(boolify(isGreaterThan(booleanSample, anotherSample)));
        Assert.assertFalse(boolify(isLessThanOrEqual(booleanSample, anotherSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(booleanSample, anotherSample)));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testBooleanAddition() {
        add(asTypedObject(false), asTypedObject(true));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testBooleanSubtraction() {
        subtract(asTypedObject(false), asTypedObject(true));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testBooleanMultiplication() {
        multiply(asTypedObject(false), asTypedObject(true));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testBooleanDivision() {
        divide(asTypedObject(false), asTypedObject(true));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testBooleanModulus() {
        modulus(asTypedObject(false), asTypedObject(true));
    }

    /***
     * TIMESTAMP
     ***/

    @Test
    public void testCastingToTimestamp() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject longSample = asTypedObject(1435553876000L);
        Assert.assertTrue(boolify(isEqualTo(timestampedObject, longSample)));
    }

    @Test
    public void testFailCastingDecimalToTimestampButCastOtherWay() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject decimalSample = asTypedObject(new BigDecimal("1.2"));
        Assert.assertFalse(boolify(isEqualTo(timestampedObject, decimalSample)));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingStringToTimestamp() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject stringSample = asTypedObject("1435553876000");
        isEqualTo(timestampedObject, stringSample);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingDoubleToTimestamp() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject doubleSample = asTypedObject(1.0);
        isEqualTo(timestampedObject, doubleSample);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailCastingBooleanToTimestamp() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject booleanSample = asTypedObject(true);
        isEqualTo(timestampedObject, booleanSample);
    }

    @Test
    public void testTimestampTypeComparisons() {
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));

        Assert.assertTrue(boolify(isEqualTo(timestampSample, timestampSample)));
        Assert.assertFalse(boolify(isNotEqualTo(timestampSample, timestampSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(timestampSample, timestampSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(timestampSample, timestampSample)));

        TypedObject anotherSample = asTypedObject(1435553876001L);

        Assert.assertFalse(boolify(isEqualTo(timestampSample, anotherSample)));
        Assert.assertTrue(boolify(isNotEqualTo(timestampSample, anotherSample)));
        Assert.assertTrue(boolify(isLessThan(timestampSample, anotherSample)));
        Assert.assertFalse(boolify(isGreaterThan(timestampSample, anotherSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(timestampSample, anotherSample)));
        Assert.assertFalse(boolify(isGreaterThanOrEqual(timestampSample, anotherSample)));
    }

    @Test
    public void testTimestampArithmetic() {
        Assert.assertTrue(equals((Timestamp) add(asTypedObject(new Timestamp(1435553876000L)),
                                                 asTypedObject(new Timestamp(1000L))).data,
                                 new Timestamp(1435553877000L)));
        Assert.assertTrue(equals((Timestamp) subtract(asTypedObject(new Timestamp(1435553876000L)),
                                                      asTypedObject(new Timestamp(1435553876000L))).data,
                                 new Timestamp(0L)));
        Assert.assertTrue(equals((Timestamp) multiply(asTypedObject(new Timestamp(16000L)),
                                                      asTypedObject(new Timestamp(2L))).data,
                                 new Timestamp(32000L)));
        Assert.assertTrue(equals((Timestamp) divide(asTypedObject(new Timestamp(14000L)),
                                                    asTypedObject(new Timestamp(3L))).data,
                                 new Timestamp(4666L)));
        Assert.assertTrue(equals((Timestamp) modulus(asTypedObject(new Timestamp(1435553876001L)),
                                                     asTypedObject(new Timestamp(1435553876000L))).data,
                                 new Timestamp(1L)));
    }

    /*****************************/

    @Test
    public void testLogicalNegate() {
        Assert.assertTrue(boolify(logicalNegate(asTypedObject(false))));
        Assert.assertFalse(boolify(logicalNegate(asTypedObject(true))));
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testFailLogicalNegateNull() {
        logicalNegate(null);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalNegateLong() {
        logicalNegate(asTypedObject(1L));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalNegateDouble() {
        logicalNegate(asTypedObject(1.0));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalNegateDecimal() {
        logicalNegate(asTypedObject(new BigDecimal("1.0")));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalNegateTimestamp() {
        logicalNegate(asTypedObject(new Timestamp(1L)));
    }

    @Test
    public void testLogicalOr() {
        Assert.assertTrue(boolify(logicalOr(asTypedObject(false), asTypedObject(true))));
        Assert.assertTrue(boolify(logicalOr(asTypedObject(true), asTypedObject(false))));
        Assert.assertTrue(boolify(logicalOr(asTypedObject(true), asTypedObject(true))));
        Assert.assertFalse(boolify(logicalOr(asTypedObject(false), asTypedObject(false))));
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testFailLogicalOrNull() {
        logicalOr(null, asTypedObject(1L));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalOrLong() {
        logicalOr(asTypedObject(false), asTypedObject(1L));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalOrDouble() {
        logicalOr(asTypedObject(1.2), asTypedObject(false));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalOrDecimal() {
        logicalOr(asTypedObject(true), asTypedObject(new BigDecimal("1.2")));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalOrTimestamp() {
        logicalOr(asTypedObject(new Timestamp(1L)), asTypedObject(false));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalOrString() {
        logicalOr(asTypedObject("false"), asTypedObject("true"));
    }

    @Test
    public void testLogicalAnd() {
        Assert.assertFalse(boolify(logicalAnd(asTypedObject(false), asTypedObject(true))));
        Assert.assertFalse(boolify(logicalAnd(asTypedObject(true), asTypedObject(false))));
        Assert.assertFalse(boolify(logicalAnd(asTypedObject(false), asTypedObject(false))));
        Assert.assertTrue(boolify(logicalAnd(asTypedObject(true), asTypedObject(true))));
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testFailLogicalAndNull() {
        logicalAnd(asTypedObject(true), null);
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalAndLong() {
        logicalAnd(asTypedObject(false), asTypedObject(1L));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalAndDouble() {
        logicalAnd(asTypedObject(1.2), asTypedObject(false));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalAndDecimal() {
        logicalAnd(asTypedObject(true), asTypedObject(new BigDecimal("1.2")));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalAndTimestamp() {
        logicalAnd(asTypedObject(new Timestamp(1L)), asTypedObject(false));
    }

    @Test(expectedExceptions = {ClassCastException.class})
    public void testFailLogicalAndString() {
        logicalAnd(asTypedObject("false"), asTypedObject("true"));
    }

    /************************************/

    @Test
    public void testDispatchedCasting() {
        TypedObject first = asTypedObject("42");
        TypedObject result = TypeSystem.perform(TypeSystem.UnaryOperation.CAST, first);
        Assert.assertTrue(boolify(isEqualTo(first, result)));
    }

    @Test
    public void testDefaultCasting() {
        TypeSystem.Operations operations = new CustomOperations();
        Assert.assertNull(operations.cast(asTypedObject(42L)));
    }
}

