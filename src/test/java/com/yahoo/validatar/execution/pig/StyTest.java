package com.yahoo.validatar.execution.pig;

import com.yahoo.validatar.common.Metadata;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.parse.yaml.YAML;
import org.apache.pig.PigServer;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.joda.time.DateTime;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.yahoo.validatar.OutputCaptor.runWithoutOutput;

public class StyTest {
    public static final double EPSILON = 0.00001;

    private final String[] defaults = {"--pig-exec-type", "local"};

    private Sty sty;

    private Schema.FieldSchema makeFieldSchema(String alias, byte type) {
        return new Schema.FieldSchema(alias, type);
    }

    private Tuple makeTuple(Object... contents) throws ExecException {
        Tuple mocked = Mockito.mock(Tuple.class);
        if (contents == null)  {
            Mockito.when(mocked.get(0)).thenReturn(null);
            return mocked;
        }
        for (int i = 0; i < contents.length; ++i) {
            Mockito.when(mocked.get(i)).thenReturn(contents[i]);
        }
        return mocked;
    }

    private PigServer getServer() throws IOException {
        return getServer("local", new Properties());
    }

    private PigServer getServer(String execType, Properties properties) throws IOException {
        PigServer server = new PigServer(execType, properties);
        return Mockito.spy(server);
    }

    private Schema getSchema(Schema.FieldSchema... schema) {
        Schema mocked = Mockito.mock(Schema.class);
        Mockito.when(mocked.getFields()).thenReturn(Arrays.asList(schema));
        return mocked;
    }

    private PigServer withMockSchema(PigServer server, Schema schema) throws IOException {
        Mockito.doReturn(schema).when(server).dumpSchema(Mockito.anyString());
        return server;
    }

    private PigServer withMockResult(PigServer server, Tuple... results) throws IOException {
        Mockito.doReturn(results == null ? null : Arrays.asList(results).iterator())
               .when(server).openIterator(Mockito.anyString());
        return server;
    }

    private Sty getSty(PigServer server) throws IOException {
        Sty sty = new Sty();
        Sty spiedSty = Mockito.spy(sty);
        Mockito.doReturn(server).when(spiedSty).getPigServer(Mockito.anyString());
        return spiedSty;
    }

    private Sty getSty() {
        return new Sty();
    }

    private Query getQueryFrom(String file, String name) throws FileNotFoundException {
        File testFile = new File(getClass().getClassLoader().getResource(file).getFile());
        TestSuite testSuite = new YAML().parse(new FileInputStream(testFile));
        return testSuite.queries.stream().filter(q -> name.equals(q.name)).findAny().get();
    }

    @BeforeMethod
    public void setup() throws IOException {
        sty = getSty(getServer());
        sty.setup(defaults);
    }

    @Test
    public void testDefaults() {
        Assert.assertTrue(sty.setup(new String[0]));
        Assert.assertEquals(sty.getName(), Sty.ENGINE_NAME);
        runWithoutOutput(sty::printHelp);
    }

    @Test
    public void testNullQuery() {
        Query query = new Query();
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertTrue(query.failed());
        Assert.assertTrue(query.getMessages().get(0).contains("java.lang.NullPointerException"));
    }

    @Test
    public void testNullSchema() throws IOException {
        Query query = new Query();
        query.value = "";
        sty = getSty(withMockResult(withMockSchema(getServer(), null), (Tuple[]) null));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertTrue(query.failed());
        Assert.assertTrue(query.getMessages().get(0).contains("No metadata of columns found"));
    }

    @Test
    public void testNullTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fake = getSchema(makeFieldSchema("a", DataType.CHARARRAY));
        sty = getSty(withMockResult(withMockSchema(getServer(), fake), null, null));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> result = query.getResult().getColumn("a");
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testNullValueInTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fakeSchema = getSchema(makeFieldSchema("a", DataType.CHARARRAY));
        Tuple fakeTuple = makeTuple((Object[]) null);

