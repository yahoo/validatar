/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.common;

import com.yahoo.validatar.TestHelpers;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static com.yahoo.validatar.TestHelpers.asColumn;
import static com.yahoo.validatar.common.TypeSystem.Type;
import static com.yahoo.validatar.common.TypeSystem.approx;
import static com.yahoo.validatar.common.TypeSystem.asTypedObject;
import static com.yahoo.validatar.common.TypeSystem.unifySize;
import static java.util.Arrays.asList;

public class TypeSystemTest {
    private TypeSystem system = new TypeSystem();
    private Operators operators = new Operators();

    private class CustomOperations implements Operations {
    }

    public static final double EPSILON = 0.00001;

    private boolean equals(Double first, Double second) {
        return Math.abs(first - second) < EPSILON;
    }

    private boolean equals(Timestamp first, Timestamp second) {
        return first.getTime() == second.getTime();
    }

    private TypedObject add(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.ADD, first, second);
    }

    private TypedObject subtract(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.SUBTRACT, first, second);
    }

    private TypedObject multiply(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.MULTIPLY, first, second);
    }

    private TypedObject divide(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.DIVIDE, first, second);
    }

    private TypedObject modulus(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.MODULUS, first, second);
    }

    private TypedObject isEqualTo(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.EQUAL, first, second);
    }

    private TypedObject isNotEqualTo(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.NOT_EQUAL, first, second);
    }

    private TypedObject isLessThanOrEqual(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.LESS_EQUAL, first, second);
    }

    private TypedObject isGreaterThanOrEqual(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.GREATER_EQUAL, first, second);
    }

    private TypedObject isLessThan(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.LESS, first, second);
    }

    private TypedObject isGreaterThan(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.GREATER, first, second);
    }

    private TypedObject logicalAnd(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.AND, first, second);
    }

    private TypedObject logicalOr(TypedObject first, TypedObject second) {
        return TypeSystem.perform(Operations.BinaryOperation.OR, first, second);
    }

    private TypedObject logicalNegate(TypedObject first) {
        return TypeSystem.perform(Operations.UnaryOperation.NOT, first);
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

    @Test(expectedExceptions = NullPointerException.class)
    public void testFirstOperandNull() {
        TypedObject stringSample = asTypedObject("foo");
        TypeSystem.compare(null, stringSample);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSecondOperandNull() {
        TypedObject stringSample = asTypedObject("foo");
        TypeSystem.compare(stringSample, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testBothOperandsNull() {
        TypeSystem.compare(null, null);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testNotUnifiableTypes() {
        TypedObject booleanSample = asTypedObject(false);
        TypedObject longSample = asTypedObject(123L);
        TypeSystem.compare(longSample, booleanSample);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testNonArithmeticOperableTypes() {
        TypedObject booleanSample = asTypedObject(false);
        add(booleanSample, booleanSample);
    }

    /***
     * STRING
     ***/

    @Test
    public void testCastingToString() {
        TypedObject stringedObject = asTypedObject("");

        stringedObject.data = "123";
        TypedObject longSample = asTypedObject(123L);
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(stringedObject, longSample)));

        stringedObject.data = "1.23";
        TypedObject doubleSample = asTypedObject(1.23);
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(stringedObject, doubleSample)));

        stringedObject.data = "12312312.2312412431321314123123123124123123123";
        TypedObject decimalSample = asTypedObject(new BigDecimal("12312312.2312412431321314123123123124123123123"));
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(stringedObject, decimalSample)));

        stringedObject.data = "false";
        TypedObject booleanSample = asTypedObject(false);
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(stringedObject, booleanSample)));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingTimestampToString() {
        TypedObject stringedObject = asTypedObject("2015-06-28 21:57:56.0");
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(stringedObject, timestampSample);
    }

    @Test
    public void testStringTypeComparisons() {
        TypedObject stringSample = asTypedObject("sample");

        Assert.assertTrue(TestHelpers.boolify(isEqualTo(stringSample, stringSample)));
        Assert.assertFalse(TestHelpers.boolify(isNotEqualTo(stringSample, stringSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThanOrEqual(stringSample, stringSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(stringSample, stringSample)));

        TypedObject anotherSample = asTypedObject("foo");

        Assert.assertFalse(TestHelpers.boolify(isEqualTo(stringSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isNotEqualTo(stringSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isLessThan(stringSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThan(stringSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isLessThanOrEqual(stringSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(stringSample, anotherSample)));
    }

    @Test
    public void testStringArithmetic() {
        Assert.assertEquals((String) (add(asTypedObject("sample"), asTypedObject("foo")).data), "samplefoo");
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testStringSubtraction() {
        subtract(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testStringMultiplication() {
        multiply(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testStringDivision() {
        divide(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions = ClassCastException.class)
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
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(longedObject, stringSample)));

        longedObject.data = 1435553876000L;
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(longedObject, timestampSample)));
    }

    @Test
    public void testFailCastingDoubleToLongButCastOtherWay() {
        TypedObject longedObject = asTypedObject(1L);
        TypedObject doubleSample = asTypedObject(1.0);
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(longedObject, doubleSample)));
    }

    @Test
    public void testFailCastingDecimalToLongButCastOtherWay() {
        TypedObject longedObject = asTypedObject(0L);
        TypedObject decimalSample = asTypedObject(new BigDecimal("12.23"));
        Assert.assertFalse(TestHelpers.boolify(isEqualTo(longedObject, decimalSample)));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingBooleanToLong() {
        TypedObject longedObject = asTypedObject(0L);
        TypedObject booleanSample = asTypedObject(false);
        isEqualTo(longedObject, booleanSample);
    }

    @Test
    public void testLongTypeComparisons() {
        TypedObject longSample = asTypedObject(42L);

        Assert.assertTrue(TestHelpers.boolify(isEqualTo(longSample, longSample)));
        Assert.assertFalse(TestHelpers.boolify(isNotEqualTo(longSample, longSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThanOrEqual(longSample, longSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(longSample, longSample)));

        TypedObject anotherSample = asTypedObject(51L);

        Assert.assertFalse(TestHelpers.boolify(isEqualTo(longSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isNotEqualTo(longSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThan(longSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isGreaterThan(longSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThanOrEqual(longSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isGreaterThanOrEqual(longSample, anotherSample)));
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
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(doubledObject, longSample)));

        doubledObject.data = 3.14159265;
        TypedObject stringSample = asTypedObject("3.14159265");
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(doubledObject, stringSample)));
    }

    @Test
    public void testFailCastingDecimalToDoubleButCastOtherWay() {
        TypedObject doubledObject = asTypedObject(12.23);
        TypedObject decimalSample = asTypedObject(new BigDecimal("12.23"));
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(doubledObject, decimalSample)));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingTimestampToDouble() {
        TypedObject doubledObject = asTypedObject(12.23);
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(doubledObject, timestampSample);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingBooleanToDouble() {
        TypedObject doubledObject = asTypedObject(12.23);
        TypedObject booleanSample = asTypedObject(false);
        isEqualTo(doubledObject, booleanSample);
    }

    @Test
    public void testDoubleTypeComparisons() {
        TypedObject doubleSample = asTypedObject(42.4242);

        Assert.assertTrue(TestHelpers.boolify(isEqualTo(doubleSample, doubleSample)));
        Assert.assertFalse(TestHelpers.boolify(isNotEqualTo(doubleSample, doubleSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThanOrEqual(doubleSample, doubleSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(doubleSample, doubleSample)));

        TypedObject anotherSample = asTypedObject(3.14159265);

        Assert.assertFalse(TestHelpers.boolify(isEqualTo(doubleSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isNotEqualTo(doubleSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isLessThan(doubleSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThan(doubleSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isLessThanOrEqual(doubleSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(doubleSample, anotherSample)));
    }

    @Test
    public void testDoubleArithmetic() {
        Assert.assertTrue(equals((Double) add(asTypedObject(14.25), asTypedObject(4.50)).data, Double.valueOf(18.75)));
        Assert.assertTrue(equals((Double) subtract(asTypedObject(14.34), asTypedObject(14.0)).data, Double.valueOf(0.34)));
        Assert.assertTrue(equals((Double) multiply(asTypedObject(4.25), asTypedObject(4.0)).data, Double.valueOf(17.0)));
        Assert.assertTrue(equals((Double) divide(asTypedObject(14.0), asTypedObject(4.0)).data, Double.valueOf(3.5)));
    }

    @Test(expectedExceptions = ClassCastException.class)
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
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(decimaledObject, stringSample)));

        decimaledObject.data = new BigDecimal("123");
        TypedObject longSample = asTypedObject(123L);
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(decimaledObject, longSample)));

        decimaledObject.data = new BigDecimal("1.23");
        TypedObject doubleSample = asTypedObject(1.23);
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(decimaledObject, doubleSample)));

        decimaledObject.data = new BigDecimal("1435553876000");
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(decimaledObject, timestampSample);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingBooleanToDecimal() {
        TypedObject decimaledObject = asTypedObject(new BigDecimal("0.1"));
        TypedObject booleanSample = asTypedObject(false);
        isEqualTo(decimaledObject, booleanSample);
    }

    @Test
    public void testDecimalTypeComparisons() {
        TypedObject decimalSample = asTypedObject(new BigDecimal("12345678912345.123456789123456789123456789"));

        Assert.assertTrue(TestHelpers.boolify(isEqualTo(decimalSample, decimalSample)));
        Assert.assertFalse(TestHelpers.boolify(isNotEqualTo(decimalSample, decimalSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThanOrEqual(decimalSample, decimalSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(decimalSample, decimalSample)));

        TypedObject anotherSample = asTypedObject("3.1415926535897932384626433832795029");

        Assert.assertFalse(TestHelpers.boolify(isEqualTo(decimalSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isNotEqualTo(decimalSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isLessThan(decimalSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThan(decimalSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isLessThanOrEqual(decimalSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(decimalSample, anotherSample)));
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
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(booleanedObject, stringSample)));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingLongToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject longSample = asTypedObject(1L);
        isEqualTo(booleanedObject, longSample);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingDoubleToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject doubleSample = asTypedObject(1.0);
        isEqualTo(booleanedObject, doubleSample);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingDecimalToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject decimalSample = asTypedObject(new BigDecimal("1.2"));
        isEqualTo(booleanedObject, decimalSample);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingTimestampToBoolean() {
        TypedObject booleanedObject = asTypedObject(false);
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(booleanedObject, timestampSample);
    }

    @Test
    public void testBooleanTypeComparisons() {
        TypedObject booleanSample = asTypedObject(true);

        Assert.assertTrue(TestHelpers.boolify(isEqualTo(booleanSample, booleanSample)));
        Assert.assertFalse(TestHelpers.boolify(isNotEqualTo(booleanSample, booleanSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThanOrEqual(booleanSample, booleanSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(booleanSample, booleanSample)));

        TypedObject anotherSample = asTypedObject(false);

        Assert.assertFalse(TestHelpers.boolify(isEqualTo(booleanSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isNotEqualTo(booleanSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isLessThan(booleanSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThan(booleanSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isLessThanOrEqual(booleanSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(booleanSample, anotherSample)));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testBooleanAddition() {
        add(asTypedObject(false), asTypedObject(true));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testBooleanSubtraction() {
        subtract(asTypedObject(false), asTypedObject(true));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testBooleanMultiplication() {
        multiply(asTypedObject(false), asTypedObject(true));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testBooleanDivision() {
        divide(asTypedObject(false), asTypedObject(true));
    }

    @Test(expectedExceptions = ClassCastException.class)
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
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(timestampedObject, longSample)));
    }

    @Test
    public void testFailCastingDecimalToTimestampButCastOtherWay() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject decimalSample = asTypedObject(new BigDecimal("1.2"));
        Assert.assertFalse(TestHelpers.boolify(isEqualTo(timestampedObject, decimalSample)));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingStringToTimestamp() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject stringSample = asTypedObject("1435553876000");
        isEqualTo(timestampedObject, stringSample);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingDoubleToTimestamp() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject doubleSample = asTypedObject(1.0);
        isEqualTo(timestampedObject, doubleSample);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailCastingBooleanToTimestamp() {
        TypedObject timestampedObject = asTypedObject(new Timestamp(1435553876000L));
        TypedObject booleanSample = asTypedObject(true);
        isEqualTo(timestampedObject, booleanSample);
    }

    @Test
    public void testTimestampTypeComparisons() {
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));

        Assert.assertTrue(TestHelpers.boolify(isEqualTo(timestampSample, timestampSample)));
        Assert.assertFalse(TestHelpers.boolify(isNotEqualTo(timestampSample, timestampSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThanOrEqual(timestampSample, timestampSample)));
        Assert.assertTrue(TestHelpers.boolify(isGreaterThanOrEqual(timestampSample, timestampSample)));

        TypedObject anotherSample = asTypedObject(1435553876001L);

        Assert.assertFalse(TestHelpers.boolify(isEqualTo(timestampSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isNotEqualTo(timestampSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThan(timestampSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isGreaterThan(timestampSample, anotherSample)));
        Assert.assertTrue(TestHelpers.boolify(isLessThanOrEqual(timestampSample, anotherSample)));
        Assert.assertFalse(TestHelpers.boolify(isGreaterThanOrEqual(timestampSample, anotherSample)));
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
        Assert.assertTrue(TestHelpers.boolify(logicalNegate(asTypedObject(false))));
        Assert.assertFalse(TestHelpers.boolify(logicalNegate(asTypedObject(true))));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testFailLogicalNegateNull() {
        logicalNegate(null);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalNegateLong() {
        logicalNegate(asTypedObject(1L));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalNegateDouble() {
        logicalNegate(asTypedObject(1.0));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalNegateDecimal() {
        logicalNegate(asTypedObject(new BigDecimal("1.0")));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalNegateTimestamp() {
        logicalNegate(asTypedObject(new Timestamp(1L)));
    }

    @Test
    public void testLogicalOr() {
        Assert.assertTrue(TestHelpers.boolify(logicalOr(asTypedObject(false), asTypedObject(true))));
        Assert.assertTrue(TestHelpers.boolify(logicalOr(asTypedObject(true), asTypedObject(false))));
        Assert.assertTrue(TestHelpers.boolify(logicalOr(asTypedObject(true), asTypedObject(true))));
        Assert.assertFalse(TestHelpers.boolify(logicalOr(asTypedObject(false), asTypedObject(false))));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testFailLogicalOrNull() {
        logicalOr(null, asTypedObject(1L));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalOrLong() {
        logicalOr(asTypedObject(false), asTypedObject(1L));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalOrDouble() {
        logicalOr(asTypedObject(1.2), asTypedObject(false));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalOrDecimal() {
        logicalOr(asTypedObject(true), asTypedObject(new BigDecimal("1.2")));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalOrTimestamp() {
        logicalOr(asTypedObject(new Timestamp(1L)), asTypedObject(false));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalOrString() {
        logicalOr(asTypedObject("false"), asTypedObject("true"));
    }

    @Test
    public void testLogicalAnd() {
        Assert.assertFalse(TestHelpers.boolify(logicalAnd(asTypedObject(false), asTypedObject(true))));
        Assert.assertFalse(TestHelpers.boolify(logicalAnd(asTypedObject(true), asTypedObject(false))));
        Assert.assertFalse(TestHelpers.boolify(logicalAnd(asTypedObject(false), asTypedObject(false))));
        Assert.assertTrue(TestHelpers.boolify(logicalAnd(asTypedObject(true), asTypedObject(true))));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testFailLogicalAndNull() {
        logicalAnd(asTypedObject(true), null);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalAndLong() {
        logicalAnd(asTypedObject(false), asTypedObject(1L));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalAndDouble() {
        logicalAnd(asTypedObject(1.2), asTypedObject(false));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalAndDecimal() {
        logicalAnd(asTypedObject(true), asTypedObject(new BigDecimal("1.2")));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalAndTimestamp() {
        logicalAnd(asTypedObject(new Timestamp(1L)), asTypedObject(false));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testFailLogicalAndString() {
        logicalAnd(asTypedObject("false"), asTypedObject("true"));
    }

    /************************************/

    @Test
    public void testDispatchedCasting() {
        TypedObject first = asTypedObject("42");
        TypedObject result = TypeSystem.perform(Operations.UnaryOperation.CAST, first);
        Assert.assertTrue(TestHelpers.boolify(isEqualTo(first, result)));
    }

    @Test
    public void testDefaultCasting() {
        Operations operations = new CustomOperations();
        Assert.assertNull(operations.cast(asTypedObject(42L)));
    }

    @Test
    public void testApprox() {
        Assert.assertTrue(TestHelpers.boolify(approx(asTypedObject(100L), asTypedObject(95L), asTypedObject(0.1))));
        Assert.assertFalse(TestHelpers.boolify(approx(asTypedObject(100L), asTypedObject(89L), asTypedObject(0.1))));
        Assert.assertFalse(TestHelpers.boolify(approx(asTypedObject(95L), asTypedObject(105L), asTypedObject(0.04))));
        Assert.assertTrue(TestHelpers.boolify(approx(asTypedObject(97L), asTypedObject(100L), asTypedObject(0.04))));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*between 0 and 1.*")
    public void testApproxBadPercentageLower() {
        approx(asTypedObject(100L), asTypedObject(95L), asTypedObject(-0.2));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*between 0 and 1.*")
    public void testApproxBadPercentageUpper() {
        approx(asTypedObject(100L), asTypedObject(95L), asTypedObject(1.2));
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testApproxBadPercentageType() {
        approx(asTypedObject(100L), asTypedObject(95L), new TypedObject(0.2, Type.LONG));
    }

    @Test
    public void testApproxColumnarScalarPercent() {
        Assert.assertTrue(TestHelpers.boolify(approx(asColumn(Type.LONG, 100L, 99L, 98L),
                asColumn(Type.LONG, 97L, 96L, 95L),
                asColumn(Type.DOUBLE, 0.05))));

        Assert.assertFalse(TestHelpers.boolify(approx(asColumn(Type.LONG, 100L, 99L, 98L),
                asColumn(Type.LONG, 96L, 95L, 92L),
                asColumn(Type.DOUBLE, 0.05))));

    }

    @Test
    public void testSizeUnificationPassThrough() {
        Column a = new Column(asList(asTypedObject(false), asTypedObject(true), asTypedObject(true)));
        Column b = new Column(asList(asTypedObject(1L), asTypedObject(2L), asTypedObject(5L)));
        List<TypedObject> dataA = a.getValues();
        List<TypedObject> dataB = b.getValues();
        unifySize(a, b);
        // In place
        Assert.assertTrue(a.getValues() == dataA);
        Assert.assertTrue(b.getValues() == dataB);

        Assert.assertEquals(a.get(0).data, false);
        Assert.assertEquals(a.get(1).data, true);
        Assert.assertEquals(a.get(2).data, true);
        Assert.assertEquals(b.get(0).data, 1L);
        Assert.assertEquals(b.get(1).data, 2L);
        Assert.assertEquals(b.get(2).data, 5L);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*had no data.*")
    public void testSizeUnificationVectorAndEmpty() {
        Column a = new Column(asList(asTypedObject(false), asTypedObject(true), asTypedObject(true)));
        Column b = new Column();
        unifySize(a, b);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*had no data.*")
    public void testSizeUnificationEmptyAndVector() {
        Column a = new Column();
        Column b = new Column(asList(asTypedObject(false), asTypedObject(true), asTypedObject(true)));
        unifySize(a, b);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*different sizes.*")
    public void testSizeUnificationVectorAndVector() {
        Column a = new Column(asList(asTypedObject(false), asTypedObject(true), asTypedObject(true)));
        Column b = new Column(asList(asTypedObject(1L), asTypedObject(2L)));
        unifySize(a, b);
    }

    @Test
    public void testSizeUnificationScalarAndVector() {
        Column a = new Column(asList(asTypedObject(false)));
        Column b = new Column(asList(asTypedObject(1L), asTypedObject(2L), asTypedObject(5L)));
        List<TypedObject> dataA = a.getValues();
        List<TypedObject> dataB = b.getValues();
        unifySize(a, b);

        Assert.assertTrue(a.getValues() == dataA);
        Assert.assertTrue(b.getValues() == dataB);

        Assert.assertEquals(a.get(0).data, false);
        Assert.assertEquals(a.get(1).data, false);
        Assert.assertEquals(a.get(2).data, false);
        Assert.assertEquals(b.get(0).data, 1L);
        Assert.assertEquals(b.get(1).data, 2L);
        Assert.assertEquals(b.get(2).data, 5L);
    }

    @Test
    public void testSizeUnificationVectorAndScalar() {
        Column a = new Column(asList(asTypedObject(false), asTypedObject(true), asTypedObject(true)));
        Column b = new Column(asList(asTypedObject(1L)));
        List<TypedObject> dataA = a.getValues();
        List<TypedObject> dataB = b.getValues();
        unifySize(a, b);

        Assert.assertTrue(a.getValues() == dataA);
        Assert.assertTrue(b.getValues() == dataB);

        Assert.assertEquals(a.get(0).data, false);
        Assert.assertEquals(a.get(1).data, true);
        Assert.assertEquals(a.get(2).data, true);
        Assert.assertEquals(b.get(0).data, 1L);
        Assert.assertEquals(b.get(1).data, 1L);
        Assert.assertEquals(b.get(2).data, 1L);
    }

    @Test
    public void testApproxColumnarVectorPercent() {
        Assert.assertTrue(TestHelpers.boolify(approx(asColumn(Type.LONG, 100L, 100L, 100L),
                asColumn(Type.LONG, 98L, 97L, 96L),
                asColumn(Type.DOUBLE, 0.03, 0.04, 0.05))));

        Assert.assertFalse(TestHelpers.boolify(approx(asColumn(Type.LONG, 100L, 100L, 100L),
                asColumn(Type.LONG, 96L, 97L, 96L),
                asColumn(Type.DOUBLE, 0.03, 0.04, 0.05))));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*percentage column.*")
    public void testApproxColumnarVectorPercentBadSize() {
        approx(asColumn(Type.LONG, 100L, 100L, 100L), asColumn(Type.LONG, 98L, 97L, 96L),
               asColumn(Type.DOUBLE, 0.03, 0.04));
    }
}

