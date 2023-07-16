package util;

import common.GenCodeRequest;
import common.Settings;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

public class ObjectCodeGenerator {
    private class ObjectCode {
        int level;
        int constructorLevel;
        String variableType;
        String referenceName;
        Object object;

        String keyType;
        String valueType;

        String assignmentCode;

        ObjectCode(int level, String referenceName, Object object, String variableType) {
            this.level = level;
            this.referenceName = referenceName;
            this.object = object;
            this.constructorLevel = level;
            this.variableType = variableType;

            Class<?> clazz = object.getClass();

            if (clazz.isArray()) {
                assignmentCode = getArrayCode(object, level, referenceName);
            } else if (object instanceof Collection) {
                assignmentCode = getCollectionCode((Collection<?>) object, level, referenceName);
            } else if (object instanceof Map) {
                assignmentCode = getMapCode((Map<?, ?>) object, level, referenceName);
            } else {
                assignmentCode = getPojoCode(object, level, referenceName);
            }
        }

        String getConstructCode() {
            Class<?> clazz = object.getClass();
            String simpleName = clazz.getName()
                    .replaceAll(".*\\.", "")
                    .replaceAll(".*\\$\\d+", "")
                    .replaceAll("\\$", ".");
            String constructorClass = simpleName.replace(";", "[]");
            String className0;
            if (variableType == null) {
                className0 = constructorClass;
            } else {
                className0 = settings.useBaseClasses ? variableType : constructorClass;
            }

            String constructorCall;
            if (clazz.isArray()) {
                int length = Array.getLength(object);
                constructorCall = simpleName.replace(";", "[" + length + "]");
            } else {
                constructorCall = simpleName + generateCtorGenerics() + "()";
            }

            return className0 + generateVarGenerics() + " " + referenceName + " = new " + constructorCall + ";\n";
        }

        private String generateVarGenerics() {
            if (keyType != null && valueType != null) {
                return String.format("<%s, %s>", keyType, valueType);
            } else if (keyType != null) {
                return String.format("<%s>", keyType);
            }
            return "";
        }

        private String generateCtorGenerics() {
            if (keyType != null || valueType != null) {
                return "<>";
            }
            return "";
        }

        private String getMapCode(Map<?, ?> object, int level, String referenceName) {
            StringBuilder str = new StringBuilder();
            Class<?> keyClass = null;
            Class<?> valueClass = null;
            for (Map.Entry<?, ?> entry : object.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                keyClass = narrow(keyClass, key);
                valueClass = narrow(valueClass, value);
                String keyStr = createObjectCode(key, level + 1, null, null);
                String valStr = createObjectCode(value, level + 1, null, null);
                str.append(referenceName).append(".put(").append(keyStr).append(", ").append(valStr).append(");\n");
            }
            if (keyClass != null) {
                keyType = keyClass.getSimpleName();
            }
            if (valueClass != null) {
                valueType = valueClass.getSimpleName();
            }
            return str.toString();
        }

        private String getCollectionCode(Collection<?> object, int level, String referenceName) {
            StringBuilder str = new StringBuilder();
            Class<?> keyClass = null;
            for (Object ele : object) {
                keyClass = narrow(keyClass, ele);
                String eleVal = createObjectCode(ele, level + 1, null, null);
                str.append(referenceName).append(".add(").append(eleVal).append(");\n");
            }
            if (keyClass != null) {
                keyType = keyClass.getSimpleName();
            }
            return str.toString();
        }

        private String getArrayCode(Object object, int level, String referenceName) {
            StringBuilder str = new StringBuilder();
            int length = Array.getLength(object);
            for (int i = 0; i < length; i++) {
                String eleVal = createObjectCode(Array.get(object, i), level + 1, null, null);
                str.append(referenceName).append("[").append(i).append("] = ").append(eleVal).append(";\n");
            }
            return str.toString();
        }
    }

    private final Object rootObj;
    private final Settings settings;

    private String variableName;
    private String variableType;

    private final UniqueNameGenerator uniqueNameGenerator = new UniqueNameGenerator();
    private final Map<Object, ObjectCode> existingObjectCode = new IdentityHashMap<>();

    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    public ObjectCodeGenerator(Object rootObj, Settings settings) {
        this.rootObj = rootObj;
        this.settings = settings;
    }

