/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leiodctool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import static leiodctool.LeiodcMain.CTYPENET;
import static leiodctool.LeiodcMain.CTYPESERIAL;
import static leiodctool.LeiodcMain.IAIMODE;
import static leiodctool.LeiodcMain.aigroups;
import static leiodctool.LeiodcMain.aogroups;
import static leiodctool.LeiodcMain.digroups;
import static leiodctool.LeiodcMain.devidname;
import static leiodctool.LeiodcMain.dogroups;
import static leiodctool.LeiodcMain.conntype;
import static leiodctool.LeiodcMain.devcom;
import static leiodctool.LeiodcMain.disettings;
import static leiodctool.LeiodcMain.IDIMODE;
import static leiodctool.LeiodcMain.IDIFLT;
import static leiodctool.LeiodcMain.IDODUR;
import static leiodctool.LeiodcMain.IDOMODE;
import static leiodctool.LeiodcMain.aisettings;
import static leiodctool.LeiodcMain.aivalues;
import static leiodctool.LeiodcMain.dosettings;
import static leiodctool.LeiodcMain.fwversion;
import static leiodctool.LeiodcMain.serport;


/**
 *
 * @author dell
 */
//public class Modbus extends Thread implements ReceiveEvent {
public class Modbus extends Thread {
    public static int devaddr = 1;
    public static int txDelay = 50;
    public static int timeout = 2000;
    public static final ByteBuffer rxrawbuff = ByteBuffer.allocate(256);
    public static int cmdOn = 0, cmdOff = 0;
    public static final int[] configbuff = new int[2048];

    public static enum RxState {
        RXOK,                                   // Message received sucessfully, CRC ok
        RXNOMSG,                                // No message received, read() returned 0 bytes or message incomplete
        RXLENERR,                               // Length error, CRC is not checked
        RXDATAERR,                              // Data error
        RXCRCERR,                               // CRC error
        RXTIMEOUT,                              // Message receive timeout
        TXMSG,                                  // Message is being transmitted
    }
    private static RxState cstate = RxState.RXOK;     // Receive state

    public static enum MsgType {
        DEVID,                                  // Device ID request message
        RDFWVER,                                // Device Firmware version request message
        RDCOMCFG,                               // Device COM port config read message
        DISTATUS,                               // DI status message
        RDDICFG,                                // DI settings read message
        DOSTATUS,                               // DO status message
        RDDOCFG,                                // DO settings read message
        AIVALUES,                               // AI values message
        RDAICFG,                                // AI settings read message
        RDCONFIG,                               // Read all configuration
        WRDOCMD,                                // DO command message
        WRRESTART,                              // Restart LEIODC message
        WRCOMCFG,                               // Device COM port config write message
        WRDICFG,                                // DI settings write message
        WRDOCFG,                                // DO settings write message
    }
    private static MsgType reqtype;

    private static enum Dstates {
        DISABLED,                               // Device communiation disabled
        INIT,                                   // Intialization state
        PERIODIC,                               // Periodic polling
    }
    //private static Dstates devstate = Dstates.DISABLED;

    private static final int BITCOMCFG = 1;     // Write COM port settings to LEIODC
    private static final int BITIOCFG = 2;      // Write IO settings to LEIODC
    private static final int BITRESTART = 4;    // Write Restart command to LEIODC
    private static final int BITCONFIG = 8;     // Read all configuration
    private static int cmdWrite = 0;

    private static int rxlength, txlength, rxbytecnt, rxerror;
    private static final ByteBuffer txbuff = ByteBuffer.allocate(256);
    private static final byte[] rxbuff = new byte[256];
    private static final int CFGBLOCK = 64;
    private static byte rxfunc;
    private static int reqsubtype, norespcnt;
    private static long txtime;
    private static final int[] mcomcfg = new int[9];
    private static int[] wrcomvals;
    private static int[][] wrdivals, wrdovals;
    private static final SyncControl syncObj = new SyncControl();
    private static ModbusEvent evread;
    private static ModbusEvent evwrcom, evwriocfg;
    private static LeiodcMain.SyncPort portctrl;
    private static final boolean SIMULATERX = false;
    private static final boolean NOPERIODICDEBUG = false;
    private static final String SIMDEVID = "LEIODC-AT-2200";

