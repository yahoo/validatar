/*
 * Copyright 2014-2016 Yahoo! Inc.
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

package com.yahoo.validatar.common;

import joptsimple.OptionParser;

import java.io.IOException;

/**
 * Any class that needs to print help should implement this.
 */
public interface Helpable {
    /**
     * Prints help to System.out.
     */
    void printHelp();

    /**
     * Prints the parser's help with the given header.
     * @param header A String header to write.
     * @param parser A @link {joptsimple.OptionParser} parser that will be used to print help to System.out.
     */
    static void printHelp(String header, OptionParser parser) {
        System.out.println("\n" + header + ":");
        try {
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println();
    }
}
