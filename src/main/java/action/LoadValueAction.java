package action;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.WatchNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.ClassNotLoadedException;
import config.Config;
import org.jetbrains.annotations.NotNull;
import ui.SavedVariableView;
import ui.common.SimplePopupHint;
import util.debugger.NodeComponents;
import util.exception.StackFrameThreadException;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class LoadValueAction extends XDebuggerTreeActionBase {
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        XValueNodeImpl node = getSelectedNode(e.getDataContext());
        if (node instanceof WatchNode) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    protected boolean isEnabled(@NotNull XValueNodeImpl node, @NotNull AnActionEvent e) {
        return super.isEnabled(node, e) && node.getValueContainer().getModifier() != null;
    }

    @Override
    protected void perform(XValueNodeImpl node, @NotNull String nodeName, AnActionEvent e) {
        JBPopup popup;
        try {
            popup = createPopup(node, e);
        } catch (Exception ex) {
            ex.printStackTrace();
            SimplePopupHint.error("Cannot create saved value list: " + ex.getMessage(), e.getDataContext());
            return;
        }

        InputEvent event = e.getInputEvent();
        if (event instanceof MouseEvent) {
            MouseEvent mouse = (MouseEvent) event;
            Point point = mouse.getLocationOnScreen();
            popup.showInScreenCoordinates(mouse.getComponent(), point);
        } else {
            popup.showInBestPositionFor(e.getDataContext());
        }
    }

    @NotNull
    private JBPopup createPopup(XValueNodeImpl node, AnActionEvent e) throws StackFrameThreadException, EvaluateException, ClassNotLoadedException {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(
                new SavedVariableView(new NodeComponents(node), e.getDataContext()), null)
                .setTitle("Select Value")
                .setMovable(true)
                .setMinSize(new Dimension(screen.width / 3, screen.height / 3))
                .setResizable(true)
                .addListener(new JBPopupListener() {
                    @Override
                    public void beforeShown(@NotNull LightweightWindowEvent event) {
                    }

                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        Config.tableDimension = event.asPopup().getContent().getSize();

                        // PropertiesComponent.setValues deprecated
                        String[] values = new String[]{
                                String.valueOf(Config.tableDimension.width),
                                String.valueOf(Config.tableDimension.height)
                        };
                        String value = StringUtil.join(values, "\n");
                        PropertiesComponent.getInstance().setValue(Config.DIMENSION_KEY, value);
                    }
                })
                .createPopup();

        if (Config.tableDimension != null) {
            popup.setSize(Config.tableDimension);
        }

        return popup;
    }
}
