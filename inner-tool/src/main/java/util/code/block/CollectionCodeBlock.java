package util.code.block;

import common.Settings;
import util.code.BaseObjectCodeGenerator;
import util.code.Code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static util.code.ObjectCodeHelper.narrow;
import static util.code.ObjectCodeHelper.shouldUseGenerics;

public class CollectionCodeBlock extends CodeBlock {
    static class Element {
        Code code;

        Element(Code code) {
            this.code = code;
        }
    }

    private final List<Element> elements = new ArrayList<>();

    private String keyType;

    public CollectionCodeBlock(Object object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
    }

    @Override
    public void walkObjectsTree(BaseObjectCodeGenerator objectCodeGenerator) {
        Class<?> keyClass = null;

        boolean shouldUseGenerics = shouldUseGenerics(settings, object.getClass());
        for (Object ele : (Collection<?>) object) {
            if (shouldUseGenerics) {
                keyClass = narrow(keyClass, ele);
            }
            Code objectCode = objectCodeGenerator.createObjectCode(ele, level + 1, null, null);
            elements.add(new Element(objectCode));
        }

        if (keyClass != null) {
            keyType = keyClass.getSimpleName();
        }
    }

    @Override
    public String generateAssignmentCode() {
        StringBuilder str = new StringBuilder();
        for (Element element : elements) {
            str.append(referenceName).append(".add(").append(element.code.getCode()).append(");\n");
        }
        return str.toString();
    }

    @Override
    public String generateVarGenerics() {
        if (keyType != null) {
            return String.format("<%s>", keyType);
        }
        return "";
    }

    @Override
    public String generateCtorGenerics() {
        if (keyType != null) {
            return "<>";
        }
        return "";
    }
}
