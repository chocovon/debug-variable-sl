import common.Settings;
import common.GenCodeRequest;

public class GenCodeHelper {

    public static String genCode(Object object) {
        GenCodeRequest genCodeRequest = new GenCodeRequest();
        Settings settings = new Settings();
        settings.setFormat("java");
        settings.setSkipNulls(false);
        settings.setSkipDefaults(false);
        settings.setSupportUnderscore(false);
        settings.setUseBaseClasses(false);
        settings.setAddEmptyLines(false);
        settings.setMaxLevel(10);
        genCodeRequest.setSettings(settings);
        return SaveLoader.genCodeInternal(object, genCodeRequest).code;
    }

    public static String genCode(Object object, Settings settings) {
        GenCodeRequest genCodeRequest = new GenCodeRequest();
        genCodeRequest.setSettings(settings);
        return SaveLoader.genCodeInternal(object, genCodeRequest).code;
    }

    public static String genCode(Object object, GenCodeRequest genCodeRequest) {
        return SaveLoader.genCodeInternal(object, genCodeRequest).code;
    }
}
