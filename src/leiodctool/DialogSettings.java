/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import gnu.io.SerialPort;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import static javax.swing.GroupLayout.Alignment.CENTER;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import static leiodctool.ButtonPanel.maxPreferredSize;

/**
 *
 * @author dell
 */
public class DialogSettings extends JDialog {
    private final JComboBox comboType = new JComboBox();
    private final JComboBox comboComPort = new JComboBox();
    private final JComboBox comboBaudrate = new JComboBox();
    private final JRadioButton radioParityNone = new JRadioButton("None");
    private final JRadioButton radioParityEven = new JRadioButton("Even");
    private final JRadioButton radioParityOdd = new JRadioButton("Odd");
    private final CardLayout layoutCard = new CardLayout();
    private final JPanel panelCard = new JPanel();
    private final JTextField textAddress = new JTextField();
    private final JTextField textTxDelay = new JTextField();
    private final JTextField textIPaddr = new JTextField();
    private final JTextField textNetPort = new JTextField();
    private static LeiodcMain.SyncPort portctrl;
    private final CtrlKeyEvent ctrlKeyEvent = new CtrlKeyEvent();


    private class CtrlKeyEvent implements KeyEventDispatcher {
        protected boolean pressed = false;

        @Override
        public boolean dispatchKeyEvent(KeyEvent ke) {
            switch (ke.getID()) {
            case KeyEvent.KEY_PRESSED:
                if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                    pressed = true;
                    //System.out.println("Ctrl pressed!");
                }
                break;

            case KeyEvent.KEY_RELEASED:
                if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                    pressed = false;
                    //System.out.println("Ctrl released!");
                }
                break;
            }
            return false;
        }
    }


    private class MouseRightClick extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent me) {
            if (me.getButton() == MouseEvent.BUTTON3) {    // Button3 = right button
                //System.out.println("Right button pressed");
                if (ctrlKeyEvent.pressed) {
                    //System.out.println("Entering Advanced mode");
                    portctrl.advancedMode();
                    actionOK(null);
                }
            }
        }
    }


    public DialogSettings(JFrame frame, LeiodcMain.SyncPort syncport) {
        super(frame, "Communication settings");
        //JDialog dialog = new JDialog(frame, "Mydialog");

        JPanel panelMain = new JPanel();
        GroupLayout layoutMain = new GroupLayout(panelMain);
        panelMain.setLayout(layoutMain);

        portctrl = syncport;
        final boolean pcopen = syncport.isPortOpen();

        panelCard.setLayout(layoutCard);
        panelCard.add(serialPanel(pcopen));
        panelCard.add(netPanel(pcopen));

        comboType.setToolTipText("Select PC Connection Type");
        comboType.addItem("Serial");
        comboType.addItem("Network");
        comboType.setPreferredSize(new Dimension(100, 20));
        comboType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionTypeSel(ae, pcopen);
            }
         });

        comboType.setSelectedIndex(LeiodcMain.conntype);
        JPanel panelType = createTitledPanel("Connection", 0);
        panelType.add(comboType);

        textAddress.setToolTipText("LEIODC Modbus device address");
        textAddress.setPreferredSize(new Dimension(60, 26));
        textAddress.setText(((Integer) Modbus.devaddr).toString());
        JPanel panelAddress = createTitledPanel("Address", 0);
        panelAddress.add(textAddress);

        textTxDelay.setToolTipText("Time interval between received and sent message in milliseconds");
        textTxDelay.setPreferredSize(new Dimension(60, 26));
        textTxDelay.setText(((Integer) Modbus.txDelay).toString());
        JPanel panelTxDelay = createTitledPanel("TxDelay", 0);
        panelTxDelay.add(textTxDelay);


        ActionListener listenerOK = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionOK(ae);
            }
        };
        ActionListener listenerCancel = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionCancel(ae);
            }
        };
        ButtonPanel panelButtons = new ButtonPanel("OK", "Cancel", listenerOK, listenerCancel, null, null);
        panelButtons.button1.addMouseListener(new MouseRightClick());

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ctrlKeyEvent);


        layoutMain.setHorizontalGroup(layoutMain.createParallelGroup(CENTER)
            .addGroup(layoutMain.createSequentialGroup()
                .addComponent(panelType, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addComponent(panelAddress, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addComponent(panelTxDelay, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
            .addGroup(layoutMain.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelCard, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addGap(10)
                .addContainerGap())
            .addComponent(panelButtons, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
        );
        layoutMain.setVerticalGroup(layoutMain.createParallelGroup(LEADING)
            .addGroup(layoutMain.createSequentialGroup()
                .addContainerGap()
                .addGroup(layoutMain.createParallelGroup(CENTER)
                    .addComponent(panelType, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                    .addComponent(panelAddress, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                    .addComponent(panelTxDelay, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                .addGap(5)
                .addComponent(panelCard, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addGap(5)
                .addComponent(panelButtons, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                .addContainerGap())
        );


        // Assign Main panel to Dialog, set Default button and Escape key event
        panelMain.setOpaque(true);
        setContentPane(panelMain);
        getRootPane().setDefaultButton(panelButtons.button1);
        //ActionListener listenerCancel = this::actionCancel;
        getRootPane().registerKeyboardAction(listenerCancel, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);


        //Show it
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        setModal(true);
        pack();
        setLocationRelativeTo(frame);
        //setVisible(true);
    }


    private JPanel serialPanel(boolean pcopen) {
        JPanel panelSerial = new JPanel();
        GroupLayout layout = new GroupLayout(panelSerial);
        panelSerial.setLayout(layout);

        Dimension comboSize = new Dimension();

        comboComPort.setToolTipText("Select PC COM port");
        comboComPort.setModel(new DefaultComboBoxModel(ComPort.portNames));
        if (ComPort.portNames.length > 0)
            comboComPort.setSelectedIndex(ComPort.selectedPort);
        //comboComPort.setSelectedIndex(1);
        JPanel panelPort = createTitledPanel("COM port", 0);
        panelPort.add(comboComPort);

        comboBaudrate.setToolTipText("Select PC Baudrate");
        //comboBaudrate.addItem("aaa");
        comboBaudrate.setModel(new DefaultComboBoxModel(ComPort.baudlist));
        comboBaudrate.setSelectedItem(ComPort.baudrate);
        JPanel panelBaudrate = createTitledPanel("Baudrate", 0);
        panelBaudrate.add(comboBaudrate);

        maxPreferredSize(comboComPort, comboSize);
        maxPreferredSize(comboBaudrate, comboSize);
        if (comboSize.width < 130)
            comboSize.width = 130;
        comboComPort.setPreferredSize(comboSize);
        comboBaudrate.setPreferredSize(comboSize);


        ButtonGroup bgroup = new ButtonGroup();
        Dimension radioSize = new Dimension();

        maxPreferredSize(radioParityNone, radioSize);
        maxPreferredSize(radioParityEven, radioSize);
        maxPreferredSize(radioParityOdd, radioSize);
        radioParityNone.setPreferredSize(radioSize);
        radioParityEven.setPreferredSize(radioSize);
        radioParityOdd.setPreferredSize(radioSize);
        bgroup.add(radioParityNone);
        bgroup.add(radioParityEven);
        bgroup.add(radioParityOdd);


        switch (ComPort.parity) {
        case SerialPort.PARITY_NONE:
            radioParityNone.setSelected(true);
            break;
        case SerialPort.PARITY_ODD:
            radioParityOdd.setSelected(true);
            break;
        case SerialPort.PARITY_EVEN:
            radioParityEven.setSelected(true);
            break;
        }

        JPanel panelParity = createTitledPanel("Parity", 1);
        panelParity.add(radioParityNone);
        panelParity.add(radioParityEven);
        panelParity.add(radioParityOdd);
        panelParity.setPreferredSize(new Dimension(radioSize.width + 20, ((radioSize.height + 6) * panelParity.getComponentCount()) + 26));


        layout.setHorizontalGroup(layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(panelPort, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                    .addComponent(panelBaudrate, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                .addGap(10)
                .addComponent(panelParity, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
        );
        layout.setVerticalGroup(layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelPort, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                        .addGap(5)
                        .addComponent(panelBaudrate, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                .addComponent(panelParity, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)))
        );
        if (pcopen) {
            comboComPort.setEnabled(false);
            comboBaudrate.setEnabled(false);
            radioParityNone.setEnabled(false);
            radioParityEven.setEnabled(false);
            radioParityOdd.setEnabled(false);
            comboComPort.setToolTipText("Close port to select a different port");
            comboBaudrate.setToolTipText("Close port to change baudrate");
            radioParityNone.setToolTipText("Close port to change parity");
            radioParityEven.setToolTipText("Close port to change parity");
            radioParityOdd.setToolTipText("Close port to change parity");
        }
        return panelSerial;
    }


    private JPanel netPanel(boolean pcopen) {
        JPanel panelNet = new JPanel();
        GroupLayout layout = new GroupLayout(panelNet);
        panelNet.setLayout(layout);

        textIPaddr.setPreferredSize(new Dimension(128, 26));
        textIPaddr.setText(Net.ipaddress.getHostAddress());
        textIPaddr.setToolTipText("LEIODC IP address");
        JPanel panelAddress = createTitledPanel("IP Address", 0);
        panelAddress.add(textIPaddr);

        textNetPort.setPreferredSize(new Dimension(60, 26));
        textNetPort.setText(((Integer) Net.port).toString());
        textNetPort.setToolTipText("LEIODC TCP port number");
        JPanel panelPort = createTitledPanel("Port", 0);
        panelPort.add(textNetPort);


        layout.setHorizontalGroup(layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(panelAddress, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                    .addComponent(panelPort, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                .addGap(10)
                //.addComponent(panelParity, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
            )
        );
        layout.setVerticalGroup(layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panelAddress, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                        .addGap(5)
                        .addComponent(panelPort, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE))
                //.addComponent(panelParity, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                ))
        );
        if (pcopen) {
            textIPaddr.setEnabled(false);
            textNetPort.setEnabled(false);
            textIPaddr.setToolTipText("Disconnect to change IP address");
            textNetPort.setToolTipText("Disconnect to change TCP port number");
        }
        return panelNet;
    }


    private JPanel createTitledPanel(String title, int boxlayout) {
        JPanel panel = new JPanel();

        if (boxlayout == 1) {
            BoxLayout mybox = new BoxLayout(panel, BoxLayout.Y_AXIS);
            //GridLayout mygrid = new GridLayout(3, 1);
           // panel.setLayout(mygrid);
        }
        else {
            panel.setLayout(new BorderLayout());
        }

        TitledBorder titled = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP);
        //TitledBorder titled = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.TOP, new java.awt.Font("Ubuntu", 0, 12));
        titled.setTitleFont(UIManager.getFont("TitledBorder.font").deriveFont(Font.PLAIN));
        panel.setBorder(titled);
        return panel;
    }


    private int convertToInt(String text, int low, int up) {
        int value = -1;

        if (text != null) {
            try {
                int ival = Integer.parseInt(text);
                if ((ival > low) && (ival < up))
                   value = ival;
            }
            catch (NumberFormatException ex) {
                // TODO we can raise the format warning
            }
        }
        return value;
    }


    private void actionTypeSel(ActionEvent ae, boolean pcopen) {
        switch (comboType.getSelectedIndex()) {
        case LeiodcMain.CTYPESERIAL:
            layoutCard.first(panelCard);
            if (pcopen) {
                comboType.setEnabled(false);
                comboType.setToolTipText("Close port to change connection type");
            }
            break;
        case LeiodcMain.CTYPENET:
            layoutCard.first(panelCard);
            layoutCard.next(panelCard);
            if (pcopen) {
                comboType.setEnabled(false);
                comboType.setToolTipText("Disconnect to change connection type");
            }
            break;
        }
    }


    private void actionOK(ActionEvent ae) {
        int intval;

        intval = convertToInt(textAddress.getText(), 0, 255);
        if (intval > 0)
           Modbus.devaddr = intval;

        intval = convertToInt(textTxDelay.getText(), 0, 5000);
        if (intval > 0)
             Modbus.txDelay = intval;

        LeiodcMain.conntype = comboType.getSelectedIndex();

        switch (LeiodcMain.conntype) {
        case LeiodcMain.CTYPESERIAL:
            ComPort.selectedPort = comboComPort.getSelectedIndex();
            ComPort.baudrate = (Integer) comboBaudrate.getSelectedItem();
            if (radioParityNone.isSelected() == true)
                ComPort.parity = SerialPort.PARITY_NONE;
            else if (radioParityEven.isSelected() == true)
                ComPort.parity = SerialPort.PARITY_EVEN;
            else if (radioParityOdd.isSelected() == true)
                ComPort.parity = SerialPort.PARITY_ODD;
            break;

        case LeiodcMain.CTYPENET:
            try {
                Net.ipaddress = InetAddress.getByName(textIPaddr.getText());
            }
            catch (UnknownHostException ex) {
                //Logger.getLogger(DialogSettings.class.getName()).log(Level.SEVERE, null, ex);
            }
            intval = convertToInt(textNetPort.getText(), 1, 65535);
            if (intval > 0)
                Net.port = intval;
            break;
        }
        portctrl.portButtCaption();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(ctrlKeyEvent);
        dispose();
    }


    private void actionCancel(ActionEvent ae) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(ctrlKeyEvent);
        dispose();
    }
}
