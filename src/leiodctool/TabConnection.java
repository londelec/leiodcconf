/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import gnu.io.SerialPort;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.CENTER;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import static leiodctool.ComPort.baudrate;
import static leiodctool.ComPort.parity;
import static leiodctool.ComPort.portNames;
import static leiodctool.ComPort.selectedPort;
import static leiodctool.LeiodcMain.fwversion;
import static leiodctool.Net.ipaddress;
import static leiodctool.Net.port;

/**
 *
 * @author dell
 */
public class TabConnection extends JPanel {
    public static JLabel labelFW = new JLabel("LEIODC FW: ?");
    protected static TableComms ttab;
    private static final JLabel labelPC = new JLabel("Open COM port / Connect via network");
    private static ButtonPanel panelButtons;

    public final WriteStatusLabel labelStatus = new WriteStatusLabel() {
        @Override
        public void onSuccess(Modbus.MsgType reqtype) {
            super.onSuccess(reqtype);
            ttab.saveChanges();
        };
    };


    public TabConnection() {
        JPanel panelLeiodc = new ImagePanel("leiodc_pic.png");
        JPanel panelPC = new ImagePanel("laptop.png");
        JPanel panelLoop = new ImagePanel("loop.png");
        JPanel panelTable = panelComSettings();
        labelPC.setToolTipText("Change PC settings in Edit->Settings");
        GroupLayout layoutMain = new GroupLayout(this);


        layoutMain.setHorizontalGroup(layoutMain.createParallelGroup(LEADING)
            .addGroup(layoutMain.createSequentialGroup()
                .addContainerGap()
                .addGroup(layoutMain.createParallelGroup(LEADING)
                    .addComponent(labelPC, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                    .addGroup(layoutMain.createSequentialGroup()
                            .addGap(148)
                            .addGroup(layoutMain.createParallelGroup(LEADING)
                                .addGroup(layoutMain.createSequentialGroup()
                                    .addComponent(panelLoop, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                                    .addComponent(panelLeiodc, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                                .addComponent(panelTable, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                            .addGap(0))
                    .addComponent(panelPC, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                .addContainerGap())
        );
        layoutMain.setVerticalGroup(layoutMain.createParallelGroup(LEADING)
            .addGroup(layoutMain.createSequentialGroup()
                .addContainerGap()
                .addGroup(layoutMain.createParallelGroup(CENTER)
                    .addGroup(layoutMain.createSequentialGroup()
                        .addComponent(panelPC, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                        .addGap(5)
                        .addComponent(labelPC, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                    .addGroup(layoutMain.createSequentialGroup()
                        .addGap(3)
                        .addComponent(panelLoop, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                    .addComponent(panelLeiodc, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                .addComponent(panelTable, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addContainerGap())
        );
        setLayout(layoutMain);
    }


    private JPanel panelComSettings() {
        JPanel mypanel = new JPanel();
        JLabel tlabel = new JLabel("LEIODC COM port settings");
        tlabel.setToolTipText("LEIODC serial port settings");

        GroupLayout layoutTab = new GroupLayout(mypanel);
        mypanel.setLayout(layoutTab);

        ttab = new TableComms();
        JScrollPane scrollCom = new JScrollPane(ttab);
        int height = ttab.getPreferredSize().height + ttab.getTableHeader().getPreferredSize().height + 6;
        int width = ttab.getPreferredSize().width;
        ttab.setEnabled(false);
        scrollCom.setPreferredSize(new Dimension(width, height));

        ActionListener listenerWrite = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionWrite(ae);
            }
        };
        ActionListener listenerCancel = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionCancel(ae);
            }
        };
        panelButtons = new ButtonPanel("Write Settings", "Cancel Changes", listenerWrite, listenerCancel, "Write settings to device", "Cancel all changes made to the table");
        panelButtons.setEna(false);

        layoutTab.setHorizontalGroup(layoutTab.createParallelGroup(CENTER)
            .addGroup(layoutTab.createSequentialGroup()
                .addContainerGap()
                .addGroup(layoutTab.createParallelGroup(LEADING)
                    .addComponent(labelFW, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                    .addComponent(tlabel, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                    .addComponent(scrollCom, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                    .addComponent(labelStatus, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                .addGap(10)
                .addContainerGap())
            .addComponent(panelButtons, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
        );
        layoutTab.setVerticalGroup(layoutTab.createParallelGroup(LEADING)
            .addGroup(layoutTab.createSequentialGroup()
                .addContainerGap()
                .addComponent(labelFW, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addGap(5)
                .addComponent(tlabel, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addComponent(scrollCom, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addComponent(panelButtons, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addComponent(labelStatus, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addContainerGap())
        );
        return mypanel;
    }

    public void updateComLabel(boolean pcopen, boolean failed) {
        String descr = null;

        if (pcopen == false) {
            ttab.setEnabled(pcopen);
            panelButtons.setEna(pcopen);
        }
        labelStatus.clearStatus();

        updateFWlabel(pcopen);

        switch (LeiodcMain.conntype) {
        case LeiodcMain.CTYPESERIAL:
            if (portNames.length > 0) {
                if (failed) {
                    descr = "Can't open '" +  portNames[selectedPort] + "'";
                }
                else {
                    descr = "Port " + portNames[selectedPort] + "; " + Integer.toString(baudrate) + "; ";
                    switch (parity) {
                    case SerialPort.PARITY_NONE:
                        descr += "N; ";
                        break;
                    case SerialPort.PARITY_ODD:
                        descr += "O; ";
                        break;
                    case SerialPort.PARITY_EVEN:
                        descr += "E; ";
                        break;
                    }
                    descr += "8; 1";
                }
            }
            else {
                failed = true;
                descr = "Port: Not available";
            }
            break;

        case LeiodcMain.CTYPENET:
            if (failed)
                descr = "Can't connect to ";
            else
                descr = "LAN ";
            descr += ipaddress.getHostAddress() + ":" + Integer.toString(port);
            break;
        }

        if (failed)
            labelPC.setForeground(Color.RED);
        else
            labelPC.setForeground(Color.BLACK);
        labelPC.setText(descr);
    }

    public void updateFWlabel(boolean pcopen) {
        if ((!pcopen) || (fwversion < 0))
            labelFW.setText("LEIODC FW: ?");
        else
            labelFW.setText("LEIODC FW: V" + Float.toString(((float) fwversion) / 100));
    }

    public void buttonEnable(boolean ena) {
        panelButtons.setEna(ena);
        if (ena == false)
             ttab.updateTable();
        ttab.setEnabled(ena);
    }

    private void actionWrite(ActionEvent evt) {
        Modbus.writeCOMsettings(labelStatus, ttab.getValues());
    }

    private void actionCancel(ActionEvent evt) {
        ttab.updateTable();
        labelStatus.clearStatus();
    }
}
