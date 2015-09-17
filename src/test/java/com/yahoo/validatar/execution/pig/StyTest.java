package com.yahoo.validatar.execution.pig;

import com.yahoo.validatar.common.Metadata;
import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.parse.yaml.YAML;
import org.apache.pig.PigServer;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.yahoo.validatar.OutputCaptor.runWithoutOutput;

public class StyTest {
    private String[] defaults = {"--pig-exec-type", "local"};

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
        PigServer spiedServer = Mockito.spy(server);
        return spiedServer;
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
