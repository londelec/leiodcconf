/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.CENTER;
import static javax.swing.GroupLayout.Alignment.LEADING;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import leiodctool.Modbus.RxState;

/**
 *
 * @author dell
 */
public class TraceLog extends JPanel {
    private static JTextArea textLog;
    private static JCheckBox checkBoxLogEnable;
    private static String msgbuff;


    public TraceLog() {
        GroupLayout layoutMain = new GroupLayout(this);
        setLayout(layoutMain);

        JScrollPane scrollLog = new JScrollPane();
        textLog = new JTextArea();
        checkBoxLogEnable = new JCheckBox();
        JButton buttonClear = new JButton();

        //TextTrace.setColumns(20);
        //TextTrace.setRows(5);
        scrollLog.setViewportView(textLog);

        checkBoxLogEnable.setText("Log Enabled");
        checkBoxLogEnable.setToolTipText("Enable/Disable logging");

        buttonClear.setText("*");
        buttonClear.setToolTipText("Clear trace");
        buttonClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionClear(ae);
            }
        });


        layoutMain.setHorizontalGroup(layoutMain.createParallelGroup(LEADING)
                .addGroup(layoutMain.createSequentialGroup()
                    .addComponent(checkBoxLogEnable)
                    .addGap(10)
                    .addComponent(buttonClear))
                .addComponent(scrollLog)
        );
        layoutMain.setVerticalGroup(layoutMain.createParallelGroup(LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layoutMain.createSequentialGroup()
                .addGroup(layoutMain.createParallelGroup(CENTER)
                    .addComponent(checkBoxLogEnable)
                    .addComponent(buttonClear))
                .addGap(2)
                .addComponent(scrollLog))
        );
    }


    public static void rxLog(byte[] rxbuff, int rxlength, RxState cstate) {
        msgbuff = ("?\n");

        switch (cstate) {
        case RXOK:
            msgbuff = "< ";
            formatmsg(rxbuff, rxlength);
            break;

        case RXTIMEOUT:
            msgbuff = "Message receive timeout\n";
            break;

        case RXDATAERR:
            msgbuff = "ERR< ";
            formatmsg(rxbuff, rxlength);
            break;

        case RXCRCERR:
            msgbuff = "CRC ERR< ";
            formatmsg(rxbuff, rxlength);
            break;
        }

        if (checkBoxLogEnable.isSelected() == true)
            textLog.append(msgbuff);
    }


    public static void txLog(byte[] txbuff, int txlength) {
        msgbuff = "> ";
        formatmsg(txbuff, txlength);

        if (checkBoxLogEnable.isSelected() == true)
            textLog.append(msgbuff);
    }


    public static void msgLog(String msg) {
        //if (checkBoxLogEnable.isSelected() == true) {
            textLog.append(msg);
            textLog.append("\n");
        //}
    }


    private static void formatmsg(byte[] cbuff, int count) {
        int bytecnt, ubyte;
        StringBuilder sbuilder = new StringBuilder();

        for (bytecnt = 0; bytecnt < count; bytecnt++) {
            //msgbuff += " ";
            sbuilder.append(" ");

            if (cbuff[bytecnt] < 0) {
                ubyte = 256 + cbuff[bytecnt];
            }
            else
                ubyte = cbuff[bytecnt];

            if (ubyte < 16)
                sbuilder.append("0");

            sbuilder.append(Integer.toHexString(ubyte));
        }
        sbuilder.append("\n");
        msgbuff += sbuilder.toString();
    }


    private static void actionClear(ActionEvent evt) {
        textLog.setText("");
    }
}
