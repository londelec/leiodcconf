/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 *
 * @author dell
 */
public class LeiodcMain extends javax.swing.JFrame {
    //public static LEIODCmain mainFrame;
    private static Modbus modthread;
    public static ComPort serport;
    public static Net netcomms;

    public static final int CTYPESERIAL = 0;
    public static final int CTYPENET = 1;
    public static int conntype = CTYPESERIAL;

    public static String devidname;
    public static int fwversion = -1;
    public static int digroups = 0, dogroups = 0;
    public static int aigroups = 0, aogroups = 0;
    public static int distatus = 0, dostatus = 0;
    public static final int[] aivalues = new int[8];
    public static int[] devcom = new int[7];
    public static final int[][] disettings = new int[2][16];
    public static final int[][] dosettings = new int[2][16];
    public static final int[][] aisettings = new int[1][16];
    public static final int IDIMODE = 0;
    public static final int IDIFLT = 1;
    public static final int IDOMODE = 0;
    public static final int IDODUR = 1;
    public static final int IAIMODE = 0;

    private static TabDevConfig panelDevCfg;
    private static TabConnection panelComms;
    private JMenuItem menuItemAbout, menuItemSettings, menuItemRestart, menuItemDump, menuItemExit;
    private javax.swing.JLabel labelActivity;
    private javax.swing.JPanel ledActivity;
    private static int actshow;
    private static long actTime;
    private static boolean devonline;

    private static final String SWVERSION = "V1.0";
    private static final String COPYRIGHT = "© 2020 Londelec UK Ltd\nThis program comes with absolutely no warranty.";
    private static String buildDate = "Not available";

    private final SyncPort syncPort = new SyncPort();

    static int debugcnt = 0;

