/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import static leiodctool.LeiodcMain.aivalues;


/**
 *
 * @author dell
 */
public class TableAIvals extends JTable {
    private static final int COLUMNAIVAL = 1;
    private static final int COLUMNAIUNIT = 2;
    private static final int COLUMNAISCALING = 3;
    private static final int COLUMNSAI = 4;
    protected static final int UCUSTOM = 0;
    protected static final int UCURRENT = 1;
    protected static final int UVOLTAGE = 2;
    protected static final int COLWIDTH0 = 28;
    protected static final int COLWIDTHUNIT = 110;
    protected static final float MULTCURRENT = (float) (1024 / (32768 * 49.9));
    protected static final float MULTVOLTAGE = (float) ((1.024 * 22.22) / (32768 * 2.22));
    protected ModelAI model;
    protected static int[] taiunits = new int[8];
    protected static float[] taiscaling = new float[8];
    private final JComboBox unitCombo;

    protected String[] ctooltips;
    private final String[] scaleNames;


    public TableAIvals() {
        ctooltips = new String[COLUMNSAI];
        ctooltips[0] = "Analog Input number";
        ctooltips[COLUMNAIVAL] = "Analog Value";
        ctooltips[COLUMNAIUNIT] = "Select display units";
        ctooltips[COLUMNAISCALING] = "Scaling coefficient";
        scaleNames = new String[] {"custom", "current (mA)", "voltage (V)"};

        unitCombo = new JComboBox(new DefaultComboBoxModel(scaleNames));

        model = new TableAIvals.ModelAI();
        setModel(model);

        for (int i = 0; i < taiscaling.length; i++) {
            taiscaling[i] = 1;
        }

        setColumnSelectionAllowed(true);
        setDropMode(DropMode.ON);
        setShowGrid(true);
    }


    protected class ModelAI extends TModelLE {

        public ModelAI() {
            colNames = new String[COLUMNSAI];
            colNames[0] = "AI";
            colNames[COLUMNAIVAL] = "Value";
            colNames[COLUMNAIUNIT] = "Unit";
            colNames[COLUMNAISCALING] = "Coeff";
        }


        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            if (colIndex <= COLUMNAIVAL)
                return false;
            else if (colIndex == COLUMNAISCALING) {
                switch (taiunits[rowIndex]) {
                case UCURRENT:
                case UVOLTAGE:
                    return false;
                }
            }
            return true;
        }


        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            Object returnValue = null;

            switch (colIndex) {
            case 0:
                returnValue = rowIndex + 1;
                break;

            case COLUMNAIVAL:
                switch (taiunits[rowIndex]) {
                case UCURRENT:
                case UVOLTAGE:
                    DecimalFormat df = new DecimalFormat("#.####");
                    returnValue = df.format(aivalues[rowIndex] * taiscaling[rowIndex]);
                    break;

                default:
                    float fval = aivalues[rowIndex] * taiscaling[rowIndex];

                    if ((fval > 9999) || (fval < -9999)) {
                        returnValue = Float.toString(fval);
                    }
                    else {
                        DecimalFormat ddf = new DecimalFormat("#.#####");
                        returnValue = ddf.format(fval);
                        //returnValue = String.format("%.5f", fval);
                    }
                    break;
                }
                break;

            case COLUMNAIUNIT:
                returnValue = scaleNames[taiunits[rowIndex]];
                break;

            case COLUMNAISCALING:
                returnValue = Float.toString(taiscaling[rowIndex]);
                break;
            }
            return returnValue;
        }


        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            float fval;

            if (value == null)  // Selected value might be null e.g. when no selection made by comboBox
                return;

            switch (colIndex) {
            case COLUMNAIUNIT:
                for (int i = 0; i < scaleNames.length; i++) {
                    if (scaleNames[i].equals(value)) {
                        taiunits[rowIndex] = i;
                        switch (i) {
                        case UCURRENT:
                            taiscaling[rowIndex] = MULTCURRENT;
                            model.fireTableCellUpdated(rowIndex, COLUMNAISCALING);
                            break;
                        case UVOLTAGE:
                            taiscaling[rowIndex] = MULTVOLTAGE;
                            model.fireTableCellUpdated(rowIndex, COLUMNAISCALING);
                            break;
                        default:
                            break;
                        }
                        break;
                    }
                }
                break;

            case COLUMNAISCALING:
                try {
                    fval = Float.parseFloat(value.toString());
                    taiscaling[rowIndex] = fval;
                }
                catch (NumberFormatException ex) {
                    // TODO we can raise the format warning
                }
                break;
            }
        }
    }


    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(MouseEvent me) {
                //String tip = null;
                Point p = me.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return ctooltips[realIndex];
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

                case COLUMNAIUNIT:
                    tc.setPreferredWidth(COLWIDTHUNIT);
                    tc.setCellEditor(new DefaultCellEditor(unitCombo));
                    break;
                }
                super.addColumn(tc);
            }
        };
    }


    public void updateTable() {
        clearSelection();
        getDefaultEditor(Object.class).stopCellEditing();

        model.fireTableDataChanged();
    }
}
