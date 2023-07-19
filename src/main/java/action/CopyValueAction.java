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

import static config.Config.GEN_CODE_SETTINGS_KEY;

public class CopyValueAction extends XDebuggerTreeActionBase {
    private static final Settings initialSettings = loadSettings();

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
                    return PluginSaveLoader.genJavaCode(node, settings);
                } catch (Exception exception) {
                    return "Error: " + exception.getMessage();
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
