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

package com.yahoo.validatar.assertion;

import com.yahoo.validatar.common.Test;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ANTLRInputStream;

public class Assertor {

    /**
     * Takes a Map of column names to column values and list of asserts and performs the assertions
     * and places the results into the tests.
     *
     * @param columns Column name to Column values
     * @param tests List<Test> using these columns
     */
    public static void assertAll(Map<String, List<String>> columns, List<Test> tests) {
        Map<String, String> singleRowColumns = getAsOneEntry(columns);
        for (Test test : tests) {
            assertOneTest(singleRowColumns, test);
        }
    }

    private static Map<String, String> getAsOneEntry(Map<String, List<String>> data) {
        // IMPORTANT!
        // Only interpreting as a single row result set. Temporary, will re-enable later
        Map<String, String> row = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> e : data.entrySet()) {
            row.put(e.getKey(), e.getValue().get(0));
        }
        return row;
    }

    private static void assertOneTest(Map<String, String> columns, Test test) {
        List<String> assertions = test.asserts;

        // Check for invalid input
        if (assertions == null || assertions.size() == 0) {
            test.setFailed();
            test.addMessage("NULL assertion! : No assertion was provided!");
            return;
        }

        for (String assertion : assertions) {
            assertOneAssertion(assertion, columns, test);
        }
    }

    private static void assertOneAssertion(String assertion, Map<String, String> row, Test test) {
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
            String message = e.toString();
            test.setFailed();
            test.addMessage(assertion + " : " + e.toString());
        }
    }
}

