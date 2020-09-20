/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.awt.Color;
import javax.swing.JLabel;


/**
 *
 * @author dell
 */
public class WriteStatusLabel extends JLabel implements Modbus.ModbusEvent {

    /*public  WriteStatusLabel() {
        super("Test");
    }*/

    @Override
    public void onSuccess(Modbus.MsgType reqtype) {
        setForeground(Color.BLACK);
        setText("Settings written OK!");
    };

    @Override
    public void onFail(Modbus.MsgType reqtype) {
        setForeground(Color.RED);
        setText("Setting write Failed!");
    };

    public void clearStatus() {
        setText("");
    }
}
