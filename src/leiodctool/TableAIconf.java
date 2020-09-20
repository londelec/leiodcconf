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
import static leiodctool.LeiodcMain.IAIMODE;
import static leiodctool.LeiodcMain.aigroups;
import static leiodctool.LeiodcMain.aisettings;


/**
 *
 * @author dell
 */
public class TableAIconf extends TableLE {
    private static final int COLUMNAIMODE = 1;
    //private static final int COLUMNAIFLT = 2;
    private static final int COLUMNSAI = 2;
    protected ModelAI model;
    protected static int[][] taisett = new int[aisettings.length][16];
    private final JComboBox modeCombo;
    private final DefaultCellEditor cellEditorCombo;


    public TableAIconf() {
        ctooltips = new String[COLUMNSAI];
        ctooltips[0] = "Analog Input number";
        ctooltips[COLUMNAIMODE] = "Operation mode";
        //ctooltips[COLUMNAIFLT] = "Input chatter filter value in milliseconds";

        modeNames = new String[] {"1 - Analog Input"};
        modeCombo = new JComboBox(new DefaultComboBoxModel(modeNames));
        cellEditorCombo = new DefaultCellEditor(modeCombo);

        model = new TableAIconf.ModelAI();
        setModel(model);

        for (int j = 0; j < taisett.length; j++) {
            for (int i = 0; i < 16; i++) {
                taisett[j][i] = -1;
            }
        }

        setColumnSelectionAllowed(true);
        setDropMode(DropMode.ON);
        setShowGrid(true);
    }


    protected class ModelAI extends TModelLE {

        public ModelAI() {
            colNames = new String[COLUMNSAI];
            colNames[0] = "AI";
            colNames[COLUMNAIMODE] = "NOT IMPELEMENTED";
            //colNames[COLUMNAIMODE] = "Mode";
            //colNames[COLUMNAIFLT] = "Filter";
        }


        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            int aix;
            Object returnValue = null;

            switch (colIndex) {
            case 0:
                returnValue = rowIndex + 1;
                break;

            case COLUMNAIMODE:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if (taisett[aix][rowIndex] >= 0) {
                        returnValue = getModeString(taisett[aix][rowIndex]);
                    }
                }
                break;

            /*case COLUMNDIFLT:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if (tdisett[aix][rowIndex] >= 0)
                        returnValue = Integer.toString(tdisett[aix][rowIndex]);
                }
                break;*/
            }
            return returnValue;
        }


        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            int intval, aix;

            if (value == null)  // Selected value might be null e.g. when no selection made by comboBox
                return;

            switch (colIndex) {
            case COLUMNAIMODE:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    if ((intval = getModeVal(value.toString())) >= 0) {
                        taisett[aix][rowIndex] = intval;
                    }
                }
                break;

            /*case COLUMNDIFLT:
                if ((aix = resolveIx(colIndex)) >= 0) {
                    try {
                        intval = Integer.parseInt(value.toString());
                        tdisett[aix][rowIndex] = intval;
                    }
                    catch (NumberFormatException ex) {
                        // TODO we can raise the format warning
                    }
                }
                break;*/
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
                case COLUMNAIMODE:
                //case COLUMNAIFLT:
                    if ((aix = resolveIx(column)) >= 0) {
                        if (taisett[aix][row] != aisettings[aix][row])
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

                case COLUMNAIMODE:
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

        for (int i = 0; i < aisettings.length; i++) {
            System.arraycopy(aisettings[i], 0, taisett[i], 0, aigroups << 2);
        }
        model.fireTableDataChanged();
    }


    public int[][] getValues() {
        getDefaultEditor(Object.class).stopCellEditing();
        cellEditorCombo.stopCellEditing();
        return taisett;
    }


    public void saveChanges() {
        for (int i = 0; i < aisettings.length; i++) {
            System.arraycopy(taisett[i], 0, aisettings[i], 0, aigroups << 2);
        }
        model.fireTableDataChanged();
    }


    private int resolveIx(int column) {
        int ix = -1;

        switch (column) {
        case COLUMNAIMODE:
            ix = IAIMODE;
            break;
        /*case COLUMNAIFLT:
            ix = IDIFLT;
            break;*/
        }
        return ix;
    }
}
