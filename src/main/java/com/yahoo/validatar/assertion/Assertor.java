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
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypedObject;

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
     * @param results A Result object containing the results of the queries.
     * @param tests List<Test> using these columns
     */
    public static void assertAll(Result results, List<Test> tests) {
        for (Test test : tests) {
            assertOneTest(results, test);
        }
    }

    private static void assertOneTest(Result results, Test test) {
        List<String> assertions = test.asserts;

        // Check for invalid input
        if (assertions == null || assertions.size() == 0) {
            test.setFailed();
            test.addMessage("No assertion was provided!");
            return;
        }

        for (String assertion : assertions) {
            assertOneAssertion(assertion, results, test);
        }
    }

    private static void assertOneAssertion(String assertion, Result results, Test test) {
        try {
            ANTLRInputStream in = new ANTLRInputStream(assertion);
            GrammarLexer lexer = new GrammarLexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            GrammarParser parser = new GrammarParser(tokens);
            parser.setCurrentRow(getAsOneEntry(results));
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

    private static Map<String, TypedObject> getAsOneEntry(Result results) {
        // IMPORTANT!
        // Only interpreting as a single row result set. Temporary, will re-enable later
        Map<String, TypedObject> row = new HashMap<>();
        for (Map.Entry<String, List<TypedObject>> e : results.getColumns().entrySet()) {
            row.put(e.getKey(), e.getValue().get(0));
        }
        return row;
    }

}

