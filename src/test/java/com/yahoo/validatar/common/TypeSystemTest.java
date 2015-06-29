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

import static com.yahoo.validatar.common.TypeSystem.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.sql.Timestamp;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

public class TypeSystemTest {
    public static final double EPSILON = 0.00001;

    private boolean boolify(TypedObject type) {
        return (Boolean) type.data;
    }

    private boolean equals(Double first, Double second) {
        return Math.abs(first - second) < EPSILON;
    }

    @Test
    public void testTypedObjectConversions() {
        Boolean booleanValue = true;
        Assert.assertEquals((Boolean) asTypedObject(booleanValue).data, Boolean.valueOf(true));
        Assert.assertEquals(asTypedObject(booleanValue).type, Type.BOOLEAN);
        String stringValue = "foo";
        Assert.assertEquals((String) asTypedObject(stringValue).data, "foo");
        Assert.assertEquals(asTypedObject(stringValue).type, Type.STRING);

        Long longValue = 131412300000000000L;
        Assert.assertEquals((Long) asTypedObject(longValue).data, Long.valueOf(131412300000000000L));
        Assert.assertEquals(asTypedObject(longValue).type, Type.LONG);

        Double doubleValue = 235242523.04;
        Assert.assertEquals((Double) asTypedObject(doubleValue).data, Double.valueOf(235242523.04));
        Assert.assertEquals(asTypedObject(doubleValue).type, Type.DOUBLE);

        BigDecimal decimalValue = new BigDecimal("234235234234223425151231231151231.123141231231411231231");
        Assert.assertEquals((BigDecimal) asTypedObject(decimalValue).data, new BigDecimal("234235234234223425151231231151231.123141231231411231231"));
        Assert.assertEquals(asTypedObject(decimalValue).type, Type.DECIMAL);

        long timeNow = System.currentTimeMillis();
        Timestamp timestampValue = new Timestamp(timeNow);
        Assert.assertEquals((Timestamp) asTypedObject(timestampValue).data, new Timestamp(timeNow));
        Assert.assertEquals(asTypedObject(timestampValue).type, Type.TIMESTAMP);
    }

    @Test(expectedExceptions={NullPointerException.class})
    public void testFirstOperandNull() {
        TypedObject stringSample = asTypedObject("foo");
        TypeSystem.compare(null, stringSample);
    }

    @Test(expectedExceptions={NullPointerException.class})
    public void testSecondOperandNull() {
        TypedObject stringSample = asTypedObject("foo");
        TypeSystem.compare(stringSample, null);
    }

    @Test(expectedExceptions={NullPointerException.class})
    public void testBothOperandsNull() {
        TypeSystem.compare(null, null);
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testNotUnifiableTypes() {
        TypedObject booleanSample = asTypedObject(false);
        TypedObject longSample = asTypedObject(123L);
        TypeSystem.compare(longSample, booleanSample);
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testNonArithmeticOperableTypes() {
        TypedObject booleanSample = asTypedObject(false);
        TypeSystem.add(booleanSample, booleanSample);
    }

    /**************************************************** STRING ******************************************************/

    @Test
    public void testCastingToString() {
        TypedObject stringedObject = new TypedObject("", Type.STRING);

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

        stringedObject.data = "2015-06-28 21:57:56.0";
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        Assert.assertTrue(boolify(isEqualTo(stringedObject, timestampSample)));
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
        add(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testStringSubtraction() {
        subtract(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testStringMultiplication() {
        multiply(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testStringDivision() {
        divide(asTypedObject("sample"), asTypedObject("foo"));
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testStringModulus() {
        modulus(asTypedObject("sample"), asTypedObject("foo"));
    }

    /**************************************************** LONG ******************************************************/

    @Test
    public void testCastingToLong() {
        TypedObject longedObject = new TypedObject(0L, Type.LONG);

        longedObject.data = 123235L;
        TypedObject stringSample = asTypedObject("123235");
        Assert.assertTrue(boolify(isEqualTo(longedObject, stringSample)));

        longedObject.data = 1435553876000L;
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        Assert.assertTrue(boolify(isEqualTo(longedObject, timestampSample)));
    }

    @Test
    public void testFailCastingDoubleToLongButCastOtherWay() {
        TypedObject longedObject = new TypedObject(1L, Type.LONG);
        TypedObject doubleSample = asTypedObject(1.0);
        Assert.assertTrue(boolify(isEqualTo(longedObject, doubleSample)));
    }

    @Test
    public void testFailCastingDecimalToLongButCastOtherWay() {
        TypedObject longedObject = new TypedObject(0L, Type.LONG);
        TypedObject decimalSample = asTypedObject(new BigDecimal("12.23"));
        Assert.assertFalse(boolify(isEqualTo(longedObject, decimalSample)));
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testFailCastingBooleanToLong() {
        TypedObject longedObject = new TypedObject(0L, Type.LONG);
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
        Assert.assertEquals((Long) add(asTypedObject(14L), asTypedObject(4L)).data, Long.valueOf(18L));
        Assert.assertEquals((Long) subtract(asTypedObject(4L), asTypedObject(14L)).data, Long.valueOf(-10L));
        Assert.assertEquals((Long) multiply(asTypedObject(14L), asTypedObject(3L)).data, Long.valueOf(42L));
        Assert.assertEquals((Long) divide(asTypedObject(14L), asTypedObject(4L)).data, Long.valueOf(3L));
        Assert.assertEquals((Long) modulus(asTypedObject(14L), asTypedObject(4L)).data, Long.valueOf(2L));
    }

    /**************************************************** DOUBLE ******************************************************/

    @Test
    public void testCastingToDouble() {
        TypedObject doubledObject = new TypedObject(0.0, Type.DOUBLE);

        doubledObject.data = (Double) 123.0;
        TypedObject longSample = asTypedObject(123L);
        Assert.assertTrue(boolify(isEqualTo(doubledObject, longSample)));

        doubledObject.data = (Double) 3.14159265;
        TypedObject stringSample = asTypedObject("3.14159265");
        Assert.assertTrue(boolify(isEqualTo(doubledObject, stringSample)));
    }

    @Test
    public void testFailCastingDecimalToDoubleButCastOtherWay() {
        TypedObject doubledObject = new TypedObject(12.23, Type.DOUBLE);
        TypedObject decimalSample = asTypedObject(new BigDecimal("12.23"));
        Assert.assertTrue(boolify(isEqualTo(doubledObject, decimalSample)));
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testFailCastingTimestampToDouble() {
        TypedObject doubledObject = new TypedObject(12.23, Type.DOUBLE);
        TypedObject timestampSample = asTypedObject(new Timestamp(1435553876000L));
        isEqualTo(doubledObject, timestampSample);
    }

    @Test(expectedExceptions={ClassCastException.class})
    public void testFailCastingBooleanToDouble() {
        TypedObject doubledObject = new TypedObject(12.23, Type.DOUBLE);
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

    @Test(expectedExceptions={ClassCastException.class})
    public void testDoubleModulus() {
        modulus(asTypedObject(14.0), asTypedObject(4.0));
    }

}
