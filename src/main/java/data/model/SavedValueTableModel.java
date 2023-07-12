package data.model;

import java.util.List;
import javax.swing.table.AbstractTableModel;

public class SavedValueTableModel extends AbstractTableModel {
    List<SavedValue> data;
    String[] columns = new String[]{"time", "name", "type", "source", "project"};
    boolean showAll = false;

    public void showAll(boolean showAll) {
        this.showAll = showAll;
        this.fireTableDataChanged();
    }

    public boolean shouldShowAll() {
        return this.showAll;
    }

    public SavedValueTableModel(List<SavedValue> data) {
        this.data = data;
    }

    public String getColumnName(int column) {
        return this.columns[column];
    }

    public int getRowCount() {
        return this.data.size();
    }

    public int getColumnCount() {
        return this.columns.length;
    }

    public SavedValue removeRow(int row) {
        SavedValue deleted = data.remove(row);
        this.fireTableRowsDeleted(row, row);
        return deleted;
    }

    public SavedValue getRow(int row) {
        return data.get(row);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        SavedValue row = data.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return row.getId();
            case 1:
                return row.getName();
            case 2:
                return row.getType();
            case 3:
                return row.getSource();
            case 4:
                return row.getProject();
            default:
                return null;
        }
    }
}
