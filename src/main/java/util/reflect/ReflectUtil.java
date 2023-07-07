package util.reflect;

import java.lang.reflect.Field;

public class ReflectUtil {
    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Throwable ignored) {
        }
        return null;
    }
}
