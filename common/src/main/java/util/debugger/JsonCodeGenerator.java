package util.debugger;

import com.sun.jdi.*;
import common.Settings;
import org.jetbrains.annotations.NotNull;
import util.exception.JsonSerializeException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static util.debugger.ValueUtil.invokeMethod;

public class JsonCodeGenerator {
    private static final String[] TO_STRING_ENABLED_TYPES = {
            "java.lang.Enum",
            "java.util.Date",
            "java.util.Calendar",
            "java.math.BigDecimal",
            "java.math.BigInteger"
    };

    private static final String[] WRAPPER_TYPES = {
            "java.lang.Integer",
            "java.lang.Byte",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.Boolean",
            "java.lang.Character"
    };

    private static final String JAVA_LANG_OBJECT = "java.lang.Object";
    private static final long TIME_LIMIT = 70000000;

    private long timeStamp;

    private final ThreadReference thread;
    private final Settings settings;
    private Set<Object> refPath;

    public JsonCodeGenerator(ThreadReference thread, Settings settings) {
        this.thread = thread;
        this.settings = settings;
    }

    public String toJson(Value value) throws Exception {
        timeStamp = System.currentTimeMillis();
        refPath = new HashSet<>();
        return toJsonInner(value, 0);
    }

    private String toJsonInner(Value value, int depth) throws Exception {
        if (System.currentTimeMillis() - timeStamp > TIME_LIMIT) {
            throw new JsonSerializeException("JSON serializing timed out, probably the object is too big to JSON.");
        }

        if (value == null || depth >= settings.getMaxLevel()) {
            return null;
        }

        if (value instanceof IntegerValue) {
            return String.valueOf(((IntegerValue) value).value());
        }
        if (value instanceof DoubleValue) {
            return String.valueOf(((DoubleValue) value).value());
        }
        if (value instanceof FloatValue) {
            return String.valueOf(((FloatValue) value).value());
        }
        if (value instanceof ShortValue) {
            return String.valueOf(((ShortValue) value).value());
        }
        if (value instanceof ByteValue) {
            return String.valueOf(((ByteValue) value).value());
        }
        if (value instanceof LongValue) {
            return String.valueOf(((LongValue) value).value());
        }
        if (value instanceof CharValue) {
            return "\"" + escape(String.valueOf(((CharValue) value).value())) + "\"";
        }
        if (value instanceof BooleanValue) {
            return String.valueOf(((BooleanValue) value).value());
        }
        if (value instanceof StringReference) {
            return "\"" + escape(((StringReference) value).value()) + "\"";
        }

        if (value instanceof ObjectReference) {
            long id = ((ObjectReference) value).uniqueID();
            if (refPath.contains(id)) {
                return null;
            }
            refPath = new HashSet<>(refPath);
            refPath.add(id);
        }

        if (value instanceof ArrayReference) {
            ArrayReference arrayValue = (ArrayReference) value;
            StringBuilder str = new StringBuilder();
            str.append("[");
            boolean hasOne = false;
            for (Value v : arrayValue.getValues()) {
                String toJsonInner = toJsonInner(v, depth + 1);
                if (toJsonInner == null) {
                    continue;
                }
                str.append(toJsonInner);
                str.append(",");
                hasOne = true;
            }
            if (hasOne) {
                str.delete(str.length() - 1, str.length());
            }
            str.append("]");
            return str.toString();
        }

        if (value instanceof ObjectReference) {
            Set<String> allInheritedTypes = getAllInheritedTypes(value);
            ObjectReference objectValue = (ObjectReference) value;

            if (isSimpleObject(allInheritedTypes)) {
                return toJsonInner(objectValue.getValue(((ClassType) value.type()).fieldByName("value")), depth + 1);
            } else if (allInheritedTypes.contains("java.util.Map")) {
                ObjectReference keySet = (ObjectReference) invokeMethod(objectValue, "keySet", thread);
                ArrayReference keyArr = (ArrayReference) invokeMethod(keySet, "toArray", thread);

                StringBuilder str = new StringBuilder();
                str.append("{");
                boolean hasOne = false;
                for (Value key : keyArr.getValues()) {
                    Value val = invokeMethod(objectValue, "get", thread, key);
                    String keyStr;
                    if (isSimpleValue(key)) {
                        String simpleValStr = toJsonInner(key, depth + 1);
                        if (simpleValStr != null && simpleValStr.startsWith("\"")) {
                            keyStr = simpleValStr;
                        } else {
                            keyStr = '"' + simpleValStr + '"';
                        }
                    } else {
                        Value stringValue = invokeMethod((ObjectReference) key, "toString", thread);
                        keyStr = '"' + escape(((StringReference) stringValue).value()) + '"';
                    }

                    String valueString = toJsonInner(val, depth + 1);
                    if (shouldSkipValue(valueString)) {
                        continue;
                    }
                    str.append(keyStr).append(":").append(valueString);
                    str.append(",");
                    hasOne = true;
                }
                if (hasOne) {
                    str.delete(str.length() - 1, str.length());
                }
                str.append("}");
                return str.toString();
            } else if (allInheritedTypes.contains("java.util.Collection")) {
                return toJsonInner(invokeMethod(objectValue, "toArray", thread), depth + 1);
            }

            // types enabled with toString() method usage
            for (String toStringType : TO_STRING_ENABLED_TYPES) {
                if (allInheritedTypes.contains(toStringType)) {
                    return toJsonInner(invokeMethod(objectValue, "toString", thread), depth + 1);
                }
            }


            StringBuilder str = new StringBuilder();
            str.append("{");
            boolean hasOne = false;
            for (Map.Entry<Field, Value> fieldValueEntry : objectValue.getValues(((ClassType) value.type()).allFields()).entrySet()) {
                if (fieldValueEntry.getKey().isStatic()) {
                    continue;
                }

                if (fieldValueEntry.getKey().isFinal() && settings.isSkipFinal()) {
                    continue;
                }

                String fieldName = fieldValueEntry.getKey().name();
                if (fieldName.startsWith("this$") && fieldValueEntry.getKey().isFinal()) {
                    continue;
                }

                String fieldValue = toJsonInner(fieldValueEntry.getValue(), depth + 1);
                if (shouldSkipValue(fieldValue)) {
                    continue;
                }

                str.append("\"").append(fieldName).append("\"");
                str.append(":");
                str.append(fieldValue);
                str.append(",");
                hasOne = true;
            }
            if (hasOne) {
                str.delete(str.length() - 1, str.length());
            }
            str.append("}");

            return str.toString();
        }

        throw new JsonSerializeException("Unforeseen value type for : " + value.type().name());
    }

