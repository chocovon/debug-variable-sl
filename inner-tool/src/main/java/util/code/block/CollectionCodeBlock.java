package util.code.block;

import common.Settings;
import util.code.BaseObjectCodeGenerator;

import java.util.Collection;

import static util.code.ObjectCodeHelper.narrow;
import static util.code.ObjectCodeHelper.shouldUseGenerics;

public class CollectionCodeBlock extends CodeBlock {
    private String assignmentCode;

    private String keyType;

    public CollectionCodeBlock(Object object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
    }

    @Override
    public void walkObjectsTree(BaseObjectCodeGenerator objectCodeGenerator) {
        StringBuilder str = new StringBuilder();
        Class<?> keyClass = null;

        boolean shouldUseGenerics = shouldUseGenerics(settings, object.getClass());
        for (Object ele : (Collection<?>) object) {
            if (shouldUseGenerics) {
                keyClass = narrow(keyClass, ele);
            }
            String eleVal = objectCodeGenerator.createObjectCode(ele, level + 1, null, null);
            str.append(referenceName).append(".add(").append(eleVal).append(");\n");
        }

        if (keyClass != null) {
            keyType = keyClass.getSimpleName();
        }

        assignmentCode = str.toString();
    }

    @Override
    public String generateAssignmentCode() {
        return assignmentCode;
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
