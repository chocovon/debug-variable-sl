package util.code;

import common.Settings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;

import static util.code.ObjectCodeHelper.escape;
import static util.code.ObjectCodeHelper.isWrapperType;

public class ObjectCodeGeneratorCore {
    private final Map<Object, ObjectCode> existingObjectCode = new IdentityHashMap<>();
    private final UniqueNameGenerator uniqueNameGenerator = new UniqueNameGenerator();
    private final Settings settings;

    public ObjectCodeGeneratorCore(Settings settings) {
        this.settings = settings;
    }

    public Code createObjectCode(Object object, int level, String variableType, String variableName) {
        if (object == null || level > this.settings.getMaxLevel()) {
            return new Code("null");
        } else if (isWrapperType(object.getClass())) {
            if (object instanceof Integer) {
                if (object.equals(Integer.MAX_VALUE)) {
                    return new Code("Integer.MAX_VALUE");
                } else if (object.equals(Integer.MIN_VALUE)) {
                    return new Code("Integer.MIN_VALUE");
                }
                return new Code(object.toString());
            } else if (object instanceof Float) {
                return new Code(object + "f");
            } else if (object instanceof Long) {
                if (object.equals(Long.MAX_VALUE)) {
                    return new Code("Long.MAX_VALUE");
                } else if (object.equals(Long.MIN_VALUE)) {
                    return new Code("Long.MIN_VALUE");
                }
                return new Code(object + "L");
            } else if (object instanceof Character) {
                return new Code("'" + object + "'");
            } else {
                return new Code(object.toString());
            }
        } else if (object instanceof String) {
            return new Code("\"" + escape((String) object) + "\"");
        } else if (object instanceof Enum) {
            return new Code(object.getClass().getSimpleName() + "." + object);
        } else if (object instanceof Date) {
            return new Code("new " + object.getClass().getSimpleName() + "(" + ((Date) object).getTime() + "L)");
        } else if (object instanceof BigDecimal) {
            return new Code("new " + object.getClass().getSimpleName() + "(" + object + ")");
        } else if (object instanceof BigInteger) {
            return new Code("new " + object.getClass().getSimpleName() + "(\"" + object + "\")");
        } else {
            ObjectCode existed = this.existingObjectCode.get(object);
            if (existed != null) {
                if (existed.constructorLevel < level) {
                    existed.constructorLevel = level;
                }
                return new Code(existed.referenceName, existed);
            } else {
                String referenceName = variableName != null
                        ? uniqueNameGenerator.createUniqueName(variableName)
                        : uniqueNameGenerator.genReferenceName(object.getClass());
                ObjectCode objectCode = new ObjectCode(this.settings, level, referenceName, object, variableType);
                this.existingObjectCode.put(object, objectCode);
                objectCode.visitChildren(this);
                return new Code(referenceName, objectCode);
            }
        }
    }

    public Map<Object, ObjectCode> getExistingObjectCode() {
        return existingObjectCode;
    }
}