    /**
     * Creates new form NewJFrame
     * Library path on various OS:
     *
     * os.name: Linux
     * os.arch: i386
     * java.library.path:
     *  /usr/lib/jvm/java-8-openjdk-i386/jre/lib/amd64:
     *  /usr/lib/jvm/java-8-openjdk-i386/jre/lib/i386:
     *  /usr/java/packages/lib/i386:
     *  /usr/lib/i386-linux-gnu/jni:
     *  /lib/i386-linux-gnu:
     *  /usr/lib/i386-linux-gnu:
     *  /usr/lib/jni:
     *  /lib:
     *  /usr/lib:.
     *
     * C:\Users\VBox\Documents\leiodctool>java -jar leiodctool.jar
     * os.name: Windows 7
     * os.arch: x86
     * java.library.path:
     *  C:\ProgramData\Oracle\Java\javapath;
     *  C:\Windows\Sun\Java\bin;
     *  C:\Windows\system32;
     *  C:\Windows;.
     *
     * C:\Program Files\Java\jre1.8.0_261\bin>java -jar d:\Documents\LEIODC\leiodctool.jar
     * os.name: Windows 7
     * os.arch: amd64
     * java.library.path:
     *  C:\Program Files\Java\jre1.8.0_261\bin;
     *  C:\Windows\Sun\Java\bin;
     *  C:\Program Files (x86)\Common Files\Oracle\Java\javapath;
     *  C:\Windows\system32;
     *  C:\Windows;.
     *
     */
    public LeiodcMain() {
        //System.out.println("os.name: " + System.getProperty("os.name"));
        //System.out.println("os.arch: " + System.getProperty("os.arch"));
        //System.out.println("java.library.path: " + System.getProperty("java.library.path"));  // Should contain '/usr/lib/jni' this is where we need to put librxtxSerial-2.2pre1.so

        initComponents();

        panelComms = new TabConnection();
        TPane.addTab("Connection", panelComms);
        TPane.addTab("Trace", new TraceLog());
        //TabbedDev.setEnabledAt(1, false);
        TPane.remove(0);
        ButtonTest.setVisible(false);
        ButtonPoll.setEnabled(false);
        jMenuDevice.setVisible(false);
        jMenuDevice.setEnabled(false);

        BufferedImage image = null;
        try {
            image = ImageIO.read(getClass().getResource("/leiodctool/gnibbles.png"));
        } catch (IOException ex) {
            Logger.getLogger(LeiodcMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (image != null)
            setIconImage(image);

        checkLibrary();
        initMenu();
        initComms();
        initRevision();

        //Dimension aaa = new Dimension(460, 660);
        //Dimension aaa = getSize();
        //setSize(aaa);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (syncPort.isPortOpen()) {
                    switch (conntype) {
                    case CTYPESERIAL:
                        serport.closePort();
                        break;

                    case CTYPENET:
                        Net.closeSocket();
                        break;
                    }
                }
            }
        });
    }


    private class ReadEvent implements Modbus.ModbusEvent {

        @Override
        public void onSuccess(Modbus.MsgType reqtype) {
            switch (reqtype) {
            case DEVID:
                /*if (debugcnt == 0)
                    debugcnt++;
                else
                    digroups = 4;*/

                if (panelDevCfg == null) {
                    panelDevCfg = new TabDevConfig(devidname);
                    TPane.addTab(devidname, panelDevCfg);
                }
                else if (!devidname.equals(TPane.getTitleAt(TPane.indexOfComponent(panelDevCfg)))) {
                    TPane.remove(panelDevCfg);
                    panelDevCfg = new TabDevConfig(devidname);
                    TPane.addTab(devidname, panelDevCfg);
                }
                break;

            case RDFWVER:
                panelComms.updateFWlabel(syncPort.isPortOpen());
                break;

            case RDCOMCFG:
                TabConnection.ttab.updateTable();
                break;

            case DISTATUS:
                panelDevCfg.updateDIstatus();
                break;

            case DOSTATUS:
                panelDevCfg.updateDOstatus();
                break;

            case AIVALUES:
                panelDevCfg.updateAIvalues();
                break;

            case RDDICFG:
                panelDevCfg.updateDItab();
                break;

            case RDDOCFG:
                panelDevCfg.updateDOtab();
                break;

            case RDAICFG:
                panelDevCfg.updateAItab();
                break;

            case RDCONFIG:
                String ascii = "";
                for (int i = 0; i < Modbus.configbuff.length; i++) {
                    ascii += "0x" + Integer.toHexString(Modbus.configbuff[i]);
                    if (i < (Modbus.configbuff.length - 1))
                        ascii += ",";
                }
                writeFile("leiodc.txt", ascii);
                break;
            }
            activityLed();
        };

        @Override
        public void onFail(Modbus.MsgType reqtype) {
            // TODO Modbus Exception received
        };
    }


    public class SyncPort {
        private boolean pcopen = false;

        public void openClose(boolean openp) {
            String failmsg;

            synchronized (syncPort) {
                if (openp == syncPort.pcopen)
                    return;

                if (openp == false) {
                    modthread.pollToggle(true);   // This is synchronized, may block
                    fwversion = -1;
                    TPane.remove(panelDevCfg);
                    panelDevCfg = null;
                    TabConnection.ttab.clearTable();
                }


                switch (conntype) {
                case CTYPESERIAL:
                    if (openp == false) {
                        serport.closePort();
                    }
                    else {
                        if ((failmsg = serport.openPort()) != null) {
                            TraceLog.msgLog(failmsg);
                            panelComms.updateComLabel(openp, true);
                            return;     // Port open failed
                        }
                    }
                    break;

                case CTYPENET:
                    if (openp == false) {
                        if ((failmsg = Net.closeSocket()) != null) {
                            TraceLog.msgLog(failmsg);
                        }
                    }
                    else {
                        if ((failmsg = Net.connectSocket()) != null) {
                            TraceLog.msgLog(failmsg);
                            panelComms.updateComLabel(openp, true);
                            return;     // Connect failed
                        }
                    }
                    break;
                }

                panelComms.updateComLabel(openp, false);
                ButtonPoll.setEnabled(openp);
                syncPort.pcopen = openp;
                portButtCaption();
            }
        }

        public boolean isPortOpen() {
            synchronized(syncPort) {
                return syncPort.pcopen;
            }
        }

        public void portButtCaption() {
            boolean openp = syncPort.isPortOpen();

            switch (conntype) {
            case CTYPESERIAL:
                ButtonPort.setToolTipText("Open/Close PC serial port");
                if (openp == false)
                    ButtonPort.setText("Open Port");
                else
                    ButtonPort.setText("Close Port");
                break;

            case CTYPENET:
                ButtonPort.setToolTipText("Connect/Disconnect to LEIODC");
                if (openp == false)
                    ButtonPort.setText("Connect");
                else
                    ButtonPort.setText("Disconnect");
                break;
            }
        }

        public void pollOnOff(boolean ena) {
            if (ena == true)
                ButtonPoll.setText("Disable");
            else {
                ButtonPoll.setText("Enable");
                offline();
            }

            panelComms.buttonEnable(ena);
            panelComms.labelStatus.clearStatus();
            jMenuDevice.setEnabled(ena);

            if (panelDevCfg != null) {
                panelDevCfg.buttonEnable(ena);
                panelDevCfg.labelStatus.clearStatus();
            }
        }

        public void timeout() {
            labelActivity.setText("Timeout");
            ledActivity.setVisible(false);
            devonline = false;
        }

        public void offline() {
            labelActivity.setText("Offline");
            ledActivity.setVisible(false);
            devonline = false;
        }

        public void advancedMode() {
            jMenuDevice.setVisible(true);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ButtonPort = new javax.swing.JButton();
        ButtonPoll = new javax.swing.JButton();
        TPane = new javax.swing.JTabbedPane();
        PanelSampleDev = new javax.swing.JPanel();
        PanelSampleGroup = new javax.swing.JPanel();
        LabelSample1 = new javax.swing.JLabel();
        LabelSample2 = new javax.swing.JLabel();
        PanelSampleIO1 = new javax.swing.JPanel();
        ButtonSampleIO1 = new javax.swing.JButton();
        PanelSampleIO2 = new javax.swing.JPanel();
        TextSampleIO1 = new javax.swing.JTextField();
        TextSampleIO2 = new javax.swing.JTextField();
        ButtonTest = new javax.swing.JButton();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuEdit = new javax.swing.JMenu();
        jMenuDevice = new javax.swing.JMenu();
        jMenuHelp = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LEIODC tool");

        ButtonPort.setText("Open Port");
        ButtonPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonPortActionPerformed(evt);
            }
        });

        ButtonPoll.setText("Enable");
        ButtonPoll.setToolTipText("Enable/Disable communication to LEIODC");
        ButtonPoll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonPollActionPerformed(evt);
            }
        });

        TPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        PanelSampleGroup.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Group1", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Ubuntu", 0, 12))); // NOI18N
        PanelSampleGroup.setName(""); // NOI18N

        LabelSample1.setLabelFor(ButtonSampleIO1);
        LabelSample1.setText("IO1");

        LabelSample2.setLabelFor(ButtonSampleIO1);
        LabelSample2.setText("IO2");

        PanelSampleIO1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        PanelSampleIO1.setPreferredSize(new java.awt.Dimension(32, 22));

        ButtonSampleIO1.setForeground(javax.swing.UIManager.getDefaults().getColor("Button.highlight"));
        ButtonSampleIO1.setText("Bu");
        ButtonSampleIO1.setToolTipText("");
        ButtonSampleIO1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        ButtonSampleIO1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        ButtonSampleIO1.setDefaultCapable(false);
        ButtonSampleIO1.setName(""); // NOI18N
        ButtonSampleIO1.setPreferredSize(new java.awt.Dimension(28, 18));

        javax.swing.GroupLayout PanelSampleIO1Layout = new javax.swing.GroupLayout(PanelSampleIO1);
        PanelSampleIO1.setLayout(PanelSampleIO1Layout);
        PanelSampleIO1Layout.setHorizontalGroup(
            PanelSampleIO1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelSampleIO1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(ButtonSampleIO1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        PanelSampleIO1Layout.setVerticalGroup(
            PanelSampleIO1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelSampleIO1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(ButtonSampleIO1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        PanelSampleIO2.setBackground(new java.awt.Color(243, 126, 9));
        PanelSampleIO2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        PanelSampleIO2.setPreferredSize(new java.awt.Dimension(32, 22));

        javax.swing.GroupLayout PanelSampleIO2Layout = new javax.swing.GroupLayout(PanelSampleIO2);
        PanelSampleIO2.setLayout(PanelSampleIO2Layout);
        PanelSampleIO2Layout.setHorizontalGroup(
            PanelSampleIO2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 28, Short.MAX_VALUE)
        );
        PanelSampleIO2Layout.setVerticalGroup(
            PanelSampleIO2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 18, Short.MAX_VALUE)
        );

        TextSampleIO1.setToolTipText("DIfilter");
        TextSampleIO1.setPreferredSize(new java.awt.Dimension(40, 26));

        TextSampleIO2.setPreferredSize(new java.awt.Dimension(40, 26));

        javax.swing.GroupLayout PanelSampleGroupLayout = new javax.swing.GroupLayout(PanelSampleGroup);
        PanelSampleGroup.setLayout(PanelSampleGroupLayout);
        PanelSampleGroupLayout.setHorizontalGroup(
            PanelSampleGroupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelSampleGroupLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PanelSampleGroupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(PanelSampleGroupLayout.createSequentialGroup()
                        .addComponent(PanelSampleIO1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addGroup(PanelSampleGroupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(LabelSample1)
                            .addComponent(LabelSample2)))
                    .addComponent(PanelSampleIO2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(PanelSampleGroupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TextSampleIO2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(TextSampleIO1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(25, 25, 25))
        );
        PanelSampleGroupLayout.setVerticalGroup(
            PanelSampleGroupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelSampleGroupLayout.createSequentialGroup()
                .addGroup(PanelSampleGroupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(PanelSampleIO1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(TextSampleIO1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(LabelSample1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(1, 1, 1)
                .addGroup(PanelSampleGroupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(LabelSample2, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(PanelSampleIO2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(TextSampleIO2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(55, 55, 55))
        );

        javax.swing.GroupLayout PanelSampleDevLayout = new javax.swing.GroupLayout(PanelSampleDev);
        PanelSampleDev.setLayout(PanelSampleDevLayout);
        PanelSampleDevLayout.setHorizontalGroup(
            PanelSampleDevLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelSampleDevLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(PanelSampleGroup, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(294, Short.MAX_VALUE))
        );
        PanelSampleDevLayout.setVerticalGroup(
            PanelSampleDevLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelSampleDevLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(PanelSampleGroup, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(386, Short.MAX_VALUE))
        );

        TPane.addTab("Sample", PanelSampleDev);

        ButtonTest.setText("ID device");
        ButtonTest.setToolTipText("Press to automatically identify the device");
        ButtonTest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ButtonTestActionPerformed(evt);
            }
        });

        jMenuFile.setMnemonic('F');
        jMenuFile.setText("File");
        jMenuBar.add(jMenuFile);

        jMenuEdit.setMnemonic('E');
        jMenuEdit.setText("Edit");
        jMenuBar.add(jMenuEdit);

        jMenuDevice.setMnemonic('D');
        jMenuDevice.setText("Device");
        jMenuBar.add(jMenuDevice);

        jMenuHelp.setMnemonic('H');
        jMenuHelp.setText("Help");
        jMenuBar.add(jMenuHelp);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TPane, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ButtonPort)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ButtonPoll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ButtonTest)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ButtonPort)
                    .addComponent(ButtonPoll)
                    .addComponent(ButtonTest))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(TPane))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ButtonPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ButtonPortActionPerformed
        syncPort.openClose(!syncPort.isPortOpen());
    }//GEN-LAST:event_ButtonPortActionPerformed

    private void ButtonPollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ButtonPollActionPerformed
        modthread.pollToggle(!syncPort.isPortOpen());  // This is synchronized, may block
    }//GEN-LAST:event_ButtonPollActionPerformed

    private void ButtonTestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ButtonTestActionPerformed
        //Modbus.reqtype = MsgType.DEVID;
        Modbus.sendState();
    }//GEN-LAST:event_ButtonTestActionPerformed

    private void activityLed() {
        long currtime = System.currentTimeMillis();

        if ((actTime + 400) < currtime) {
            actTime = currtime;

            ledActivity.setVisible((actshow & 1) == 0);
            actshow++;

            if (!devonline) {
                labelActivity.setText("Online");
                devonline = true;
            }
        }
    }


    private void initComms() {
        netcomms = new Net();
        (modthread = new Modbus(new ReadEvent(), syncPort)).start();
        //serport = new ComPort(modthread);
        serport = new ComPort();


        switch (conntype) {
        case CTYPESERIAL:
            // TODO graphics for serial
            break;

        case CTYPENET:
            // TODO graphics for network
            break;
        }


        syncPort.portButtCaption();
        //Timer timer = new Timer();
        /*timer.schedule(new TimerTask() {
            @Override
            public void run() {
            // Your database code here
                System.out.println("Hello from timer!");
            }
        }, 1500);*/

        labelActivity = new javax.swing.JLabel();
        labelActivity.setText("Offline");
        labelActivity.setToolTipText("Communication status");

        ledActivity = new javax.swing.JPanel();
        ledActivity.setBackground(new java.awt.Color(0, 135, 27));
        ledActivity.setFocusable(false);
        ledActivity.setVisible(false);

        javax.swing.GroupLayout jActivitypLayout = new javax.swing.GroupLayout(ledActivity);
        ledActivity.setLayout(jActivitypLayout);
        jActivitypLayout.setHorizontalGroup(
            jActivitypLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(10, 10, 10)
        );
        jActivitypLayout.setVerticalGroup(
            jActivitypLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(10, 10, 10)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        //GroupLayout layout = (GroupLayout) getContentPane().getLayout();
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ButtonPort)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ButtonPoll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ButtonTest))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(labelActivity)
                        .addGap(16, 16, 16))
                    .addComponent(ledActivity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10))
            .addComponent(TPane)
        );
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(ButtonTest)
                    .addComponent(ButtonPoll)
                    .addComponent(ButtonPort)
                    .addComponent(labelActivity)
                    .addComponent(ledActivity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(TPane))
        );
        getContentPane().setLayout(layout);
    }


    private void initMenu() {
        menuItemAbout = new JMenuItem("About", KeyEvent.VK_A);
        //menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        //menuItemAbout.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menuItemAbout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionAbout(ae);
            }
        });
        jMenuHelp.add(menuItemAbout);

        menuItemSettings = new JMenuItem("Settings", KeyEvent.VK_S);
        menuItemSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionSettings(ae);
            }
        });
        jMenuEdit.add(menuItemSettings);

        menuItemRestart = new JMenuItem("Restart", KeyEvent.VK_R);
        menuItemRestart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Modbus.writeRestart();
            }
        });
        jMenuDevice.add(menuItemRestart);

        menuItemDump = new JMenuItem("Dump Config", KeyEvent.VK_D);
        menuItemDump.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                 Modbus.dumpConfig();
            }
        });
        jMenuDevice.add(menuItemDump);

        menuItemExit = new JMenuItem("Exit", KeyEvent.VK_X);
        menuItemExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                actionExit(ae);
            }
        });
        jMenuFile.add(menuItemExit);
    }


    private void actionAbout(ActionEvent ae) {
        Object message = "LEIODC configuration utility " + SWVERSION + "\nBuild date: " + buildDate + "\n" + COPYRIGHT;
        JOptionPane.showMessageDialog(this, message, "About LEIODC tool", JOptionPane.PLAIN_MESSAGE);
    }


    private void actionSettings(ActionEvent ae) {

        //new DialogSettings(this);
        //DialogSettings dialog = new DialogSettings(this);

        new DialogSettings(this, syncPort).setVisible(true);
        /*JFrame frame = new JFrame("DialogDemo");
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        DialogSettings newContentPane = new DialogSettings(frame);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);*/
    }


    private void actionExit(ActionEvent ae) {
        System.exit(0);
    }


    private boolean writeFile(String filename, String data) {
        FileWriter writer;
        try {
            writer = new FileWriter(filename);
            writer.write(data);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(LeiodcMain.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }


    private void checkLibrary() {
        String osname = System.getProperty("os.name");
        boolean is64bit = false;
        File libFile;
        String libpath;
        Path link, target;

        if (System.getProperty("os.arch").contains("64"))
            is64bit = true;

        if (osname.toLowerCase().contains("linux")) {
            if ((libpath = findLibpath(":", "jni")) == null) {
                return;
            }

            //libFile = new File("/home/dell/test.bin");
            //libpath = "/home/dell";
            libFile = new File(libpath + "/librxtxSerial.so");
            if (libFile.exists()) {
                //System.out.println("Link librxtxSerial.so found in " + libpath);
                return;
            }

            if (!(new File(libpath)).canWrite()) {
                System.out.println("No write permissions to copy librxtxSerial.so to " + libpath);
                actionExit(null);
            }

            link = libFile.toPath();
            libFile = new File(libpath + "/librxtxSerial-2.2pre1.so");
            target = libFile.toPath();

            if (is64bit) {
                System.out.println("Linux 64bit found");
                //copyLibrary("/leiodctool/rxtxSerial_x64.dll", libFile);
                // TODO need to copy library on 64bit Linux
                System.out.println("You need to copy librxtxSerial-xxxxx.so to " + libpath);
            }
            else {
                //System.out.println("Linux 32bit found");
                if (!copyLibrary("/leiodctool/librxtxSerial-2.2pre1.so", libFile)) {
                    try {
                        Files.createSymbolicLink(link, target);
                    } catch (IOException ex) {
                        if ((ex.toString().indexOf("FileAlreadyExistsException")) < 0)   // Ignore error if link already exists
                            Logger.getLogger(LeiodcMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return;
                }
                System.out.println("You need to copy librxtxSerial-2.2pre1.so to " + libpath);
            }
            actionExit(null);
        }
        else if (osname.toLowerCase().contains("windows")) {
            libpath = findLibpath(";", "system32");

            libFile = new File(libpath + "\\rxtxSerial.dll");
            if (libFile.exists()) {
                //System.out.println("rxtxSerial.dll found in " + libpath);
                return;
            }

            if (is64bit) {
                //System.out.println("Windows 64bit found");
                if (copyLibrary("/leiodctool/rxtxSerial_x64.dll", libFile)) {
                    actionExit(null);
                }
            }
            else {
                //System.out.println("Windows 32bit found");
                if (copyLibrary("/leiodctool/rxtxSerial_x86.dll", libFile)) {
                    actionExit(null);
                }
            }
        }
    }


    private String findLibpath(String delim, String dirname) {
        File tempDir;
        String libpath[] = System.getProperty("java.library.path").split(delim);

        for (int i = 0; i < libpath.length; i++) {
            if (libpath[i].contains(dirname)) {
                tempDir = new File(libpath[i]);
                if (tempDir.exists())
                    return libpath[i];
            }
        }
        return null;
    }


    private boolean copyLibrary(String libname, File libpath) {
        boolean failure = false;
        InputStream ilink = (getClass().getResourceAsStream(libname));

        if (ilink == null) {
            System.out.println("Cant't find " + libname + " in jar file" );
            return true;
        }

        try {
            Files.copy(ilink, libpath.getAbsoluteFile().toPath());
        } catch (IOException ex) {
            if ((ex.toString().indexOf("FileAlreadyExistsException")) < 0) {   // Ignore error if file already exists
                Logger.getLogger(LeiodcMain.class.getName()).log(Level.SEVERE, null, ex);
                failure = true;
            }
        }

        try {
            ilink.close();
        } catch (IOException ex) {
            Logger.getLogger(LeiodcMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return failure;
    }


    private void initRevision() {
        long dLong = 0;

        String filepath;
        FileSystem jarFS;


        filepath = getClass().getResource(getClass().getSimpleName() + ".class").toString();
        if (filepath.startsWith("jar:")) {  // When executing from JAR
            //System.out.println("fullpath " + filepath);
            filepath = filepath.substring(0, filepath.indexOf("!/"));
        }
        else {  // When Debugging
            filepath = "jar:file:/home/dell/Documents/Firmware/java/LEIODCconf/dist/leiodctool.jar";
        }
        //System.out.println("trimmed " + filepath);


        try {
            jarFS = FileSystems.newFileSystem(URI.create(filepath), Collections.<String, Object>emptyMap());
            Path resourcePath = jarFS.getPath("/META-INF/MANIFEST.MF");
            FileTime fileTime = Files.getLastModifiedTime(resourcePath);
            dLong = fileTime.toMillis();
        } catch (IOException ex) {
            Logger.getLogger(LeiodcMain.class.getName()).log(Level.SEVERE, null, ex);
        }


        if (dLong > 0) {
            Date modDate = new Date(dLong);
            buildDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(modDate);
        }
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(LeiodcMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LeiodcMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LeiodcMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LeiodcMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>



        //Logger logger = Logger.getAnonymousLogger();
        // LOG this level to the log
        //logger.setLevel(Level.FINEST);
        //System.out.println("Logging level is: " + logger.getLevel());

        //ConsoleHandler handler = new ConsoleHandler();
        // PUBLISH this level
        //handler.setLevel(Level.FINER);
        //logger.addHandler(handler);

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LeiodcMain().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static javax.swing.JButton ButtonPoll;
    private static javax.swing.JButton ButtonPort;
    private javax.swing.JButton ButtonSampleIO1;
    private javax.swing.JButton ButtonTest;
    private javax.swing.JLabel LabelSample1;
    private javax.swing.JLabel LabelSample2;
    private javax.swing.JPanel PanelSampleDev;
    private javax.swing.JPanel PanelSampleGroup;
    private javax.swing.JPanel PanelSampleIO1;
    private javax.swing.JPanel PanelSampleIO2;
    private javax.swing.JTabbedPane TPane;
    private javax.swing.JTextField TextSampleIO1;
    private javax.swing.JTextField TextSampleIO2;
    private javax.swing.JMenuBar jMenuBar;
    public javax.swing.JMenu jMenuDevice;
    private javax.swing.JMenu jMenuEdit;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenu jMenuHelp;
    // End of variables declaration//GEN-END:variables
}
