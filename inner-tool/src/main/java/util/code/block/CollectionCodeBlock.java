package util.code.block;

import common.Settings;
import util.code.Code;
import util.code.ObjectCodeGeneratorCore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static util.code.ObjectCodeHelper.narrow;
import static util.code.ObjectCodeHelper.shouldUseGenerics;

public class CollectionCodeBlock extends CodeBlock<Collection<?>> {
    static class Element {
        Code code;

        Element(Code code) {
            this.code = code;
        }
    }

    private final List<Element> elements = new ArrayList<>();
    private final boolean isArraysAsList;

    private String keyType;

    public CollectionCodeBlock(Collection<?> object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
        this.isArraysAsList = "java.util.Arrays$ArrayList".equals(object.getClass().getName());
    }

    @Override
    public void visitChildren(ObjectCodeGeneratorCore objectCodeGeneratorCore) {
        Class<?> keyClass = null;

        boolean shouldUseGenerics = shouldUseGenerics(settings, object.getClass());
        for (Object ele : object) {
            if (shouldUseGenerics) {
                keyClass = narrow(keyClass, ele);
            }
            Code objectCode = objectCodeGeneratorCore.createObjectCode(ele, level + 1, null, null);

            elements.add(new Element(objectCode));
        }

        if (keyClass != null) {
            keyType = keyClass.getSimpleName();
        }
    }

    @Override
    public boolean hasEmptyAssignment() {
        return !isArraysAsList && elements.isEmpty() || isArraysAsList && object.isEmpty();
    }

    public String generateConstructorCodeWithAssignment(String variableType) {
        if (isArraysAsList) {
            return "List" + generateVarGenerics() + " " + referenceName + " = " + generateInlineCode() + ";\n";
        }

        return generateConstructorCode(variableType) + generateAssignmentCode();
    }

    @Override
    public String generateConstructorCode(String variableType) {
        String constructorClassName = clazz.getSimpleName();

        String variableClassName = getVariableClassName(clazz, constructorClassName, variableType);
        String constructorCall = generateConstructorCall(constructorClassName);

        return variableClassName + generateVarGenerics() + " " + referenceName + " = new " + constructorCall + ";\n";
    }

    public String generateInlineCode() {
        StringBuilder str = new StringBuilder();
        str.append("Arrays.asList(");
        boolean secondTime = false;
        for (Element element : elements) {
            if (secondTime) {
                str.append(", ");
            }
            str.append(element.code.getCode());
            secondTime = true;
        }
        str.append(")");
        return str.toString();
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
