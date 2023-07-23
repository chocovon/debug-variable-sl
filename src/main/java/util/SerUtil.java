package util;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SerUtil {
    public static final String UTF_8 = StandardCharsets.UTF_8.name();

    public static <T> T parseObject(String serString, Class<T> clazz) throws Exception {
        Map<String, String> values = new HashMap<>();
        String[] split = serString.split("\n");
        for (String line : split) {
            String name = line.substring(0, line.indexOf("="));
            String value = line.substring(line.indexOf("=") + 1);
            values.put(name, value);
        }

        return recursiveRead(clazz.getConstructor().newInstance(), "", values);
    }


    private static <T> T recursiveRead(T object, String prefix, Map<String, String> values) throws Exception {
        Class<?> clazz = object.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            Class<?> type = field.getType();
            String value = values.get(prefix + field.getName());
            if ("null".equals(value)) {
                field.set(object, null);
            } else if (type.isPrimitive()) {
                if (type.equals(boolean.class)) {
                    field.setBoolean(object, "true".equals(value));
                } else if (type.equals(int.class)) {
                    field.set(object, Integer.parseInt(value));
                } else {
                    throw new Error(type + " is not supported");
                }
            } else if (type.equals(String.class)) {
                String substring = value.substring(1, value.length() - 1);
                field.set(object, URLDecoder.decode(substring, UTF_8));
            } else {
                recursiveRead(field.get(object), prefix + field.getName() + ".", values);
            }
        }
        return object;
    }

    public static <T> String writeValueAsString(T object) throws Exception {
        return recursiveWrite(object, "", new StringBuilder()).toString();
    }

    private static <T> StringBuilder recursiveWrite(T object, String prefix, StringBuilder stringBuilder) throws Exception {
        Class<?> clazz = object.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            Object fieldObject = field.get(object);
            Class<?> type = field.getType();
            String name = prefix + field.getName();
            if (fieldObject == null) {
                stringBuilder.append(name + "=" + null + "\n");
            } else if (type.isPrimitive()) {
                stringBuilder.append(name + "=" + fieldObject + "\n");
            } else if (type.equals(String.class)) {
                stringBuilder.append(name + "=" + "\"" + URLEncoder.encode(String.valueOf(fieldObject), UTF_8) + "\"" + "\n");
            } else {
                recursiveWrite(fieldObject, name + ".", stringBuilder);
            }
        }
        return stringBuilder;
    }

}
