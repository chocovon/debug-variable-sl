package action;

import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.impl.watch.ThisDescriptorImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.*;
import common.Settings;
import org.jetbrains.annotations.NotNull;
import ui.common.SimplePopupHint;
import util.debugger.NodeComponents;
import util.debugger.PluginSaveLoader;
import util.debugger.JsonCodeGenerator;
import util.debugger.ValueUtil;
import util.exception.JsonSerializeException;
import util.exception.SaveValueInnerException;
import util.exception.StackFrameThreadException;

import java.io.IOException;

public class SaveValueAction extends XDebuggerTreeActionBase {
    @Override
    protected void perform(XValueNodeImpl node, @NotNull String nodeName, AnActionEvent e) {
        try {
            PluginSaveLoader.save(node);
            SimplePopupHint.ok("Value saved", e.getDataContext());
        } catch (ClassNotLoadedException | EvaluateException | IOException |
                IncompatibleThreadStateException | InvalidTypeException | StackFrameThreadException knownException) {
            knownException.printStackTrace();
            SimplePopupHint.error("Save value failed: " + knownException.getMessage(), e.getDataContext());
        } catch (InvocationException invocationException) {
            try {
                NodeComponents nodeComponents = new NodeComponents(node);
                Value cause = ValueUtil.invokeMethod(invocationException.exception(), "getCause", nodeComponents.thread);
                SimplePopupHint.error("Method invocation exception: " + invocationException.getMessage(), e.getDataContext());
                throw new RuntimeException(new JsonCodeGenerator(nodeComponents.thread, new Settings()).toJson(cause));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (SaveValueInnerException saveValueInnerException) {
            saveValueInnerException.printStackTrace();
            SimplePopupHint.error("Save value failed: cannot serialize current type", e.getDataContext());
        } catch (JsonSerializeException jsonSerializeException) {
            jsonSerializeException.printStackTrace();
            SimplePopupHint.warn("Value saved with no JSON: " + jsonSerializeException.getMessage(), e.getDataContext());
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
