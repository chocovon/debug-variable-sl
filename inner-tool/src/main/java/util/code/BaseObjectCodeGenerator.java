package util.code;

public interface BaseObjectCodeGenerator {
    Code createObjectCode(Object object, int level, String variableType, String variableName);
}
