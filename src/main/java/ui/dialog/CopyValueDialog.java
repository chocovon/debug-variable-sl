package ui.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.components.JBScrollPane;
import common.Settings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.function.Function;

import static config.Config.GEN_CODE_SETTINGS_KEY;

public class CopyValueDialog extends DialogWrapper {

    private final TextEditorComponent textArea;
    private final Settings settings;

    private boolean settingsChanged = false;

    public CopyValueDialog(@Nullable Project project, Settings settings, Function<Settings, String> codeProvider) {
        super(project, true);
        this.settings = settings;

        textArea = new TextEditorComponent(project, settings, codeProvider);
        textArea.setPreferredSize(new Dimension(400, 300));

        setTitle("Extract Value as Java or Json code");

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

        String code = textArea.getText();

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(code), null);

        if (settingsChanged) {
            try {
                String json = new ObjectMapper().writeValueAsString(settings);
                PropertiesComponent.getInstance().setValue(GEN_CODE_SETTINGS_KEY, json);
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
        JScrollPane component = new JBScrollPane(textArea, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        component.setBorder(null);
        textAreaComponent.setComponent(component);
        panel.add(textAreaComponent, BorderLayout.CENTER);

        panel.add(new RightPanel().createRightPanel(settings, this::handleUpdate), BorderLayout.EAST);

        return panel;
    }

    private void handleUpdate(Settings settings) {
        textArea.reload(settings);
        settingsChanged = true;
    }
}

