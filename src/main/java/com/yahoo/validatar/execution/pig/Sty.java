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

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.execution.Engine;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Logger;
import org.apache.pig.PigServer;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class Sty implements Engine {
    public static final String PIG_EXEC_TYPE = "pig-exec-type";
    public static final String PIG_OUTPUT_ALIAS = "pig-output-alias";
    public static final String PIG_SETTING = "pig-setting";

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

    private class FieldDetail {
        public String alias;
        public byte type;

        public FieldDetail(String alias, byte type) {
            this.alias = alias;
            this.type = type;
        }
    }

    private OptionParser parser = new OptionParser() {
        {
            acceptsAll(singletonList(PIG_EXEC_TYPE), "The exec-type for Pig to use.  This is the " +
                                                       "-x argument used when running Pig. Ex: local, mr, tez etc. ")
                .withRequiredArg()
                .describedAs("Pig execution type")
                .defaultsTo(DEFAULT_EXEC_TYPE);
            acceptsAll(singletonList(PIG_OUTPUT_ALIAS), "The default name of the alias where the result is." +
                                                          "This should contain the data that will be collected")
                .withRequiredArg()
                .describedAs("Pig default output alias")
                .defaultsTo(DEFAULT_OUTPUT_ALIAS);
            acceptsAll(singletonList(PIG_SETTING), "Settings and their values. The -D params that would " +
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
        defaultExecType = (String) options.valueOf(PIG_EXEC_TYPE);
        defaultOutputAlias = (String) options.valueOf(PIG_OUTPUT_ALIAS);
        properties = getProperties(options);
        // We will boot up a PigServer per query, so nothing else to do...
        return true;
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("Pig engine options", parser);
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
            // dumpSchema will also, unfortunately, print the schema to stdout.
            List<FieldDetail> metadata = getFieldDetails(server.dumpSchema(alias));
            populateColumns(metadata, result);
            while (queryResults.hasNext()) {
                populateRow(queryResults.next(), metadata, result);
            }
            server.shutdown();
        } catch (IOException ioe) {
            log.error("Problem with Pig query: \n" + queryValue, ioe);
            query.setFailure(ioe.toString());
        } catch (Exception e) {
            log.error("Error occurred while processing Pig query: " + queryValue, e);
            query.setFailure(e.toString());
        }
    }

    private void populateColumns(List<FieldDetail> metadata, Result result) throws IOException {
        if (metadata.isEmpty()) {
            throw new IOException("No metadata of columns found for Pig query");
        }
        metadata.forEach(m -> result.addColumn(m.alias));
    }

    private void populateRow(Tuple row, List<FieldDetail> metadata, Result result) throws ExecException {
        if (row == null) {
            log.info("Skipping null row in results...");
            return;
        }
        for (int i = 0; i < metadata.size(); ++i) {
            FieldDetail column = metadata.get(i);
            TypedObject value = getTypedObject(row.get(i), column);
            log.info("Column: " + column.alias + "\tType: " + column.type +
                     "\tValue: " + (value == null ? "null" : value.data));
            result.addColumnRow(column.alias, value);
        }
    }

    TypedObject getTypedObject(Object data, FieldDetail detail) throws ExecException {
        byte type = detail.type;
        switch (type) {
            case DataType.BOOLEAN:
                return TypeSystem.asTypedObject(DataType.toBoolean(data, type));
            case DataType.INTEGER:
            case DataType.LONG:
                return TypeSystem.asTypedObject(DataType.toLong(data, type));
            case DataType.FLOAT:
            case DataType.DOUBLE:
                return TypeSystem.asTypedObject(DataType.toDouble(data, type));
            case DataType.DATETIME:
                return TypeSystem.asTypedObject(new Timestamp(DataType.toDateTime(data, type).getMillis()));
            case DataType.BYTE:
            case DataType.BYTEARRAY:
            case DataType.CHARARRAY:
                return TypeSystem.asTypedObject(DataType.toString(data, type));
            case DataType.BIGINTEGER:
            case DataType.BIGDECIMAL:
                return TypeSystem.asTypedObject(DataType.toBigDecimal(data, type));
            case DataType.TUPLE:
            case DataType.BAG:
            case DataType.MAP:
            case DataType.INTERNALMAP:
            case DataType.GENERIC_WRITABLECOMPARABLE:
            case DataType.ERROR:
            case DataType.UNKNOWN:
            case DataType.NULL:
            default:
                return null;
        }
    }

    List<FieldDetail> getFieldDetails(Schema schema) {
        if (schema == null) {
            return Collections.emptyList();
        }
        return schema.getFields().stream().map(f -> new FieldDetail(f.alias, f.type)).collect(Collectors.toList());
    }

    Optional<String> getKey(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        return value == null || value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    Properties getProperties(OptionSet options) {
        List<String> settings = (List<String>) options.valuesOf(PIG_SETTING);
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
