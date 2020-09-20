/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.awt.Component;
import java.awt.Font;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import static leiodctool.LeiodcMain.IDODUR;
import static leiodctool.LeiodcMain.IDOMODE;
import static leiodctool.LeiodcMain.dogroups;
import static leiodctool.LeiodcMain.dosettings;

/**
 *
 * @author dell
 */
public class TableDO extends TableLE {
    private static final int COLUMNDOCTRL = 1;
    private static final int COLUMNDOMODE = 2;
    private static final int COLUMNDODUR = 3;
    private static final int COLUMNSDO = 4;
    protected ModelDO model;
    private static final int[][] tdosett = new int[dosettings.length][16];
    protected static int tcmdcbox;
    private final JComboBox modeCombo;
    private final DefaultCellEditor cellEditorCombo;


    public TableDO() {
        ctooltips = new String[COLUMNSDO];
        ctooltips[0] = "Digital Output number";
        ctooltips[COLUMNDOCTRL] = "Click to send command";
        ctooltips[COLUMNDOMODE] = "Operation mode";
        ctooltips[COLUMNDODUR] = "Output active duration in milliseconds";

        modeNames = new String[] {"1 - Pulse Output"};
        modeCombo = new JComboBox(new DefaultComboBoxModel(modeNames));
        cellEditorCombo = new DefaultCellEditor(modeCombo);

        model = new ModelDO();
        setModel(model);

        for (int j = 0; j < tdosett.length; j++) {
            for (int i = 0; i < 16; i++) {
                tdosett[j][i] = -1;
            }
        }

        setColumnSelectionAllowed(true);
        setDropMode(DropMode.ON);
        setShowGrid(true);
    }


    protected class ModelDO extends TModelLE {

        public ModelDO() {
            colNames = new String[COLUMNSDO];
            colNames[0] = "DO";
            colNames[COLUMNDOCTRL] = "Cmd";
            colNames[COLUMNDOMODE] = "Mode";
            colNames[COLUMNDODUR] = "Duration";
        }


        @Override
        public Class getColumnClass(int colIndex) {

            if (colIndex == COLUMNDOCTRL)
                return Boolean.class;

            return Object.class;
        }


        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            int aix;
            Object returnValue = null;

            switch (colIndex) {
            case 0:
                returnValue = rowIndex + 1;
                break;

            case COLUMNDOCTRL:
                if (
                        ((tcmdcbox & (1 << rowIndex)) == 0) &&
                        ((Modbus.cmdOn  & (1 << rowIndex)) == 0)){
                    returnValue = false;
                }
                else {
                    returnValue = true;
                }
                break;

            case COLUMNDOMODE:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if (tdosett[aix][rowIndex] >= 0) {
                        returnValue = getModeString(tdosett[aix][rowIndex]);
                    }
                }
                break;

            case COLUMNDODUR:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if (tdosett[aix][rowIndex] >= 0) {
                        returnValue = Integer.toString(tdosett[aix][rowIndex]);
                    }
                }
                break;
            }
            return returnValue;
        }


        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            int intval, aix;

             if (value == null)  // Selected value might be null e.g. when no selection made by comboBox
                return;

            switch (colIndex) {
            case COLUMNDOCTRL:
                if ((tcmdcbox & (1 << rowIndex)) == 0) {
                //if ((Boolean) value == true) {
                    Modbus.cmdOn |= (1 << rowIndex);
                    tcmdcbox |= (1 << rowIndex);
                }
                else {
                    Modbus.cmdOff |= (1 << rowIndex);
                    tcmdcbox &= ~(1 << rowIndex);
                }
                break;

            case COLUMNDOMODE:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if ((intval = getModeVal(value.toString())) >= 0) {
                        tdosett[aix][rowIndex] = intval;
                    }
                }
                break;

            case COLUMNDODUR:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    try {
                        intval = Integer.parseInt(value.toString());
                        tdosett[aix][rowIndex] = intval;
                    }
                    catch (NumberFormatException ex) {
                        // TODO we can raise the format warning
                    }
                }
                break;
            }
        }
    }


    @Override
    public TableCellRenderer getDefaultRenderer(Class<?> type) {
        if (type != Object.class) {
            return super.getDefaultRenderer(type);
        }
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tblData, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                int aix;
                Component cellComponent = super.getTableCellRendererComponent(tblData, value, isSelected, hasFocus, row, column);

                switch (column) {
                case COLUMNDOMODE:
                case COLUMNDODUR:
                    if ((aix = resolveIx(column)) >= 0) {
                        if (tdosett[aix][row] != dosettings[aix][row])
                            cellComponent.setFont(cellComponent.getFont().deriveFont(Font.BOLD));
                    }
                    break;
                }
                return cellComponent;
            }
        };
    }


    @Override
    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel() {
            @Override
            public void addColumn(TableColumn tc) {
                switch (tc.getModelIndex()) {
                case 0:
                    tc.setPreferredWidth(COLWIDTH0);
                    tc.setMinWidth(COLWIDTH0);
                    break;

                case COLUMNDOCTRL:
                    tc.setPreferredWidth(40);
                    break;

                case COLUMNDOMODE:
                    tc.setPreferredWidth(COLWIDTHMODE);
                    tc.setCellEditor(cellEditorCombo);
                    break;

                case COLUMNDODUR:
                    tc.setPreferredWidth(40);
                    break;
                }
                super.addColumn(tc);
            }
        };
    }


    public void updateTable() {
        clearSelection();
        getDefaultEditor(Object.class).stopCellEditing();
        cellEditorCombo.stopCellEditing();

        for (int i = 0; i < dosettings.length; i++) {
            System.arraycopy(dosettings[i], 0, tdosett[i], 0, dogroups << 2);
        }
        model.fireTableDataChanged();
    }


    public int[][] getValues() {
        getDefaultEditor(Object.class).stopCellEditing();
        cellEditorCombo.stopCellEditing();
        return tdosett;
    }


    public void saveChanges() {
        for (int i = 0; i < dosettings.length; i++) {
            System.arraycopy(tdosett[i], 0, dosettings[i], 0, dogroups << 2);
        }
        model.fireTableDataChanged();
    }


    private int resolveIx(int column) {
        int ix = -1;

        switch (column) {
        case COLUMNDOMODE:
            ix = IDOMODE;
            break;
        case COLUMNDODUR:
            ix = IDODUR;
            break;
        }
        return ix;
    }
}
