package com.chasetech.pcount.TSC;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.chasetech.pcount.library.MainLibrary;
import com.example.tscdll.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Vinid on 10/26/2015.
 */
public class BPrinter extends TSCActivity {

    private Context mContext;

    public static String selectedPrinter = "BT-SPP";

    File ejDir;

    public static BluetoothAdapter mBluetoothAdapter;

    private InputStream iStream;
    private Thread workerThread;

    public static String MAC_ADDRESS = "";
    public String tsclines2 = "_______________________________________________________\n";
    public String tsclines = "------------------------------------------------------------------------------------------------------------------\n";

    public String woosimLines2 = "_________________________________________________________________";
    public String woosimLines2_4inch = "__________________________________________________________";
    public String woosimLines = "----------------------------------------------------------------";
    //public String woosimLines_4inch = "---------------------------------------------------------------------";
    public String woosimLines_4inch = "-------------------------------------------------------------------------------------------";


    public int startRow = 100 ;//86;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    public BPrinter(Context ctx)
    {
        super();
        mContext = ctx;
    }

    public String GenerateStringTSCPrint(String strBody, double totalLength, int numOfItems, int numPrint, boolean isFourInch) {

        //double lengthmm = 5;
        //double totlength = 0;

        String widthSize = "2.8";
        String fontsize = "7";
        String fontboldname = "1.EFT";
        String fontboldsize = "2";
        String barcodePadright = "20";
        String speed = "4";
        if(isFourInch) {
            widthSize = "4";
            fontsize = "10";
            fontboldname = "ROMAN.TTF";
            fontboldsize = "10";
            barcodePadright = "15";
            speed = "6";
        }

        String totalbody = "";

        String printerCommands = "";
        String printerPrintCommand = "";

        String[] strBodyperNewline = strBody.split("\\r?\\n");
        String body = "";

        int totrow = startRow;
        //totlength = strBodyperNewline.length * lengthmm; //strBodyperNewline.length

        DecimalFormat df = new DecimalFormat("0.00");
        String formate = df.format(totalLength);
        double finalLength = totalLength;

        try{
            finalLength = (Double) df.parse(formate);
            if(numOfItems == 45) {
                finalLength -= 3;
            }
            else if (numOfItems > 60) {
                finalLength -= 4;
            }
            else if (numOfItems > 75) {
                finalLength -= 5;
            }
            else if (numOfItems > 90) {
                finalLength -= 6;
            }
            else if (numOfItems > 100) {
                finalLength -= 7;
            }
        }
        catch (ParseException pex) { Log.e("ParseException", pex.getMessage()); }

        printerCommands += "BACKFEED 230\n";
        printerCommands += "SIZE " + widthSize + "," + String.valueOf(finalLength) + "\n";
        printerCommands += "DIRECTION 0,0\n";
        printerCommands += "SPEED " + speed + "\n";
        printerCommands += "CODEPAGE 1252\n";

        for (String strPrint : strBodyperNewline) {
            // GET BARCODE STYLE
            if(strPrint.contains("BARCODE")) {
                String[] barSplit = strPrint.split(";");
                String barcodeString = barSplit[0] + "" + barcodePadright + "," + totrow + "," + barSplit[1];
                body += barcodeString + "\n";
            }
            else {
                // DISPLAY ITEM VALUES TO CENTER
                if(strPrint.contains("INFO")) {
                    String rowInfo = strPrint.split(";")[1];

                    // MAKE FSO AND FSO VAL BOLD
                    if(strPrint.contains("*")){
                        String[] strFso = rowInfo.split("\\*");
                        String otherval = strFso[0];
                        String fso = strFso[1];
                        body += "TEXT 0," + (totrow - 50) + ",\"ROMAN.TTF\",0,1," + fontsize + "," + "\"" + otherval + "\"\n";
                        body += "TEXT 390," + (totrow - 50) + ",\"" + fontboldname + "\",0,1," + fontboldsize + "," + "\"" + fso + "\"\n";
                    }
                    else body += "TEXT 0," + (totrow - 50) + ",\"ROMAN.TTF\",0,1," + fontsize + "," + "\"" + rowInfo + "\"\n";
                }
                else {
                    // MAKE TOTALS BOLD
                    if(strPrint.contains("Total:")) {
                        body += "TEXT 0," + totrow + ",\"" + fontboldname + "\",0,1," + fontboldsize + "," + "\"" + strPrint + "\"\n";
                    }
                    else if(strPrint.contains("ENDLINE")) {
                        body += "TEXT 0," + totrow + ",\"ROMAN.TTF\",0,1,1," + "\" \"\n";
                    }
                    else body += "TEXT 0," + totrow + ",\"ROMAN.TTF\",0,1," + fontsize + "," + "\"" + strPrint + "\"\n";
                }
            }
            totrow += 32;
        }

        printerPrintCommand += "PRINT " + numPrint + ",1\n";
        totalbody = printerCommands + body + printerPrintCommand;
        return totalbody;
    }

