package com.chasetech.pcount.Woosim;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.Set;

/**
 * Created by ULTRABOOK on 5/5/2016.
 */
public class WoosimPrinter2 {

    private Context mContext;
    String PRINTER_NAME = "WOOSIM";
    String MAC_ADDRESS = "";
    public static BluetoothAdapter mBluetoothAdapter;

    public WoosimPrinter2(Context ctx) {
        this.mContext = ctx;
    }

    public boolean Open()
    {
        boolean res = false;

        if(!FindBluetoothPrinterDevice())
            return res;


/*        try {
            // Standard SerialPortService ID
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);

            mBluetoothSocket.connect();
            oStream = mBluetoothSocket.getOutputStream();
            iStream = mBluetoothSocket.getInputStream();

            BeginListenforData();
            res = true;
        } catch (NullPointerException e) {
            //mbox.ShowMessage("NullPointerException_OpenConn", e.getMessage());
            Log.e("NullPointerException", e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            //mbox.ShowMessage("Exception_Openconn", e.getMessage());
            Log.e("Exception", e.getMessage());
            e.printStackTrace();
        }*/

        return res;
    }

    private boolean FindBluetoothPrinterDevice()
    {
        boolean res = false;
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                Log.e("Bluetooth unavailable", "Bluetooth function is not available in this device.");
                /*mbox.ShowMessage("Not Available", "Bluetooth function is not available in this device");*/
                return res;
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {

                    if (device.getName().equals(PRINTER_NAME)) { // NAME OF PAIRED BLUETOOTH PRINTER
                        MAC_ADDRESS = device.getAddress();
                        res = true;
                        break;
                    }
                }
            }
        }
        catch (NullPointerException nex) {
            nex.printStackTrace();
            Log.e("NullPointerException", nex.getMessage());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Log.e("Exception", ex.getMessage());
        }

        return res;
    }
}
