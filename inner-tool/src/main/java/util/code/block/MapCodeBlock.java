package util.code.block;

import common.Settings;
import util.code.BaseObjectCodeGenerator;

import java.util.Map;

import static util.code.ObjectCodeHelper.narrow;
import static util.code.ObjectCodeHelper.shouldUseGenerics;


public class MapCodeBlock extends CodeBlock {
    private String assignmentCode;

    private String keyType;
    private String valueType;

    public MapCodeBlock(Object object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
    }

    @Override
    public void walkObjectsTree(BaseObjectCodeGenerator objectCodeGenerator) {
        StringBuilder str = new StringBuilder();

        Class<?> keyClass = null;
        Class<?> valueClass = null;

        boolean shouldUseGenerics = shouldUseGenerics(settings, object.getClass());
        for (Map.Entry<?, ?> entry : ((Map<?,?>)object).entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (shouldUseGenerics) {
                keyClass = narrow(keyClass, key);
                valueClass = narrow(valueClass, value);
            }

            String keyStr = objectCodeGenerator.createObjectCode(key, level + 1, null, null);
            String valStr = objectCodeGenerator.createObjectCode(value, level + 1, null, null);
            str.append(referenceName).append(".put(").append(keyStr).append(", ").append(valStr).append(");\n");
        }

        if (keyClass != null) {
            keyType = keyClass.getSimpleName();
        }
        if (valueClass != null) {
            valueType = valueClass.getSimpleName();
        }

        assignmentCode = str.toString();
    }

    @Override
    public String generateAssignmentCode() {
        return assignmentCode;
    }

    @Override
    public String generateVarGenerics() {
        if (keyType != null && valueType != null) {
            return String.format("<%s, %s>", keyType, valueType);
        }
        return "";
    }

    @Override
    public String generateCtorGenerics() {
        if (keyType != null || valueType != null) {
            return "<>";
        }
        return "";
    }
}
