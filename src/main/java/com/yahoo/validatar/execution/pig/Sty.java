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

import com.yahoo.validatar.execution.Engine;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem;
import com.yahoo.validatar.common.TypedObject;

import static java.util.Arrays.*;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.log4j.Logger;

import java.io.IOException;

public class Sty implements Engine {
    protected final Logger log = Logger.getLogger(getClass());

    /** Engine name. */
    public static final String ENGINE_NAME = "pig";


    private OptionParser parser = new OptionParser() {
        {
            acceptsAll(asList("pig-setting"), "Settings and their values. Ex: 'mapreduce.job.acl-view-job=*'")
                    .withRequiredArg()
                    .describedAs("Pig generic settings to use.");
            allowsUnrecognizedOptions();
        }
    };

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setup(String[] arguments) {
        OptionSet options = parser.parse(arguments);
        return true;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public void execute(Query query) {
        Result result = query.createResults();
    }
}
