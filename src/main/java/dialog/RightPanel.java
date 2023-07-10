package dialog;

import com.intellij.ui.JBIntSpinner;
import com.intellij.util.ui.JBUI;
import data.Settings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class RightPanel {
    private Settings settings;
    private Consumer<Settings> onSettingsChange;

    public JPanel getRightPanel(Settings settings, Consumer<Settings> onSettingsChange) {
        this.settings = settings;
        this.onSettingsChange = onSettingsChange;

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        main.add(stickLeft(createCheckBoxes()));
        main.add(spinner());

        main.setBorder(JBUI.Borders.emptyTop(5));

        return stickLeft(main);
    }

    public static JPanel stickLeft(JPanel panel) {
        JPanel main = new JPanel();
        main.setLayout(new FlowLayout(FlowLayout.LEFT));
        main.add(panel);
        return main;
    }

    @NotNull
    private JPanel spinner() {
        JPanel panel = new JPanel();

        JLabel label = new JLabel("Max depth:");
        panel.add(label);

        JBIntSpinner depthSpinner = new JBIntSpinner(50, 1, 100);
        depthSpinner.setNumber(settings.getMaxLevel());
        depthSpinner.addChangeListener(e -> {
            settings.setMaxLevel(depthSpinner.getNumber());
            onSettingsChange.accept(settings);
        });
        panel.add(depthSpinner);

        return panel;
    }

    // Add the checkboxes
    private JPanel createCheckBoxes() {
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));

        checkboxPanel.add(createCheckBox("Remove null values", settings.isSkipNulls(), settings::setSkipNulls));
        checkboxPanel.add(createCheckBox("Remove default values", settings.isSkipDefaults(), settings::setSkipDefaults));
        checkboxPanel.add(createCheckBox("Use base classes", settings.isUseBaseClasses(), settings::setUseBaseClasses));
        checkboxPanel.add(createCheckBox("Add empty lines", settings.isAddEmptyLines(), settings::setAddEmptyLines));

        return checkboxPanel;
    }

    private Component createCheckBox(String label, boolean initValue, Consumer<Boolean> fieldRef) {
        JCheckBox checkBox = new JCheckBox(label);
        checkBox.getModel().setSelected(initValue);
        checkBox.getModel().addActionListener(e -> {
            fieldRef.accept(checkBox.getModel().isSelected());
            onSettingsChange.accept(settings);
        });
        return checkBox;
    }
}
