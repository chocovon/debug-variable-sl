package util.code.block;

import common.Settings;
import util.code.BaseObjectCodeGenerator;
import util.code.Code;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static util.code.ObjectCodeHelper.firstUpper;
import static util.code.ObjectCodeHelper.getAllFields;
import static util.code.ObjectCodeHelper.isDefaultValue;

public class PojoCodeBlock extends CodeBlock {
    enum Type {FIELD, SETTER}

    static class Element {
        Type type;
        String name;
        Code code;

        Element(Type type, String name, Code code) {
            this.type = type;
            this.name = name;
            this.code = code;
        }
    }

    private final List<Element> elements = new ArrayList<>();

    public PojoCodeBlock(Object object, Settings settings, int level, String referenceName) {
        super(object, settings, level, referenceName);
    }

    @Override
    public void walkObjectsTree(BaseObjectCodeGenerator objectCodeGenerator) {
        Class<?> clz = object.getClass();
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

                if (setter != null) {
                    Class<?>[] parameterTypes = setter.getParameterTypes();
                    String fieldClassName = parameterTypes.length == 1 ? parameterTypes[0].getSimpleName() : null;
                    Code objectCode = objectCodeGenerator.createObjectCode(value, level + 1, fieldClassName, fieldName);
                    elements.add(new Element(Type.SETTER, setter.getName(), objectCode));
                } else {
                    String fieldClassName = field.getType().getSimpleName();
                    Code objectCode = objectCodeGenerator.createObjectCode(value, level + 1, fieldClassName, fieldName);
                    elements.add(new Element(Type.FIELD, field.getName(), objectCode));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field", e);
            }
        }
    }

    @Override
    public String generateAssignmentCode() {
        StringBuilder str = new StringBuilder();
        for (Element element : elements) {
            str.append(referenceName).append(".");
            switch (element.type) {
                case FIELD:
                    str.append(element.name).append(" = ").append(element.code.getCode());
                    break;
                case SETTER:
                    str.append(element.name).append("(").append(element.code.getCode()).append(")");
                    break;
            }
            str.append(";\n");
        }
        return str.toString();
    }
}
