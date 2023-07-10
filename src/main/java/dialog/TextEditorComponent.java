package dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import data.Settings;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class TextEditorComponent extends JComponent {

    private final Function<Settings, String> codeProvider;
    private EditorEx editor;

    public TextEditorComponent(Settings settings, Function<Settings, String> codeProvider) {
        this.codeProvider = codeProvider;
        Project project = ProjectManager.getInstance().getDefaultProject();
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");

        // Get the Java file type
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType javaFileType = fileTypeManager.getFileTypeByExtension("java");

        // Create the editor with Java syntax support
        editor = (EditorEx)editorFactory.createEditor(document, project, javaFileType, false);
        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setLineNumbersShown(true);

        // Set the editor colors scheme (optional)
        EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        editor.setColorsScheme(colorsScheme);
        setLayout(new BorderLayout());
        add(editor.getComponent(), BorderLayout.CENTER);

        reload(settings);
    }

    public EditorEx getEditor() {
        return editor;
    }

    public void reload(Settings settings) {
        String code = codeProvider.apply(settings);
        ApplicationManager.getApplication().runWriteAction(() -> {
            editor.getDocument().setText(code);
        });
    }
}