    public ObjectCodeGenerator(Object rootObj, GenCodeRequest genCodeRequest) {
        this.rootObj = rootObj;
        this.settings = genCodeRequest.getSettings();
        this.variableName = genCodeRequest.getVariableName();
        String variableType = genCodeRequest.getVariableType();
        if (variableType != null) {
            this.variableType = variableType
                    .replaceAll(".*\\.", "")
                    .replace("$", ".");
        }
    }

    public String genCode() {
        String root = createObjectCode(this.rootObj, 0, this.variableType, this.variableName);

        List<ObjectCode> outputCodes = new ArrayList<>(existingObjectCode.values());
        Collections.sort(outputCodes, new Comparator<ObjectCode>() {
            @Override
            public int compare(ObjectCode a, ObjectCode b) {
                if (b.constructorLevel == a.constructorLevel) {
                    if (a.level == b.level) {
                        return a.referenceName.compareTo(b.referenceName);
                    } else {
                        return a.level - b.level;
                    }
                } else {
                    return b.constructorLevel - a.constructorLevel;
                }
            }
        });

        StringBuilder ret = new StringBuilder();
        int curLevel = Integer.MAX_VALUE;
        Map<Integer, List<ObjectCode>> remainingFieldsCode = new HashMap<>();
        for (ObjectCode objectCode : outputCodes) {
            //meaning highest constructor level end, dump all remained field setters at that level
            if (curLevel > objectCode.constructorLevel) {
                List<ObjectCode> remained = remainingFieldsCode.get(curLevel);
                if (remained != null) {
                    for (ObjectCode fieldCode : remained) {
                        ret.append(fieldCode.assignmentCode);
                    }
                    remainingFieldsCode.remove(curLevel);
                }
                curLevel = objectCode.constructorLevel;
            }

            // add empty line before new
            if (ret.length() != 0 && settings.addEmptyLines) {
                ret.append("\n");
            }

            ret.append(objectCode.getConstructCode());
            if (objectCode.constructorLevel == objectCode.level) {
                ret.append(objectCode.assignmentCode);
            } else {
                putToList(remainingFieldsCode, objectCode.level, objectCode);
            }
        }
        for (List<ObjectCode> remains : remainingFieldsCode.values()) {
            for (ObjectCode r : remains) {
                ret.append(r.assignmentCode);
            }
        }

        if (ret.length() == 0) {
            String simpleName = rootObj.getClass().getSimpleName();
            ret.append(String.format("%s %s = %s;", simpleName, firstLower(simpleName), root));
        }

        return ret.toString();
    }

    private String createObjectCode(Object object, int level, String variableType, String variableName) {
        if (object == null || level > this.settings.maxLevel) {
            return "null";
        } else if (isWrapperType(object.getClass())) {
            if (object instanceof Float) {
                return object + "f";
            } else if (object instanceof Long) {
                return object + "L";
            } else if (object instanceof Character) {
                return "'" + object + "'";
            } else {
                return object.toString();
            }
        } else if (object instanceof String) {
            return "\"" + object + "\"";
        } else if (object instanceof Enum) {
            return object.getClass().getSimpleName() + "." + object;
        } else if (object instanceof Date) {
            return "new " + object.getClass().getSimpleName() + "(" + ((Date) object).getTime() + ")";
        } else if (object instanceof BigDecimal) {
            return "new " + object.getClass().getSimpleName() + "(" + object + ")";
        } else if (object instanceof BigInteger) {
            return "new " + object.getClass().getSimpleName() + "(\"" + object + "\")";
        } else {
            ObjectCode existed = existingObjectCode.get(object);
            if (existed != null) {
                if (existed.constructorLevel < level) {
                    existed.constructorLevel = level;
                }
                return existed.referenceName;
            } else {
                String referenceName = variableName != null
                        ? uniqueNameGenerator.createUniqueName(variableName)
                        : genReferenceName(object.getClass());
                ObjectCode objectCode = new ObjectCode(level, referenceName, object, variableType);
                existingObjectCode.put(object, objectCode);
                return referenceName;
            }
        }
    }


