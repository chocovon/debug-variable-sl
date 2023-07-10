package util;

import java.util.HashSet;
import java.util.Set;

public class UniqueNameGenerator {
    private final Set<String> referenceNames = new HashSet<>();

    public String createUniqueName(String name) {
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

}