    public boolean CreateTextFile(File destFolder, String txtFilename, String strBody)
    {
        boolean res = false;
        if(!destFolder.exists())
            destFolder.mkdirs();

        File destinationTextDIR = new File(destFolder, txtFilename);

        try {
            FileWriter writer = new FileWriter(destinationTextDIR);
            writer.append(strBody);
            writer.flush();
            writer.close();

            res = true;
        }
        catch (IOException e) {
            e.printStackTrace();
        }

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

                    if (device.getName().equals(selectedPrinter)) { // NAME OF PAIRED BLUETOOTH PRINTER
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

    @Override
    public String status() {
        return super.status();
    }

    @Override
    public void downloadbmp(String filename) {
        super.downloadbmp(filename);
    }

    @Override
    public void sendfile(String filename) {
        super.sendfile(filename);
    }

/*    public void DownloadFile(String filename) {
        try {
            FileInputStream fis = new FileInputStream("/sdcard/Download/" + filename);
            byte[] data = new byte[fis.available()];
            int[] FF = new int[data.length];
            String download = "DOWNLOAD F,\"" + filename + "\"," + data.length + ",";
            byte[] download_head = download.getBytes();

            while(fis.read(data) != -1) {
                ;
            }

            this.OutStream.write(download_head);
            this.OutStream.write(data);
            fis.close();
        } catch (Exception var7) {
            ;
        }
    }*/

    public boolean Open()
    {
        boolean res = false;

        if(!FindBluetoothPrinterDevice())
            return res;

        super.openport(MAC_ADDRESS);
        res = true;


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

    public boolean Close()
    {
        boolean res = false;

        try {
            stopWorker = true;
/*            oStream.close();
            iStream.close();
            mBluetoothSocket.close();*/
            super.closeport();
            res = true;
        }
        catch (NullPointerException e) {
            Log.e("NullPointerException", e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            Log.e("Exception", e.getMessage());
            e.printStackTrace();
        }

        return res;
    }

    private void BeginListenforData()
    {
        try {

            final ProgressDialog pbPrinterWaitDialog = new ProgressDialog(mContext);
            pbPrinterWaitDialog.setTitle("Printer");
            pbPrinterWaitDialog.setMessage("Setting Printer..");
            pbPrinterWaitDialog.setProgressStyle(pbPrinterWaitDialog.STYLE_SPINNER);
            pbPrinterWaitDialog.setCancelable(false);
            pbPrinterWaitDialog.show();

            final Handler handler = new Handler();

            // This is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted()
                            && !stopWorker) {

                        try {

                            int bytesAvailable = iStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                iStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length);
                                        final String data = new String(
                                                encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            public void run() {
                                                pbPrinterWaitDialog.setMessage("Setting printer.. " + data);
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }

                            }
                            pbPrinterWaitDialog.dismiss();

                        } catch (Exception ex) {
                            stopWorker = true;
                        }

                    }
                }
            });

            workerThread.start();
        }
        catch (NullPointerException e) {
            //mbox.ShowMessage("NullException_BeginListenData", e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            //mbox.ShowMessage("Exception_BeginListenData", e.getMessage());
            e.printStackTrace();
        }
    }

    public void PrintString(String strData) {

        try {
            //oStream.write(strData.getBytes());
            super.sendcommand(strData);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            Log.e("NullPointerException", e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception", e.getMessage());
        }
    }

    public String ConvertPRNToString(String filepath) {
        boolean bReturn = false;
        String totline = "";

        try {
            FileInputStream fstream = new FileInputStream(filepath);
            InputStreamReader inputreader = new InputStreamReader(fstream);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String lines = "";
            String line1 = "";
            String downloadline = "DOWNLOAD F,\"FAMILY.BAS\"\n";
            while ((lines = buffreader.readLine()) != null) {
                line1 += lines;
            }
            line1 += "\nEOP";
            totline = downloadline + line1;
            bReturn = true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("FileNotFoundException", e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e("FileNotFoundException", e.getMessage());
        }

/*        //TODO: ADD CODE
        FileInputStream inputStream = null;
        ByteArrayInputStream byteArrayInputStream; //[4]
        Integer totalWrite = 0;
        StringBuffer sb = new StringBuffer();
        try {
            inputStream = new FileInputStream(filepath);  //[5]

            byte[] buf = new byte[2048];
            int readCount = 0;
            do {
                readCount = inputStream.read(buf);
                if (readCount > 0) {
                    totalWrite += readCount;
                    byte[] bufOut = new byte[readCount];
                    System.arraycopy(buf, 0, bufOut, 0, readCount);
                    oStream.write(bufOut);
                }
            } while (readCount > 0); //[6]
            inputStream.close();
            Log.wtf("Success", String.format("printed " + totalWrite.toString() + " bytes"));
            bReturn = true;
        } catch (IOException e) {
            Log.e("IOException", "Exception in printFile: " + e.getMessage());
        }*/

        return totline;
    }
}