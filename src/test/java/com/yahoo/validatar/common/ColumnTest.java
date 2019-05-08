package com.yahoo.validatar.common;

import com.yahoo.validatar.common.TypeSystem.Type;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Iterator;

import static java.util.Arrays.asList;

public class ColumnTest {
    @Test
    public void testEmpty() {
        Column column = new Column();
        Assert.assertTrue(column.isEmpty());
        Assert.assertFalse(column.isScalar());
        Assert.assertFalse(column.isVector());
        Assert.assertEquals(column.size(), 0);
    }

    @Test
    public void testWrappingSingleItem() {
        Column column = new Column(new TypedObject(42L, Type.LONG));
        Assert.assertFalse(column.isEmpty());
        Assert.assertTrue(column.isScalar());
        Assert.assertFalse(column.isVector());
        Assert.assertEquals(column.size(), 1);
        Assert.assertEquals(column.get(0).type, Type.LONG);
        Assert.assertEquals(column.get(0).data, 42L);
        Assert.assertEquals(column.first().type, Type.LONG);
        Assert.assertEquals(column.first().data, 42L);
    }

    @Test
    public void testWrappingMultipleItem() {
        Column column = new Column(asList(new TypedObject(42L, Type.LONG), new TypedObject(84L, Type.LONG)));
        Assert.assertFalse(column.isEmpty());
        Assert.assertFalse(column.isScalar());
        Assert.assertTrue(column.isVector());
        Assert.assertEquals(column.size(), 2);
        Assert.assertEquals(column.get(0).type, Type.LONG);
        Assert.assertEquals(column.get(0).data, 42L);
        Assert.assertEquals(column.first().type, Type.LONG);
        Assert.assertEquals(column.first().data, 42L);
        Assert.assertEquals(column.get(1).type, Type.LONG);
        Assert.assertEquals(column.get(1).data, 84L);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testNegativePosition() {
        Column column = new Column();
        column.get(-1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testOutOfRangePosition() {
        Column column = new Column();
        column.get(0);
    }

    @Test
    public void testAddition() {
        Column column = new Column();
        column.add(new TypedObject(42L, Type.LONG));
        column.add(new TypedObject(84L, Type.LONG));

        Assert.assertEquals(column.size(), 2);

        Column another = new Column(asList(new TypedObject(1L, Type.LONG), new TypedObject(2L, Type.LONG)));
        column.add(another);
        Assert.assertEquals(column.size(), 4);

        Assert.assertEquals(column.get(2).type, Type.LONG);
        Assert.assertEquals(column.get(2).data, 1L);
        Assert.assertEquals(column.get(3).type, Type.LONG);
        Assert.assertEquals(column.get(3).data, 2L);
    }

    @Test
    public void testCopy() {
        Column column = new Column();
        column.add(new TypedObject(42L, Type.LONG));
        column.add(new TypedObject(84L, Type.LONG));
        column.add((TypedObject) null);

        Column copy = column.copy();

        Assert.assertEquals(column.get(1).type, Type.LONG);
        Assert.assertEquals(column.get(1).data, 84L);
        Assert.assertEquals(copy.get(1).type, Type.LONG);
        Assert.assertEquals(copy.get(1).data, 84L);
        Assert.assertNull(copy.get(2));

        Assert.assertFalse(column.get(1) == copy.get(1));
    }

    @Test
    public void testIteration() {
        Column column = new Column();
        column.add(new TypedObject(42L, Type.LONG));
        column.add(new TypedObject(84L, Type.LONG));

        Long sum = column.stream().map(a -> (Long) a.data).reduce(0L, (a, b) -> a + b);
        Assert.assertEquals(sum, Long.valueOf(126));

        sum = 0L;
        Iterator<TypedObject> iterator = column.iterator();
        while (iterator.hasNext()) {
            sum += (Long) iterator.next().data;
        }
        Assert.assertEquals(sum, Long.valueOf(126));
    }

    @Test
    public void testToString() {
        Column column = new Column();
        column.add(new TypedObject(42L, Type.LONG));
        column.add(new TypedObject(84L, Type.LONG));

        Assert.assertEquals(column.toString(), "[<42, LONG>, <84, LONG>]");
    }
}
