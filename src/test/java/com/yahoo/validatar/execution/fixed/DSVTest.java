package com.yahoo.validatar.execution.fixed;

import com.yahoo.validatar.common.Column;
import com.yahoo.validatar.common.Metadata;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.Result;
import com.yahoo.validatar.common.TypeSystem.Type;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.yahoo.validatar.OutputCaptor.runWithoutOutput;
import static com.yahoo.validatar.TestHelpers.asColumn;
import static com.yahoo.validatar.TestHelpers.getQueryFrom;
import static com.yahoo.validatar.TestHelpers.isEqual;
import static java.util.Collections.singletonMap;

public class DSVTest {
    private final String[] defaults = {"--csv-delimiter", ","};
    private DSV dsv;

    @BeforeMethod
    public void setup() {
        dsv = new DSV();
        dsv.setup(defaults);
    }

    @Test
    public void testDefaults() {
        Assert.assertTrue(dsv.setup(new String[0]));
        Assert.assertEquals(dsv.getName(), DSV.ENGINE_NAME);
        runWithoutOutput(dsv::printHelp);
    }

    @Test
    public void testEmptyQuery() {
        Query query = new Query();
        query.value = "";
        dsv.execute(query);

        Assert.assertFalse(query.failed());
        Assert.assertEquals(query.getResult().numberOfRows(), 0);
        Assert.assertEquals(query.getResult().getColumns().size(), 0);
    }

    @Test
    public void testMissingValuesForStrings() {
        Query query = new Query();
        query.value = "foo,bar\n0,1\n,3";
        dsv.execute(query);

        Assert.assertFalse(query.failed());
        Column foo = query.getResult().getColumn("foo");
        Column bar = query.getResult().getColumn("bar");
        Assert.assertEquals(foo.get(0).data, "0");
        Assert.assertEquals(foo.get(1).data, "");
        Assert.assertEquals(bar.get(0).data, "1");
        Assert.assertEquals(bar.get(1).data, "3");
    }

    @Test
    public void testMissingValuesForNonStrings() {
        Query query = new Query();

        query.metadata = new ArrayList<>();
        Metadata metadata = new Metadata();
        metadata.key = "foo";
        metadata.value = Type.LONG.name();
        query.metadata.add(metadata);

        query.value = "foo,bar\n0,1\n,3";

        dsv.execute(query);

        Assert.assertFalse(query.failed());
        Column foo = query.getResult().getColumn("foo");
        Column bar = query.getResult().getColumn("bar");
        Assert.assertEquals(foo.get(0).data, 0L);
        Assert.assertNull(foo.get(1));
        Assert.assertEquals(bar.get(0).data, "1");
        Assert.assertEquals(bar.get(1).data, "3");
    }

    @Test
    public void testDuplicateHeader() {
        Query query = new Query();
        query.value = ",";
        dsv.execute(query);

        Assert.assertTrue(query.failed());
        Assert.assertTrue(query.getMessages().get(0).contains("header contains a duplicate"));
    }

    @Test
    public void testBadPath() {
        Query query = new Query();
        query.value = "src/test/resources/csv-tests";
        dsv.execute(query);

        Assert.assertFalse(query.failed());
        Assert.assertEquals(query.getResult().numberOfRows(), 0);
        Assert.assertEquals(query.getResult().getColumns().size(), 0);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*Header row not found.*")
    public void testNoHeader() {
        DSV.getTypeMapping(null, null);
    }

    @Test
    public void testTypeMapping() {
        Map<String, Integer> headers = new HashMap<>();
        headers.put("foo", 0);
        headers.put("bar", 1);
        headers.put("baz", 2);
        headers.put("qux", 3);

        Map<String, String> mapping = new HashMap<>();
        mapping.put("foo", "DECIMAL");
        mapping.put("baz", "TIMESTAMP");

        Map<String, Type> result = DSV.getTypeMapping(headers, mapping);
        Assert.assertEquals(result.get("foo"), Type.DECIMAL);
        Assert.assertEquals(result.get("bar"), Type.STRING);
        Assert.assertEquals(result.get("baz"), Type.TIMESTAMP);
        Assert.assertEquals(result.get("qux"), Type.STRING);
    }

    @Test
    public void testForcedDefaultTypeMapping() {
        Map<String, Type> result = DSV.getTypeMapping(singletonMap("foo", 0), singletonMap("foo", "GARBAGE"));
        Assert.assertEquals(result.get("foo"), Type.STRING);
    }

    @Test
    public void testStringLoading() throws IOException {
        Query query = getQueryFrom("csv-tests/sample.yaml", "StringTest");
        dsv.execute(query);

        Result actual = query.getResult();
        Result expected = new Result("StringTest");
        expected.addColumn("A", asColumn(Type.STRING, "foo", "baz", "foo"));
        expected.addColumn("B", asColumn(Type.DOUBLE, 234.3, 9.0, 42.0));
        expected.addColumn("C", asColumn(Type.STRING, "bar", "qux", "norf"));

        Assert.assertTrue(isEqual(actual, expected));
    }

    @Test
    public void testFileLoading() throws IOException {
        Query query = getQueryFrom("csv-tests/sample.yaml", "FileLoadingTest");
        dsv.execute(query);

        Result actual = query.getResult();
        Result expected = new Result("FileLoadingTest");
        // We should have skipped a row that contained more fields than the header
        // We should have nulled out a field that was missing a value but was not a STRING.
        // We should have used an empty STRING for a missing field that was of type STRING
        expected.addColumn("fieldA", asColumn(Type.STRING, "foo", "baz", "qux", "bar"));
        expected.addColumn("fieldB", asColumn(Type.LONG, 15L, 42L, 0L, null));
        expected.addColumn("fieldC", asColumn(Type.DECIMAL, new BigDecimal("34.2"), new BigDecimal("4.2"),
                                                            new BigDecimal("8.4"), new BigDecimal("0.2")));
        expected.addColumn("fieldD", asColumn(Type.STRING, "bar", "bar", "norf", ""));

        Assert.assertTrue(isEqual(actual, expected));
    }

    @Test
    public void testCustomDelimiter() throws IOException {
        Query query = getQueryFrom("csv-tests/sample.yaml", "CustomDelim");
        dsv.execute(query);

        Result actual = query.getResult();
        Result expected = new Result("CustomDelim");
        expected.addColumn("A", asColumn(Type.STRING, "foo", "baz", "foo"));
        expected.addColumn("B", asColumn(Type.STRING, "234.3", "9", "42"));
        expected.addColumn("C", asColumn(Type.STRING, "bar", "qux", "norf"));

        Assert.assertTrue(isEqual(actual, expected));
    }
}