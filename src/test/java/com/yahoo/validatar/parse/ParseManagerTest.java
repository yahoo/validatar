package com.yahoo.validatar.parse;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ParseManagerTest {
    @Test
    public void testFailLoadOfNonFile() {
        ParseManager manager = new ParseManager();
        TestSuite testSuite = manager.getTestSuite(new File("src/test/resources"));
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfUnknownFile() {
        ParseManager manager = new ParseManager();
        TestSuite testSuite = manager.getTestSuite(new File("src/test/resources/log4j.properties"));
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfBadExtensionFile() {
        ParseManager manager = new ParseManager();
        TestSuite testSuite = manager.getTestSuite(new File("src/test/resources/log4j.properties"));
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfNoExtensionFile() {
        ParseManager manager = new ParseManager();
        TestSuite testSuite = manager.getTestSuite(new File("LICENSE"));
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfDisappearingFile() {
        ParseManager manager = new ParseManager();
        File mocked = Mockito.mock(File.class);
        Mockito.when(mocked.isFile()).thenReturn(true);
        Mockito.when(mocked.getName()).thenReturn("foo.yaml");
        Mockito.when(mocked.getPath()).thenReturn(null);
        TestSuite testSuite = manager.getTestSuite(mocked);
        Assert.assertNull(testSuite);
    }

    @Test
    public void testFailLoadOfNullPath() {
        ParseManager manager = new ParseManager();
        List<TestSuite> loaded = manager.load(null);
        Assert.assertTrue(loaded.isEmpty());
    }

    @Test
    public void testFailLoadOfEmptyDirectory() {
        ParseManager manager = new ParseManager();
        File mocked = Mockito.mock(File.class);
        Mockito.when(mocked.isFile()).thenReturn(false);
        Mockito.when(mocked.listFiles()).thenReturn(null);
        List<TestSuite> loaded = manager.load(mocked);
        Assert.assertTrue(loaded.isEmpty());
    }

    @Test
    public void testLoadOfDirectory() {
        ParseManager manager = new ParseManager();
        List<TestSuite> loaded = manager.load(new File("src/test/resources/sample-tests"));
        Assert.assertEquals(loaded.size(), 3);
    }

    @Test
    public void testFailExpansion() {
        Query query = new Query();
        query.value = "${var}";
        ParseManager manager = new ParseManager();
        manager.expandParameters(query, Collections.emptyMap());
        Assert.assertEquals(query.value, "${var}");
    }

    @Test
    public void testExpansion() {
        Query query = new Query();
        query.value = "${var}";
        ParseManager manager = new ParseManager();
        manager.expandParameters(query, Collections.singletonMap("var", "foo"));
        Assert.assertEquals(query.value, "foo");
    }
}
