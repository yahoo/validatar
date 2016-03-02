package com.yahoo.validatar.common;

import lombok.Getter;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class PluggableTest {
    public static class NormalClassTest {
        @Getter
        public int count = 0;

        public NormalClassTest() {
            count = 1;
        }
    }

    public static class UninstantiableTest extends NormalClassTest {
        public UninstantiableTest() throws InstantiationException {
            throw new InstantiationException();
        }
    }

    public static class IllegalAccessTest extends NormalClassTest {
        public IllegalAccessTest() throws IllegalAccessException {
            throw new IllegalAccessException();
        }
    }

    public static class NormalSubClassTest extends NormalClassTest {
        public NormalSubClassTest() {
            super();
            count = 10;
        }
    }

    public static class AnotherNormalSubClassTest extends NormalClassTest {
        public AnotherNormalSubClassTest() {
            super();
            count = 32;
        }
    }

    @Test
    public void testPluginDefaults() {
        Pluggable<NormalClassTest> pluggable = new Pluggable<>(Arrays.asList(NormalSubClassTest.class,
                                                                             AnotherNormalSubClassTest.class),
                                                               "key", "");
        Assert.assertNotNull(pluggable.getParser());
        Set<NormalClassTest> plugins = pluggable.getPlugins(new String[0]);
        Assert.assertEquals(plugins.size(), 2);
        int sum = 0;
        for (NormalClassTest item : plugins) {
            sum += item.getCount();
        }
        Assert.assertEquals(sum, 42);
    }

    @Test
    public void testIllegalAccessClass() {
        Pluggable<NormalClassTest> pluggable = new Pluggable<>(Arrays.asList(IllegalAccessTest.class), "key", "");
        Set<NormalClassTest> plugins = pluggable.getPlugins(new String[0]);
        Assert.assertEquals(plugins.size(), 0);
    }

    @Test
    public void testUninstantiableClass() {
        Pluggable<NormalClassTest> pluggable = new Pluggable<>(Arrays.asList(UninstantiableTest.class), "key", "");
        Set<NormalClassTest> plugins = pluggable.getPlugins(new String[0]);
        Assert.assertEquals(plugins.size(), 0);
    }

    @Test
    public void testArgumentClassLoading() {
        Pluggable<Helpable> pluggable = new Pluggable<>(Collections.emptyList(), "custom", "");
        String[] arguments = { "--custom", "com.yahoo.validatar.FakeTestClass",
                               "--custom", "com.yahoo.validatar.FakeTestClass" };
        Set<Helpable> plugins = pluggable.getPlugins(arguments);
        Assert.assertEquals(plugins.size(), 1);
    }
}
