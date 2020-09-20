/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.CENTER;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import static leiodctool.LeiodcMain.digroups;
import static leiodctool.LeiodcMain.dogroups;
import static leiodctool.LeiodcMain.dostatus;
import static leiodctool.LeiodcMain.aigroups;
import static leiodctool.TableDO.tcmdcbox;


/**
 *
 * @author dell
 */
public class TabDevConfig extends JPanel {
    private final JPanel panelDevImage;
    private final JPanel panelTables;
    private final JLabel labelDevName;
    private JLabel labelDI, labelDO, labelAIconf, labelAIvals;
    private TableDI tableDI;
    private TableDO tableDO;
    private TableAIconf tableAIconf;
    private TableAIvals tableAIvals;
    private JScrollPane scrollPaneDI, scrollPaneDO, scrollPaneAIconf, scrollPaneAIvals;
    private final ButtonPanel panelButtons;
    private final JLabel[] labelIOled = new JLabel[16];
    private final JLabel[] labelCommsLed = new JLabel[8];
    private final JPanel[] panelIOled = new JPanel[16];
    private final JPanel[] panelCommsLed = new JPanel[8];

    private static final Color YELLOWOFF = new Color(64, 64, 0);
    private static final Color YELLOWON = new Color(255, 255, 0);
    private static final Color REDOFF = new Color(64, 0, 0);
    private static final Color REDON = new Color(255, 0, 0);
    private static final Color GREENOFF = new Color(0, 64, 0);
    private static final Color GREENON = new Color(0, 255, 0);
    private static final Color WHITEOFF = new Color(160, 160, 160);
    private static final Color WHITEON = new Color(255, 255, 255);
    private static final Color ORANGEOFF = new Color(96, 40, 0);
    private static final Color ORANGEON = new Color(96, 40, 0);
    private static final Color LEGREEN = new Color(169, 208, 70);


    public final WriteStatusLabel labelStatus = new WriteStatusLabel() {
        @Override
        public void onSuccess(Modbus.MsgType reqtype) {
            super.onSuccess(reqtype);

            if (tableDI != null)
                tableDI.saveChanges();
            if (tableDO != null)
                tableDO.saveChanges();
            if (tableAIconf != null)
                tableAIconf.saveChanges();
        };
    };


    public TabDevConfig(String devname) {
        int i;
        Font ledNameFont = new Font("Sans", Font.BOLD, 10);
        Dimension ledSize = new Dimension(20, 12);

        panelDevImage = new ImagePanel("leiodc_front.png");
        panelTables = new JPanel();

        labelDevName = new JLabel();
        labelDevName.setForeground(LEGREEN);
        labelDevName.setFont(new Font("Sans", Font.BOLD, 13));


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
        panelButtons = new ButtonPanel("Write Settings", "Cancel Changes", listenerWrite, listenerCancel, "Write settings to device", "Cancel all changes made to tables");


        for (i = 0; i < 16; i++) {
            labelIOled[i] = new JLabel();
            labelIOled[i].setLabelFor(panelIOled[i]);
            labelIOled[i].setForeground(Color.white);
            labelIOled[i].setFont(ledNameFont);

            panelIOled[i] = new JPanel();
            panelIOled[i].setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            panelIOled[i].setPreferredSize(ledSize);


            /*ButtonIOctrl[i] = new JButton();
            ButtonIOctrl[i].setForeground(UIManager.getDefaults().getColor("Button.highlight"));
            ButtonIOctrl[i].setToolTipText("Click to send Activate Output");
            ButtonIOctrl[i].setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            ButtonIOctrl[i].setDefaultCapable(false);
            ButtonIOctrl[i].setPreferredSize(new java.awt.Dimension(28, 18));*/
        }

        for (i = 0; i < 8; i++) {
            labelCommsLed[i] = new JLabel();
            labelCommsLed[i].setLabelFor(panelCommsLed[i]);
            labelCommsLed[i].setForeground(Color.white);
            labelCommsLed[i].setFont(ledNameFont);

            panelCommsLed[i] = new JPanel();
            panelCommsLed[i].setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            panelCommsLed[i].setPreferredSize(ledSize);
            panelCommsLed[i].setBackground(GREENOFF);
        }

        labelCommsLed[0].setText("Pwr");
        labelCommsLed[1].setText("Run");
        labelCommsLed[2].setText("Tx1");
        labelCommsLed[3].setText("Rx1");
        labelCommsLed[4].setText("Tx2");
        labelCommsLed[5].setText("Rx2");
        labelCommsLed[6].setText("Tx3");
        labelCommsLed[7].setText("Rx3");
        panelCommsLed[0].setBackground(GREENON);

        setIOtypes();
        createDeviceOverlay(devname);
        createTables();
        initLayout();
    }