    private boolean shouldSkipValue(String valueString) {
        if (valueString == null && settings.isSkipNulls()) {
            return true;
        }

        return settings.isSkipDefaults() && (
                "false".equals(valueString)
                        || "0".equals(valueString)
                        || "0.0".equals(valueString)
        );
    }

    private static boolean isSimpleObject(Set<String> allInheritedTypes) {
        for (String wrapperType : WRAPPER_TYPES) {
            if (allInheritedTypes.contains(wrapperType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSimpleValue(Value value) {
        if (value == null || value instanceof PrimitiveValue || value instanceof StringReference) {
            return true;
        } else if (value instanceof ObjectReference) {
            return isSimpleObject(getAllInheritedTypes(value));
        } else {
            return false;
        }
    }

    @NotNull
    private static Set<String> getAllInheritedTypes(Value value) {
        Set<String> allInheritedTypes = new HashSet<>();
        ClassType type = ((ClassType) value.type());
        allInheritedTypes.add(type.name());
        for (InterfaceType iType : type.allInterfaces()) {
            allInheritedTypes.add(iType.name());
        }
        while (type.superclass() != null) {
            type = type.superclass();
            allInheritedTypes.add(type.name());
        }
        allInheritedTypes.remove(JAVA_LANG_OBJECT);
        return allInheritedTypes;
    }

    private static String escape(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}