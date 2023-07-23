package util.code;

import common.Settings;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

class ObjectCodeHelper {
    static String getSimpleNameFromSuperClass(Class<?> clazz) {
        String simpleName = "";
        while (simpleName.isEmpty() && clazz != null) {
            simpleName = getSimpleName(clazz.getName());
            clazz = clazz.getSuperclass();
        }
        return simpleName;
    }

    static String getSimpleName(String name) {
        return name
                .replaceAll(".*\\.", "")
                .replaceAll(".*\\$\\d+", "")
                .replaceAll("\\$", ".")
                .replace(";", "[]");
    }
    static boolean isUseGenerics(Settings settings, Class<?> clazz) {
        if (!settings.isUseGenerics()) {
            return false;
        }

        if (settings.isUseKnownGenerics()) {
            return knownGenerics.contains(clazz);
        }

        return true;
    }

    static List<Field> getAllFields(Class<?> type) {
        return getAllFields(new ArrayList<Field>(), type);
    }

    static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    static boolean isWrapperType(Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    static String firstLower(String str) {
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    static String firstUpper(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * find common generic class for collections.
     */
    static Class<?> narrow(Class<?> clazz, Object object) {
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

        Class<?> finder;

        // find common root.
        Set<Class<?>> classes = new HashSet<>();
        for (finder = newClazz; finder != null; finder = finder.getSuperclass()) {
            classes.add(finder);
        }

        for (finder = clazz; !classes.contains(finder); ) {
            finder = finder.getSuperclass();
        }

        return finder;
    }

    static String escape(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n\"\n  + \"")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    // do not use generics for maps and collections descendants: children may be plain classes.
    private static final Set<Class<?>> knownGenerics = new HashSet<>(Arrays.asList(
            ArrayList.class, LinkedList.class, Vector.class,
            HashSet.class, LinkedHashSet.class, TreeSet.class, EnumSet.class,
            ArrayDeque.class, PriorityQueue.class,
            HashMap.class, LinkedHashMap.class, TreeMap.class, EnumMap.class,
            WeakHashMap.class, IdentityHashMap.class, Hashtable.class,

            ArrayBlockingQueue.class, ConcurrentHashMap.class, ConcurrentLinkedQueue.class,
            ConcurrentLinkedDeque.class, CopyOnWriteArrayList.class, CopyOnWriteArraySet.class,
            ConcurrentSkipListMap.class, LinkedBlockingDeque.class, LinkedBlockingQueue.class,
            LinkedTransferQueue.class, PriorityBlockingQueue.class, SynchronousQueue.class
    ));

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
}
