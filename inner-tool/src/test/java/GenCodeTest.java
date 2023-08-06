import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import common.GenCodeRequest;
import common.Settings;

import javax.swing.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenCodeTest {

    @Test
    public void testString() {
        String simple = "hello";
        String genCode = GenCodeTestHelper.genCode(simple);
        Assert.assertEquals("String string = \"hello\";", genCode);
    }

    @Test
    public void testInt() {
        int simple = 5;
        String genCode = GenCodeTestHelper.genCode(simple);
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
        String genCode = GenCodeTestHelper.genCode(testObject, settings);
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
        String genCode = GenCodeTestHelper.genCode(testObject, settings);
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
            Date date = new Date(1689838505598L);
            E e = E.VAL;

            BigDecimal db = new BigDecimal(1);
            BigInteger bi = new BigInteger("1");
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.setSkipNulls(true);
        settings.setSkipDefaults(true);
        String genCode = GenCodeTestHelper.genCode(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.aLong = 1L;\n" +
                "testObject.flt = 1.0f;\n" +
                "testObject.dlb = 1.0;\n" +
                "testObject.c = '1';\n" +
                "testObject.date = new Date(1689838505598L);\n" +
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
        String genCode = GenCodeTestHelper.genCode(testObject, settings);
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
        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.some = 1;\n" +
                "\n" +
                "TestObject testObject2 = new TestObject();\n" +
                "testObject2.some = 1;\n" +
                "\n" +
                "TestObject[] testObjectArr = new TestObject[]{testObject, testObject2};", genCode);
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

        String genCode = GenCodeTestHelper.genCode(testObject, settings);
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

        String genCode = GenCodeTestHelper.genCode(testObject, settings);
        Assert.assertEquals("Map<Integer, U> users = new HashMap<>();\n" +
                "users.put(1, new U1());\n" +
                "users.put(2, new U2());\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.users = users;\n", genCode);
    }

    @Test
    public void testLevelSorting() {
        class A {
            Object b = "ok";
        }

        class B {
            A a = new A();
        }

        class TestObject {
            B b = new B();
            A xa = new A();
        }

        TestObject obj = new TestObject();
        obj.xa.b = obj.b;

        String genCode = GenCodeTestHelper.genCode(obj);

        // compare with generated code:
        {
            B b = new B();
            A a = new A();
            a.b = "ok";
            A xa = new A();
            xa.b = b;
            b.a = a;
            TestObject testObject = new TestObject();
            testObject.b = b;
            testObject.xa = xa;

            String genCode2 = GenCodeTestHelper.genCode(testObject);

            Assert.assertEquals(genCode, genCode2);
        }

        Assert.assertEquals("B b = new B();\n" +
                "\n" +
                "A a = new A();\n" +
                "a.b = \"ok\";\n" +
                "\n" +
                "A xa = new A();\n" +
                "xa.b = b;\n" +
                "\n" +
                "b.a = a;\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.b = b;\n" +
                "testObject.xa = xa;\n", genCode);
    }

    @Test
    public void testArrayReplaceWithBaseAndGenerics() {
        class A {
        }
        class B extends A {
        }
        class C extends A {
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
        testObject.users1.add(new B());
        testObject.users1.add(new B());
        testObject.users1.add(new C());

        testObject.users2.add(new B());
        testObject.users2.add(new A());

        testObject.users3.add(new A());
        testObject.users3.add(new B());

        testObject.users4.add(new B());
        testObject.users4.add(new C());

        testObject.users5.add(1);
        testObject.users5.add("x");

        Settings settings = new Settings();
        settings.setUseBaseClasses(true);

        String genCode = GenCodeTestHelper.genCode(testObject, settings);
        Assert.assertEquals("List<A> users1 = new ArrayList<>();\n" +
                "users1.add(null);\n" +
                "users1.add(new B());\n" +
                "users1.add(new B());\n" +
                "users1.add(new C());\n" +
                "\n" +
                "List<A> users2 = new ArrayList<>();\n" +
                "users2.add(new B());\n" +
                "users2.add(new A());\n" +
                "\n" +
                "List<A> users3 = new ArrayList<>();\n" +
                "users3.add(new A());\n" +
                "users3.add(new B());\n" +
                "\n" +
                "List<A> users4 = new ArrayList<>();\n" +
                "users4.add(new B());\n" +
                "users4.add(new C());\n" +
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
        String genCode = GenCodeTestHelper.genCode(testObject, genCodeRequest);
        Assert.assertEquals("Object hello = new TestObject();\n", genCode);
    }

    @Test
    public void testCharArray() {
        class TestObject {
            char[] text = new char[]{'t', 'e', 'x', 't'};
        }

        TestObject testObject = new TestObject();
        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("char[] text = new char[]{'t', 'e', 'x', 't'};\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.text = text;\n", genCode);
    }

    @Test
    public void testStringBuilder() {
        class TestObject {
            StringBuilder stringBuilder = new StringBuilder();
            StringBuilder stringBuilder2 = new StringBuilder();
        }

        TestObject testObject = new TestObject();
        testObject.stringBuilder2.append("hello");
        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.stringBuilder = new StringBuilder(\"\");\n" +
                "testObject.stringBuilder2 = new StringBuilder(\"hello\");\n", genCode);
    }


    @Test
    public void testComplexArray() {
        class TestObject {
            Integer[] some = new Integer[]{1, 2, 3};
        }

        TestObject testObject = new TestObject();
        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("Integer[] some = new Integer[]{1, 2, 3};\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.some = some;\n", genCode);
    }


    @Test
    public void testComplexArrayInside() {
        class TestObject {
            Integer[] some = new Integer[]{1, 2, 3};
        }

        TestObject[] testObject = new TestObject[]{new TestObject(), new TestObject()};
        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("Integer[] some = new Integer[]{1, 2, 3};\n" +
                "Integer[] some2 = new Integer[]{1, 2, 3};\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.some = some;\n" +
                "\n" +
                "TestObject testObject2 = new TestObject();\n" +
                "testObject2.some = some2;\n" +
                "\n" +
                "TestObject[] testObjectArr = new TestObject[]{testObject, testObject2};", genCode);
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
        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("Inner inner = new Inner();\n" +
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
        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("Filter filter = new Filter();\n" +
                "filter.name = \"name\";\n" +
                "filter.value = \"value\";\n" +
                "\n" +
                "Filter filter2 = new Filter();\n" +
                "filter2.name = \"name\";\n" +
                "filter2.value = \"value\";\n" +
                "\n" +
                "Filter[] _filters = new Filter[]{filter, filter2};\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject._filters = _filters;\n", genCode);
    }

    @Test
    public void testArraysAsListInField() {
        class TestObject {
            List<Integer> list = Arrays.asList(1, 2, 3);
        }

        String genCode = GenCodeTestHelper.genCode(new TestObject());
        Assert.assertEquals("List<Integer> list = Arrays.asList(1, 2, 3);\n" +
                "\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.list = list;\n", genCode);
    }

    @Test
    public void testArraysAsList() {
        String genCode = GenCodeTestHelper.genCode(Arrays.asList(1, 2, 3));

        Assert.assertEquals("List<Integer> arrayList = Arrays.asList(1, 2, 3);\n", genCode);
    }

    @Test
    public void testArrayLevel0() {
        String genCode = GenCodeTestHelper.genCode(new int[]{1, 2, 3});

        Assert.assertEquals("int[] intArr = new int[]{1, 2, 3};", genCode);
    }

    @Test
    public void testAllNull() {
        class Filter {
            String name = "name";
            String value = "value";
        }

        class TestObject {
            Filter[] _filters = new Filter[5];
        }

        TestObject testObject = new TestObject();
        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("Filter[] _filters = new Filter[5];\n" +
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
            private Object _object = new Object();

            public void setFilters(Filter[] _filters) {
                this._filters = _filters;
            }
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.setSupportUnderscores(true);

        String genCode = GenCodeTestHelper.genCode(testObject, settings);
        Assert.assertEquals("Filter filter = new Filter();\n" +
                "filter.name = \"name\";\n" +
                "filter.value = \"value\";\n" +
                "\n" +
                "Filter filter2 = new Filter();\n" +
                "filter2.name = \"name\";\n" +
                "filter2.value = \"value\";\n" +
                "\n" +
                "Filter[] filters = new Filter[]{filter, filter2};\n" +
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

        String genCode = GenCodeTestHelper.genCode(testObject);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "\n" +
                "Inner inner = new Inner();\n" +
                "inner.parent = testObject;\n" +
                "\n" +
                "testObject.inner = inner;\n", genCode);
    }

    @Test
    public void testRemainingFieldsCode() {
        class Level3 {
            Object level2;
        }

        class Level2 {
            Object level1;
            Level3 level3;
        }

        class Level1 {
            Map<Integer, Level1> map1 = new HashMap<>();
        }

        Level1 level1 = new Level1();
        Level2 level2 = new Level2();
        Level3 level3 = new Level3();

        level1.map1.put(1, level1);
        level2.level1 = level1;

        level2.level3 = level3;
        level3.level2 = level2;


        String genCode = GenCodeTestHelper.genCode(level3);
        Assert.assertEquals("Level1 level1 = new Level1();\n" +
                "\n" +
                "HashMap<Integer, Level1> map1 = new HashMap<>();\n" +
                "map1.put(1, level1);\n" +
                "\n" +
                "Level3 level3 = new Level3();\n" +
                "\n" +
                "level1.map1 = map1;\n" +
                "\n" +
                "Level2 level2 = new Level2();\n" +
                "level2.level1 = level1;\n" +
                "level2.level3 = level3;\n" +
                "\n" +
                "level3.level2 = level2;\n", genCode);
    }

    @Test
    public void testDontUseGenerics() {
        Map map = new HashMap();
        map.put(1, "1");
        map.put(2, "2");

        Settings settings = new Settings();
        settings.setUseGenerics(false);
        String genCode = GenCodeTestHelper.genCode(map, settings);
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
        String genCode = GenCodeTestHelper.genCode(map, settings);
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

        String genCode = GenCodeTestHelper.genCode(map);
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

        String genCode = GenCodeTestHelper.genCode(testObject);
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

        String genCode = GenCodeTestHelper.genCode(t3);
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

    @Test
    public void testFinal() {
        class TestObject {
            final long aLong = 1;
            final float flt = 1;
            final double dlb = 1;
            final char c = '1';
            final Date date = new Date(1689838505598L);
            final E e = E.VAL;

            final BigDecimal db = new BigDecimal(1);
            final BigInteger bi = new BigInteger("1");
        }

        TestObject testObject = new TestObject();
        Settings settings = new Settings();
        settings.setSkipNulls(true);
        settings.setSkipDefaults(true);
        settings.setSkipFinal(false);
        String genCode = GenCodeTestHelper.genCode(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.aLong = 1L; // final field;\n" +
                "testObject.flt = 1.0f; // final field;\n" +
                "testObject.dlb = 1.0; // final field;\n" +
                "testObject.c = '1'; // final field;\n" +
                "testObject.date = new Date(1689838505598L); // final field;\n" +
                "testObject.e = E.VAL; // final field;\n" +
                "testObject.db = new BigDecimal(1); // final field;\n" +
                "testObject.bi = new BigInteger(\"1\"); // final field;\n", genCode);
    }

    @Test
    @Ignore("lots of code with timestamps")
    public void testField() throws NoSuchFieldException {
        Settings settings = new Settings();
        settings.setSkipNulls(true);
        settings.setSkipDefaults(true);
        settings.setSkipFinal(false);
        settings.setSkipPrivate(false);

        Field field = settings.getClass().getDeclaredField("format");
        String genCode = GenCodeTestHelper.genCode(field, settings);
        Assert.assertEquals("lots of code", genCode);
    }

    @Test
    @Ignore("lots of code with timestamps")
    public void testFrame() throws NoSuchFieldException {
        Settings settings = new Settings();
        settings.setSkipNulls(true);
        settings.setSkipDefaults(true);
        settings.setSkipFinal(false);
        settings.setSkipPrivate(false);

        JFrame frame = new JFrame();
        String genCode = GenCodeTestHelper.genCode(frame, settings);
        Assert.assertEquals("lots of code", genCode);
    }
}
