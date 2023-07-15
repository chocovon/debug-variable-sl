package util;

import data.Settings;

/**
 * @see message.GenCodeMessage
 */
public class GenCodeRequest {
    private Settings settings = new Settings();

    private String variableType;
    private String variableName;

    public GenCodeRequest() {
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public String getVariableType() {
        return variableType;
    }

    public void setVariableType(String variableType) {
        this.variableType = variableType;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }
}