/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author dell
 */
public class TableComms extends TableLE {
    private static final int ROWBAUDRATE = 0;
    private static final int ROWPARITY = 1;
    private static final int ROWTXDELAY = 2;
    private static final int ROWTIMEOUT = 3;
    private static final int ROWT35 = 4;
    private static final int ROWDEVADDR = 5;
    private static final int ROWINTERFACE = 6;
    private static final int ROWCOMMS = 7;
    private static final int COLUMNNAME = 0;
    private static final int COLUMNVAL = 1;
    private static final int COLUMNDEFAULT = 2;
    private static final int COLUMNSCOMMS = 3;
    private static final int BROFFSET = 17;
    private final int[] tcomms = new int[ROWCOMMS];
    private final int[] tdefaults = new int[ROWCOMMS];
    private final String[] rtooltips;
    private final String[] rowNames;
    private final ModelComms model;
    private static final String[] interfaceList = new String[] {"RS485", "RS232", "RS422"};
    private static final String[] parityList = new String[] {"None", "Even", "Odd"};
    private final JComboBox baudrateCombo = new JComboBox(new DefaultComboBoxModel(ComPort.baudlist));
    private final JComboBox parityCombo = new JComboBox(new DefaultComboBoxModel(parityList));
    private final JComboBox interfaceCombo = new JComboBox(new DefaultComboBoxModel(interfaceList));
    private final JTextField textEditor = new JTextField();
    //private final JFormattedTextField textEditor = new JFormattedTextField();
    private final MyCellEditor myCellEditor;
    private static final String[] colNames = new String[] {
        "Name", "Value", "Default"
    };