    private String getPojoCode(Object object, int level, String referenceName) {
        Class<?> clz = object.getClass();
        StringBuilder str = new StringBuilder();
        List<Field> fields = getAllFields(clz);
        for (Field field : fields) {
            try {
                field.setAccessible(true);

                int modifiers = field.getModifiers();
                if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                    continue; // ignore final and static fields
                }

                Class<?> type = field.getType();
                String fieldName = field.getName();

                Method setter = null;
                try {
                    String setterName = firstUpper(fieldName);
                    setter = clz.getMethod("set" + setterName, type);
                } catch (NoSuchMethodException ignored) {
                }

                if (this.settings.supportUnderscores && setter == null && field.getName().startsWith("_")) {
                    try {
                        fieldName = fieldName.substring(1);
                        String setterName = firstUpper(fieldName);
                        setter = clz.getMethod("set" + setterName, type);
                    } catch (NoSuchMethodException ignored) {
                    }
                }

                if (setter == null && Modifier.isPrivate(modifiers)) {
                    continue;
                }

                Object value = field.get(object);
                if (value == null && this.settings.skipNulls) {
                    continue;
                }

                if (this.settings.skipDefaults && type.isPrimitive()) {
                    if (Objects.equals(value, false)) {
                        continue;
                    }
                    if (Objects.equals(value, 0)) {
                        continue;
                    }
                    if (Objects.equals(value, 0L)) {
                        continue;
                    }
                    if (Objects.equals(value, 0D)) {
                        continue;
                    }
                    if (Objects.equals(value, 0F)) {
                        continue;
                    }
                }

                str.append(referenceName).append(".");
                if (setter != null) {
                    Class<?>[] parameterTypes = setter.getParameterTypes();
                    String fieldClassName = parameterTypes.length == 1 ? parameterTypes[0].getSimpleName() : null;
                    String fieldVal = createObjectCode(value, level + 1, fieldClassName, fieldName);
                    str.append(setter.getName()).append("(").append(fieldVal).append(")");
                } else {
                    String fieldClassName = field.getType().getSimpleName();
                    String fieldVal = createObjectCode(value, level + 1, fieldClassName, fieldName);
                    str.append(field.getName()).append(" = ").append(fieldVal);
                }
                str.append(";\n");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return str.toString();
    }

    private String genReferenceName(Class<?> clz) {
        String name = clz.getSimpleName();
        if (name.length() == 0) {
            String[] parts = clz.getName().split(Pattern.quote("."));
            name = parts[parts.length - 1];
        }

        if (name.length() == 0) {
            name = "unknownType";
        }

        name = firstLower(name);
        if (clz.isArray()) {
            name = name.replace("[]", "");
            name = name + "Arr";
        }

        return uniqueNameGenerator.createUniqueName(name);
    }

    private static List<Field> getAllFields(Class<?> type) {
        return getAllFields(new ArrayList<Field>(), type);
    }

    private static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    private static boolean isWrapperType(Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    private static Set<Class<?>> getWrapperTypes() {
        Set<Class<?>> ret = new HashSet<>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        return ret;
    }

    private static String firstLower(String str) {
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private static String firstUpper(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private static <K, V> void putToList(Map<K, List<V>> map, K key, V value) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }

    private static Class<?> narrow(Class<?> clazz, Object object) {
        if (object == null) {
            return clazz;
        }

        Class<?> newClazz = object.getClass();

        if (clazz == null) {
            return newClazz;
        }

        if (clazz.equals(Object.class) || clazz.equals(newClazz)) {
            return clazz;
        }

        if (clazz.isAssignableFrom(newClazz)) {
            return clazz;
        } else if (newClazz.isAssignableFrom(clazz)) {
            return newClazz;
        }

        // find common root.
        Set<Class<?>> classes = new HashSet<>();
        for (Class<?> finder = newClazz; finder != Object.class; finder = finder.getSuperclass()) {
            if (finder.equals(clazz)) {
                return finder;
            } else {
                classes.add(finder);
            }
        }

        for (Class<?> finder = clazz; finder != Object.class; finder = finder.getSuperclass()) {
            if (classes.contains(finder)) {
                return finder;
            }
        }

        return Object.class;
    }
}
