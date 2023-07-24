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
import ui.dialog.CopyValueDialog;
import util.debugger.JsonCodeProvider;
import util.debugger.PluginSaveLoader;
import util.json.JsonUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import static config.Config.GEN_CODE_SETTINGS_KEY;

public class CopyValueAction extends XDebuggerTreeActionBase {
    private static final boolean isExtractorPlugin = determineIsExtractorPlugin();
    private static final Settings initialSettings = loadSettings();

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

    private static Settings loadSettings() {
        try {
            String defaultJson = "{}";
            String json = PropertiesComponent.getInstance().getValue(GEN_CODE_SETTINGS_KEY, defaultJson);
            Settings ret = new ObjectMapper().readValue(json, Settings.class);
            if (defaultJson.equals(json) && isExtractorPlugin) {
                ret.setFormat("json");
            }
            return ret;
        } catch (Exception e) {
            return new Settings();
        }
    }

    @Override
    protected void perform(XValueNodeImpl node, @NotNull String nodeName, AnActionEvent e) {
        CopyValueDialog popup = new CopyValueDialog(e.getProject(), initialSettings, settings -> {
            try {
                if (isExtractorPlugin && settings.getFormat().equals("json")) {
                    String jsonString = JsonCodeProvider.genJsonString(node);
                    if (settings.isPrettyFormat()) {
                        jsonString = JsonUtil.formatJson(node.getTree().getProject(), jsonString);
                    }
                    return jsonString;
                } else {
                    return PluginSaveLoader.genJavaCode(node, settings);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return "Error: " + throwable.getMessage();
            }
        });
        popup.show();
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
