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
import com.intellij.openapi.editor.ex.FoldingModelEx;
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
                psiFile.setName("snippet.json");

                if (psiFile != null) {
                    document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                }
            }
        }

        if (document == null) {
            document = editorFactory.createDocument(content.toCharArray());
        }

        EditorEx editor = (EditorEx) editorFactory.createEditor(document, project, fileType, false);
        editor.getFoldingModel().setFoldingEnabled(true);

        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setLineNumbersShown(true);
        editorSettings.setLineMarkerAreaShown(true);
        editorSettings.setFoldingOutlineShown(true);

        foldCode(editor, code, settings);

        CodeFoldingManager.getInstance(project).updateFoldRegions(editor);

        editor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));

        EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
        editor.setColorsScheme(colorsScheme);

        return editor;
    }

    private void foldCode(EditorEx editor, String code, Settings settings) {
        if ("java".equals(settings.getFormat())) {
            FoldingModelEx folding = editor.getFoldingModel();

            editor.getFoldingModel().runBatchFoldingOperation(() -> {
                folding.clearFoldRegions();

                String[] lines = code.split("\n");
                int position = 0;
                int startPosition = 0;
                String startString = "";
                int blockSize = 0;
                for (int i = 0, linesLength = lines.length; i < linesLength; i++) {
                    String line = lines[i];
                    int length = line.length();
                    if (length == 0) {
                        if (blockSize > 1) {
                            folding.addFoldRegion(startPosition, position - 1, startString + " {...}");
                        }
                        blockSize = 0;
                        startString = "";
                        startPosition = position + 1;
                    } else if (i == linesLength - 1) {
                        if (blockSize > 0) { // 0 instead of adding 1
                            folding.addFoldRegion(startPosition, code.length() - 1, startString + " {...}");
                        }
                    } else {
                        if (startString.isEmpty()) {
                            startString = line;
                        }
                        blockSize++;
                    }
                    position += length + 1;
                }
            });
        }
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

            foldCode(editor, code, settings);
        });
    }

    public void dispose() {
        EditorFactory.getInstance().releaseEditor(editor);
    }
}

