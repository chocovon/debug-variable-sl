package ui;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.components.BorderLayoutPanel;
import data.model.SavedValue;
import data.model.SavedValueTableModel;
import org.jetbrains.annotations.NotNull;
import ui.common.SimplePopupHint;
import util.file.FileUtil;
import util.debugger.NodeComponents;
import util.debugger.PluginSaveLoader;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import static config.Config.*;


public class SavedVariableView extends BorderLayoutPanel {
    JBTable table;
    SavedValueTableModel tableModel;
    NodeComponents node;
    DataContext dataContext;

    public SavedVariableView(NodeComponents node, DataContext dataContext) {
        this.node = node;
        this.dataContext = dataContext;
        this.tableModel = createTableModel();
        this.table = createJbTable(this.tableModel);

        add(new JBScrollPane(this.table, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);
    }

    @NotNull
    private JPanel createControlPanel() {
        JPanel opts = new JPanel();
        opts.add(createShowAllCheckBox());
        opts.add(Box.createHorizontalGlue());
        opts.add(createDeleteRowButton());
        opts.add(Box.createHorizontalGlue());
        opts.add(createLoadButton());
        opts.setLayout(new BoxLayout(opts, BoxLayout.LINE_AXIS));
        return opts;
    }

    @NotNull
    private JCheckBox createShowAllCheckBox() {
        JCheckBox showAll = new JBCheckBox("Show all");
        showAll.setAlignmentX(LEFT_ALIGNMENT);
        showAll.addItemListener(e -> this.tableModel.showAll(e.getStateChange() == ItemEvent.SELECTED));
        return showAll;
    }

    @NotNull
    private JButton createDeleteRowButton() {
        JButton deleteRowButton = new JButton("Delete");
        deleteRowButton.setAlignmentX(CENTER_ALIGNMENT);
        deleteRowButton.addActionListener(this::deleteRow);
        return deleteRowButton;
    }

    @NotNull
    private JButton createLoadButton() {
        JButton loadButton = new JButton("Load");
        loadButton.setAlignmentX(RIGHT_ALIGNMENT);
        loadButton.addActionListener(e -> {
            int row = this.table.getSelectedRow();
            if (row >= 0) {
                SavedValue value = this.tableModel.getRow(this.table.convertRowIndexToModel(row));
                try {
                    PluginSaveLoader.load(this.node, value);
                    SimplePopupHint.ok("Value loaded", this);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SimplePopupHint.error("Value load failed: " + ex.getMessage(), this);
                }
            }
        });
        return loadButton;
    }

    @NotNull
    private SavedValueTableModel createTableModel() {
        List<SavedValue> savedValueList = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(DEFAULT_PATH_ABSOLUTE + META_NAME))){
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                try {
                    String[] lineData = line.split("\\|", 6);
                    SavedValue savedValue = new SavedValue();

                    savedValue.setId(lineData[0]);
                    savedValue.setProject(lineData[1]);
                    savedValue.setSource(lineData[2]);
                    savedValue.setType(lineData[3]);
                    savedValue.setName(lineData[4]);
                    savedValue.setVal(lineData[5]);

                    if (savedValue.isPrimitive()) {
                        savedValue.setJson(lineData[5]);
                    } else {
                        savedValue.setJson(FileUtil.readFileOrEmpty(DEFAULT_PATH_ABSOLUTE + savedValue.getId() + JSON_SUFFIX));
                    }

                    savedValueList.add(savedValue);
                } catch (Exception e) {
                    try (PrintWriter pw = new PrintWriter(new FileOutputStream(
                                    new File(DEFAULT_PATH_ABSOLUTE + "error.txt"), true))){
                        pw.println("[" + line + "]" + e.getMessage());
                    }
//                    e.printStackTrace(pw);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new SavedValueTableModel(savedValueList);
    }

    @NotNull
    private JBTable createJbTable(SavedValueTableModel tableModel) {
        JBTable table = new JBTable(tableModel) {
            @Override
            public Component prepareRenderer(@NotNull TableCellRenderer renderer,
                                             int row, int col) {

                Component c = super.prepareRenderer(renderer, row, col);
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent)c;

                    String strVal = tableModel.getRow(this.convertRowIndexToModel(row)).getVal();
                    jc.setToolTipText(strVal);
                }
                return c;
            }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new PopupHandler() {

            @Override
            public void invokePopup(Component comp, int x, int y) {
                getDeletePopup(x, y).show(comp, x, y);
            }
        });

