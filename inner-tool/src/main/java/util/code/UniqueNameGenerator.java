package util.code;

import java.util.HashSet;
import java.util.Set;

import static util.code.ObjectCodeHelper.firstLower;
import static util.code.ObjectCodeHelper.getSimpleNameFromSuperClass;

class UniqueNameGenerator {
    private final Set<String> referenceNames = new HashSet<>();

    String createUniqueName(String name) {
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

    String genReferenceName(Class<?> clz) {
        String name = clz.getSimpleName();
        if (name.length() == 0) { // anonymous
            name = getSimpleNameFromSuperClass(clz);
        }

        name = firstLower(name);
        if (clz.isArray()) {
            name = name.replace("[]", "");
            name = name + "Arr";
        }

        return createUniqueName(name);
    }
}
