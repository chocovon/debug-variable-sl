package ui.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import common.Settings;
import org.jetbrains.annotations.Nullable;
import util.file.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.function.Function;

import static config.Config.DEFAULT_PATH_ABSOLUTE;
import static config.Config.PLUGIN_SETTINGS_JSON;

public class CopyValueDialog extends DialogWrapper {

    private final TextEditorComponent textArea;
    private final Settings settings;

    private boolean needToSaveSettings = false;

    public CopyValueDialog(@Nullable Project project, Settings settings, Function<Settings, String> codeProvider) {
        super(project, true);
        this.settings = settings;

        textArea = new TextEditorComponent(project, settings, codeProvider);

        setTitle("Copy Value to Clipboard as Java Code");

        setSize(800, 600);

        init();
    }

    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();

        getOKAction().putValue(Action.NAME, "Copy and Close");
    }

    @Override
    protected void doOKAction() {
        // Add your custom action for the OK button here
        // This method is called when the user clicks the OK button
        // You can perform any desired logic or processing

        String code = textArea.getEditor().getDocument().getText();

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(code), null);

        if (needToSaveSettings) {
            try {
                String json = new ObjectMapper().writeValueAsString(settings);
                FileUtil.saveFile(json, DEFAULT_PATH_ABSOLUTE + PLUGIN_SETTINGS_JSON);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Call super to close the dialog
        super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Add the text area
        LabeledComponent<JScrollPane> textAreaComponent = new LabeledComponent<>();
        textAreaComponent.setText("Generated code");
        JScrollPane component = new JScrollPane(textArea);
        component.setBorder(null);
        textAreaComponent.setComponent(component);
        panel.add(textAreaComponent, BorderLayout.CENTER);

        panel.add(new RightPanel().getRightPanel(settings, settings1 -> {
            textArea.reload(settings1);
            needToSaveSettings = true;
        }), BorderLayout.EAST);

        textArea.getEditor().getComponent().requestFocus();

        return panel;
    }
}

