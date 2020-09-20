/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dell
 */
//public class ComPort implements SerialPortEventListener {
public class ComPort {
    private SerialPort serialPort;
    public InputStream inputStream;
    private OutputStream outputStream;
    //private final ReceiveEvent rxlisten;
    public static final Integer[] baudlist = new Integer[] {300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200};
    public static int baudrate = 9600;
    public static int parity = SerialPort.PARITY_EVEN;
    public static String[] portNames;
    public static int selectedPort = 0;


    /*public interface ReceiveEvent extends EventListener {

        public void rxEvent(int rxlength);
    }*/


    //public ComPort(ReceiveEvent re) {
    public ComPort() {
        Enumeration enPorts;
        CommPortIdentifier portId;
        int portCount = 0;
        String[] allPorts = new String[32];

        //rxlisten = re;

        //String osName = System.getProperty("os.name");
        //String deviceDirectory = getDeviceDirectory();

        enPorts = CommPortIdentifier.getPortIdentifiers();
        //CommPortIdentifier.addPortName("/dev/ttyUSB2", 1, null);

        while (enPorts.hasMoreElements()) {
            portId = (CommPortIdentifier) enPorts.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                //if (portId.getName().equals("COM1")) {
                //if (portId.getName().equals("/dev/ttyS0")) {
                    //SimpleRead reader = new SimpleRead();
                //}
                //ComboPorts.addItem(portId.getName());
                allPorts[portCount] = portId.getName();
                portCount++;
            }
        }
        portNames = new String[portCount];
        System.arraycopy(allPorts, 0, portNames, 0, portCount);
        Arrays.sort(portNames);
    }


    /*@Override
    public void serialEvent(SerialPortEvent event) {

        switch(event.getEventType()) {
        case SerialPortEvent.BI:
        case SerialPortEvent.OE:
        case SerialPortEvent.FE:
        case SerialPortEvent.PE:
        case SerialPortEvent.CD:
        case SerialPortEvent.CTS:
        case SerialPortEvent.DSR:
        case SerialPortEvent.RI:
        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
            break;

        case SerialPortEvent.DATA_AVAILABLE:
            try {
                while (inputStream.available() > 0) {
                    int numBytes = inputStream.read(Modbus.rxrawbuff.array());
                    rxlisten.rxEvent(numBytes);
                }
                //System.out.print(new String(readBuffer));
            } catch (IOException e) {
                System.out.println(e);
            }
            break;
        }
    }*/


    public void sendComport(byte[] txbuff, int txlength) {
        try {
            outputStream.write(txbuff, 0, txlength);
            //if (inputStream.available() > 0)
            //    System.out.print("Flushing: " + inputStream.available());
            while (inputStream.available() > 0) {   // Flush input stream
                inputStream.read();
            }
        } catch (IOException ex) {
            Logger.getLogger(LeiodcMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public String openPort() {
        CommPortIdentifier portId;

        if (portNames.length == 0)
            return "No COM ports available";

        if (selectedPort > portNames.length)
            return "Invalid selected COM port index " + Integer.toString(selectedPort) + ". Must be less than " + Integer.toString(portNames.length);

        try {
            portId = CommPortIdentifier.getPortIdentifier(portNames[selectedPort]);
        } catch (NoSuchPortException ex) {
            return "COM port '" + portNames[selectedPort] + "' : " + ex.getMessage();
        }

        try {
            serialPort = (SerialPort) portId.open("LeiodcConf", 2000);
        } catch (PortInUseException ex) {
            return "COM port '" + portNames[selectedPort] + "' open : " + ex.getMessage();
        }

        try {
            inputStream = serialPort.getInputStream();
        } catch (IOException ex) {
            return "COM port '" + portNames[selectedPort] + "' input stream : " + ex.getMessage();
        }

        try {
            outputStream = serialPort.getOutputStream();
        } catch (IOException ex) {
            return "COM port '" + portNames[selectedPort] + "' output stream : " + ex.getMessage();
        }

        try {
           serialPort.setSerialPortParams(baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, parity);
        } catch (UnsupportedCommOperationException ex) {
           return "COM port '" + portNames[selectedPort] + "' set params : " + ex.getMessage();
        }

        /*try {
            serialPort.addEventListener(this);
	} catch (TooManyListenersException ex) {
            return "COM port '" + portNames[selectedPort] + "' add listener : " + ex.getMessage();
        }
        serialPort.notifyOnDataAvailable(true);*/
        return null;
    }


    public void closePort() {
        serialPort.close();
    }
}
