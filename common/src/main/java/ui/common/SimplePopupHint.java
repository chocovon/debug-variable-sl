package ui.common;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class SimplePopupHint {
    public enum Type {
        SUCCESS,
        WARNING,
        ERROR
    }

    JBPopup popup;

    private SimplePopupHint(JBPopup popup) {
        this.popup = popup;
    }

    public void showInBestPositionFor(DataContext dataContext) {
        this.popup.showInBestPositionFor(dataContext);
        Timer timer = new Timer(4000, e -> this.popup.dispose());
        timer.start();
    }

    public void showInCenterOf(Component comp) {
        this.popup.showInCenterOf(comp);
        Timer timer = new Timer(4000, e -> this.popup.dispose());
        timer.start();
    }

    public void showInCenter() {
        this.popup.showInFocusCenter();
        Timer timer = new Timer(4000, e -> this.popup.dispose());
        timer.start();
    }

    public static void error(String msg) {
        createSimpleHintPopup(msg, Type.ERROR).showInCenter();
    }

    public static void ok(String msg, DataContext dataContext) {
        createSimpleHintPopup(msg, Type.SUCCESS).showInBestPositionFor(dataContext);
    }

    public static void warn(String msg, DataContext dataContext) {
        createSimpleHintPopup(msg, Type.WARNING).showInBestPositionFor(dataContext);
    }

    public static void error(String msg, DataContext dataContext) {
        createSimpleHintPopup(msg, Type.ERROR).showInBestPositionFor(dataContext);
    }

    public static void ok(String msg, Component comp) {
        createSimpleHintPopup(msg, Type.SUCCESS).showInCenterOf(comp);
    }

    public static void warn(String msg, Component comp) {
        createSimpleHintPopup(msg, Type.WARNING).showInCenterOf(comp);
    }

    public static void error(String msg, Component comp) {
        createSimpleHintPopup(msg, Type.ERROR).showInCenterOf(comp);
    }

    @NotNull
    public static SimplePopupHint createSimpleHintPopup(String msg, Type type) {
        JToolTip toolTip = new JToolTip();

        toolTip.setTipText(msg);
        toolTip.setOpaque(true);
        switch (type) {
            case SUCCESS:
                toolTip.setBackground(JBColor.GREEN);
                break;
            case WARNING:
                toolTip.setBackground(JBColor.YELLOW);
                break;
            default:
                toolTip.setBackground(JBColor.RED);
                break;
        }

        return new SimplePopupHint(
                JBPopupFactory.getInstance().createComponentPopupBuilder(toolTip, null)
                .setShowBorder(false)
                .setShowShadow(false)
                .createPopup()
        );
    }
}
