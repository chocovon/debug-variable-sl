import com.alibaba.fastjson.JSON;
import message.LoadMessage;
import message.SaveMessage;
import util.KryoUtil;
import util.ObjectCodeGenerator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class SaveLoader {
    public static Method[] methods = new Method[3];

    public static Method[] saveLoadMethods() {
        Method[] allMethods = SaveLoader.class.getDeclaredMethods();
        for (Method m : allMethods) {
            switch (m.getName()) {
                case "save":
                    methods[0] = m;
                    break;
                case "load":
                    methods[1] = m;
                    break;
                case "getCode":
                    methods[2] = m;
                    break;
            }
        }
        return methods;
    }

    public static SaveMessage save(Object object) {
        SaveMessage message = new SaveMessage();
        message.status = "ok";
        try {
            byte[] bytes = KryoUtil.toBytes(object);
            message.kryo = new String(bytes, StandardCharsets.ISO_8859_1);
        } catch (Throwable e) {
            message.status = "kryo";
            message.kryo = getStackTrace(e);
            return message;
        }
        try {
            message.json = JSON.toJSONString(object, true);
        } catch (Throwable e) {
            message.status = "json";
            message.json = getStackTrace(e);
            return message;
        }
        return message;
    }

    public static LoadMessage load(String isoBytes) {
        LoadMessage loadMessage = new LoadMessage();
        try {
            Object object = KryoUtil.loadObject(isoBytes.getBytes(StandardCharsets.ISO_8859_1));
            Class.forName(object.getClass().getName(), true, KryoUtil.getClassLoader());  //prevent class not loaded in normal cases
            loadMessage.status = "ok";
            loadMessage.object = object;
        } catch (Throwable e) {
            loadMessage.status = "err";
            loadMessage.err = getStackTrace(e);
        }
        return loadMessage;
    }

    public static LoadMessage getCode(String isoBytes) {
        LoadMessage codeMessage = new LoadMessage();
        try {
            Object object = KryoUtil.loadObject(isoBytes.getBytes(StandardCharsets.ISO_8859_1));
            codeMessage.status = "ok";
            codeMessage.code = new ObjectCodeGenerator(object, Integer.MAX_VALUE).genCode();
        } catch (Throwable e) {
            codeMessage.status = "err";
            codeMessage.err = getStackTrace(e);
        }
        return codeMessage;
    }

    private static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
