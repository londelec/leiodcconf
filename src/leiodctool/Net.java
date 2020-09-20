/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static leiodctool.Modbus.timeout;


/**
 *
 * @author dell
 */
public class Net {
    public static InetAddress ipaddress;
    public static int port;
    private static AsynchronousSocketChannel cliSock;

    public Net() {
        try {
            //ipaddress = InetAddress.getByName("127.0.0.1");
            ipaddress = InetAddress.getByName("192.168.64.18");
            //ipaddress = InetAddress.getByName("185.69.24.202");
        } catch (UnknownHostException ex) {
            //Logger.getLogger(LEIODCmain.class.getName()).log(Level.SEVERE, null, ex);
        }
        port = 64950;
        //port = 64035;
    }


    public static String connectSocket() {
        try {
            cliSock = AsynchronousSocketChannel.open();
        } catch (IOException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        }


        InetSocketAddress ipsockaddr = new InetSocketAddress(ipaddress, port);
        try {
            cliSock.connect(ipsockaddr).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            return "Connect() " + ipaddress.getHostAddress() + ":" + Integer.toString(port) + " " + ex.getMessage();
        } catch (TimeoutException ex) {
            return "Connect() " + ipaddress.getHostAddress() + ":" + Integer.toString(port) + " timeout " + ex.getMessage();
        }
        return null;
    }


    public static String closeSocket() {
        try {
            cliSock.close();
        } catch (IOException ex) {
            return "Close() " + ipaddress.getHostAddress() + ":" + Integer.toString(port) + " " + ex.getMessage();
        }
        return null;
    }


    public static void sendSocket(ByteBuffer txbuff) {
        //byte[] message = new byte[] {0x10, 0x49, 0x01, 0x4a, 0x16};
        //ByteBuffer buffer = ByteBuffer.wrap(message);
        Future result = cliSock.write(txbuff);
    }


    public static String recvSocket(ByteBuffer rxbuff) {
        //int aaa = 0;
        try {
            cliSock.read(rxbuff).get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Net.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            return "Read() " + ipaddress.getHostAddress() + ":" + Integer.toString(port) + " error " + ex.getMessage();
        } catch (TimeoutException ex) {
            return "Read() " + ipaddress.getHostAddress() + ":" + Integer.toString(port) + " timeout";
        }
        return null;
    }
}
