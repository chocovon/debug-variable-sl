package util.code;

import common.GenCodeRequest;
import common.Settings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static util.CompatibilityUtil.putToList;
import static util.code.ObjectCodeHelper.escape;
import static util.code.ObjectCodeHelper.firstLower;
import static util.code.ObjectCodeHelper.getSimpleName;
import static util.code.ObjectCodeHelper.isWrapperType;

public class ObjectCodeGenerator implements BaseObjectCodeGenerator {
    private final Object rootObj;
    private final Settings settings;

    private final String variableName;
    private final String variableType;

    private final UniqueNameGenerator uniqueNameGenerator = new UniqueNameGenerator();
    private final Map<Object, ObjectCode> existingObjectCode = new IdentityHashMap<>();

    public ObjectCodeGenerator(Object rootObj, GenCodeRequest genCodeRequest) {
        this.rootObj = rootObj;
        this.settings = genCodeRequest.getSettings();
        this.variableName = genCodeRequest.getVariableName();
        String variableType = genCodeRequest.getVariableType();
        this.variableType = variableType != null ? getSimpleName(variableType) : null;
    }

    public String genCode() {
        String root = createObjectCode(this.rootObj, 0, this.variableType, this.variableName).getCode();

        List<ObjectCode> outputCodes = new ArrayList<>(existingObjectCode.values());
        Collections.sort(outputCodes, new Comparator<ObjectCode>() {
            @Override
            public int compare(ObjectCode a, ObjectCode b) {
                if (b.constructorLevel == a.constructorLevel) {
                    if (a.level == b.level) {
                        return a.referenceName.compareTo(b.referenceName);
                    } else {
                        return a.level - b.level;
                    }
                } else {
                    return b.constructorLevel - a.constructorLevel;
                }
            }
        });

        StringBuilder ret = new StringBuilder();
        int curLevel = Integer.MAX_VALUE;
        Map<Integer, List<ObjectCode>> remainingFieldsCode = new HashMap<>();
        boolean assignmentJustAdded = false;
        for (ObjectCode objectCode : outputCodes) {
            //meaning highest constructor level end, dump all remained field setters at that level
            if (curLevel > objectCode.constructorLevel) {
                List<ObjectCode> remained = remainingFieldsCode.get(curLevel);
                if (remained != null) {
                    for (ObjectCode fieldCode : remained) {
                        // ret.append("// deep forward " + fieldCode.referenceName);
                        ret.append("\n");
                        ret.append(fieldCode.generateAssignmentCode());
                        assignmentJustAdded = true;
                    }
                    remainingFieldsCode.remove(curLevel);
                }
                curLevel = objectCode.constructorLevel;
            }

            // case when constructor and assignment are going together will be processed special way
            if (objectCode.hasEmptyAssignment() || objectCode.constructorLevel != objectCode.level) {
                if (objectCode.hasEmptyAssignment() && objectCode.referenceCount == 1 && curLevel != 0) {
                    objectCode.forceInline = true;
                } else {
                    appendEmptyLine(ret, objectCode, assignmentJustAdded);
                    ret.append(objectCode.generateConstructorCode());
                    assignmentJustAdded = false;
                }
            }

            if (!objectCode.hasEmptyAssignment()) { // no need to handle empty assignments
                if (objectCode.constructorLevel == objectCode.level) {
                    appendEmptyLine(ret, objectCode, assignmentJustAdded);
                    ret.append(objectCode.generateConstructorCodeWithAssignment());
                    assignmentJustAdded = true;
                } else {
                    putToList(remainingFieldsCode, objectCode.level, objectCode);
                }
            }
        }

        for (List<ObjectCode> remains : remainingFieldsCode.values()) {
            for (ObjectCode r : remains) {
                ret.append("\n");
                ret.append(r.generateAssignmentCode());
            }
        }

        if (ret.length() == 0) {
            String simpleName = rootObj.getClass().getSimpleName();
            ret.append(String.format("%s %s = %s;", simpleName, firstLower(simpleName), root));
        }

        return ret.toString();
    }

    private void appendEmptyLine(StringBuilder ret, ObjectCode objectCode, boolean assignmentJustAdded) {
        // add empty line before new block with assignments or after real assignments
        if (ret.length() != 0 && !objectCode.hasEmptyAssignment() || assignmentJustAdded) {
            ret.append("\n");
        }
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
            ObjectCode existed = existingObjectCode.get(object);
            if (existed != null) {
                if (existed.constructorLevel < level) {
                    existed.constructorLevel = level;
                }
                return new Code(existed.referenceName, existed);
            } else {
                String referenceName = variableName != null
                        ? uniqueNameGenerator.createUniqueName(variableName)
                        : uniqueNameGenerator.genReferenceName(object.getClass());
                ObjectCode objectCode = new ObjectCode(this, settings, level, referenceName, object, variableType);
                existingObjectCode.put(object, objectCode);
                objectCode.walkObjectsTree();
                return new Code(referenceName, objectCode);
            }
        }
    }
}
