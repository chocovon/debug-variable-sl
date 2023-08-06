package util.code;

import common.GenCodeRequest;
import common.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static util.CompatibilityUtil.putToList;
import static util.code.ObjectCodeHelper.firstLower;
import static util.code.ObjectCodeHelper.getSimpleName;

public class ObjectCodeGenerator {
    private final Object rootObj;
    private final Settings settings;

    private final String variableName;
    private final String variableType;

    public ObjectCodeGenerator(Object rootObj, GenCodeRequest genCodeRequest) {
        this.rootObj = rootObj;
        this.settings = genCodeRequest.getSettings();
        this.variableName = genCodeRequest.getVariableName();
        String variableType = genCodeRequest.getVariableType();
        this.variableType = variableType != null ? getSimpleName(variableType) : null;
    }

    public String genCode() {
        ObjectCodeGeneratorCore objectCodeGeneratorCore = new ObjectCodeGeneratorCore(settings);
        String root = objectCodeGeneratorCore.createObjectCode(this.rootObj, 0, this.variableType, this.variableName).getCode();
        Map<Object, ObjectCode> existingObjectCode = objectCodeGeneratorCore.getExistingObjectCode();

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
}
