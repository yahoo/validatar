/*
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache 2 license. Please see LICENSE file in the project root for terms.
 */
package com.yahoo.validatar;

import org.apache.commons.io.output.NullOutputStream;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class OutputCaptor {
    public static final PrintStream NULL = new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM);
    public static final PrintStream OUT = new PrintStream(new FileOutputStream(FileDescriptor.out));
    public static final PrintStream ERR = new PrintStream(new FileOutputStream(FileDescriptor.err));

    public static void redirectToDevNull() {
        System.setOut(NULL);
        System.setErr(NULL);
    }

    public static void redirectToStandard() {
        System.setOut(OUT);
        System.setErr(ERR);
    }

    public static void runWithoutOutput(Runnable function) {
        redirectToDevNull();
        function.run();
        redirectToStandard();
    }
}
