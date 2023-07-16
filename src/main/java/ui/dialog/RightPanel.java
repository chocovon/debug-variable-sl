package ui.dialog;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import common.Settings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class RightPanel {
    private Settings settings;
    private Consumer<Settings> onSettingsChange;

    private final Map<String, JComponent> panels = new HashMap<>();

    public JPanel createRightPanel(Settings settings, Consumer<Settings> onSettingsChange) {
        this.settings = settings;
        this.onSettingsChange = onSettingsChange;

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.add(stickLeft(new JBLabel("Output format:")));
        String[] values = {"Java", "JSON"};
        ComboBox<String> comboBox = new ComboBox<>(values);
        comboBox.addActionListener(e -> {
            String selected = comboBox.getModel().getSelectedItem().toString().toLowerCase(Locale.ROOT);
            panels.forEach((name, component) -> component.setVisible(name.equals(selected)));
            settings.setFormat(selected);
            onSettingsChange.accept(settings);
        });
        main.add(comboBox);

        panels.put("java", createJavaPanel(settings.format));
        panels.put("json", createJsonPanel(settings.format));
        panels.forEach((name, component) -> main.add(component));

        main.setBorder(JBUI.Borders.emptyTop(5));

        return stickLeft(main);
    }

    @NotNull
    private JPanel createJsonPanel(String format) {
        JPanel jsonPanel = new JPanel();
        jsonPanel.setVisible("json".equals(format));
        jsonPanel.setLayout(new BoxLayout(jsonPanel, BoxLayout.Y_AXIS));
        jsonPanel.add(stickLeft(createJsonCheckBoxes()));
        return jsonPanel;
    }

    @NotNull
    private JPanel createJavaPanel(String format) {
        JPanel javaPanel = new JPanel();
        javaPanel.setVisible("java".equals(format));
        javaPanel.setLayout(new BoxLayout(javaPanel, BoxLayout.Y_AXIS));
        javaPanel.add(stickLeft(createJavaCheckBoxes()));
        javaPanel.add(stickLeft(createDepthSpinner()));
        return javaPanel;
    }

    public static JPanel stickLeft(JComponent panel) {
        JPanel main = new JPanel();
        main.setLayout(new FlowLayout(FlowLayout.LEFT));
        main.add(panel);
        return main;
    }

    @NotNull
    private JPanel createDepthSpinner() {
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
    private JPanel createJavaCheckBoxes() {
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));

        checkboxPanel.add(createCheckBox("Remove null values", settings.isSkipNulls(), settings::setSkipNulls));
        checkboxPanel.add(createCheckBox("Remove default values", settings.isSkipDefaults(), settings::setSkipDefaults));
        checkboxPanel.add(createCheckBox("Support underscores", settings.isSupportUnderscores(), settings::setSupportUnderscores));
        checkboxPanel.add(createCheckBox("Use base classes", settings.isUseBaseClasses(), settings::setUseBaseClasses));
        checkboxPanel.add(createCheckBox("Add empty lines", settings.isAddEmptyLines(), settings::setAddEmptyLines));

        return checkboxPanel;
    }

    // Add the checkboxes
    private JPanel createJsonCheckBoxes() {
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));

        checkboxPanel.add(createCheckBox("Pretty format", settings.isPrettyFormat(), settings::setPrettyFormat));

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