        sty = getSty(withMockResult(withMockSchema(getServer(), fakeSchema), fakeTuple));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> result = query.getResult().getColumn("a");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertNull(result.get(0));
    }

    @Test
    public void testNullTypeInTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fakeSchema = getSchema(makeFieldSchema("a", DataType.NULL));
        Tuple fakeTuple = makeTuple("something");

        sty = getSty(withMockResult(withMockSchema(getServer(), fakeSchema), fakeTuple));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> result = query.getResult().getColumn("a");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertNull(result.get(0));
    }

    @Test
    public void testBooleanTypeInTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fakeSchema = getSchema(makeFieldSchema("a", DataType.BOOLEAN));
        Tuple fakeTuple = makeTuple(Boolean.valueOf(false));

        sty = getSty(withMockResult(withMockSchema(getServer(), fakeSchema), fakeTuple));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> result = query.getResult().getColumn("a");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).data, Boolean.valueOf(false));
    }

    @Test
    public void testIntegerTypeInTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fakeSchema = getSchema(makeFieldSchema("a", DataType.INTEGER),
                                      makeFieldSchema("b", DataType.LONG));
        Tuple fakeTuple = makeTuple(Integer.valueOf(42), Long.valueOf(84));

        sty = getSty(withMockResult(withMockSchema(getServer(), fakeSchema), fakeTuple));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> columnOne = query.getResult().getColumn("a");
        List<TypedObject> columnTwo = query.getResult().getColumn("b");
        Assert.assertNotNull(columnOne);
        Assert.assertEquals(columnOne.size(), 1);
        Assert.assertEquals(columnOne.get(0).data, Long.valueOf(42));
        Assert.assertNotNull(columnTwo);
        Assert.assertEquals(columnTwo.size(), 1);
        Assert.assertEquals(columnTwo.get(0).data, Long.valueOf(84));
    }

    @Test
    public void testFloatTypeInTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fakeSchema = getSchema(makeFieldSchema("a", DataType.FLOAT),
                                      makeFieldSchema("b", DataType.DOUBLE));
        Tuple fakeTuple = makeTuple(Float.valueOf(2.1f), Double.valueOf(4.2));

        sty = getSty(withMockResult(withMockSchema(getServer(), fakeSchema), fakeTuple));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> columnOne = query.getResult().getColumn("a");
        List<TypedObject> columnTwo = query.getResult().getColumn("b");
        Assert.assertNotNull(columnOne);
        Assert.assertEquals(columnOne.size(), 1);
        Assert.assertTrue(Math.abs((Double) columnOne.get(0).data - Double.valueOf(2.1)) < EPSILON);
        Assert.assertNotNull(columnTwo);
        Assert.assertEquals(columnTwo.size(), 1);
        Assert.assertTrue(Math.abs((Double) columnTwo.get(0).data - Double.valueOf(4.2)) < EPSILON);
    }

    @Test
    public void testBigNumericTypeInTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fakeSchema = getSchema(makeFieldSchema("a", DataType.BIGINTEGER),
                                      makeFieldSchema("b", DataType.BIGDECIMAL));
        Tuple fakeTuple = makeTuple(new BigInteger("42"), new BigDecimal("42.1"));

        sty = getSty(withMockResult(withMockSchema(getServer(), fakeSchema), fakeTuple));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> columnOne = query.getResult().getColumn("a");
        List<TypedObject> columnTwo = query.getResult().getColumn("b");
        Assert.assertNotNull(columnOne);
        Assert.assertEquals(columnOne.size(), 1);
        Assert.assertEquals(columnOne.get(0).data, new BigDecimal("42"));
        Assert.assertNotNull(columnTwo);
        Assert.assertEquals(columnTwo.size(), 1);
        Assert.assertEquals(columnTwo.get(0).data, new BigDecimal("42.1"));
    }

    @Test
    public void testStringTypeInTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fakeSchema = getSchema(makeFieldSchema("a", DataType.BYTE),
                                      makeFieldSchema("b", DataType.BYTEARRAY),
                                      makeFieldSchema("c", DataType.CHARARRAY));
        Tuple fakeTuple = makeTuple(Byte.valueOf("1"), new DataByteArray("foo".getBytes()), "bar");

        sty = getSty(withMockResult(withMockSchema(getServer(), fakeSchema), fakeTuple));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> columnOne = query.getResult().getColumn("a");
        List<TypedObject> columnTwo = query.getResult().getColumn("b");
        List<TypedObject> columnThree = query.getResult().getColumn("c");
        Assert.assertNotNull(columnOne);
        Assert.assertEquals(columnOne.size(), 1);
        Assert.assertEquals(columnOne.get(0).data, "1");
        Assert.assertNotNull(columnTwo);
        Assert.assertEquals(columnTwo.size(), 1);
        Assert.assertEquals(columnTwo.get(0).data, "foo");
        Assert.assertNotNull(columnThree);
        Assert.assertEquals(columnThree.size(), 1);
        Assert.assertEquals(columnThree.get(0).data, "bar");
    }

    @Test
    public void testDateTypeInTuple() throws IOException {
        Query query = new Query();
        query.value = "";
        Schema fakeSchema = getSchema(makeFieldSchema("a", DataType.DATETIME));
        Tuple fakeTuple = makeTuple(new DateTime(142151414341L));

        sty = getSty(withMockResult(withMockSchema(getServer(), fakeSchema), fakeTuple));
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
        List<TypedObject> result = query.getResult().getColumn("a");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).data, new Timestamp(142151414341L));
    }

    @Test
    public void testDefaultedQuery() throws FileNotFoundException {
        Query query = getQueryFrom("pig-tests/sample.yaml", "Defaults");

        // Loading
        Sty sty = getSty();
        sty.setup(defaults);
        runWithoutOutput(() -> sty.execute(query));

        Assert.assertFalse(query.failed());
        Map<String, List<TypedObject>> result = query.getResult().getColumns();
        Assert.assertEquals(result.size(), 2);
        List<TypedObject> names = result.get("Defaults.name");
        List<TypedObject> counts = result.get("Defaults.count");
        Assert.assertEquals(names.size(), 1);
        Assert.assertEquals(counts.size(), 1);
        Assert.assertEquals(names.get(0).data, String.valueOf("baz"));
        Assert.assertEquals(counts.get(0).data, Long.valueOf(-26));
    }

    @Test
    public void testBadProperties() throws IOException {
        String[] problemSetting = {"--pig-setting", "pig.tmpFileCompression:false",
                                   "--pig-setting", "pig.maxCombinedSplitSize=1024"};
        Assert.assertTrue(sty.setup(problemSetting));

        Query query = getQueryFrom("pig-tests/sample.yaml", "Defaults");
        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
    }

    @Test
    public void testBadMetadataQuery() throws FileNotFoundException {
        Query query = getQueryFrom("pig-tests/sample.yaml", "Defaults");
        // Null value
        Metadata badOne = new Metadata();
        badOne.key = Sty.METADATA_ALIAS_KEY;
        badOne.value = null;
        // Empty value
        Metadata badTwo = new Metadata();
        badOne.key = Sty.METADATA_ALIAS_KEY;
        badOne.value = "";
        query.metadata = Arrays.asList(badOne, badTwo);

        runWithoutOutput(() -> sty.execute(query));
        Assert.assertFalse(query.failed());
    }

    @Test
    public void testMetadataQuery() throws FileNotFoundException {
        Query query = getQueryFrom("pig-tests/sample.yaml", "Query");

        runWithoutOutput(() -> sty.execute(query));

        Assert.assertFalse(query.failed());
        Map<String, List<TypedObject>> result = query.getResult().getColumns();
        Assert.assertEquals(result.size(), 2);
        List<TypedObject> names = result.get("Query.name");
        List<TypedObject> totals = result.get("Query.total");
        Assert.assertEquals(names.size(), 1);
        Assert.assertEquals(totals.size(), 1);
        Assert.assertEquals(names.get(0).data, String.valueOf("foo"));
        Assert.assertEquals(totals.get(0).data, Long.valueOf(3));
    }
}
