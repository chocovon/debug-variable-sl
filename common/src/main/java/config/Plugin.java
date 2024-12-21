package config;

import action.CopyValueAction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

public class Plugin {
    public static final boolean isExtractorPlugin = determineIsExtractorPlugin();

    private static boolean determineIsExtractorPlugin() {
        try {
            URL resource = CopyValueAction.class.getResource("/META-INF/plugin.xml");
            if (resource != null) {
                String content = new BufferedReader(new InputStreamReader(resource.openStream()))
                        .lines().collect(Collectors.joining("\n"));
                return content.contains("com.github.chocovon.debug-variable-extractor");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
