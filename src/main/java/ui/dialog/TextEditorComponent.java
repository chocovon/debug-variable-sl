package ui.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import common.Settings;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class TextEditorComponent extends JComponent {

    private final Project project;
    private final Function<Settings, String> codeProvider;
    private EditorEx editor;

    public TextEditorComponent(Project project, Settings settings, Function<Settings, String> codeProvider) {
        this.project = project;
        this.codeProvider = codeProvider;

        String code = codeProvider.apply(settings);

        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument(code);

        // Get the file type
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType fileType = fileTypeManager.getFileTypeByExtension(settings.getFormat());

        // Create the editor with syntax support
        editor = (EditorEx)editorFactory.createEditor(document, project, fileType, false);
        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setLineNumbersShown(true);

        // Set the editor colors scheme (optional)
        EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        editor.setColorsScheme(colorsScheme);
        setLayout(new BorderLayout());
        add(editor.getComponent(), BorderLayout.CENTER);
    }

    public String getText() {
        return editor.getDocument().getText();
    }

    public void setFileType(String fileTypeName) {
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType fileType = fileTypeManager.getFileTypeByExtension(fileTypeName);
        editor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));
    }

    public void reload(Settings settings) {
        setFileType(settings.getFormat());
        String code = codeProvider.apply(settings);
        ApplicationManager.getApplication().runWriteAction(() -> {
            editor.getDocument().setText(code);
        });
    }
}

