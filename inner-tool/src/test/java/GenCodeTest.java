import message.GenCodeMessage;
import org.junit.Assert;
import org.junit.Test;
import util.Settings;

import java.util.HashMap;
import java.util.Map;

public class GenCodeTest {
    @Test
    public void testString()  {
        String simple = "hello";
        GenCodeMessage genCodeMessage = SaveLoader.genCode(simple);
        Assert.assertEquals("String string = \"hello\";", genCodeMessage.code);
    }

    @Test
    public void testInt() {
        int simple = 5;
        GenCodeMessage genCodeMessage = SaveLoader.genCode(simple);
        Assert.assertEquals("Integer integer = 5;", genCodeMessage.code);
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
        GenCodeMessage genCodeMessage = SaveLoader.genCodeInternal(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n", genCodeMessage.code);
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
        GenCodeMessage genCodeMessage = SaveLoader.genCodeInternal(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n", genCodeMessage.code);
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
        GenCodeMessage genCodeMessage = SaveLoader.genCodeInternal(testObject, settings);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.bool = false;\n" +
                "testObject.integer = 0;\n", genCodeMessage.code);
    }

    @Test
    public void testPojoArray() {
        class TestObject {
            Integer some = 1;
        }

        TestObject[] testObject = new TestObject[]{new TestObject(), new TestObject()};
        GenCodeMessage genCodeMessage = SaveLoader.genCode(testObject);
        Assert.assertEquals("TestObject testObject = new TestObject();\n" +
                "testObject.some = 1;\n" +
                "TestObject testObject2 = new TestObject();\n" +
                "testObject2.some = 1;\n" +
                "TestObject[] testObjectArr = new TestObject[]();\n" +
                "testObjectArr[0] = testObject;\n" +
                "testObjectArr[1] = testObject2;\n", genCodeMessage.code);
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

        GenCodeMessage genCodeMessage = SaveLoader.genCodeInternal(testObject, settings);
        Assert.assertEquals("Map groups = new HashMap();\n" +
                "groups.put(1, \"admins\");\n" +
                "Map users = new HashMap();\n" +
                "users.put(1, \"henady\");\n" +
                "TestObject testObject = new TestObject();\n" +
                "testObject.users = users;\n" +
                "testObject.setGroups(groups);\n", genCodeMessage.code);
    }
}
