package util.code.block;

import common.Settings;
import util.code.BaseObjectCodeGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static util.code.ObjectCodeHelper.firstUpper;
import static util.code.ObjectCodeHelper.getAllFields;
import static util.code.ObjectCodeHelper.isDefaultValue;

public class PojoCodeBlock extends CodeBlock {
    private String assignmentCode;

    public PojoCodeBlock(Object object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
    }

    @Override
    public void walkObjectsTree(BaseObjectCodeGenerator objectCodeGenerator) {
        Class<?> clz = object.getClass();
        StringBuilder str = new StringBuilder();
        List<Field> fields = getAllFields(clz); // TODO: fix duplicate names. (LinkedHashSet?)
        for (Field field : fields) {
            try {
                field.setAccessible(true);

                int modifiers = field.getModifiers();

                if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                    continue; // ignore final and static fields
                }

                Class<?> type = field.getType();
                String fieldName = field.getName();

                Method setter = null;
                try {
                    String setterName = firstUpper(fieldName);
                    setter = clz.getMethod("set" + setterName, type);
                } catch (NoSuchMethodException ignored) {
                }

                if (settings.isSupportUnderscores() && setter == null && field.getName().startsWith("_")) {
                    try {
                        fieldName = fieldName.substring(1);
                        String setterName = firstUpper(fieldName);
                        setter = clz.getMethod("set" + setterName, type);
                    } catch (NoSuchMethodException ignored) {
                    }
                }

                if (setter == null && Modifier.isPrivate(modifiers)) {
                    continue;
                }

                Object value = field.get(object);

                if (value == null && settings.isSkipNulls()) {
                    continue;
                }

                if (type.isPrimitive() && settings.isSkipDefaults() && isDefaultValue(value)) {
                    continue;
                }

                str.append(referenceName).append(".");

                if (setter != null) {
                    Class<?>[] parameterTypes = setter.getParameterTypes();
                    String fieldClassName = parameterTypes.length == 1 ? parameterTypes[0].getSimpleName() : null;
                    String fieldVal = objectCodeGenerator.createObjectCode(value, level + 1, fieldClassName, fieldName);
                    str.append(setter.getName()).append("(").append(fieldVal).append(")");
                } else {
                    String fieldClassName = field.getType().getSimpleName();
                    String fieldVal = objectCodeGenerator.createObjectCode(value, level + 1, fieldClassName, fieldName);
                    str.append(field.getName()).append(" = ").append(fieldVal);
                }
                str.append(";\n");
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field", e);
            }
        }

        assignmentCode = str.toString();
    }

    @Override
    public String generateAssignmentCode() {
        return assignmentCode;
    }
}