    private static class SyncControl extends Object {
        private Dstates devstate = Dstates.DISABLED;
    }

    public interface ModbusEvent extends EventListener {

        public void onSuccess(MsgType reqtype);

        public void onFail(MsgType reqtype);
    }


    public Modbus(ModbusEvent rdlistener, LeiodcMain.SyncPort syncport) {
        evread = rdlistener;
        portctrl = syncport;
    }

    /*@Override
    public void rxEvent(int rawlength) {
        receiveModbus(rawlength);
    }*/

    private static void receiveModbus(int rawlength) {

        for (int rawcnt = 0; rawcnt < rawlength; rawcnt++) {
            rxbuff[rxbytecnt] = rxrawbuff.get(rawcnt);

            switch (rxbytecnt) {
            case 0:		// Device address
                if (devaddr != makepos(rxbuff[rxbytecnt])) {
                    rxerror = 1;
                }
                break;

            case 1:		// Modbus function
                rxfunc = rxbuff[rxbytecnt];
                if (rxfunc == 0)
                    rxerror = 1;
                break;

            case 2:		// Data length or CRC in case of an exception
                if ((rxfunc & 0x80) == 0) {
                    switch (rxfunc) {
                    case 0x01:
                    case 0x02:
                    case 0x03:
                    case 0x04:
                    case 0x07:
                    case 0x11:
                        rxlength = makepos(rxbuff[rxbytecnt]) + 5;	// Length including header and CRC
                        break;
                    case 0x05:
                    case 0x06:
                    case 0x10:
                        rxlength = 8;			// Length including header and CRC
                        break;
                    }
                }
                else {
                    rxlength = 5;
                }
                break;

            default:
                break;
            }
            rxbytecnt++;
        }


	if (rxerror != 0) {			// error occurred
            //rxlength = rxbytecnt;		// Return all received bytes
            cstate = RxState.RXDATAERR;
	}
        else if ((rxbytecnt == 0) || (rxbytecnt < rxlength)) {  // No bytes recevied or message recevied partially - more bytes to come
            //rxlength = 0;
            //System.out.println("Segmented Rx rawlength: " + rawlength + " rxlength: " + rxlength);
            cstate = RxState.RXNOMSG;
            return;
	}
        else if (rxbytecnt == rxlength) {
            int crc;
            crc = build_crc16(rxbuff, rxlength);

            if (
                    (rxbuff[rxlength - 1] != (byte) (crc >> 8)) ||
                    (rxbuff[rxlength - 2] != (byte) (crc & 0xff))) {   // Check CRC
		//return RXENCRCERR;
                if (SIMULATERX == true)
                    cstate = RxState.RXOK;
                else
                    cstate = RxState.RXCRCERR;
            }
            else {
                cstate = RxState.RXOK;
            }
	}
        else
            cstate = RxState.RXLENERR;


        TraceLog.rxLog(rxbuff, rxlength, cstate);
        if (cstate == RxState.RXOK) {
            synchronized(syncObj) {
                if (syncObj.devstate != Dstates.DISABLED) {
                    decoderx();
                    syncObj.notifyAll();
                }
            }
        }
    }


