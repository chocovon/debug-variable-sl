import com.alibaba.fastjson.JSON;
import message.GenCodeMessage;
import message.LoadMessage;
import message.SaveMessage;
import util.GenCodeRequest;
import util.KryoUtil;
import util.ObjectCodeGenerator;
import util.Settings;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SaveLoader {
    public static final Method[] methods = new Method[4];

    static {
        Map<String, Integer> map = new HashMap<>();

        map.put("save", map.size());
        map.put("load", map.size());
        map.put("getCode", map.size());
        map.put("genCodeExternal", map.size());

        Method[] allMethods = SaveLoader.class.getDeclaredMethods();
        for (Method m : allMethods) {
            Integer index = map.get(m.getName());
            if (index != null) {
                methods[index] = m;
            }
        }
    }

    public static Method[] saveLoadMethods() {
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
            Settings settings = new Settings();
            settings.skipNulls = false;
            codeMessage.code = new ObjectCodeGenerator(object, settings).genCode();
        } catch (Throwable e) {
            codeMessage.status = "err";
            codeMessage.err = getStackTrace(e);
        }
        return codeMessage;
    }

    public static GenCodeMessage genCode(Object object) {
        return genCodeInternal(object, new GenCodeRequest());
    }

    public static GenCodeMessage genCodeExternal(Object object, String genCodeRequestAsJson) {
        return genCodeInternal(object, JSON.parseObject(genCodeRequestAsJson, GenCodeRequest.class));
    }

    public static GenCodeMessage genCodeInternal(Object object, Settings settings) {
        GenCodeRequest genCodeRequest = new GenCodeRequest();
        genCodeRequest.setSettings(settings);
        return genCodeInternal(object, genCodeRequest);
    }

    public static GenCodeMessage genCodeInternal(Object object, GenCodeRequest genCodeRequest) {
        GenCodeMessage genCodeMessage = new GenCodeMessage();

        try {
            genCodeMessage.code = new ObjectCodeGenerator(object, genCodeRequest).genCode();
            genCodeMessage.status = "ok";
        } catch (Throwable e) {
            genCodeMessage.err = getStackTrace(e);
            genCodeMessage.status = "kryo";
            return genCodeMessage;
        }

        return genCodeMessage;
    }

    private static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
