package com.yahoo.validatar.common;

import com.yahoo.validatar.OutputCaptor;
import joptsimple.OptionParser;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;

public class HelpableTest {
    @Test(expectedExceptions = {RuntimeException.class})
    public void testFailPrintHelp() throws IOException {
        OptionParser mocked = Mockito.mock(OptionParser.class);
        Mockito.doThrow(new IOException()).when(mocked).printHelpOn(Mockito.any(OutputStream.class));
        OutputCaptor.runWithoutOutput(() -> Helpable.printHelp("", mocked));
    }
}
