package util.code.block;

import common.Settings;
import util.code.Code;
import util.code.ObjectCodeGeneratorCore;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static util.code.ObjectCodeHelper.escape;

public class ArrayCodeBlock extends CodeBlock {

    public static final String NEW_LINE_WITH_SPACES = "\n        ";

    static class Element {
        int index;
        Code code;

        Element(int index, Code code) {
            this.index = index;
            this.code = code;
        }
    }

    private final List<Element> elements = new ArrayList<>();
    private final boolean isCharArray;

    public ArrayCodeBlock(Object object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
        this.isCharArray = "char[]".equals(object.getClass().getSimpleName());
    }

    @Override
    public void visitChildren(ObjectCodeGeneratorCore objectCodeGeneratorCore) {
        if (this.isCharArray) {
            return;
        }

        int length = Array.getLength(object);
        for (int i = 0; i < length; i++) {
            Code code = objectCodeGeneratorCore.createObjectCode(Array.get(object, i), level + 1, null, null);

            elements.add(new Element(i, code));
        }
    }

    @Override
    public String generateConstructorCodeWithAssignment(String variableType) {
        String simpleName = clazz.getSimpleName();
        String variableClassName = getVariableClassName(clazz, simpleName, variableType);

        String base = variableClassName + generateVarGenerics() + " " + referenceName
                + " = new " + simpleName + "{";

        String elementsList = isCharArray
                ? generateCharArraysElementsList(base.length())
                : generateArraysElementsList(base.length());
        return base + elementsList + "};";
    }

    @Override
    protected String generateConstructorCall(String constructorClassName) {
        return constructorClassName.replace("[]", "[" + Array.getLength(object) + "]");
    }

    @Override
    public String generateAssignmentCode() {
        StringBuilder str = new StringBuilder();
        for (Element element : elements) {
            if (element.code.getCode() == null && settings.isSkipNulls()) {
                continue;
            }
            str.append(referenceName).append("[").append(element.index).append("] = ").append(element.code.getCode()).append(";\n");
        }
        return str.toString();
    }

    @Override
    public boolean hasEmptyAssignment() {
        return !this.isCharArray && elements.isEmpty() || this.isCharArray && ((char[]) object).length == 0;
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
                str.append(NEW_LINE_WITH_SPACES);
                line.setLength(0);
                length = 8;
            }
            line.append(element.code.getCode());
            secondTime = true;
        }

        str.append(line);

        return str.toString();
    }

    private String generateCharArraysElementsList(int length) {
        StringBuilder str = new StringBuilder();
        StringBuilder line = new StringBuilder();

        boolean secondTime = false;
        for (char element : (char[]) object) {
            if (secondTime) {
                line.append(", ");
            }
            if (length + line.length() > 80) {
                str.append(line);
                str.append(NEW_LINE_WITH_SPACES);
                line.setLength(0);
                length = 8;
            }
            line.append(toChar(element));
            secondTime = true;
        }

        str.append(line);

        return str.toString();
    }

    private String toChar(char element) {
        if (element < 32) {
            return "" + ((int) element);
        }
        return "'" + escape("" + element) + "'";
    }
}
