/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar.assertion;

import com.yahoo.validatar.common.Column;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.Test;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.List;

@Slf4j
public class Assertor {
    /**
     * Takes a Results object and a List of Test, performs the assertions and updates the Tests with the results.
     *
     * @param results A {@link List} of {@link Result} object containing the results of the queries.
     * @param tests   A {@link List} of {@link Test} using these results.
     */
    public static void assertAll(List<Result> results, List<Test> tests) {
        tests.stream().forEach(t -> checkAssertions(results, t));
    }

    private static void checkAssertions(List<Result> results, Test test) {
        List<String> assertions = test.asserts;
        // Check for invalid input
        if (assertions == null || assertions.size() == 0) {
            test.setFailed();
            test.addMessage("No assertion was provided!");
            return;
        }
        AssertVisitor visitor = new AssertVisitor(results);
        assertions.stream().forEach(a -> checkAssertion(a, visitor, test));
    }

    private static void checkAssertion(String assertion, AssertVisitor visitor, Test test) {
        log.info("Running assertion: {}", assertion);
        try {
            CharStream in = CharStreams.fromString(assertion);
            GrammarLexer lexer = new GrammarLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            GrammarParser parser = new GrammarParser(tokens);

            Expression expression = visitor.visit(parser.statement());
            // This expression will evaluate to a boolean Column of true or false TypedObjects. It needs no data.
            Column result = expression.evaluate();

            if (hasFailures(result)) {
                log.info("Assertion failed. Result had false values: {}", result);
                test.setFailed();
                test.addMessage(assertion + " was false for these values " + visitor.getExaminedColumns());
            }
        } catch (Exception e) {
            test.setFailed();
            test.addMessage(assertion + " : " + e.toString());
            log.error("Assertion failed with exception", e);
        } finally {
            visitor.reset();
        }
    }

    private static boolean hasFailures(Column result) {
        return result.stream().anyMatch(t -> !((Boolean) t.data));
    }
}
