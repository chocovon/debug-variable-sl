import org.junit.Assert;
import org.junit.Test;
import util.GenCodeRequest;
import data.Settings;

import java.util.HashMap;
import java.util.Map;

public class GenCodeTest {

    @Test
    public void testString() {
        String simple = "hello";
        String genCode = GenCodeHelper.genCode(simple);
        Assert.assertEquals("String string = \"hello\";", genCode);
    }

    @Test
    public void testInt() {
        int simple = 5;
        String genCode = GenCodeHelper.genCode(simple);
        Assert.assertEquals("Integer integer = 5;", genCode);
    }

    @Test
    public void testStaticFinalAndNull() {
        class TestObject {
            public static final int SOME = 100;
            Integer inn;
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.skipNulls = true;
        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n", genCode);
    }

    @Test
    public void testSkipDefaults() {
        class TestObject {
            boolean test;
            int integer;
            long aLong;
            float flt;
            double dlb;

            Boolean testBig;
            Integer integerBig;
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.skipNulls = true;
        settings.skipDefaults = true;
        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n", genCode);
    }

    @Test
    public void testSkipDefaultsAndWrapper() {
        class TestObject {
            Boolean bool = Boolean.FALSE;
            Integer integer = 0;
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.skipNulls = true;
        settings.skipDefaults = true;
        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.bool = false;\n" +
                "testObject.integer = 0;\n", genCode);
    }

    @Test
    public void testPojoArray() {
        class TestObject {
            Integer some = 1;
        }

        TestObject[] testObject = new TestObject[]{new TestObject(), new TestObject()};
        String genCode = GenCodeHelper.genCode(testObject);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.some = 1;\n" +
                "TestObject testObject2 = new TestObject();\n" +
                "testObject2.some = 1;\n" +
                "TestObject[] testObjectArr = new TestObject[2];\n" +
                "testObjectArr[0] = testObject;\n" +
                "testObjectArr[1] = testObject2;\n", genCode);
    }

    @Test
    public void testReplaceWithBase() {
        class TestObject {
            Map users = new HashMap<>();
            Map groups;

            public void setGroups(Map groups) {
                this.groups = groups;
            }
        }

        TestObject testObject = new TestObject();
        testObject.users.put(1, "henady");
        HashMap groups = new HashMap();
        groups.put(1, "admins");
        testObject.setGroups(groups);

        Settings settings = new Settings();
        settings.useBaseClasses = true;

        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("Map groups = new HashMap();\n" +
                "groups.put(1, \"admins\");\n" +
                "\n" +
                "Map users = new HashMap();\n" +
                "users.put(1, \"henady\");\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.users = users;\n" +
                "testObject.setGroups(groups);\n", genCode);
    }

    @Test
    public void testVarName() {
        class TestObject {
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.skipNulls = true;
        settings.useBaseClasses = true;
        GenCodeRequest genCodeRequest = new GenCodeRequest();
        genCodeRequest.setSettings(settings);
        genCodeRequest.setVariableType("Object");
        genCodeRequest.setVariableName("hello");
        String genCode = GenCodeHelper.genCode(testObject, genCodeRequest);
        Assert.assertEquals("Object hello = new TestObject();\n", genCode);
    }

    @Test
    public void testComplexArray() {
        class TestObject {
            Integer[] some = new Integer[]{1, 2, 3};
        }

        TestObject testObject = new TestObject();
        String genCode = GenCodeHelper.genCode(testObject);
        Assert.assertEquals("Integer[] some = new Integer[3];\n" +
                "some[0] = 1;\n" +
                "some[1] = 2;\n" +
                "some[2] = 3;\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.some = some;\n", genCode);
    }


    @Test
    public void testComplexArrayInside() {
        class TestObject {
            Integer[] some = new Integer[]{1, 2, 3};
        }

        TestObject[] testObject = new TestObject[]{new TestObject(), new TestObject()};
        String genCode = GenCodeHelper.genCode(testObject);
        Assert.assertEquals("Integer[] some = new Integer[3];\n" +
                "some[0] = 1;\n" +
                "some[1] = 2;\n" +
                "some[2] = 3;\n" +
                "Integer[] some2 = new Integer[3];\n" +
                "some2[0] = 1;\n" +
                "some2[1] = 2;\n" +
                "some2[2] = 3;\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.some = some;\n" +
                "TestObject testObject2 = new TestObject();\n" +
                "testObject2.some = some2;\n" +
                "TestObject[] testObjectArr = new TestObject[2];\n" +
                "testObjectArr[0] = testObject;\n" +
                "testObjectArr[1] = testObject2;\n", genCode);
    }


    @Test
    public void testInner() {
        class TestObject {
            class Inner {
                int x = 1;
            }

            Inner inner = new Inner();
        }

        TestObject testObject = new TestObject();
        String genCode = GenCodeHelper.genCode(testObject);
        Assert.assertEquals("TestObject.Inner inner = new TestObject.Inner();\n" +
                "inner.x = 1;\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.inner = inner;\n", genCode);
    }


    @Test
    public void testInnerArray() {
        class Filter {
            String name = "name";
            String value = "value";
        }

        class TestObject {
            Filter[] _filters = new Filter[]{new Filter(), new Filter()};
        }

        TestObject testObject = new TestObject();
        String genCode = GenCodeHelper.genCode(testObject);
        Assert.assertEquals("Filter filter = new Filter();\n" +
                "filter.name = \"name\";\n" +
                "filter.value = \"value\";\n" +
                "Filter filter2 = new Filter();\n" +
                "filter2.name = \"name\";\n" +
                "filter2.value = \"value\";\n" +
                "Filter[] _filters = new Filter[2];\n" +
                "_filters[0] = filter;\n" +
                "_filters[1] = filter2;\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject._filters = _filters;\n", genCode);
    }


    @Test
    public void testInnerArrayUnderscore() {
        class Filter {
            String name = "name";
            String value = "value";
        }

        class TestObject {
            private Filter[] _filters = new Filter[]{new Filter(), new Filter()};

            public void setFilters(Filter[] _filters) {
                this._filters = _filters;
            }
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.supportUnderscore = true;

        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("Filter filter = new Filter();\n" +
                "filter.name = \"name\";\n" +
                "filter.value = \"value\";\n" +
                "\n" +
                "Filter filter2 = new Filter();\n" +
                "filter2.name = \"name\";\n" +
                "filter2.value = \"value\";\n" +
                "\n" +
                "Filter[] filters = new Filter[2];\n" +
                "filters[0] = filter;\n" +
                "filters[1] = filter2;\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.setFilters(filters);\n", genCode);
    }
}
