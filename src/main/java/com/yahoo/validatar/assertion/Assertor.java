/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.assertion;

import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.Test;
import com.yahoo.validatar.common.TypedObject;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Assertor {
    /**
     * Takes a Results object and a List of Test, performs the assertions
     * and updates the Tests with the results.
     *
     * @param results A Result object containing the results of the queries.
     * @param tests   A list of Test using these results
     */
    public static void assertAll(Result results, List<Test> tests) {
        // IMPORTANT!
        // Only interpreting as a single row result set. Temporary, will re-enable later
        Map<String, TypedObject> singleRow = getAsOneEntry(results);
        tests.stream().forEach(t -> assertOne(singleRow, t));
    }

    private static void assertOne(Map<String, TypedObject> row, Test test) {
        List<String> assertions = test.asserts;
        // Check for invalid input
        if (assertions == null || assertions.size() == 0) {
            test.setFailed();
            test.addMessage("No assertion was provided!");
            return;
        }
        assertions.stream().forEach(a -> assertOneAssertion(a, row, test));
    }

    private static void assertOneAssertion(String assertion, Map<String, TypedObject> row, Test test) {
        log.info("Running assertion: {}", assertion);
        try {
            ANTLRInputStream in = new ANTLRInputStream(assertion);
            GrammarLexer lexer = new GrammarLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            GrammarParser parser = new GrammarParser(tokens);
            parser.setCurrentRow(row);
            if (!parser.expression().value) {
                test.setFailed();
                test.addMessage(assertion + " was false for these values " + parser.getLookedUpValues());
            }
        } catch (Exception e) {
            test.setFailed();
            test.addMessage(assertion + " : " + e.toString());
            log.error("Assertion failed with exception", e);
        }
    }

    private static Map<String, TypedObject> getAsOneEntry(Result results) {
        // Need an actual collector instead of Collectors.toMap since merge doesn't work with null values.
        return results.getColumns().entrySet().stream()
               .collect(HashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue().size() == 0 ? null : e.getValue().get(0)),
                        HashMap::putAll);
    }
}

