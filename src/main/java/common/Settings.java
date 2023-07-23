package common;

public class Settings {
    private String format = "java";

    private boolean skipNulls = true;
    private boolean skipDefaults = true;
    private boolean supportUnderscores = true;
    private boolean useBaseClasses = true;
    private boolean useGenerics = true;
    private boolean useKnownGenerics = true;

    private int maxLevel = 10;

    private boolean prettyFormat = true; // json

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

    public boolean isUseGenerics() {
        return useGenerics;
    }

    public void setUseGenerics(boolean useGenerics) {
        this.useGenerics = useGenerics;
    }

    public boolean isUseKnownGenerics() {
        return useKnownGenerics;
    }

    public void setUseKnownGenerics(boolean useKnownGenerics) {
        this.useKnownGenerics = useKnownGenerics;
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
