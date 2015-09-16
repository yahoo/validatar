package com.yahoo.validatar.execution.pig;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.parse.yaml.YAML;
import org.apache.pig.PigServer;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.yahoo.validatar.OutputCaptor.runWithoutOutput;

public class StyTest {
    private String[] defaults = {"--pig-exec-type", "local"};

    private Sty sty;

    private PigServer getServer(String execType, Properties properties) throws IOException {
        PigServer server = new PigServer(execType, properties);
        PigServer spiedServer = Mockito.spy(server);
        return spiedServer;
    }

    private PigServer withMockSchema(PigServer server, Schema schema) throws IOException {
        Mockito.doReturn(schema).when(server).dumpSchema(Mockito.anyString());
        return server;
    }

    private Sty getSty(PigServer server) throws IOException {
        Sty sty = new Sty();
        Sty spiedSty = Mockito.spy(sty);
        Mockito.doReturn(server).when(spiedSty).getPigServer(Mockito.anyString());
        return spiedSty;
    }

    private Query getQueryFrom(String file, String name) throws FileNotFoundException {
        File testFile = new File(getClass().getClassLoader().getResource(file).getFile());
        TestSuite testSuite = new YAML().parse(new FileInputStream(testFile));
        return testSuite.queries.stream().filter(q -> name.equals(q.name)).findAny().get();
    }

    @BeforeMethod
    public void setup() throws IOException {
        sty = getSty(getServer("local", new Properties()));
    }

    @Test
    public void testDefaults() {
        Assert.assertTrue(sty.setup(new String[0]));
        Assert.assertEquals(sty.getName(), Sty.ENGINE_NAME);
    }

    @Test
    public void testDefaultedQuery() throws FileNotFoundException {
        Query query = getQueryFrom("pig-tests/sample.yaml", "Defaults");

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
