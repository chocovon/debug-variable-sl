package util.code.block;

import common.Settings;
import util.code.BaseObjectCodeGenerator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static util.code.ObjectCodeHelper.getSimpleName;

public class ArrayCodeBlock extends CodeBlock {
    static class Element {
        int index;
        String value;

        Element(int index, String value) {
            this.index = index;
            this.value = value;
        }
    }

    private final List<Element> elements = new ArrayList<>();

    public ArrayCodeBlock(Object object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
    }

    @Override
    public void walkObjectsTree(BaseObjectCodeGenerator objectCodeGenerator) {
        int length = Array.getLength(object);
        for (int i = 0; i < length; i++) {
            String eleVal = objectCodeGenerator.createObjectCode(Array.get(object, i), level + 1, null, null);
            elements.add(new Element(i, eleVal));
        }
    }

    @Override
    public String generateConstructorCodeWithAssignment(String variableType) {
        String simpleName = getSimpleName(clazz.getName());
        String variableClassName = getVariableClassName(clazz, simpleName, variableType);

        String base = variableClassName + generateVarGenerics() + " " + referenceName
                + " = new " + simpleName + "{";

        return base + generateArraysElementsList(base.length()) + "};";
    }

    @Override
    protected String generateConstructorCall(String constructorClassName) {
        return constructorClassName.replace("[]", "[" + Array.getLength(object) + "]");
    }

    @Override
    public String generateAssignmentCode() {
        StringBuilder str = new StringBuilder();
        for (Element element : elements) {
            if (element.value == null && settings.isSkipNulls()) {
                continue;
            }
            str.append(referenceName).append("[").append(element.index).append("] = ").append(element.value).append(";\n");
        }
        return str.toString();
    }

    @Override
    public boolean hasEmptyAssignment() {
        return elements.isEmpty();
    }

    private String generateArraysElementsList(int length) {
        StringBuilder str = new StringBuilder();
        StringBuilder line = new StringBuilder();

        boolean secondTime = false;
        for (Element element : elements) {
            if (secondTime) {
                line.append(", ");
            }
            if (length + line.length() > 80) {
                str.append(line);
                str.append("\n        ");
                line.setLength(0);
                length = 8;
            }
            line.append(element.value);
            secondTime = true;
        }

        str.append(line);

        return str.toString();
    }
}
