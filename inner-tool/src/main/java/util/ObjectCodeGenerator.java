package util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

public class ObjectCodeGenerator {
    private static class ObjectCode {
        int level;
        int constructorLevel;
        String referenceName;
        String className;
        String assignmentCode;

        ObjectCode(int level, String referenceName, String className) {
            this.level = level;
            this.referenceName = referenceName;
            this.className = className;
            this.constructorLevel = level;
        }

        String getConstructCode() {
            return className + " " + referenceName + " = new " + className + "();\n";
        }
    }

    private final Object rootObj;
    private final int maxLevel;

    private final Set<String> referenceNames = new HashSet<>();
    private final Map<Object, ObjectCode> existingObjectCode = new IdentityHashMap<>();

    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    public ObjectCodeGenerator(Object rootObj, int maxLevel) {
        this.rootObj = rootObj;
        this.maxLevel = maxLevel;
    }

    public String genCode() {
        String root = createObjectCode(this.rootObj, 0);

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

        ret.append(root);
        return ret.toString();
    }

    private String createObjectCode(Object object, int level) {
        if (object == null || level > this.maxLevel) {
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
            return object.getClass().getSimpleName() + "." + object.toString();
        } else if (object instanceof Date) {
            return "new " + object.getClass().getSimpleName() + "(" + ((Date) object).getTime() + ")";
        } else {
            ObjectCode existed = existingObjectCode.get(object);
            if (existed != null) {
                if (existed.constructorLevel < level) {
                    existed.constructorLevel = level;
                }
                return existed.referenceName;
            } else {
                Class<?> clz = object.getClass();
                String referenceName = genReferenceName(clz);
                ObjectCode objectCode = new ObjectCode(level, referenceName, clz.getSimpleName());
                existingObjectCode.put(object, objectCode);

                String code;
                if (clz.isArray()) {
                    code = getArrayCode(object, level, referenceName);
                } else if (object instanceof Collection) {
                    code = getCollectionCode((Collection<?>) object, level, referenceName);
                } else if (object instanceof Map) {
                    code = getMapCode((Map<?, ?>) object, level, referenceName);
                } else {
                    code = getPojoCode(object, level, referenceName);
                }

                objectCode.assignmentCode = code;

                return referenceName;
            }
        }
    }

    private String getMapCode(Map<?, ?> object, int level, String referenceName) {
        StringBuilder str = new StringBuilder();
        for (Object key : object.keySet()) {
            String keyStr = createObjectCode(key, level + 1);
            String valStr = createObjectCode(object.get(key), level + 1);
            str.append(referenceName).append(".put(").append(keyStr).append(", ").append(valStr).append(");\n");
        }
        return str.toString();
    }

    private String getCollectionCode(Collection<?> object, int level, String referenceName) {
        StringBuilder str = new StringBuilder();
        for (Object ele : object) {
            String eleVal = createObjectCode(ele, level + 1);
            str.append(referenceName).append(".add(").append(eleVal).append(");\n");
        }
        return str.toString();
    }

    private String getArrayCode(Object object, int level, String referenceName) {
        StringBuilder str = new StringBuilder();
        int length = Array.getLength(object);
        for (int i = 0; i < length; i ++) {
            String eleVal = createObjectCode(Array.get(object, i), level + 1);
            str.append(referenceName).append("[").append(i).append("] = ").append(eleVal).append(";\n");
        }
        return str.toString();
    }

    private String getPojoCode(Object object, int level, String referenceName) {
        Class<?> clz = object.getClass();
        StringBuilder str = new StringBuilder();
        List<Field> fields = getAllFields(clz);
        for (Field field : fields) {
            try {
                field.setAccessible(true);

                Method setter = null;
                try {
                    setter = clz.getMethod("set" + firstUpper(field.getName()), field.getType());
                } catch (NoSuchMethodException ignored) {
                }
                if (setter == null && Modifier.isPrivate(field.getModifiers())) {
                    continue;
                }

                String fieldVal = createObjectCode(field.get(object), level + 1);

                str.append(referenceName).append(".");
                if (setter != null) {
                    str.append(setter.getName()).append("(").append(fieldVal).append(")");
                } else {
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
        if (!referenceNames.contains(name)) {
            referenceNames.add(name);
            return name;
        }
        int i = 2;
        while (referenceNames.contains(name + i)) {
            i++;
        }
        name = name + i;
        referenceNames.add(name);
        return name;
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

    private static <K,V> void putToList(Map<K, List<V>> map, K key, V value) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
    }
}