    private static void prepareTx(int func, int reg, int count, int values[]) {
        txlength = 0;
        txbuff.clear();     // Reset buffer limit

        txbuff.put(0, (byte) devaddr);
        txbuff.put(1, (byte) func);

        switch (func) {
        case 0x01:
        case 0x02:
        case 0x03:
        case 0x04:
            txbuff.put(2, (byte) ((reg >> 8) & 0xff));
            txbuff.put(3, (byte) (reg & 0xff));
            txbuff.put(4, (byte) ((count >> 8) & 0xff));
            txbuff.put(5, (byte) (count & 0xff));
            txlength = 8;
            break;

        case 0x05:
        case 0x06:
            txbuff.put(2, (byte) ((reg >> 8) & 0xff));
            txbuff.put(3, (byte) (reg & 0xff));
            txbuff.put(4, (byte) ((values[0] >> 8) & 0xff));
            txbuff.put(5, (byte) (values[0] & 0xff));
            txlength = 8;
            break;

        case 0x07:
        case 0x11:
            txlength = 4;
            break;

	case 0x10:
            txbuff.put(2, (byte) ((reg >> 8) & 0xff));
            txbuff.put(3, (byte) (reg & 0xff));
            txbuff.put(4, (byte) ((count >> 8) & 0xff));
            txbuff.put(5, (byte) (count & 0xff));
            txbuff.put(6, (byte) (count << 1));
            for (int i = 0; i < count; i++) {
                txbuff.put((i << 1) + 7, (byte) ((values[i] >> 8) & 0xff));
                txbuff.put((i << 1) + 8, (byte) (values[i] & 0xff));
            }
            txlength = 9 + (count << 1);
            break;
        }


        if (txlength < 4) {
            txlength = 0;
            return;
            //return 0;
        }

        int crc;
        crc = build_crc16(txbuff.array(), txlength);
        txbuff.put(txlength - 1, (byte) (crc >> 8));
        txbuff.put(txlength - 2, (byte) (crc & 0xff));
	rxbytecnt = 0;          // Reset Rx counter
	rxerror = 0;            // Reset Rx error state
        rxlength = 5;           // Minimal length of expected message
        cstate = RxState.TXMSG;  // Inidicates transmitted message

        if (txlength > 0) {
            txbuff.limit(txlength);
            TraceLog.txLog(txbuff.array(), txlength);
        }
    }


    private static int build_crc16(byte nData[], int wLength) {
        int i, j, crc = 0xffff;

        for (i = 0; i < wLength - 2; i++) {
            if (nData[i] > 0)
                crc ^= nData[i];
            else {
                int posint = nData[i];
                posint &= 255;
                crc ^= posint;
            }

            for (j = 0; j < 8; j++) {
                crc &= 0xffff;
                if ((crc & 1) == 1) {
                    crc >>>= 1;
                    crc ^= 0xA001;
                }
                else
                    crc >>>= 1;
            }
        }
        return crc;
    }


    private static void decoderx() {
        if ((rxfunc & 0x80) == 0) {
            switch (reqtype) {
            case DEVID:
                decode_devid();
                break;

            case RDFWVER:
                LeiodcMain.fwversion = decode04single();
                break;

            case RDCOMCFG:
                decode04mult(mcomcfg, (rxlength - 5) >> 1);
                for (int i = 0; i < devcom.length; i++) {
                    switch (i) {
                    case 0:     // Baudrate
                    case 1:     // Parity
                        devcom[i] = mcomcfg[i];
                        break;

                    case 2:     // TxDelay highbyte first
                    case 3:     // Timeout highbyte first
                        devcom[i] = mcomcfg[1 << (i - 1)];
                        devcom[i] <<= 16;
                        devcom[i] += mcomcfg[(1 << (i - 1)) + 1];
                        break;

                    default:    // Remaining settings
                        devcom[i] = mcomcfg[i + 2];
                        break;
                    }
                }
                break;

            case DISTATUS:
                LeiodcMain.distatus = decode04single();
                break;

            case DOSTATUS:
                LeiodcMain.dostatus = decode04single();
                break;

            case AIVALUES:
                decode04mult(aivalues, aigroups << 1);
                break;

            case RDDICFG:
                decode04mult(disettings[reqsubtype], digroups << 2);
                break;

            case RDDOCFG:
                decode04mult(dosettings[reqsubtype], dogroups << 2);
                break;

            case RDAICFG:
                decode04mult(aisettings[reqsubtype], aigroups << 1);
                break;

            case RDCONFIG:
                if (rxlength == ((CFGBLOCK << 1) + 5)) {
                    for (int i = 0; i < CFGBLOCK; i++) {
                        configbuff[(reqsubtype << 7) + (i << 1) + 1] = (rxbuff[(i << 1) + 3] & 255);
                        configbuff[(reqsubtype << 7) + (i << 1)] = (rxbuff[(i << 1) + 4] & 255);
                    }
                }
                else {
                    cstate = RxState.RXDATAERR;
                    if (evread != null)
                        evread.onFail(reqtype);
                    return;
                }

                if (reqsubtype < 15)
                    return;
                break;

            case WRDOCMD:
            case WRRESTART:
                return;

            case WRCOMCFG:
                if (evwrcom != null) {
                    evwrcom.onSuccess(reqtype);
                    evwrcom = null;
                }
                return;

            case WRDICFG:
                if (
                        ((reqsubtype + 1) == disettings.length) &&
                        (dogroups == 0)) {
                    if (evwriocfg != null) {
                        evwriocfg.onSuccess(reqtype);
                        evwriocfg = null;
                    }
                }
                return;

            case WRDOCFG:
                if ((reqsubtype + 1) == dosettings.length) {
                    if (evwriocfg != null) {
                        evwriocfg.onSuccess(reqtype);
                        evwriocfg = null;
                    }
                }
                return;
            }

            if (evread != null)
                evread.onSuccess(reqtype);
        }
        else {  // Modbus Exception received
            switch (reqtype) {
            case WRDOCMD:
            case WRRESTART:
                break;

            case WRCOMCFG:
                if (evwrcom != null) {
                    evwrcom.onFail(reqtype);
                    evwrcom = null;
                }
                break;

            case WRDICFG:
            case WRDOCFG:
                if (evwriocfg != null) {
                    evwriocfg.onFail(reqtype);
                    evwriocfg = null;
                }
                reqtype = MsgType.DOSTATUS;     // Next status request will be selected by state machine
                break;

            default:
                if (evread != null)
                    evread.onFail(reqtype);
                break;
            }
        }
    }


