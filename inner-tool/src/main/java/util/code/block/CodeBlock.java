package util.code.block;

import common.Settings;
import util.code.ObjectCodeGeneratorCore;

import static util.code.ObjectCodeHelper.getSimpleNameFromSuperClass;

public abstract class CodeBlock<T> {
    protected final T object;

    protected final Class<?> clazz;
    protected final Settings settings;
    protected final int level;
    protected final String referenceName;

    public CodeBlock(T object, Settings settings, int level, String referenceName) {
        this.object = object;
        this.clazz = object.getClass();
        this.settings = settings;
        this.level = level;
        this.referenceName = referenceName;
    }

    public String generateConstructorCode(String variableType) {
        String constructorClassName = clazz.getSimpleName();

        String variableClassName = getVariableClassName(clazz, constructorClassName, variableType);
        String constructorCall = generateConstructorCall(constructorClassName);

        return variableClassName + generateVarGenerics() + " " + referenceName + " = new " + constructorCall + ";\n";
    }

    public String generateInlineCode() {
        String constructorClassName = clazz.getSimpleName();
        String constructorCall = generateConstructorCall(constructorClassName);

        return "new " + constructorCall;
    }

    protected String generateConstructorCall(String constructorClassName) {
        String constructorCall = constructorClassName + generateCtorGenerics() + "()";

        if (constructorClassName.isEmpty()) {
            constructorClassName = getSimpleNameFromSuperClass(clazz);
            constructorCall = constructorClassName + generateCtorGenerics() + "() {/* anonymous class */}";
        }

        return constructorCall;
    }

    /**
     * generates both constructor and assignment
     */
    public String generateConstructorCodeWithAssignment(String variableType) {
        return generateConstructorCode(variableType) + generateAssignmentCode();
    }

    public abstract void visitChildren(ObjectCodeGeneratorCore objectCodeGeneratorCore);

    /**
     * generates 'movable' assignment
     */
    public abstract String generateAssignmentCode();

    public boolean hasEmptyAssignment() {
        return generateAssignmentCode().isEmpty();
    }

    public String generateVarGenerics() {
        return "";
    }

    public String generateCtorGenerics() {
        return "";
    }


    protected String getVariableClassName(Class<?> clazz, String constructorClassName, String variableType) {
        String className = constructorClassName;

        if (variableType != null && !"Object".equals(variableType)) {
            if (settings.isUseBaseClasses()) {
                className = variableType;
            }

            if (className == null || className.isEmpty()) {
                className = variableType;
            }
        }

        if (className == null || className.isEmpty()) {
            // it is anonymous, find base class
            className = getSimpleNameFromSuperClass(clazz);
        }

        return className;
    }
}
