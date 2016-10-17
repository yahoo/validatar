/*
 * Copyright 2014-2016 Yahoo! Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        Assert.assertNotNull(pluggable.getPluginOptionsParser());
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
    public void testUnknownClass() {
        Pluggable<Object> pluggable = new Pluggable<>(Collections.emptyList(), "custom", "");
        String[] arguments = { "--custom", "foo.bar.FakeClass" };
        Set<Object> plugins = pluggable.getPlugins(arguments);
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
