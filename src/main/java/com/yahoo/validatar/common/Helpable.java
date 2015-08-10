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