    private static void decode_devid() {
        StringBuilder idstring = new StringBuilder();
        boolean iderr = true;

        checkloop:
        for(int i = 3; i < rxlength - 2; i++) {
            idstring.append((char) rxbuff[i]);
            switch(i) {
                case 3:
                    if (rxbuff[i] != 'L')
                        break checkloop;
                    break;

                case 4:
                    if (rxbuff[i] != 'E')
                        break checkloop;
                    break;

                case 5:
                    if (rxbuff[i] != 'I')
                        break checkloop;
                    break;

                case 6:
                    if (rxbuff[i] != 'O')
                        break checkloop;
                    break;

                case 7:
                    if (rxbuff[i] != 'D')
                        break checkloop;
                    break;

                case 8:
                    if (rxbuff[i] != 'C')
                        break checkloop;
                    break;

                case 9:
                case 12:
                    if (rxbuff[i] != '-')
                        break checkloop;
                    break;

                case 13:
                    if (checkascii(rxbuff[i]) == true)
                        digroups = (rxbuff[i] - 0x30);
                    else
                        break checkloop;
                    break;

                case 14:
                    if (checkascii(rxbuff[i]) == true)
                        dogroups = (rxbuff[i] - 0x30);
                    else
                        break checkloop;
                    break;

                case 15:
                    if (checkascii(rxbuff[i]) == true)
                        aigroups = (rxbuff[i] - 0x30);
                    else
                        break checkloop;
                    break;

                case 16:
                    if (checkascii(rxbuff[i]) == true) {
                        aogroups = (rxbuff[i] - 0x30);
                        iderr = false;
                    }
                    else
                        break checkloop;
                    break;
            }
        }
        if (iderr == true) {
            cstate = RxState.RXDATAERR;
        } else {
            if ((idstring.charAt(7) == 'A') && (idstring.charAt(8) == 'T')) {
                idstring.replace(7, 9, "X10");
                devidname = idstring.toString();
            }
            else if ((idstring.charAt(7) == 'M') && (idstring.charAt(8) == 'X')) {
                idstring.replace(7, 9, "X32");
                devidname = idstring.toString();
            }
            else {
                cstate = RxState.RXDATAERR;
            }
        }
    }


    private static void decode04mult(int[] reg, int regcnt) {
        if (rxlength == ((regcnt << 1) + 5)) {
            for (int i = 0; i < regcnt; i++) {
                reg[i] = makepos(rxbuff[(i << 1) + 3]);
                reg[i] <<= 8;
                reg[i] += makepos(rxbuff[(i << 1) + 4]);
            }
        }
        else {
            cstate = RxState.RXDATAERR;
        }
    }


    private static int decode04single() {
        int reg = 0;

        if (rxlength == 7) {
            reg = makepos(rxbuff[3]);
            reg <<= 8;
            reg += makepos(rxbuff[4]);
        }
        else {
            cstate = RxState.RXDATAERR;
        }
        return reg;
    }


