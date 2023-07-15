package common;

public class Settings {
    public String format = "java";
    public boolean skipNulls = true;
    public boolean skipDefaults = true;
    public boolean supportUnderscore = true;
    public boolean useBaseClasses = true;
    public boolean addEmptyLines = true;
    public int maxLevel = 10;

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

    public boolean isSupportUnderscore() {
        return supportUnderscore;
    }

    public void setSupportUnderscore(boolean supportUnderscore) {
        this.supportUnderscore = supportUnderscore;
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
}
