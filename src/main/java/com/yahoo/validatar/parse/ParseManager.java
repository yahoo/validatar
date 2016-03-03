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

package com.yahoo.validatar.parse;

import com.yahoo.validatar.common.Helpable;
import com.yahoo.validatar.common.Pluggable;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.parse.yaml.YAML;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ParseManager extends Pluggable<Parser> implements FileLoadable, Helpable {
    public static final String CUSTOM_PARSERS = "custom-parsers";
    public static final String CUSTOM_PARSER_DESCRIPTION = "Additional custom parsers to use.";

    /**
     * The Parser classes to manage.
     */
    public static final List<Class<? extends Parser>> MANAGED_PARSERS = Arrays.asList(YAML.class);

    public static final Pattern REGEX = Pattern.compile("\\$\\{(.*?)\\}");

    private final HashMap<String, Parser> availableParsers;

    /**
     * Constructor. Default.
     */
    public ParseManager(String[] arguments) {
        super(MANAGED_PARSERS, CUSTOM_PARSERS, CUSTOM_PARSER_DESCRIPTION);
        availableParsers = new HashMap<>();
        for (Parser parser : getPlugins(arguments)) {
            availableParsers.put(parser.getName(), parser);
            log.info("Setup parser {}", parser.getName());
        }
    }

    @Override
    public List<TestSuite> load(File path) {
        return getFiles(path).map(this::getTestSuite).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Stream<File> getFiles(File path) {
        if (path == null) {
            return Stream.empty();
        }
        if (path.isFile()) {
            log.info("TestSuite parameter is a file, loading...");
            return Stream.of(path);
        }
        log.info("TestSuite parameter is a folder, loading all files inside...");
        File[] files = path.listFiles();
        return files == null ? Stream.empty() : Stream.of(files).sorted();
    }

    /**
     * Takes a List of non null TestSuite and a map and replaces all the variables in the query with the value
     * in the map, in place.
     *
     * @param suites       A list of TestSuites containing parametrized queries.
     * @param parameterMap A non null map of parameters to their values.
     */
    public static void expandParameters(List<TestSuite> suites, Map<String, String> parameterMap) {
        Objects.requireNonNull(suites);

        suites.stream().filter(Objects::nonNull).map(s -> s.queries)
        .flatMap(Collection::stream).filter(Objects::nonNull)
        .forEach(q -> expandParameters(q, parameterMap));
    }

    /**
     * Takes a non null Query and replaces all the variables in the query
     * with the value in the map, in place.
     *
     * @param query        A query that is parametrized.
     * @param parameterMap A map of parameters to their values.
     */
    public static void expandParameters(Query query, Map<String, String> parameterMap) {
        Objects.requireNonNull(query);
        Objects.requireNonNull(parameterMap);
        Matcher matcher = REGEX.matcher(query.value);
        StringBuffer newQuery = new StringBuffer();
        while (matcher.find()) {
            String parameterValue = parameterMap.get(matcher.group(1));
            if (parameterValue != null) {
                matcher.appendReplacement(newQuery, parameterValue);
            }
        }
        matcher.appendTail(newQuery);
        query.value = newQuery.toString();
    }

    /**
     * Takes a non null File and parses a TestSuite out of it.
     *
     * @param path A non null File object representing the file.
     * @return The parsed TestSuite from the file. Null if it cannot be parsed.
     */
    protected TestSuite getTestSuite(File path) {
        Objects.requireNonNull(path);
        if (!path.isFile()) {
            log.error("Path {} is not a file.", path);
            return null;
        }
        Parser parser = availableParsers.get(getFileExtension(path.getName()));
        if (parser == null) {
            log.error("Unable to parse {}. File extension does not match any known parsers. Skipping...", path);
            return null;
        }
        try {
            return parser.parse(new FileInputStream(path));
        } catch (Exception e) {
            log.error("Could not parse the TestSuite", e);
            return null;
        }
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return (index > 0) ? fileName.substring(index + 1) : null;
    }

    @Override
    public void printHelp() {
        Helpable.printHelp("Advanced Options", getPluginOptionsParser());
    }
}