    public static void sendState() {
        switch (reqtype) {
        case DEVID:
            prepareTx(0x11, 0, 0, null);
            if (txlength > 0) {
                if (SIMULATERX == false) {
                    sendMessage();
                }
                else {
                    rxrawbuff.put(0, (byte) devaddr);
                    rxrawbuff.put(1, (byte) 0x11);
                    rxrawbuff.put(2, (byte) SIMDEVID.length());
                    for (int i = 0; i < SIMDEVID.length(); i++) {
                        rxrawbuff.put(3 + i, (byte) SIMDEVID.charAt(i));
                    }
                    //rxrawbuff[18] = '?';
                    //rxrawbuff[19] = '?';
                    receiveModbus(SIMDEVID.length() + 5);
                }
            }
            break;

        case RDFWVER:
            build04(0x0000, 1, 1, 104);
            break;

        case RDCOMCFG:
            if (fwversion < 105) {
                build04(0x0080, 8, 1, 0);   // Versions before V1.05 don't have Interface Type
                mcomcfg[8] = 0;
            }
            else
                build04(0x0080, mcomcfg.length, 1, 0);
            break;

        case DISTATUS:
            build04(0x0100, 1, 1, 32771);
            break;

        case DOSTATUS:
            build04(0x0300, 1, 1, 8);
            break;

        case AIVALUES:
            build04(0x0200, 8, 1, 0);
            break;

        case RDDICFG:
            switch (reqsubtype) {
            case IDIMODE:
                build04(0x0110, (digroups << 2), 1, 0);
                break;
            case IDIFLT:
                build04(0x0120, (digroups << 2), 1, 32);
                break;
            }
            break;

        case RDDOCFG:
            switch (reqsubtype) {
            case IDOMODE:
                build04(0x0310, (dogroups << 2), 1, 0);
                break;
            case IDODUR:
                build04(0x0320, (dogroups << 2), 1, 64);
                break;
            }
            break;

        case RDAICFG:
            switch (reqsubtype) {
            case IAIMODE:
                build04(0x0210, (aigroups << 1), 1, 0);
                break;
            }
            break;

        case WRDOCMD:
            build10(0x0300, new int[] {cmdOn}, 1);
            cmdOn = 0;
            break;

        case WRCOMCFG:
            for (int i = 0; i < wrcomvals.length; i++) {
                switch (i) {
                case 0:     // Baudrate
                case 1:     // Parity
                    mcomcfg[i] = wrcomvals[i];
                    break;

                case 2:     // TxDelay highbyte first
                case 3:     // Timeout highbyte first
                    mcomcfg[1 << (i - 1)] = ((wrcomvals[i] >> 16) & 0xffff);
                    mcomcfg[(1 << (i - 1)) + 1] = (wrcomvals[i] & 0xffff);
                    break;

                default:    // Remaining settings
                    mcomcfg[i + 2] = wrcomvals[i];
                    break;
                }
            }
            if (fwversion < 105) {
                build10(0x0080, mcomcfg, 8);
            }
            else
                build10(0x0080, mcomcfg, mcomcfg.length);
            break;

        case WRDICFG:
            switch (reqsubtype) {
            case IDIMODE:
                build10(0x0110, wrdivals[reqsubtype], digroups << 2);
                break;
            case IDIFLT:
                build10(0x0120, wrdivals[reqsubtype], digroups << 2);
                break;
            }
            break;

        case WRDOCFG:
            switch (reqsubtype) {
            case IDOMODE:
                build10(0x0310, wrdovals[reqsubtype], dogroups << 2);
                break;
            case IDODUR:
                build10(0x0320, wrdovals[reqsubtype], dogroups << 2);
                break;
            }
            break;

        case WRRESTART:
            prepareTx(6, 0x0077, 1, new int[] {0xaaaa});
            break;

        case RDCONFIG:
            build04(0x9000 + (reqsubtype << 6), CFGBLOCK, 1, 0);
            break;
        }
    }


    private static void build04(int reg, int regcnt, int simpolicy, int simval) {
        prepareTx(4, reg, regcnt, null);
        if (txlength > 0) {
            if (SIMULATERX == false) {
                sendMessage();
            }
            else {
                simulate04rx(regcnt, simpolicy, simval);
            }
        }
    }


