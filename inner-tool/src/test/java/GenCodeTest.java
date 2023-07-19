import org.junit.Assert;
import org.junit.Test;
import common.GenCodeRequest;
import common.Settings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
        settings.setSkipNulls(true);
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
        settings.setSkipNulls(true);
        settings.setSkipDefaults(true);
        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n", genCode);
    }

    enum E {VAL}

    @Test
    public void testSimple() {
        class TestObject {
            long aLong = 1;
            float flt = 1;
            double dlb = 1;
            char c = '1';
            Date date = new Date(112312312);
            E e = E.VAL;

            BigDecimal db = new BigDecimal(1);
            BigInteger bi = new BigInteger("1");
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.setSkipNulls(true);
        settings.setSkipDefaults(true);
        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.aLong = 1L;\n" +
                "testObject.flt = 1.0f;\n" +
                "testObject.dlb = 1.0;\n" +
                "testObject.c = '1';\n" +
                "testObject.date = new Date(112312312);\n" +
                "testObject.e = E.VAL;\n" +
                "testObject.db = new BigDecimal(1);\n" +
                "testObject.bi = new BigInteger(\"1\");\n", genCode);
    }

    @Test
    public void testSkipDefaultsAndWrapper() {
        class TestObject {
            Boolean bool = Boolean.FALSE;
            Integer integer = 0;
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.setSkipNulls(true);
        settings.setSkipDefaults(true);
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
                "\n" +
                "TestObject testObject2 = new TestObject();\n" +
                "testObject2.some = 1;\n" +
                "\n" +
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
        groups.put(1, 1);
        groups.put(2, "admins");
        testObject.setGroups(groups);

        Settings settings = new Settings();
        settings.setUseBaseClasses(true);

        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("Map<Integer, Object> groups = new HashMap<>();\n" +
                "groups.put(1, 1);\n" +
                "groups.put(2, \"admins\");\n" +
                "\n" +
                "Map<Integer, String> users = new HashMap<>();\n" +
                "users.put(1, \"henady\");\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.users = users;\n" +
                "testObject.setGroups(groups);\n", genCode);
    }

    @Test
    public void testReplaceWithBaseAndGenerics() {
        class U {
        }
        class U1 extends U {
        }
        class U2 extends U {
        }

        class TestObject {
            Map<Object, Object> users = new HashMap<>();
        }

        TestObject testObject = new TestObject();
        testObject.users.put(1, new U1());
        testObject.users.put(2, new U2());

        Settings settings = new Settings();
        settings.setUseBaseClasses(true);

        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("U1 u1 = new U1();\n" +
                "\n" +
                "U2 u2 = new U2();\n" +
                "\n" +
                "Map<Integer, U> users = new HashMap<>();\n" +
                "users.put(1, u1);\n" +
                "users.put(2, u2);\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.users = users;\n", genCode);
    }

    @Test
    public void testArrayReplaceWithBaseAndGenerics() {
        class U {
        }
        class U1 extends U {
        }
        class U2 extends U {
        }
        class U3 extends U2 {
        }
        class U4 extends U2 {
        }

        class TestObject {
            List users1 = new ArrayList<>();
            List users2 = new ArrayList<>();
            List users3 = new ArrayList<>();
            List users4 = new ArrayList<>();
            List users5 = new ArrayList<>();
        }

        TestObject testObject = new TestObject();
        testObject.users1.add(null);
        testObject.users1.add(new U1());
        testObject.users1.add(new U1());
        testObject.users1.add(new U2());

        testObject.users2.add(new U1());
        testObject.users2.add(new U());

        testObject.users3.add(new U());
        testObject.users3.add(new U1());

        testObject.users4.add(new U1());
        testObject.users4.add(new U2());

        testObject.users5.add(1);
        testObject.users5.add("x");

        Settings settings = new Settings();
        settings.setUseBaseClasses(true);

        String genCode = GenCodeHelper.genCode(testObject, settings);
        Assert.assertEquals("U u = new U();\n" +
                "\n" +
                "U1 u1 = new U1();\n" +
                "\n" +
                "U1 u12 = new U1();\n" +
                "\n" +
                "U1 u13 = new U1();\n" +
                "\n" +
                "U1 u14 = new U1();\n" +
                "\n" +
                "U1 u15 = new U1();\n" +
                "\n" +
                "U2 u2 = new U2();\n" +
                "\n" +
                "U2 u22 = new U2();\n" +
                "\n" +
                "U u3 = new U();\n" +
                "\n" +
                "List<U> users1 = new ArrayList<>();\n" +
                "users1.add(null);\n" +
                "users1.add(u1);\n" +
                "users1.add(u12);\n" +
                "users1.add(u2);\n" +
                "\n" +
                "List<U> users2 = new ArrayList<>();\n" +
                "users2.add(u13);\n" +
                "users2.add(u);\n" +
                "\n" +
                "List<U> users3 = new ArrayList<>();\n" +
                "users3.add(u3);\n" +
                "users3.add(u14);\n" +
                "\n" +
                "List<U> users4 = new ArrayList<>();\n" +
                "users4.add(u15);\n" +
                "users4.add(u22);\n" +
                "\n" +
                "List<Object> users5 = new ArrayList<>();\n" +
                "users5.add(1);\n" +
                "users5.add(\"x\");\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.users1 = users1;\n" +
                "testObject.users2 = users2;\n" +
                "testObject.users3 = users3;\n" +
                "testObject.users4 = users4;\n" +
                "testObject.users5 = users5;\n", genCode);
    }

    @Test
    public void testVarName() {
        class TestObject {
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.setSkipNulls(true);
        settings.setUseBaseClasses(true);
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
                "\n" +
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
                "\n" +
                "Integer[] some2 = new Integer[3];\n" +
                "some2[0] = 1;\n" +
                "some2[1] = 2;\n" +
                "some2[2] = 3;\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.some = some;\n" +
                "\n" +
                "TestObject testObject2 = new TestObject();\n" +
                "testObject2.some = some2;\n" +
                "\n" +
                "TestObject[] testObjectArr = new TestObject[2];\n" +
                "testObjectArr[0] = testObject;\n" +
                "testObjectArr[1] = testObject2;\n", genCode);
    }


    @Test
    public void testInner() {
        class TestObject {
            class Inner {
                int x = 1;
                private int y = 2;
            }

            Inner inner = new Inner();
        }

        TestObject testObject = new TestObject();
        String genCode = GenCodeHelper.genCode(testObject);
        Assert.assertEquals("TestObject.Inner inner = new TestObject.Inner();\n" +
                "inner.x = 1;\n" +
                "\n" +
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
                "\n" +
                "Filter filter2 = new Filter();\n" +
                "filter2.name = \"name\";\n" +
                "filter2.value = \"value\";\n" +
                "\n" +
                "Filter[] _filters = new Filter[2];\n" +
                "_filters[0] = filter;\n" +
                "_filters[1] = filter2;\n" +
                "\n" +
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
        settings.setSupportUnderscores(true);

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


    @Test
    public void testRecursive() {
        class Inner {
            Object parent;
        }

        class TestObject {
            Inner inner = new Inner();
        }

        TestObject testObject = new TestObject();
        testObject.inner.parent = testObject;

        String genCode = GenCodeHelper.genCode(testObject);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "\n" +
                "Inner inner = new Inner();\n" +
                "inner.parent = testObject;\n" +
                "testObject.inner = inner;\n", genCode);
    }

    @Test
    public void testDontUseGenerics() {
        Map map = new HashMap();
        map.put(1, "1");
        map.put(2, "2");

        Settings settings = new Settings();
        settings.setUseGenerics(false);
        String genCode = GenCodeHelper.genCode(map, settings);
        Assert.assertEquals("HashMap hashMap = new HashMap();\n" +
                "hashMap.put(1, \"1\");\n" +
                "hashMap.put(2, \"2\");\n", genCode);
    }

    @Test
    public void testDoesNotMatterKnownGenerics() {
        Map map = new HashMap();
        map.put(1, "1");
        map.put(2, "2");

        Settings settings = new Settings();
        settings.setUseKnownGenerics(false);
        String genCode = GenCodeHelper.genCode(map, settings);
        Assert.assertEquals("HashMap<Integer, String> hashMap = new HashMap<>();\n" +
                "hashMap.put(1, \"1\");\n" +
                "hashMap.put(2, \"2\");\n", genCode);
    }

    @Test
    public void testAnonMap() {
        Map map = new HashMap() {{
            put(1, "1");
            put(2, "2");
        }};

        String genCode = GenCodeHelper.genCode(map);
        Assert.assertEquals("HashMap hashMap = new HashMap() {/* anonymous class */};\n" +
                "hashMap.put(1, \"1\");\n" +
                "hashMap.put(2, \"2\");\n", genCode);
    }

    @Test
    public void testAnonMapInClass() {
        class TestObject {
            Map map = new HashMap() {{
                put(1, "1");
                put(2, "2");
            }};
        }

        TestObject testObject = new TestObject();

        String genCode = GenCodeHelper.genCode(testObject);
        Assert.assertEquals("Map map = new HashMap() {/* anonymous class */};\n" +
                "map.put(1, \"1\");\n" +
                "map.put(2, \"2\");\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.map = map;\n", genCode);
    }

    @Test
    public void testTester() {
        Map<Tester, Tester> map = new HashMap<>();
        Tester t1 = new Tester("t1", "tt");
        Tester t2 = new Tester("t2", "pp");
        map.put(t1, t2);
        map.put(t2, t1);
        Tester t3 = new Tester("t3", "hh", t1);
        map.put(t3, t3);
        t2.obj = t2;

        String genCode = GenCodeHelper.genCode(t3);
        Assert.assertEquals("Tester obj = new Tester();\n" +
                "obj.setName(\"t1\");\n" +
                "obj.setType(\"tt\");\n" +
                "obj.obj = null;\n" +
                "\n" +
                "Tester tester = new Tester();\n" +
                "tester.setName(\"t3\");\n" +
                "tester.setType(\"hh\");\n" +
                "tester.obj = obj;\n", genCode);

    }
}
