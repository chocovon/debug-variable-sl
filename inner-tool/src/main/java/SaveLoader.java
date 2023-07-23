import common.GenCodeRequest;
import common.Settings;
import message.GenCodeMessage;
import message.LoadMessage;
import message.SaveMessage;
import util.JsonUtil;
import util.KryoUtil;
import util.ObjectCodeGenerator;
import util.SerUtil;

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
        map.put("genCode", map.size());

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
            message.json = JsonUtil.toJSONString(object, true);
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
            settings.setSkipNulls(false);
            GenCodeRequest genCodeRequest = new GenCodeRequest();
            genCodeRequest.setSettings(settings);
            codeMessage.code = new ObjectCodeGenerator(object, genCodeRequest).genCode();
        } catch (Throwable e) {
            codeMessage.status = "err";
            codeMessage.err = getStackTrace(e);
        }
        return codeMessage;
    }

    public static GenCodeMessage genCode(Object object, String genCodeRequestAsString) {
        GenCodeMessage genCodeMessage = new GenCodeMessage();
        try {
            GenCodeRequest genCodeRequest = SerUtil.parseObject(genCodeRequestAsString, GenCodeRequest.class);
            genCodeMessage.status = "ok";
            genCodeMessage.code = genCodeInternal(object, genCodeRequest);
        } catch (Throwable e) {
            genCodeMessage.status = "err";
            genCodeMessage.code = getStackTrace(e);
        }

        return genCodeMessage;
    }

    public static String genCodeInternal(Object object, GenCodeRequest genCodeRequest) {
        Settings settings = genCodeRequest.getSettings();
        String format = settings.getFormat();
        switch (format) {
            case "json":
                return JsonUtil.toJSONString(object, settings.isPrettyFormat());
            case "java":
                return new ObjectCodeGenerator(object, genCodeRequest).genCode();
            default:
                return "Unknown format: " + format;
        }
    }

    private static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