    private void initLayout() {
        GroupLayout layoutMain = new GroupLayout(this);
        GroupLayout.SequentialGroup seqH, seqV;

        seqH = layoutMain.createSequentialGroup().addContainerGap();
        seqV = layoutMain.createSequentialGroup().addContainerGap();

        seqH.addGroup(layoutMain.createParallelGroup(LEADING)
            .addComponent(panelDevImage, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
        );
        seqH.addGap(5);


        seqH.addComponent(panelTables);
        seqV.addComponent(panelTables);


        //seqH.addContainerGap(96, Short.MAX_VALUE);

        layoutMain.setHorizontalGroup(layoutMain.createParallelGroup(LEADING).addGroup(seqH));

        //LayoutMyDevcfg.setVerticalGroup(LayoutMyDevcfg.createParallelGroup(LEADING).addGroup(SeqGrpV));
        layoutMain.setVerticalGroup(layoutMain.createParallelGroup(LEADING)
            .addGroup(layoutMain.createSequentialGroup()
                //.addGap(1)
                .addContainerGap()
                .addComponent(panelDevImage, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
            .addGroup(seqV)
        );

        setLayout(layoutMain);
        //Dimension aaa = getPreferredSize();
    }


    private void createDeviceOverlay(String devname) {
        int commsLedRows;
        GroupLayout layoutImage = new GroupLayout(panelDevImage);
        GroupLayout.SequentialGroup seqH, seqNameV, seqCommLedsV, seqIOledsV;
        GroupLayout.Group[] grpLedsH = new GroupLayout.Group[2];
        GroupLayout.Group[] grpRowV = new GroupLayout.Group[8];

        seqH = layoutImage.createSequentialGroup().addGap(12);            // LEDs from the Left edge
        seqNameV = layoutImage.createSequentialGroup().addGap(22);        // Device name from the Ttop
        seqCommLedsV = layoutImage.createSequentialGroup().addGap(117);   // Comms LEDs from the Top
        seqIOledsV = layoutImage.createSequentialGroup().addGap(280);     // IO LEDs from the Top

        labelDevName.setText(devname);
        seqNameV.addComponent(labelDevName);
        commsLedRows = devname.charAt(8) - 0x2f;


        for (int col = 0; col < 2; col++) {
            grpLedsH[col] = layoutImage.createParallelGroup(CENTER);

            overlayCommsLedsH(col, commsLedRows, grpLedsH[col]);
            overlayIOledsH(col, grpLedsH[col]);

            seqH.addGroup(grpLedsH[col]);
            if (dogroups > 2) {             // Smaller gaps required due to LED label size if there are more than 8 DO LEDs
                seqH.addGap(3);          // Between columns of IO LEDs
            }
            else {
                seqH.addGap(7);          // Between columns of IO LEDs
            }
        }

        for (int row = 0; row < commsLedRows; row++) {
            grpRowV[row] = layoutImage.createParallelGroup(LEADING);
            overlayCommsLedsV(layoutImage, row, grpRowV[row]);
            seqCommLedsV.addGroup(grpRowV[row]);

            if (row == 0)
                seqCommLedsV.addGap(34);     // Between power and Comms LED pairs
            else
                seqCommLedsV.addGap(5);      // Between rows of Comm LED and Name pairs
        }

        for (int row = 0; row < 8; row++) {
            grpRowV[row] = layoutImage.createParallelGroup(LEADING);
            overlayIOledsV(layoutImage, row, grpRowV[row]);
            seqIOledsV.addGroup(grpRowV[row]);

            if (row == 3)
                seqIOledsV.addGap(20);       // Between groups of IO LED and Name pairs
            else
                seqIOledsV.addGap(1);        // Between rows of IO LED and Name pairs
        }


        seqH.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        seqIOledsV.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);


        layoutImage.setHorizontalGroup(layoutImage.createParallelGroup(LEADING)
            .addGroup(layoutImage.createSequentialGroup()
                .addGap(4)                      // Device name from the Left edge
                .addComponent(labelDevName))
            .addGroup(seqH)
        );
        layoutImage.setVerticalGroup(layoutImage.createParallelGroup(LEADING)
            .addGroup(seqNameV)
            .addGroup(seqCommLedsV)
            .addGroup(seqIOledsV)
        );
        panelDevImage.setLayout(layoutImage);
    }


    private void overlayIOledsH(int col, GroupLayout.Group grpCol) {
        for (int i = 0; i < 8; i++) {
            grpCol.addComponent(labelIOled[(col << 3) + i]);
            grpCol.addComponent(panelIOled[(col << 3) + i], PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
        }
    }

    private void overlayIOledsV(GroupLayout layoutImage, int row, GroupLayout.Group grpRow) {
        GroupLayout.SequentialGroup seqV;
        GroupLayout.Group grpNamesV, grpLedsV;

        seqV = layoutImage.createSequentialGroup();
        grpNamesV = layoutImage.createParallelGroup(LEADING);
        grpLedsV = layoutImage.createParallelGroup(LEADING);

        for (int i = 0; i < 2; i++) {
            grpNamesV.addComponent(labelIOled[(i << 3) + row]);
            grpLedsV.addComponent(panelIOled[(i << 3) + row], PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
            //GrpLedsV.addComponent(PanelMyIO[(i << 3) + row]);
        }

        seqV.addGroup(grpNamesV);
        seqV.addGroup(grpLedsV);
        grpRow.addGroup(seqV);
    }

    private void overlayCommsLedsH(int col, int rows, GroupLayout.Group grpCol) {
        for (int i = 0; i < rows; i++) {
            grpCol.addComponent(labelCommsLed[(i << 1) + col]);
            grpCol.addComponent(panelCommsLed[(i << 1) + col], PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
        }
    }

    private void overlayCommsLedsV(GroupLayout layoutImage, int row, GroupLayout.Group grpRow) {
        GroupLayout.SequentialGroup seqV;
        GroupLayout.Group grpNamesV, grpLedsV;

        seqV = layoutImage.createSequentialGroup();
        grpNamesV = layoutImage.createParallelGroup(LEADING);
        grpLedsV = layoutImage.createParallelGroup(LEADING);

        for (int i = 0; i < 2; i++) {
            grpNamesV.addComponent(labelCommsLed[(row << 1) + i]);
            grpLedsV.addComponent(panelCommsLed[(row << 1) + i], PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
            //GrpLedsV.addComponent(PanelMyIO[(i << 3) + row]);
        }

        seqV.addGroup(grpNamesV);
        seqV.addGroup(grpLedsV);
        grpRow.addGroup(seqV);
    }


    private void setIOtypes() {
        Integer ionumber;


        for (int i = 0; i < 16; i++) {
            if (
                    (digroups > 3) ||
                    ((digroups > 2) && (i < 12)) ||
                    ((digroups > 1) && (i < 8)) ||
                    ((digroups > 0) && (i < 4))) {

                ionumber = i + 1;
                labelIOled[i].setText("DI" + ionumber.toString());
                panelIOled[i].setBackground(YELLOWOFF);
            }

            if (
                    (dogroups > 3) ||
                    ((dogroups > 2) && (i > 3)) ||
                    ((dogroups > 1) && (i > 7)) ||
                    ((dogroups > 0) && (i > 11))) {

                ionumber = i - ((4 - dogroups) << 2) + 1;
                labelIOled[i].setText("DO" + ionumber.toString());
                panelIOled[i].setBackground(REDOFF);
            }

            if (aigroups > 0) {
                if (i < 8) {
                    ionumber = i + 1;
                    labelIOled[i].setText("AI" + ionumber.toString() + "+");
                    panelIOled[i].setBackground(WHITEOFF);
                }
                else {
                    ionumber = (i - 8) + 1;
                    labelIOled[i].setText("AI" + ionumber.toString() + "-");
                    panelIOled[i].setBackground(ORANGEOFF);
                }
            }
        }

        // Set initial colors of LEDs
        //UpdateDIstatus();
        //updateDOstatus();
    }


    private void createTables() {
        GroupLayout layoutTables = new GroupLayout(panelTables);
        Font settingsFont = new Font("Sans", Font.PLAIN, 14);
        GroupLayout.SequentialGroup seqV;
        GroupLayout.ParallelGroup parH;
        int height;

        parH = layoutTables.createParallelGroup(LEADING);
        seqV = layoutTables.createSequentialGroup();


        if (digroups > 0) {
            tableDI = new TableDI();
            scrollPaneDI = new JScrollPane(tableDI);
            tableDI.model.rowcount = digroups << 2;

            labelDI = new JLabel("Digital Input settings");
            labelDI.setFont(settingsFont);

            parH.addComponent(labelDI);
            parH.addComponent(scrollPaneDI);
            height = tableDI.getPreferredSize().height + tableDI.getTableHeader().getPreferredSize().height + 6;
            seqV.addComponent(labelDI);
            seqV.addComponent(scrollPaneDI, PREFERRED_SIZE, height, PREFERRED_SIZE);
            seqV.addGap(20);
        }

        if (dogroups > 0) {
            tableDO = new TableDO();
            scrollPaneDO = new JScrollPane(tableDO);
            tableDO.model.rowcount = dogroups << 2;

            labelDO = new JLabel("Digital Output settings");
            labelDO.setFont(settingsFont);
            parH.addComponent(labelDO);
            parH.addComponent(scrollPaneDO);
            height = tableDO.getPreferredSize().height + tableDO.getTableHeader().getPreferredSize().height + 6;
            seqV.addComponent(labelDO);
            seqV.addComponent(scrollPaneDO, PREFERRED_SIZE, height, PREFERRED_SIZE);
            seqV.addGap(20);
        }

        if (aigroups > 0) {
            tableAIconf = new TableAIconf();
            tableAIvals = new TableAIvals();
            scrollPaneAIconf = new JScrollPane(tableAIconf);
            scrollPaneAIvals = new JScrollPane(tableAIvals);
            tableAIconf.model.rowcount = aigroups << 1;
            tableAIvals.model.rowcount = aigroups << 1;

            labelAIconf = new JLabel("Analog Input settings");
            labelAIvals = new JLabel("Analog Input values");
            labelAIconf.setFont(settingsFont);
            labelAIvals.setFont(settingsFont);
            parH.addComponent(labelAIvals);
            parH.addComponent(scrollPaneAIvals);
            parH.addComponent(labelAIconf);
            parH.addComponent(scrollPaneAIconf);

            seqV.addComponent(labelAIvals);
            height = tableAIvals.getPreferredSize().height + tableAIvals.getTableHeader().getPreferredSize().height + 6;
            seqV.addComponent(scrollPaneAIvals, PREFERRED_SIZE, height, PREFERRED_SIZE);
            seqV.addGap(10);
            seqV.addComponent(labelAIconf);
            height = tableAIconf.getPreferredSize().height + tableAIconf.getTableHeader().getPreferredSize().height + 6;
            seqV.addComponent(scrollPaneAIconf, PREFERRED_SIZE, height, PREFERRED_SIZE);
            seqV.addGap(20);
        }

        parH.addComponent(panelButtons, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
        seqV.addComponent(panelButtons, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
        parH.addComponent(labelStatus, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
        seqV.addComponent(labelStatus, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);

        layoutTables.setHorizontalGroup(parH);
        layoutTables.setVerticalGroup(seqV);
        panelTables.setLayout(layoutTables);
    }


    public void updateDIstatus() {
        for (int i = 0; i < 16; i++) {
            if (
                    (digroups > 3) ||
                    ((digroups > 2) && (i < 12)) ||
                    ((digroups > 1) && (i < 8)) ||
                    ((digroups > 0) && (i < 4))) {
                if ((LeiodcMain.distatus & (1 << i)) == 0) {
                    panelIOled[i].setBackground(YELLOWOFF);
                    //PanelIOled[i].setToolTipText("DI" + ionumber.toString() + " is OFF");
                }
                else {
                    panelIOled[i].setBackground(YELLOWON);
                }
            }
        }
    }

    public void updateDOstatus() {
        for (int i = 0; i < 16; i++) {
            if (
                    (dogroups > 3) ||
                    ((dogroups > 2) && (i > 3)) ||
                    ((dogroups > 1) && (i > 7)) ||
                    ((dogroups > 0) && (i > 11))) {

                if ((dostatus & (1 << (i - ((4 - dogroups) << 2)))) == 0) {
                    panelIOled[i].setBackground(REDOFF);
                    //PanelIOled[i].setToolTipText("DO" + ionumber.toString() + " is OFF");
                }
                else {
                    panelIOled[i].setBackground(REDON);
                }
            }
        }
        if (dostatus != tcmdcbox) {
            tcmdcbox = dostatus;
            tableDO.model.fireTableDataChanged();
        }
    }


    public void updateAIvalues() {
        if (tableAIvals != null)
            tableAIvals.updateTable();
    }


    public void updateDItab() {
        if (tableDI != null)
            tableDI.updateTable();
    }


    public void updateDOtab() {
        if (tableDO != null)
            tableDO.updateTable();
    }


    public void updateAItab() {
        if (tableAIconf != null)
            tableAIconf.updateTable();
    }


    public void buttonEnable(boolean ena) {
        panelButtons.setEna(ena);

        if (tableDI != null) {
            tableDI.setEnabled(ena);
            if (ena == false)
                tableDI.updateTable();
        }

        if (tableDO != null) {
            tableDO.setEnabled(ena);
            if (ena == false)
                tableDO.updateTable();
        }

        if (tableAIconf != null) {
            tableAIconf.setEnabled(ena);
            if (ena == false)
                tableAIconf.updateTable();
        }
    }


    private void actionWrite(ActionEvent evt) {
        int divals[][] = null, dovals[][] = null, aivals[][] = null;

        if (tableDI != null)
            divals = tableDI.getValues();

        if (tableDO != null)
            dovals = tableDO.getValues();

         if (tableAIconf != null)
            aivals = tableAIconf.getValues();

        Modbus.writeIOcfg(labelStatus, divals, dovals);
    }


    private void actionCancel(ActionEvent evt) {
        updateDItab();
        updateDOtab();
        updateAItab();
        labelStatus.clearStatus();
    }
}