    private static void simulate04rx(int regcnt, int simpolicy, int simval) {
        rxrawbuff.put(0, (byte) devaddr);
        rxrawbuff.put(1, (byte) 3);
        rxrawbuff.put(2, (byte) (regcnt << 1));
        for (int i = 0; i < regcnt; i++) {
            switch (simpolicy) {
            case 0:     // Constant values
                rxrawbuff.put((i << 1) + 3, (byte) ((simval >> 8) & 0xff));
                rxrawbuff.put((i << 1) + 4, (byte) (simval & 0xff));
                break;
            case 1:     // Increment
                rxrawbuff.put((i << 1) + 3, (byte) (((simval + i) >> 8) & 0xff));
                rxrawbuff.put((i << 1) + 4, (byte) ((simval + i) & 0xff));
                break;
            }
        }
        receiveModbus((regcnt << 1) + 5);
    }


    private static void build10(int reg, int values[], int count) {
        if (count == 1) {
            prepareTx(6, reg, count, values);
        }
        else {
            prepareTx(0x10, reg, count, values);
        }

        if (txlength > 0) {
            if (SIMULATERX == false) {
                sendMessage();
            }
            else {
                simulate10rx(reg, values, count);
            }
        }
    }


    private static void simulate10rx(int reg, int values[], int count) {
        rxrawbuff.put(0, (byte) devaddr);
        rxrawbuff.put(2, (byte) ((reg >> 8) & 0xff));
        rxrawbuff.put(3, (byte) (reg & 0xff));
        if (count == 1) {
            rxrawbuff.put(1, (byte) 6);
            rxrawbuff.put(4, (byte) ((values[0] >> 8) & 0xff));
            rxrawbuff.put(5, (byte) (values[0] & 0xff));

        }
        else {
            rxrawbuff.put(1, (byte) 0x10);
            rxrawbuff.put(4, (byte) ((count >> 8) & 0xff));
            rxrawbuff.put(5, (byte) (count & 0xff));
        }
        receiveModbus(8);
    }


    private static void sendMessage() {
        switch (conntype) {
        case CTYPESERIAL:
            LeiodcMain.serport.sendComport(txbuff.array(), txlength);
            break;

        case CTYPENET:
            Net.sendSocket(txbuff);
            break;
        }
    }


    private static int makepos(byte b) {
        int posint = b;
        posint &= 255;
        return posint;
    }

    private static boolean checkascii(byte mbyte) {
         if ((mbyte >= 0x30) && (mbyte <= 0x39)) {
             return true;
         }
         return false;
    }

    public void pollToggle(boolean stop) {
        synchronized(syncObj) {
            if (
                    (syncObj.devstate == Dstates.DISABLED) &&
                    (stop == false)) {
                syncObj.devstate = Dstates.INIT;
                reqtype = MsgType.DEVID;
                norespcnt = 0;
                wrcomvals = null;
                syncObj.notifyAll();
                portctrl.pollOnOff(true);
            }
            else {
                disablePoll();
                //syncObj.notifyAll();
            }
        }
    }

    // Call this method only from synchronized code
    private static void disablePoll() {
        syncObj.devstate = Dstates.DISABLED;
        portctrl.pollOnOff(false);
    }

    public static void writeCOMsettings(ModbusEvent listener, int[] wrdata) {
        cmdWrite |= BITCOMCFG;
        evwrcom = listener;
        wrcomvals = wrdata;
    }

    public static void writeIOcfg(ModbusEvent listener, int[][] didata, int[][] dodata) {
        cmdWrite |= BITIOCFG;
        evwriocfg = listener;
        wrdivals = didata;
        wrdovals = dodata;
    }

    public static void writeRestart() {
        cmdWrite |= BITRESTART;
    }

    public static void dumpConfig() {
        cmdWrite |= BITCONFIG;
    }

    /*public static void rxTimeout() {
        synchronized(syncObj) {
            syncObj.notifyAll();
        }
    }*/