        arrangeColumnWidth(table);

        table.setRowSorter(getSorterAndFilter(table));
        table.getColumnModel().getColumn(0).setCellRenderer(getTimeRenderer());

        return table;
    }

    private void arrangeColumnWidth(JBTable table) {
        float[] columnWidthPercentage = {0.15f, 0.05f, 0.32f, 0.32f, 0.16f};
        TableColumn column;
        TableColumnModel jTableColumnModel = table.getColumnModel();
        int tW = jTableColumnModel.getTotalColumnWidth();
        int columnCount = jTableColumnModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    private TableCellRenderer getTimeRenderer() {
        return new DefaultTableCellRenderer() {
            final SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss MM.dd");
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                long time = Long.parseLong((String) value);
                value = f.format(new Date(time));
                return super.getTableCellRendererComponent(table, value, isSelected,
                        hasFocus, row, column);
            }
        };
    }

    private TableRowSorter<TableModel> getSorterAndFilter(JBTable table) {
        TableRowSorter<TableModel> sorter
                = new TableRowSorter<>(table.getModel());

//        List<RowSorter.SortKey> sortKeys
//                = new ArrayList<>();
//        sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
//        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
//        sorter.setSortKeys(sortKeys);

        sorter.setRowFilter(getTableRowFilter());
        return sorter;
    }

    private RowFilter<TableModel, Integer> getTableRowFilter() {
        return new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                SavedValueTableModel tableModel = (SavedValueTableModel) entry.getModel();
                if (tableModel.shouldShowAll()) {
                    return true;
                }
                int row = entry.getIdentifier();
                SavedValue savedValue = tableModel.getRow(row);
                return node.variableInfo != null && savedValue.hasSameOrigin(node.variableInfo);
            }
        };
    }

    protected JPopupMenu getDeletePopup(int x, int y) {
        int row = this.table.rowAtPoint(new Point(x, y));

        if (!this.table.isRowSelected(row)) {
            this.table.changeSelection(row, row, false, false);
        }

        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyJsonItem = new JMenuItem("Copy JSON");
        copyJsonItem.addActionListener(this::copyJson);
        JMenuItem copyCodeItem = new JMenuItem("Copy Java Code");
        copyCodeItem.addActionListener(this::copyCode);
        popupMenu.add(copyJsonItem);
        popupMenu.add(copyCodeItem);
        return popupMenu;
    }

    private void copyJson(ActionEvent e) {
        int row = this.table.getSelectedRow();
        if (row >= 0) {
            SavedValue value = this.tableModel.getRow(this.table.convertRowIndexToModel(row));
            String json = value.getJson();
            if (json == null || json.length() == 0) {
                SimplePopupHint.error("No JSON saved for this variable", this);
            } else {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
                SimplePopupHint.ok("JSON copied", this);
            }
        }
    }

    private void copyCode(ActionEvent e) {
        int row = this.table.getSelectedRow();
        if (row >= 0) {
            SavedValue value = this.tableModel.getRow(this.table.convertRowIndexToModel(row));
            try {
                String code = PluginSaveLoader.getCode(this.node, value);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
                SimplePopupHint.ok("Code copied", this);
            } catch (Exception ex) {
                ex.printStackTrace();
                SimplePopupHint.error("Copy code failed: " + ex.getMessage(), this);
            }
        }
    }

    private void deleteRow(ActionEvent e) {
        int row = this.table.getSelectedRow();
        SavedValue deleted = this.tableModel.removeRow(this.table.convertRowIndexToModel(row));
        try {
            FileUtil.removeLine(DEFAULT_PATH_ABSOLUTE + META_NAME, deleted.getId());
        } catch (Exception deleteException) {
            SimplePopupHint.error("Delete failed: " + deleteException.getMessage(), this);
        }
    }
}
