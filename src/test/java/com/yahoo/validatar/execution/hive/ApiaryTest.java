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

package com.yahoo.validatar.execution.hive;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiaryTest {
    private String[] args = {"--hive-driver", "org.h2.Driver",
                             "--hive-jdbc", "jdbc:h2:mem:",
                             "--hive-setting", "mapreduce.job.queuename=default"};

    @Test
    public void testGetJDBCConnector() throws ClassNotFoundException, SQLException, Exception {
        Apiary apiary = spy(new Apiary());
        doNothing().when(apiary).setHiveSettings(any(OptionSet.class), any(Statement.class));
        Assert.assertTrue(apiary.setup(args));
        Query query = new Query();
        query.name = "Test";
        query.value = "SELECT 1 as ONE";
        apiary.execute(query);
        Assert.assertFalse(query.failed());
        Assert.assertEquals((Long) query.getResult().getColumns().get("Test.ONE").get(0).data,
                            (Long) new TypedObject(1L, TypeSystem.Type.LONG).data);
    }

    @Test
    public void testFailSetup() {
        Apiary apiary = spy(new Apiary());
        try {
            doThrow(new SQLException()).when(apiary).setHiveSettings(any(OptionSet.class), any(Statement.class));
        } catch (SQLException se) {
            Assert.fail("Should not have thrown an exception");
        }
        Assert.assertFalse(apiary.setup(args));
        try {
            doThrow(new ClassNotFoundException()).when(apiary).setupConnection(any(OptionSet.class));
        } catch (ClassNotFoundException | SQLException e) {
            Assert.fail("Should not have thrown an exception");
        }
        Assert.assertFalse(apiary.setup(args));
    }

    @Test
    public void testFailExecution() {
        Apiary apiary = spy(new Apiary());
        try {
            Statement mocked = mock(Statement.class);
            doThrow(new SQLException()).when(mocked).executeQuery(anyString());
            doNothing().when(apiary).setHiveSettings(any(OptionSet.class), any(Statement.class));
            doReturn(mocked).when(apiary).setupConnection(any(OptionSet.class));
            apiary.setup(args);
        } catch (ClassNotFoundException | SQLException e) {
            Assert.fail("Should not have thrown an exception");
        }
        Query query = new Query();
        apiary.execute(query);
        Assert.assertTrue(query.failed());
    }

    @Test
    public void testHiveSettings() {
        Apiary apiary = new Apiary();
        Statement mocked = mock(Statement.class);
        OptionParser parser = new OptionParser() {
            {
                acceptsAll(singletonList("hive-setting"), "").withRequiredArg();
            }
        };

        String[] args = {"--hive-setting", "mapreduce.job.queuename=default",
                         "--hive-setting", "hive.execution.engine=tez",
                         "--hive-setting", "hive.execution.engine=mr"};
        try {
            apiary.setHiveSettings(parser.parse(args), mocked);
            verify(mocked).executeUpdate("set mapreduce.job.queuename=default");
            verify(mocked).executeUpdate("set hive.execution.engine=tez");
            verify(mocked).executeUpdate("set hive.execution.engine=mr");
        } catch (SQLException se) {
            Assert.fail("Should not have thrown an exception");
        }
    }

    @Test
    public void testHiveTypeMappingNull() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        doReturn(true).when(mocked).wasNull();
        Assert.assertNull(apiary.getAsTypedObject(mocked, 0, Types.BIGINT));
    }

    @Test
    public void testHiveTypeMappingNullRun() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        when(mocked.wasNull()).thenReturn(false, true, true);
        Assert.assertNotNull(apiary.getAsTypedObject(mocked, 0, Types.BIGINT));
        Assert.assertNull(apiary.getAsTypedObject(mocked, 1, Types.BIGINT));
        Assert.assertNull(apiary.getAsTypedObject(mocked, 2, Types.BIGINT));
    }

    @Test
    public void testHiveTypeMappingString() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        TypedObject object;
        // DATE, CHAR, VARCHAR
        doReturn("Sample").when(mocked).getString(anyInt());
        object = apiary.getAsTypedObject(mocked, 0, Types.DATE);
        Assert.assertEquals((String) object.data, "Sample");
        Assert.assertEquals(object.type, TypeSystem.Type.STRING);

        object = apiary.getAsTypedObject(mocked, 0, Types.CHAR);
        Assert.assertEquals((String) object.data, "Sample");
        Assert.assertEquals(object.type, TypeSystem.Type.STRING);

        object = apiary.getAsTypedObject(mocked, 0, Types.VARCHAR);
        Assert.assertEquals((String) object.data, "Sample");
        Assert.assertEquals(object.type, TypeSystem.Type.STRING);
    }

    @Test
    public void testHiveTypeMappingDouble() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        TypedObject object;
        // FLOAT, DOUBLE
        doReturn(Double.valueOf(3.14)).when(mocked).getDouble(anyInt());
        object = apiary.getAsTypedObject(mocked, 0, Types.FLOAT);
        Assert.assertEquals((Double) object.data, Double.valueOf(3.14));
        Assert.assertEquals(object.type, TypeSystem.Type.DOUBLE);

        object = apiary.getAsTypedObject(mocked, 0, Types.DOUBLE);
        Assert.assertEquals((Double) object.data, Double.valueOf(3.14));
        Assert.assertEquals(object.type, TypeSystem.Type.DOUBLE);
    }

    @Test
    public void testHiveTypeMappingBoolean() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        TypedObject object;

        doReturn(Boolean.valueOf(false)).when(mocked).getBoolean(anyInt());
        object = apiary.getAsTypedObject(mocked, 0, Types.BOOLEAN);
        Assert.assertEquals((Boolean) object.data, Boolean.valueOf(false));
        Assert.assertEquals(object.type, TypeSystem.Type.BOOLEAN);
    }

    @Test
    public void testHiveTypeMappingLong() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        TypedObject object;

        doReturn(Long.valueOf(42)).when(mocked).getLong(anyInt());

        object = apiary.getAsTypedObject(mocked, 0, Types.TINYINT);
        Assert.assertEquals((Long) object.data, Long.valueOf(42));
        Assert.assertEquals(object.type, TypeSystem.Type.LONG);

        object = apiary.getAsTypedObject(mocked, 0, Types.SMALLINT);
        Assert.assertEquals((Long) object.data, Long.valueOf(42));
        Assert.assertEquals(object.type, TypeSystem.Type.LONG);

        object = apiary.getAsTypedObject(mocked, 0, Types.INTEGER);
        Assert.assertEquals((Long) object.data, Long.valueOf(42));
        Assert.assertEquals(object.type, TypeSystem.Type.LONG);

        object = apiary.getAsTypedObject(mocked, 0, Types.BIGINT);
        Assert.assertEquals((Long) object.data, Long.valueOf(42));
        Assert.assertEquals(object.type, TypeSystem.Type.LONG);
    }

    @Test
    public void testHiveTypeMappingDecimal() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        TypedObject object;

        doReturn(new BigDecimal("3.14")).when(mocked).getBigDecimal(anyInt());

        object = apiary.getAsTypedObject(mocked, 0, Types.DECIMAL);
        Assert.assertEquals((BigDecimal) object.data, new BigDecimal("3.14"));
        Assert.assertEquals(object.type, TypeSystem.Type.DECIMAL);
    }

    @Test
    public void testHiveTypeMappingTimestamp() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        TypedObject object;

        doReturn(new Timestamp(42L)).when(mocked).getTimestamp(anyInt());

        object = apiary.getAsTypedObject(mocked, 0, Types.TIMESTAMP);
        Assert.assertEquals((Timestamp) object.data, new Timestamp(42L));
        Assert.assertEquals(object.type, TypeSystem.Type.TIMESTAMP);
    }

    @Test(expectedExceptions = {UnsupportedOperationException.class})
    public void testHiveTypeMappingUnknown() throws SQLException {
        Apiary apiary = new Apiary();
        ResultSet mocked = mock(ResultSet.class);
        apiary.getAsTypedObject(mocked, 0, Types.CLOB);
    }
}
