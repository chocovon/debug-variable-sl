package common;

public class Settings {
    public String format = "java";
    public boolean skipNulls = true;
    public boolean skipDefaults = true;
    public boolean supportUnderscores = true;
    public boolean useBaseClasses = true;
    public boolean addEmptyLines = true;
    public int maxLevel = 10;

    public boolean prettyFormat = true; // json

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isSkipNulls() {
        return skipNulls;
    }

    public void setSkipNulls(boolean skipNulls) {
        this.skipNulls = skipNulls;
    }

    public boolean isSkipDefaults() {
        return skipDefaults;
    }

    public void setSkipDefaults(boolean skipDefaults) {
        this.skipDefaults = skipDefaults;
    }

    public boolean isUseBaseClasses() {
        return useBaseClasses;
    }

    public boolean isSupportUnderscores() {
        return supportUnderscores;
    }

    public void setSupportUnderscores(boolean supportUnderscores) {
        this.supportUnderscores = supportUnderscores;
    }

    public void setUseBaseClasses(boolean useBaseClasses) {
        this.useBaseClasses = useBaseClasses;
    }

    public boolean isAddEmptyLines() {
        return addEmptyLines;
    }

    public void setAddEmptyLines(boolean addEmptyLines) {
        this.addEmptyLines = addEmptyLines;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public boolean isPrettyFormat() {
        return prettyFormat;
    }

    public void setPrettyFormat(boolean prettyFormat) {
        this.prettyFormat = prettyFormat;
    }
}
