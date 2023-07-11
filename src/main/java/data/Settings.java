package data;

/**
 * @see util.Settings
 */
public class Settings {
    public boolean skipNulls = true;
    public boolean skipDefaults = true;
    public boolean useBaseClasses = true;
    public boolean addEmptyLines = true;
    public int maxLevel = 10;

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
