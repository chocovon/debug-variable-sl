package config;

import com.intellij.ide.util.PropertiesComponent;

import java.awt.*;
import java.io.File;

public class Config {
    public static String DEFAULT_PATH_SUFFIX = "/.IntelliJPlugin/DebuggerVariableSaveLoader/";
    public static String DEFAULT_PATH_ABSOLUTE = new File(System.getProperty("user.home")).getAbsolutePath() + DEFAULT_PATH_SUFFIX;
    public static String JAR_NAME = "inner-tool.jar";
    public static String DEX_NAME = "android-inner-tool.dex";
    public static String META_NAME = "meta";
    public static String JSON_SUFFIX = ".json";
    public static String ERR_SUFFIX = "error.log";

    public static Dimension tableDimension;

    public static String DIMENSION_KEY = "debug-variable-sl.defaultTableDimension";

    static {
        String[] values = PropertiesComponent.getInstance().getValues(DIMENSION_KEY);
        if (values != null) {
            tableDimension = new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
        }
    }
}
