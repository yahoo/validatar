package com.yahoo.validatar.execution.pig;

import com.yahoo.validatar.common.Query;
import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.common.TypedObject;
import com.yahoo.validatar.parse.yaml.YAML;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static com.yahoo.validatar.OutputCaptor.runWithoutOutput;

public class StyTest {
    @Test
    public void testDefaults() {
        Sty sty = new Sty();
        Assert.assertTrue(sty.setup(new String[0]));
        Assert.assertEquals(sty.getName(), Sty.ENGINE_NAME);
    }

    @Test
    public void testFullQuery() throws FileNotFoundException {
        File testFile = new File(getClass().getClassLoader().getResource("pig-tests/sample.yaml").getFile());
        TestSuite testSuite = new YAML().parse(new FileInputStream(testFile));
        Query query = testSuite.queries.stream().filter(q -> "Query".equals(q.name)).findAny().get();

        Sty sty = new Sty();
        sty.setup(new String[0]);
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
