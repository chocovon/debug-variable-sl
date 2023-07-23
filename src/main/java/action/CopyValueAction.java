package action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.ui.impl.watch.ThisDescriptorImpl;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import common.Settings;
import org.jetbrains.annotations.NotNull;
import ui.common.SimplePopupHint;
import ui.dialog.CopyValueDialog;
import util.debugger.PluginSaveLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import static config.Config.GEN_CODE_SETTINGS_KEY;

public class CopyValueAction extends XDebuggerTreeActionBase {
    private static final Settings initialSettings = loadSettings();
    private static final boolean isExtractorPlugin = determineIsExtractorPlugin();

    private static boolean determineIsExtractorPlugin() {
        try {
            URL resource = CopyValueAction.class.getResource("/META-INF/plugin.xml");
            String content = new BufferedReader(new InputStreamReader(resource.openStream()))
                    .lines().collect(Collectors.joining("\n"));
            return content.contains("com.github.chocovon.debug-variable-extractor");
        } catch (Exception e) {
            return false;
        }
    }

    private static Settings loadSettings() {
        try {
            String json = PropertiesComponent.getInstance().getValue(GEN_CODE_SETTINGS_KEY, "{}");
            return new ObjectMapper().readValue(json, Settings.class);
        } catch (Exception e) {
            return new Settings();
        }
    }

    @Override
    protected void perform(XValueNodeImpl node, @NotNull String nodeName, AnActionEvent e) {
        try {
            CopyValueDialog popup = new CopyValueDialog(e.getProject(), initialSettings, settings -> {
                try {
                    if (isExtractorPlugin && settings.getFormat().equals("json")) {
                        String jsonString = DebugVarAction.getJsonString(node);
                        if (settings.isPrettyFormat()) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            Object object = objectMapper.readValue(jsonString, Object.class);
                            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
                        }
                        return jsonString;
                    } else {
                        return PluginSaveLoader.genJavaCode(node, settings);
                    }
                } catch (Throwable throwable) {
                    return "Error: " + throwable.getMessage();
                }
            });
            popup.show();
        } catch (Exception knownException) {
            knownException.printStackTrace();
            SimplePopupHint.error("Copy as code failed: " + knownException.getMessage(), e.getDataContext());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        XValueNodeImpl node = getSelectedNode(e.getDataContext());
        if (node == null || ((JavaValue) node.getValueContainer()).getDescriptor() instanceof ThisDescriptorImpl) {
            e.getPresentation().setEnabled(false);
        }
    }
}
