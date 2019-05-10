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
import java.util.Set;

@Slf4j
public class Assertor {
    public static final String RESULT_COLUMN = "";
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
                Set<String> columnsSeen = visitor.getSeenIdentifiers();
                Result joined = visitor.getJoinedResult();
                Result releventData = Result.copy(joined, columnsSeen);
                releventData.addQualifiedColumn(RESULT_COLUMN, result);

                String assertionMessage = "Assertion " + assertion + " was false";
                String resultsMessage = "Result had false values: " + result;
                String columnsMessage = "Examined columns: " + columnsSeen;
                String relevantColumnsMessage = "Relevant column data used: \n" + releventData.prettyPrint();
                String dataMessage = "All Result data used: \n" + joined.prettyPrint();

                test.setFailed();
                test.addMessage(assertionMessage);
                test.addMessage(resultsMessage);
                test.addMessage(columnsMessage);
                test.addMessage(dataMessage);
                test.addMessage(relevantColumnsMessage);
                log.info("{}\n{}\n{}\n{}\n{}\n", assertionMessage, resultsMessage, columnsMessage, dataMessage, relevantColumnsMessage);
            }
        } catch (Exception e) {
            test.setFailed();
            String dataMessage = "Data used: \n" + visitor.getJoinedResult().prettyPrint();
            test.addMessage(assertion + " failed with exception: " + e.getMessage());
            test.addMessage(dataMessage);
            log.error("Assertion failed with exception", e);
            log.error("\n{}", dataMessage);
        } finally {
            visitor.reset();
        }
    }

    private static boolean hasFailures(Column result) {
        return result.stream().anyMatch(t -> !((Boolean) t.data));
    }
}