    private static void stateMachine() {
        synchronized(syncObj) {
            switch (syncObj.devstate) {
            case INIT:
                switch (reqtype) {
                case DEVID:
                    //devstate = DevState.DISBALED;
                    reqtype = MsgType.RDFWVER;
                    break;

                case RDFWVER:
                    reqtype = MsgType.RDCOMCFG;
                    break;

                case RDCOMCFG:
                    if (digroups > 0) {
                        reqtype = MsgType.DISTATUS;
                    }
                    else if (dogroups > 0) {
                        reqtype = MsgType.DOSTATUS;
                    }
                    else if (aigroups > 0) {
                        reqtype = MsgType.AIVALUES;
                    }
                    else {
                        disablePoll();
                    }
                    break;

                case DISTATUS:
                    reqtype = MsgType.RDDICFG;
                    reqsubtype = 0;
                    break;

                case RDDICFG:
                    reqsubtype++;
                    if (reqsubtype == disettings.length) {
                        if (dogroups > 0) {
                            reqtype = MsgType.DOSTATUS;
                        }
                        else {
                            if (NOPERIODICDEBUG == false) {
                                reqtype = MsgType.DISTATUS;
                                syncObj.devstate = Dstates.PERIODIC;
                            }
                            else {
                                disablePoll();
                            }
                        }
                    }
                    break;

                case DOSTATUS:
                    reqtype = MsgType.RDDOCFG;
                    reqsubtype = 0;
                    break;

                case RDDOCFG:
                    reqsubtype++;
                    if (reqsubtype == dosettings.length) {
                        if (NOPERIODICDEBUG == false) {
                            if (digroups > 0) {
                                reqtype = MsgType.DISTATUS;
                                syncObj.devstate = Dstates.PERIODIC;
                            }
                            else {
                                reqtype = MsgType.DOSTATUS;
                                syncObj.devstate = Dstates.PERIODIC;
                            }
                        }
                        else {
                            disablePoll();
                        }
                    }
                    break;

                case AIVALUES:
                    reqtype = MsgType.RDAICFG;
                    reqsubtype = 0;
                    break;

                case RDAICFG:
                    reqsubtype++;
                    if (reqsubtype == aisettings.length) {
                        if (NOPERIODICDEBUG == false) {
                            reqtype = MsgType.AIVALUES;
                            syncObj.devstate = Dstates.PERIODIC;
                        }
                        else {
                            disablePoll();
                        }
                    }
                    break;
                }
                break;


            case PERIODIC:
                if (cmdOn > 0) {
                    if (dogroups > 0) {
                        reqtype = MsgType.WRDOCMD;
                    }
                    else {
                        cmdOn = 0;
                    }
                }
                else if (cmdWrite > 0) {
                    if ((cmdWrite & BITCOMCFG) != 0) {
                        cmdWrite &= ~BITCOMCFG;
                        reqtype = MsgType.WRCOMCFG;
                    }
                    else if ((cmdWrite & BITIOCFG) != 0) {
                        cmdWrite &= ~BITIOCFG;
                        if (digroups > 0) {
                            reqsubtype = 0;
                            reqtype = MsgType.WRDICFG;
                        }
                        else if (dogroups > 0) {
                            reqsubtype = 0;
                            reqtype = MsgType.WRDOCFG;
                        }
                    }
                    else if ((cmdWrite & BITRESTART) != 0) {
                        cmdWrite &= ~BITRESTART;
                        reqtype = MsgType.WRRESTART;
                    }
                    else if ((cmdWrite & BITCONFIG) != 0) {
                        cmdWrite &= ~BITCONFIG;
                        reqtype = MsgType.RDCONFIG;
                        reqsubtype = 0;
                    }
                }
                else {
                    switch (reqtype) {
                    case DISTATUS:
                    case WRDOCMD:
                        if (dogroups > 0) {
                            reqtype = MsgType.DOSTATUS;
                        }
                        break;

                    case DOSTATUS:
                        if (digroups > 0) {
                            reqtype = MsgType.DISTATUS;
                        }
                        break;

                    case WRDICFG:
                        reqsubtype++;
                        if (reqsubtype == disettings.length) {
                            if (dogroups > 0) {
                                reqsubtype = 0;
                                reqtype = MsgType.WRDOCFG;
                            }
                            else {
                                syncObj.devstate = Dstates.INIT;
                                reqtype = MsgType.DISTATUS;
                            }
                        }
                        break;

                    case WRDOCFG:
                        reqsubtype++;
                        if (reqsubtype == dosettings.length) {
                            syncObj.devstate = Dstates.INIT;
                            if (digroups > 0)
                                reqtype = MsgType.DISTATUS;
                            else
                                reqtype = MsgType.DOSTATUS;
                        }
                        break;

                    case RDCONFIG:
                        reqsubtype++;
                        if (reqsubtype == ((2048 / CFGBLOCK) >> 1)) {
                            defaultPeriodc();
                        }
                        break;

                    default:
                        defaultPeriodc();
                        break;
                    }
                }
                break;
            }
        }
    }


