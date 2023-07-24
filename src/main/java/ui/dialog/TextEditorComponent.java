package ui.dialog;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.json.JsonLanguage;
import com.intellij.lang.Language;
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
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import common.Settings;

import javax.swing.*;
import java.awt.*;

public class TextEditorComponent extends JComponent {
    @FunctionalInterface
    public interface CodeProvider<T, R> {
        R generateCode(T t);
    }

    private final Project project;
    private final CodeProvider<Settings, String> codeProvider;

    private EditorEx editor;
    private String currentFileType;
    private PsiFile psiFile;
    private boolean isPrettyFormat;

    public TextEditorComponent(Project project, Settings settings, CodeProvider<Settings, String> codeProvider) {
        this.project = project;
        this.codeProvider = codeProvider;

        setLayout(new BorderLayout());

        this.currentFileType = "unknown";
        this.isPrettyFormat = settings.isPrettyFormat();
        this.psiFile = null;

        handleUpdate(settings);
    }

    private EditorEx createEditor(Project project, Settings settings, String code) {
        EditorFactory editorFactory = EditorFactory.getInstance();

        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType fileType = fileTypeManager.getFileTypeByExtension(settings.getFormat());

        String content = code.replace("\r\n", "\n");

        Document document = null;
        if (fileType instanceof LanguageFileType) {
            Language language = ((LanguageFileType) fileType).getLanguage();

            // Support only JSON for now
            if (language.is(JsonLanguage.INSTANCE)) {
                psiFile = PsiFileFactory.getInstance(project).createFileFromText(language, content);

                if (psiFile != null) {
                    document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                }
            }
        }

        if (document == null) {
            document = editorFactory.createDocument(content.toCharArray());
        }

        EditorEx editor = (EditorEx) editorFactory.createEditor(document, project, fileType, false);

        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setLineNumbersShown(true);
        editorSettings.setLineMarkerAreaShown(true);
        editorSettings.setFoldingOutlineShown(true);

        CodeFoldingManager.getInstance(project).updateFoldRegions(editor);

        editor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));

        EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        editor.setColorsScheme(colorsScheme);

        return editor;
    }

    public String getText() {
        return editor.getDocument().getText();
    }

    public void handleUpdate(Settings settings) {
        // recreate editor while format change.
        if (!currentFileType.equals(settings.getFormat())) {
            String code = codeProvider.generateCode(settings);

            currentFileType = settings.getFormat();
            psiFile = null;

            if (editor != null) {
                remove(editor.getComponent());
                EditorFactory.getInstance().releaseEditor(editor);
            }

            editor = createEditor(project, settings, code);
            add(editor.getComponent(), BorderLayout.CENTER);
            return;
        }

        // format without regeneration
        if ((isPrettyFormat != settings.isPrettyFormat()) && settings.isPrettyFormat()) {
            isPrettyFormat = true;
            new ReformatCodeProcessor(psiFile, false).run();
            return;
        }

        String code = codeProvider.generateCode(settings);
        ApplicationManager.getApplication().runWriteAction(() -> {
            editor.getDocument().setText(code);
        });
    }

    public void dispose() {
        EditorFactory.getInstance().releaseEditor(editor);
    }
}

