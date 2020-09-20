/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

/**
 *
 * @author dell
 */
public class TableLE extends JTable {
    protected String[] ctooltips;
    protected static final int COLWIDTH0 = 28;
    protected static final int COLWIDTHMODE = 110;
    protected String[] modeNames;


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

    protected final int getModeVal(String modeStr) {
        int value = -1;
        int len = modeStr.indexOf(" - ");               // Index where " - " starts
        String numString = modeStr.substring(0, len);   // Get number characters of the string
        try {
            value = Integer.parseInt(numString);
        }
        catch (NumberFormatException ex) {
            // TODO we can raise the format warning
        }
        return value;
    }

    protected final String getModeString(int modeNum) {
        for (String mdStr : modeNames) {
            if (getModeVal(mdStr) == modeNum) {
                return mdStr;
            }
        }
        return Integer.toString(modeNum) + " - Unknown";
    }
}
