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

package com.yahoo.validatar.execution.pig;

import com.yahoo.validatar.common.Metadata;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.execution.Engine;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Logger;
import org.apache.pig.PigServer;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class Sty implements Engine {
    protected final Logger log = Logger.getLogger(getClass());

    /** Engine name. */
    public static final String ENGINE_NAME = "pig";

    public static final String DEFAULT_EXEC_TYPE = "mr";
    public static final String DEFAULT_OUTPUT_ALIAS = "validatar_results";
    public static final String SETTING_DELIMITER = "=";

    public static final String METADATA_EXEC_TYPE_KEY = "exec-type";
    public static final String METADATA_ALIAS_KEY = "output-alias";

    private String defaultExecType;
    private String defaultOutputAlias;
    private Properties properties;

    private OptionParser parser = new OptionParser() {
        {
            acceptsAll(singletonList("pig-exec-type"), "The exec-type for Pig to use.  This is the " +
                                                      "-x argument used when running Pig. Ex: local, mr, tez etc. ")
                .withRequiredArg()
                .describedAs("Hive JDBC connector")
                .defaultsTo(DEFAULT_EXEC_TYPE);
            acceptsAll(singletonList("pig-output-alias"), "The name of the alias where the result of the Pig script is." +
                                                    "This should contain the data that will be collected")
                .withRequiredArg()
                .describedAs("Pig default output alias")
                .defaultsTo(DEFAULT_OUTPUT_ALIAS);
            acceptsAll(asList("pig-setting"), "Settings and their values. The -D params that would " +
                                              "have been sent to Pig. Ex: 'mapreduce.job.acl-view-job=*'")
                .withRequiredArg()
                .describedAs("Pig generic settings to use.");
            allowsUnrecognizedOptions();
        }
    };

    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    @Override
    public boolean setup(String[] arguments) {
        OptionSet options = parser.parse(arguments);
        defaultExecType = (String) options.valueOf("pig-exec-type");
        defaultOutputAlias = (String) options.valueOf("pig-output-alias");
        properties = getProperties(options);
        // We will boot up a PigServer per query, so nothing else to do...
        return true;
    }

    @Override
    public void printHelp() {
        System.out.println(ENGINE_NAME + " help:");
        try {
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            log.error(e);
        }
        System.out.println();
    }

    @Override
    public void execute(Query query) {
        String queryName = query.name;
        String queryValue = query.value;
        Map<String, String> queryMetadata = query.getMetadata();
        String execType = getKey(queryMetadata, METADATA_EXEC_TYPE_KEY).orElse(defaultExecType);
        String alias = getKey(queryMetadata, METADATA_ALIAS_KEY).orElse(defaultOutputAlias);
        log.info("Running " + queryName + " for alias " + alias + ": " + queryValue);
        try {
            PigServer server = new PigServer(execType, properties);
            server.registerScript(new ByteArrayInputStream(queryValue.getBytes()));
            Iterator<Tuple> queryResults = server.openIterator(alias);
            Result result = query.createResults();
            server.shutdown();
        } catch (IOException e) {
            log.error("Problem with Pig query: " + queryName + "\n" + queryValue, e);
            query.setFailure(e.toString());
        } catch (Exception e) {
            log.error("Unexpected error occurred while executing Pig query: " + queryName + "\n" + queryValue, e);
        }
    }

    private Optional<String> getKey(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        return value == null || value.isEmpty() ? Optional.empty(): Optional.of(value);
    }

    private Properties getProperties(OptionSet options) {
        List<String> settings = (List<String>) options.valuesOf("pig-setting");
        Properties properties = new Properties();
        for (String setting : settings) {
            String[] tokens = setting.split(SETTING_DELIMITER);
            if (tokens.length != 2) {
                log.error("Ignoring unknown Pig setting provided: " + setting);
                continue;
            }
            properties.put(tokens[0], tokens[1]);
        }
        return properties;
    }

}
