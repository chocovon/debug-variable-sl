package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompatibilityUtil {
    public static <K, V> void putToList(Map<K, List<V>> map, K key, V value) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<V>();
            map.put(key, list);
        }
        list.add(value);
    }
}
