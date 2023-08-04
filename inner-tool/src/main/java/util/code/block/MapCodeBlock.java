package util.code.block;

import common.Settings;
import util.code.Code;
import util.code.ObjectCodeGeneratorCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static util.code.ObjectCodeHelper.narrow;
import static util.code.ObjectCodeHelper.shouldUseGenerics;

public class MapCodeBlock extends CodeBlock {
    static class Element {
        Code keyCode;
        Code valueCode;

        Element(Code keyCode, Code valueCode) {
            this.keyCode = keyCode;
            this.valueCode = valueCode;
        }
    }

    private final List<Element> elements = new ArrayList<>();

    private String keyType;
    private String valueType;

    public MapCodeBlock(Object object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
    }

    @Override
    public void visitChildren(ObjectCodeGeneratorCore objectCodeGeneratorCore) {
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

            Code keyCode = objectCodeGeneratorCore.createObjectCode(key, level + 1, null, null);
            Code valueCode = objectCodeGeneratorCore.createObjectCode(value, level + 1, null, null);

            elements.add(new Element(keyCode, valueCode));
        }

        if (keyClass != null) {
            keyType = keyClass.getSimpleName();
        }
        if (valueClass != null) {
            valueType = valueClass.getSimpleName();
        }
    }

    @Override
    public String generateAssignmentCode() {
        StringBuilder str = new StringBuilder();
        for (Element element : elements) {
            String keyStr = element.keyCode.getCode();
            String valStr = element.valueCode.getCode();
            str.append(referenceName).append(".put(").append(keyStr).append(", ").append(valStr).append(");\n");
        }
        return str.toString();
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
