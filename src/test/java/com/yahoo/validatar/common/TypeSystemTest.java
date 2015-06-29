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
    private boolean boolify(TypedObject type) {
        return (Boolean) type.data;
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

    @Test
    public void testCastingToString() {
        TypedObject longSample = asTypedObject(123L);
        TypedObject stringedObject = new TypedObject("", Type.STRING);

        stringedObject.data = "123";
        Assert.assertTrue(boolify(isEqualTo(stringedObject, longSample)));
    }

    @Test
    public void testStringTypeComparisons() {
        TypedObject stringSample = asTypedObject("sample");

        Assert.assertTrue(boolify(isEqualTo(stringSample, stringSample)));
        Assert.assertFalse(boolify(isNotEqualTo(stringSample, stringSample)));
        Assert.assertTrue(boolify(isLessThanOrEqual(stringSample, stringSample)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(stringSample, stringSample)));

        TypedObject original = asTypedObject("foo");

        Assert.assertFalse(boolify(isEqualTo(stringSample, original)));
        Assert.assertTrue(boolify(isNotEqualTo(stringSample, original)));
        Assert.assertFalse(boolify(isLessThan(stringSample, original)));
        Assert.assertTrue(boolify(isGreaterThan(stringSample, original)));
        Assert.assertFalse(boolify(isLessThanOrEqual(stringSample, original)));
        Assert.assertTrue(boolify(isGreaterThanOrEqual(stringSample, original)));
    }
}
