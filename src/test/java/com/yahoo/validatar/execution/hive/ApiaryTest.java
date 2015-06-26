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
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.common.TypeSystem;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import static java.util.Arrays.*;

import java.io.File;
import java.io.FileNotFoundException;

import java.sql.SQLException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ApiaryTest {
    private OptionParser parser = new OptionParser() {
        {
            acceptsAll(asList("hive-driver"), "Fully qualified package name to the hive driver.")
                .withRequiredArg()
                .describedAs("Hive driver");
            acceptsAll(asList("hive-jdbc"), "JDBC string to the HiveServer. Ex: 'jdbc:hive2://HIVE_SERVER:PORT/DATABASENAME' ")
                .withRequiredArg()
                .describedAs("Hive JDBC connector.");
            acceptsAll(asList("hive-username"), "Hive server username.")
                .withRequiredArg()
                .describedAs("Hive server username.")
                .defaultsTo("anon");
            acceptsAll(asList("hive-password"), "Hive server password.")
                .withRequiredArg()
                .describedAs("Hive server password.")
                .defaultsTo("anon");
            allowsUnrecognizedOptions();
        }
    };

    @Test
    public void testGetJDBCConnector() throws ClassNotFoundException, SQLException, Exception {
        Apiary apiary = new Apiary();
        String[] args = {"--hive-driver", "org.h2.Driver",
                         "--hive-jdbc",   "jdbc:h2:mem:"};
        apiary.setupConnection(parser.parse(args));
        Query query = new Query();
        query.name = "Test";
        query.value = "SELECT 1 as ONE";
        apiary.execute(query);
        Assert.assertFalse(query.failed());
        Assert.assertEquals((Long) query.getResult().getColumns().get("Test.ONE").get(0).data,
                            (Long) new TypedObject(1L, TypeSystem.Type.LONG).data);
    }
}