    private static void defaultPeriodc() {
        if (digroups > 0) {
            reqtype = MsgType.DISTATUS;
        }
        else if (dogroups > 0) {
            reqtype = MsgType.DOSTATUS;
        }
        else if (aigroups > 0) {
            reqtype = MsgType.AIVALUES;
        }
        else {
            disablePoll();
        }
    }


    @Override
    public void run() {
        //System.out.println("Hello from a thread!");

        while(true) {
            synchronized(syncObj) {
                while (syncObj.devstate == Dstates.DISABLED) {
                    try {
                        syncObj.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Modbus.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }


            synchronized(syncObj) {
                if (syncObj.devstate != Dstates.DISABLED) {
                    sendState();
                    txtime = System.currentTimeMillis();
                }
            }


            if (SIMULATERX == false) {
                rxcomplete:
                switch (conntype) {
                case CTYPESERIAL:
                    while ((txtime + timeout) > System.currentTimeMillis()) {  // Enables to receive segmented messages
                        try {
                            while (serport.inputStream.available() > 0) {
                                int numBytes = serport.inputStream.read(rxrawbuff.array());
                                receiveModbus(numBytes);
                            }

                            switch (cstate) {
                            case RXNOMSG:
                            case TXMSG:
                                synchronized(syncObj) {
                                    if (syncObj.devstate == Dstates.DISABLED)
                                        break rxcomplete;
                                }
                                break;
                            default:
                                break rxcomplete;
                            }
                        } catch (IOException e) {
                            System.out.println(e);
                        }
                    }
                    /*synchronized(syncObj) {
                        try {       // Await response
                            syncObj.wait(timeout);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Modbus.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }*/
                    //System.out.println("Rx complete!");
                    break;

                case CTYPENET:
                    // long curtime = System.currentTimeMillis();
                    // while ((txtime + timeout) > curtime) {
                    while ((txtime + timeout) > System.currentTimeMillis()) {   // Enables to receive TCP segmented messages
                        rxrawbuff.position(0);
                        String failmsg;
                        if ((failmsg = Net.recvSocket(rxrawbuff)) == null) {
                            if (rxrawbuff.position() != 0)
                                receiveModbus(rxrawbuff.position());
                            if (cstate == RxState.RXOK)
                                break;
                        }
                        else {
                            TraceLog.msgLog(failmsg);
                            portctrl.openClose(false);
                            break;
                        }
                    }
                    break;
                }
            }


            switch (cstate) {
            case RXNOMSG:       // Incomplete message
            case TXMSG:         // No Rx
                synchronized(syncObj) {
                    if (syncObj.devstate == Dstates.DISABLED)
                        break;
                }

                norespcnt++;
                portctrl.timeout();

                switch (reqtype) {
                case WRDOCMD:
                    break;

                case WRDICFG:
                case WRDOCFG:
                    if (evwriocfg != null) {
                        evwriocfg.onFail(reqtype);
                        evwriocfg = null;
                    }
                    norespcnt = 100;   // Don't repeat write command after timeout
                    break;

                case WRCOMCFG:
                    if (evwrcom != null) {
                        evwrcom.onFail(reqtype);
                        evwrcom = null;
                    }
                    norespcnt = 100;    // Don't repeat write command after timeout
                    break;

                case WRRESTART:
                    syncObj.devstate = Dstates.INIT;
                    reqtype = MsgType.DEVID;
                    norespcnt = 0;      // Ignore no reply after restart command
                    break;
                }


                if (norespcnt > 3) {
                    syncObj.devstate = Dstates.INIT;
                    reqtype = MsgType.DEVID;

                    switch (conntype) {
                    case CTYPENET:
                        portctrl.openClose(false);
                        break;
                    }
                }

                cstate = RxState.RXTIMEOUT; // This is used by log function
                TraceLog.rxLog(rxbuff, rxlength, cstate);
                break;

            case RXOK:         // Rx OK
                norespcnt = 0;
                stateMachine();
                break;
            }


            synchronized(syncObj) {
                try {       // Tx Delay
                    syncObj.wait(txDelay);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Modbus.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
