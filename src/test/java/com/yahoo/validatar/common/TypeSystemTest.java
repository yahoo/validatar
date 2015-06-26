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

    public static final TypedObject getAsTypedObject(TypeSystem.Type type, Object value) {
        switch(type) {
            case STRING:
                return new TypedObject((String) value, TypeSystem.Type.STRING);
            case CHARACTER:
                return new TypedObject((Character) value, TypeSystem.Type.CHARACTER);
            case LONG:
                return new TypedObject((Long) value, TypeSystem.Type.LONG);
            case DOUBLE:
                return new TypedObject((Double) value, TypeSystem.Type.DOUBLE);
            case DECIMAL:
                return new TypedObject((BigDecimal) value, TypeSystem.Type.DECIMAL);
            case TIMESTAMP:
                return new TypedObject((Timestamp) value, TypeSystem.Type.TIMESTAMP);
            case BOOLEAN:
                return new TypedObject((Boolean) value, TypeSystem.Type.BOOLEAN);
            default:
                throw new RuntimeException("Unknown type");
        }
    }

    @Test
    public void testTypedObjectConversions() {
        Boolean booleanValue = true;
        Assert.assertEquals((Boolean) TypeSystem.asTypedObject(booleanValue).data, Boolean.valueOf(true));
        Assert.assertEquals(TypeSystem.asTypedObject(booleanValue).type, TypeSystem.Type.BOOLEAN);

        String stringValue = "foo";
        Assert.assertEquals((String) TypeSystem.asTypedObject(stringValue).data, "foo");
        Assert.assertEquals(TypeSystem.asTypedObject(stringValue).type, TypeSystem.Type.STRING);

        Character characterValue = '4';
        Assert.assertEquals((Character) TypeSystem.asTypedObject(characterValue).data, Character.valueOf('4'));
        Assert.assertEquals(TypeSystem.asTypedObject(characterValue).type, TypeSystem.Type.CHARACTER);

        Long longValue = 131412300000000000L;
        Assert.assertEquals((Long) TypeSystem.asTypedObject(longValue).data, Long.valueOf(131412300000000000L));
        Assert.assertEquals(TypeSystem.asTypedObject(longValue).type, TypeSystem.Type.LONG);

        Double doubleValue = 235242523.04;
        Assert.assertEquals((Double) TypeSystem.asTypedObject(doubleValue).data, Double.valueOf(235242523.04));
        Assert.assertEquals(TypeSystem.asTypedObject(doubleValue).type, TypeSystem.Type.DOUBLE);

        BigDecimal decimalValue = new BigDecimal("234235234234223425151231231151231.123141231231411231231");
        Assert.assertEquals((BigDecimal) TypeSystem.asTypedObject(decimalValue).data, new BigDecimal("234235234234223425151231231151231.123141231231411231231"));
        Assert.assertEquals(TypeSystem.asTypedObject(decimalValue).type, TypeSystem.Type.DECIMAL);

        long timeNow = System.currentTimeMillis();
        Timestamp timestampValue = new Timestamp(timeNow);
        Assert.assertEquals((Timestamp) TypeSystem.asTypedObject(timestampValue).data, new Timestamp(timeNow));
        Assert.assertEquals(TypeSystem.asTypedObject(timestampValue).type, TypeSystem.Type.TIMESTAMP);
    }

}
