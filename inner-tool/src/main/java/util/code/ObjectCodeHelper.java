package util.code;

import common.Settings;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class ObjectCodeHelper {
    public static String escape(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n\"\n  + \"")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String getSimpleNameFromSuperClass(Class<?> clazz) {
        String simpleName = "";
        while (simpleName.isEmpty() && clazz != null) {
            simpleName = clazz.getSimpleName();
            clazz = clazz.getSuperclass();
        }
        return simpleName;
    }

    public static String getSimpleName(String name) {
        return name
                .replaceAll(".*\\.", "")
                .replaceAll(".*\\$\\d+", "")
                .replaceAll("\\$", ".")
                .replace(";", "[]");
    }

    public static List<Field> getAllFields(Class<?> type) {
        return getAllFields(new ArrayList<Field>(), type);
    }

    static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    static final Set<Class<?>> WRAPPER_TYPES = new HashSet<>(Arrays.asList(
            Boolean.class, Character.class,
            Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class,
            Void.class
    ));

    static boolean isWrapperType(Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    static String firstLower(String str) {
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    public static String firstUpper(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * find common generic class for collections.
     */
    public static Class<?> narrow(Class<?> clazz, Object object) {
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

    // do not use generics for maps and collections descendants: children may be plain classes.
    // list of known generic classes
    static final Set<Class<?>> KNOWN_GENERICS = new HashSet<>(Arrays.asList(
            // java.util.*
            ArrayList.class, LinkedList.class, Vector.class,
            HashSet.class, LinkedHashSet.class, TreeSet.class, EnumSet.class,
            ArrayDeque.class, PriorityQueue.class,
            HashMap.class, LinkedHashMap.class, TreeMap.class, EnumMap.class,
            WeakHashMap.class, IdentityHashMap.class, Hashtable.class,

            // java.util.concurrent.*
            ArrayBlockingQueue.class, ConcurrentHashMap.class, ConcurrentLinkedQueue.class,
            ConcurrentLinkedDeque.class, CopyOnWriteArrayList.class, CopyOnWriteArraySet.class,
            ConcurrentSkipListMap.class, LinkedBlockingDeque.class, LinkedBlockingQueue.class,
            LinkedTransferQueue.class, PriorityBlockingQueue.class, SynchronousQueue.class
    ));

    public static boolean shouldUseGenerics(Settings settings, Class<?> clazz) {
        if (!settings.isUseGenerics()) {
            return false;
        }

        if (settings.isUseKnownGenerics()) {
            if ("java.util.Arrays$ArrayList".equals(clazz.getName())) {
                return true;
            }

            return KNOWN_GENERICS.contains(clazz);
        }

        return true;
    }

    public static boolean isDefaultValue(Object value) {
        if (Objects.equals(value, false)) {
            return true;
        }
        if (Objects.equals(value, 0)) {
            return true;
        }
        if (Objects.equals(value, 0L)) {
            return true;
        }
        if (Objects.equals(value, 0D)) {
            return true;
        }
        if (Objects.equals(value, 0F)) {
            return true;
        }
        return false;
    }
}
