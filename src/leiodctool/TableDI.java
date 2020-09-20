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
import static leiodctool.LeiodcMain.IDIMODE;
import static leiodctool.LeiodcMain.IDIFLT;
import static leiodctool.LeiodcMain.digroups;
import static leiodctool.LeiodcMain.disettings;


/**
 *
 * @author dell
 */
public class TableDI extends TableLE {
    //private static final int COLUMNDISTATUS = 1;
    private static final int COLUMNDIMODE = 1;
    private static final int COLUMNDIFLT = 2;
    private static final int COLUMNSDI = 3;
    protected ModelDI model;
    private static final int[][] tdisett = new int[disettings.length][16];
    private final JComboBox modeCombo;
    private final DefaultCellEditor cellEditorCombo;


    public TableDI() {
        ctooltips = new String[COLUMNSDI];
        ctooltips[0] = "Digital Input number";
        //tooltips[COLUMNDISTATUS] = "Status";
        ctooltips[COLUMNDIMODE] = "Operation mode";
        ctooltips[COLUMNDIFLT] = "Input chatter filter value in milliseconds";

        modeNames = new String[] {"1 - Digital Input"};
        modeCombo = new JComboBox(new DefaultComboBoxModel(modeNames));
        cellEditorCombo = new DefaultCellEditor(modeCombo);

        model = new ModelDI();
        setModel(model);

        for (int j = 0; j < tdisett.length; j++) {
            for (int i = 0; i < 16; i++) {
                tdisett[j][i] = -1;
            }
        }

        setColumnSelectionAllowed(true);
        setDropMode(DropMode.ON);
        setShowGrid(true);
    }


    protected class ModelDI extends TModelLE {

        public ModelDI() {
            colNames = new String[COLUMNSDI];
            colNames[0] = "DI";
            //colNames[COLUMNDISTATUS] = "Status";
            colNames[COLUMNDIMODE] = "Mode";
            colNames[COLUMNDIFLT] = "Filter";
        }


        /*@Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return colIndex > COLUMNDISTATUS;
        }*/


        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            int aix;
            Object returnValue = null;

            switch (colIndex) {
            case 0:
                returnValue = rowIndex + 1;
                break;

            /*case COLUMNDISTATUS:
                if ((DIstatus & (1 << rowIndex)) == 0) {
                    //returnValue = Integer.toString(0);
                    returnValue = "OFF";
                }
                else {
                    //returnValue = Integer.toString(1);
                    returnValue = "ON";
                }
                break;*/

            case COLUMNDIMODE:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if (tdisett[aix][rowIndex] >= 0) {
                        returnValue = getModeString(tdisett[aix][rowIndex]);
                    }
                }
                break;

            case COLUMNDIFLT:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if (tdisett[aix][rowIndex] >= 0)
                        returnValue = Integer.toString(tdisett[aix][rowIndex]);
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
            case COLUMNDIMODE:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if ((intval = getModeVal(value.toString())) >= 0) {
                        tdisett[aix][rowIndex] = intval;
                    }
                }
                break;

            case COLUMNDIFLT:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    try {
                        intval = Integer.parseInt(value.toString());
                        tdisett[aix][rowIndex] = intval;
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
                case COLUMNDIMODE:
                case COLUMNDIFLT:
                    if ((aix = resolveIx(column)) >= 0) {
                        if (tdisett[aix][row] != disettings[aix][row])
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

                case COLUMNDIMODE:
                    tc.setPreferredWidth(COLWIDTHMODE);
                    tc.setCellEditor(cellEditorCombo);
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

        for (int i = 0; i < disettings.length; i++) {
            System.arraycopy(disettings[i], 0, tdisett[i], 0, digroups << 2);
        }
        model.fireTableDataChanged();
    }


    public int[][] getValues() {
        getDefaultEditor(Object.class).stopCellEditing();
        cellEditorCombo.stopCellEditing();
        return tdisett;
    }


    public void saveChanges() {
        for (int i = 0; i < disettings.length; i++) {
            System.arraycopy(tdisett[i], 0, disettings[i], 0, digroups << 2);
        }
        model.fireTableDataChanged();
    }


    private int resolveIx(int column) {
        int ix = -1;

        switch (column) {
        case COLUMNDIMODE:
            ix = IDIMODE;
            break;
        case COLUMNDIFLT:
            ix = IDIFLT;
            break;
        }
        return ix;
    }
}
