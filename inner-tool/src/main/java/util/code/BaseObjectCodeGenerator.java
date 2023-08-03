package util.code;

public interface BaseObjectCodeGenerator {
    String createObjectCode(Object object, int level, String variableType, String variableName);
}
