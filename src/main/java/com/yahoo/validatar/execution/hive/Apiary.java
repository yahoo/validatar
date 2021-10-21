/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.execution.hive;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.execution.Engine;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

@Slf4j
public class Apiary implements Engine {
    public static final String HIVE_JDBC = "hive-jdbc";
    public static final String HIVE_DRIVER = "hive-driver";
    public static final String HIVE_USERNAME = "hive-username";
    public static final String HIVE_PASSWORD = "hive-password";
    public static final String HIVE_SETTING = "hive-setting";

    public static final String ENGINE_NAME = "hive";

    public static final String DRIVER_NAME = "org.apache.hive.jdbc.HiveDriver";
    public static final String SETTING_PREFIX = "set ";

    protected Connection connection;
    protected OptionSet options;

    private final OptionParser parser = new OptionParser() {
        {
            accepts(HIVE_JDBC, "JDBC string to the HiveServer2 with an optional database. " +
                                                 "If the database is provided, the queries must NOT have one. " +
                                                 "Ex: 'jdbc:hive2://HIVE_SERVER:PORT/[DATABASE_FOR_ALL_QUERIES]' ")
                .withRequiredArg()
                .required()
                .describedAs("Hive JDBC connector");
            accepts(HIVE_DRIVER, "Fully qualified package name to the hive driver.")
                .withRequiredArg()
                .describedAs("Hive driver")
                .defaultsTo(DRIVER_NAME);
            accepts(HIVE_USERNAME, "Hive server username.")
                .withRequiredArg()
                .describedAs("Hive server username")
                .defaultsTo("anon");
            accepts(HIVE_PASSWORD, "Hive server password.")
                .withRequiredArg()
                .describedAs("Hive server password")
                .defaultsTo("anon");
            accepts(HIVE_SETTING, "Settings and their values. Ex: 'hive.execution.engine=mr'")
                .withRequiredArg()
                .describedAs("Hive generic settings to use.");
            allowsUnrecognizedOptions();
        }
    };

    @Override
    public boolean setup(String[] arguments) {
        options = parser.parse(arguments);
        try {
            connection = setupConnection();
        } catch (ClassNotFoundException | SQLException e) {
            log.error("Could not set up the Hive engine", e);
            return false;
        }
        return true;
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("Hive engine options", parser);
    }

    @Override
    public void execute(Query query) {
        String queryName = query.name;
        String queryValue = query.value;
        log.info("Running {}: {}", queryName, queryValue);
        try (Statement statement = connection.createStatement()) {
            setHiveSettings(statement);
            ResultSet result = statement.executeQuery(queryValue);
            ResultSetMetaData metadata = result.getMetaData();
            int columns = metadata.getColumnCount();

            Result queryResult = query.createResults();

            addHeader(metadata, columns, queryResult);
            while (result.next()) {
                addRow(result, metadata, columns, queryResult);
            }
            result.close();
        } catch (SQLException e) {
            log.error("SQL problem with Hive query: {}\n{}\n{}", queryName, queryValue, e);
            query.setFailure(e.getMessage());
        }
    }

    private void addHeader(ResultSetMetaData metadata, int columns, Result queryResult) throws SQLException {
        for (int i = 1; i < columns + 1; i++) {
            String name = metadata.getColumnName(i);
            queryResult.addColumn(name);
        }
    }

    private void addRow(ResultSet result, ResultSetMetaData metadata, int columns, Result storage) throws SQLException {
        for (int i = 1; i < columns + 1; i++) {
            // The name and type getting is being done per row. We should fix it even though Hive gets it only once.
            String name = metadata.getColumnName(i);
            int type = metadata.getColumnType(i);
            TypedObject value = getAsTypedObject(result, i, type);
            storage.addColumnRow(name, value);
            log.info("Column: {}\tType: {}\tValue: {}", name, type, (value == null ? "null" : value.data));
        }
    }

    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    /**
     * Takes a value and its type and returns it as the appropriate TypedObject.
     *
     * @param results The ResultSet that has a confirmed value for reading by its iterator.
     * @param index   The index of the column in the results to get.
     * @param type    The java.sql.TypesSQL type of the value.
     * @return A non-null TypedObject representation of the value or null if the result was null.
     * @throws java.sql.SQLException if any.
     */
    TypedObject getAsTypedObject(ResultSet results, int index, int type) throws SQLException {
        if (results.getObject(index) == null || results.wasNull()) {
            return null;
        }

        TypedObject toReturn;
        switch (type) {
            case (Types.DATE):
            case (Types.CHAR):
            case (Types.VARCHAR):
                toReturn = TypeSystem.asTypedObject(results.getString(index));
                break;
            case (Types.FLOAT):
            case (Types.DOUBLE):
                toReturn = TypeSystem.asTypedObject(results.getDouble(index));
                break;
            case (Types.BOOLEAN):
                toReturn = TypeSystem.asTypedObject(results.getBoolean(index));
                break;
            case (Types.TINYINT):
            case (Types.SMALLINT):
            case (Types.INTEGER):
            case (Types.BIGINT):
                toReturn = TypeSystem.asTypedObject(results.getLong(index));
                break;
            case (Types.DECIMAL):
                toReturn = TypeSystem.asTypedObject(results.getBigDecimal(index));
                break;
            case (Types.TIMESTAMP):
                toReturn = TypeSystem.asTypedObject(results.getTimestamp(index));
                break;
            case (Types.NULL):
                toReturn = null;
                break;
            default:
                throw new UnsupportedOperationException("Unknown SQL type encountered from Hive: " + type);
        }
        return toReturn;
    }

    /**
     * Sets up the connection using JDBC.
     *
     * @return The created {@link java.sql.Statement} object.
     * @throws java.lang.ClassNotFoundException if any.
     * @throws java.sql.SQLException            if any.
     */
    Connection setupConnection() throws ClassNotFoundException, SQLException {
        // Load the JDBC driver
        String driver = (String) options.valueOf(HIVE_DRIVER);
        log.info("Loading JDBC driver: {}", driver);
        Class.forName(driver);

        // Get the JDBC connector
        String jdbcConnector = (String) options.valueOf(HIVE_JDBC);

        log.info("Connecting to: {}", jdbcConnector);
        String username = (String) options.valueOf(HIVE_USERNAME);
        String password = (String) options.valueOf(HIVE_PASSWORD);

        // Start the connection
        return DriverManager.getConnection(jdbcConnector, username, password);
    }

    /**
     * Applies any settings if provided.
     *
     * @param statement A {@link java.sql.Statement} to execute the setting updates to.
     * @throws java.sql.SQLException if any.
     */
    void setHiveSettings(Statement statement) throws SQLException {
        for (String setting : (List<String>) options.valuesOf(HIVE_SETTING)) {
            log.info("Applying setting {}", setting);
            statement.executeUpdate(SETTING_PREFIX + setting);
        }
    }
}