    public TableComms() {
        ctooltips = new String[COLUMNSCOMMS];
        ctooltips[COLUMNNAME] = "Parameter Name";
        ctooltips[COLUMNVAL] = "Value";
        ctooltips[COLUMNDEFAULT] = "Ticked when default value is entered";

        rowNames = new String[ROWCOMMS];
        rowNames[ROWBAUDRATE] = "Baudrate";
        rowNames[ROWPARITY] = "Parity";
        rowNames[ROWTXDELAY] = "TxDelay";
        rowNames[ROWTIMEOUT] = "Timeout";
        rowNames[ROWT35] = "T35 timeout";
        rowNames[ROWDEVADDR] = "Address";
        rowNames[ROWINTERFACE] = "Interface";

        rtooltips = new String[ROWCOMMS];
        rtooltips[ROWBAUDRATE] = "Baudrate";
        rtooltips[ROWPARITY] = "Parity";
        rtooltips[ROWTXDELAY] = "Time interval between received and sent message in milliseconds [0.1...6553]";
        rtooltips[ROWTIMEOUT] = "Message timeout in milliseconds [0.1...6553]";
        rtooltips[ROWT35] = "T35 timeout in milliseconds [0.1...6553]";
        rtooltips[ROWDEVADDR] = "Modbus device address [1...254]";
        rtooltips[ROWINTERFACE] = "Serial port interface";

        tdefaults[ROWBAUDRATE] = 22;    // 9600
        tdefaults[ROWPARITY] = 'E';
        tdefaults[ROWTXDELAY] = 0;      // (x100usec)
        tdefaults[ROWT35] = calcT35(tdefaults[ROWBAUDRATE]);  // (x100usec)
        tdefaults[ROWTIMEOUT] = tdefaults[ROWT35];
        tdefaults[ROWDEVADDR] = 1;
        tdefaults[ROWINTERFACE] = 0;    // RS485


        for (int i = 0; i < ROWCOMMS; i++) {
            tcomms[i] = -1;
        }


        model = new ModelComms();
        setModel(model);

        textEditor.setBorder(BorderFactory.createEmptyBorder());
        myCellEditor = new MyCellEditor(textEditor);
        //setDefaultEditor(Object.class, new MyCellEditor());
        setDefaultEditor(Object.class, myCellEditor);

        // Paste 'CTRL + V' action
        KeyStroke pasteKey = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false);
        Object pasteCmd = "paste";
        getInputMap().put(pasteKey, pasteCmd);
        getActionMap().put(pasteCmd, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionPaste(ae);
            }
        });

        setColumnSelectionAllowed(true);
        setDropMode(DropMode.ON);
        setShowGrid(true);
    }


    private class ModelComms extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return ROWCOMMS;
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int colIndex) {
            return colNames[colIndex];
        }

        @Override
        public Class getColumnClass(int colIndex) {
            if (colIndex == COLUMNDEFAULT)
                return Boolean.class;
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int colIndex) {
            return colIndex > 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int colIndex) {
            Object returnValue = null;

            switch (colIndex) {
            case COLUMNNAME:
                returnValue = rowNames[rowIndex];
                break;

            case COLUMNVAL:
                if (tcomms[rowIndex] >= 0) {    // Don't display values if -1
                    switch (rowIndex) {
                    case ROWBAUDRATE:
                         if (tcomms[rowIndex] >= BROFFSET) {
                            returnValue = ComPort.baudlist[tcomms[rowIndex] - BROFFSET];
                         }
                         break;

                    case ROWPARITY:
                        for (String par : parityList) {
                            if (par.charAt(0) == tcomms[rowIndex]) {
                                returnValue = par;
                                break;
                            }
                        }
                        break;

                    case ROWINTERFACE:
                        if (tcomms[rowIndex] < interfaceList.length) {
                            returnValue = interfaceList[tcomms[rowIndex]];
                        }
                        break;

                    default:
                         if (rowIndex == ROWDEVADDR)
                            returnValue = tcomms[rowIndex];
                         else
                            returnValue = ((float) tcomms[rowIndex]) / 10;
                        break;
                    }
                }
                break;

            case COLUMNDEFAULT:
                returnValue = (tdefaults[rowIndex] == tcomms[rowIndex]);
                break;
            }
            return returnValue;
        }


        @Override
        public void setValueAt(Object value, int rowIndex, int colIndex) {
            int intval;
            float fval;

            switch (colIndex) {
            case COLUMNVAL:
                switch (rowIndex) {
                case ROWBAUDRATE:
                    intval = baudrateCombo.getSelectedIndex() + BROFFSET;
                    tcomms[rowIndex] = intval;
                    tdefaults[ROWT35] = calcT35(tcomms[ROWBAUDRATE]);
                    tdefaults[ROWTIMEOUT] = tdefaults[ROWT35];
                    fireTableCellUpdated(ROWT35, COLUMNDEFAULT);      // Update Default T35
                    fireTableCellUpdated(ROWTIMEOUT, COLUMNDEFAULT);  // Update Default timeout
                    break;

                case ROWPARITY:
                    intval = ((String) parityCombo.getSelectedItem()).charAt(0);
                    tcomms[rowIndex] = intval;
                    break;

                case ROWINTERFACE:
                    intval = interfaceCombo.getSelectedIndex();
                    tcomms[rowIndex] = intval;
                    break;

                default:
                    if (value == null)  // Selected value might be null e.g. when no selection made by comboBox
                        return;

                   try {
                        fval = Float.valueOf(value.toString());
                        //intval = Integer.parseInt(value.toString());
                        if (rowIndex == ROWDEVADDR)
                            intval = (int) (fval);
                        else
                            intval = (int) ((fval + 0.05) * 10);
                        tcomms[rowIndex] = intval;
                    }
                    catch (NumberFormatException ex) {
                        // TODO we can raise the format warning
                    }
                    break;
                }
                fireTableCellUpdated(rowIndex, COLUMNDEFAULT);  // Update Default Checkbox
                break;

            case COLUMNDEFAULT:
                if (value == null)  // Precautionary measure
                    return;

                if ((Boolean) value == true) {
                    tcomms[rowIndex] = tdefaults[rowIndex];
                    fireTableCellUpdated(rowIndex, COLUMNVAL);  // Update Value
                }
                break;
            }
        }
    }

    //private class MyCellEditor extends AbstractCellEditor implements TableCellEditor {
    private class MyCellEditor extends DefaultCellEditor {

        public MyCellEditor(JTextField jtf) {
        //public MyCellEditor(JFormattedTextField jtf) {
            super(jtf);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            //Component editor = defEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
            Component editor = super.getTableCellEditorComponent(table, value, isSelected, row, column);

            switch (column) {
            case COLUMNVAL:
                switch (row) {
                case ROWBAUDRATE:
                    editor = baudrateCombo;
                    break;
                case ROWPARITY:
                    editor = parityCombo;
                    break;
                /*case ROWT35:
                    Locale myLocale = Locale.getDefault();
                    NumberFormat numberFormatB = NumberFormat.getInstance(myLocale);
                    numberFormatB.setMaximumFractionDigits(1);
                    JFormattedTextField jeditor = (JFormattedTextField) editor;
                    jeditor.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new NumberFormatter(numberFormatB)));
                    break;*/
                case ROWINTERFACE:
                    editor = interfaceCombo;
                    break;
                }
                break;
            }
            return editor;
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

                Component cellComponent = super.getTableCellRendererComponent(tblData, value, isSelected, hasFocus, row, column);

                switch (column) {
                case COLUMNVAL:
                    if (tcomms[row] != LeiodcMain.devcom[row])
                        cellComponent.setFont(cellComponent.getFont().deriveFont(Font.BOLD));
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
                case COLUMNNAME:
                    tc.setPreferredWidth(100);
                    break;

                case COLUMNVAL:
                    tc.setPreferredWidth(106);
                    break;

                case COLUMNDEFAULT:
                    tc.setPreferredWidth(58);
                    //tc.setCellEditor(new DefaultCellEditor(defaultCombo));
                    break;
                }
                super.addColumn(tc);
            }
        };
    }


    @Override
    public String getToolTipText(MouseEvent me) {
        String tip = null;
        Point p = me.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);

        switch (colIndex) {
        case COLUMNNAME:
        //case COLUMNVAL:
            tip = rtooltips[rowIndex];
            break;

        default:
            break;
        }
        return tip;
    }


    public void updateTable() {
        clearSelection();
        myCellEditor.stopCellEditing();
        System.arraycopy(LeiodcMain.devcom, 0, tcomms, 0, tcomms.length);
        tdefaults[ROWT35] = calcT35(tcomms[ROWBAUDRATE]);
        tdefaults[ROWTIMEOUT] = tdefaults[ROWT35];
        model.fireTableDataChanged();
    }

    public void clearTable() {
        for (int i = 0; i < ROWCOMMS; i++) {
            tcomms[i] = -1;
        }
        model.fireTableDataChanged();
    }

    public void saveChanges() {
        System.arraycopy(tcomms, 0, LeiodcMain.devcom, 0, tcomms.length);
        model.fireTableDataChanged();
    }

    public int[] getValues() {
        myCellEditor.stopCellEditing();
        return tcomms;
    }

    private int calcT35 (int baudrate) {
        if ((baudrate > BROFFSET) && ((baudrate - BROFFSET) < ComPort.baudlist.length))
            return (350000 / (ComPort.baudlist[baudrate - BROFFSET]));
        return 10;  // Default 1msec
    }

    private void actionPaste(ActionEvent ae) {
        //String aaa = ae.getActionCommand();
    }
}
