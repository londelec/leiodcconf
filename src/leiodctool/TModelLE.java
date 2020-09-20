/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import javax.swing.table.AbstractTableModel;

/**
 *
 * @author dell
 */
public class TModelLE extends AbstractTableModel {
    public int rowcount;
    protected String[] colNames;


    @Override
    public int getRowCount() {
        return rowcount;
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
    public boolean isCellEditable(int rowIndex, int colIndex) {
        return colIndex > 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
       return null;
    }
}
