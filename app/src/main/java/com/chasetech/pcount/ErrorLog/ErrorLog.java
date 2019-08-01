package com.chasetech.pcount.ErrorLog;

import android.content.Context;
import android.util.Log;

import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.library.MainLibrary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by ULTRABOOK on 5/19/2016.
 */
public class ErrorLog {

    public File fileLog;
    private Context mContext;

    public ErrorLog(String logName, Context ctx) {
        this.mContext = ctx;
        this.fileLog = new File(ctx.getExternalFilesDir(null), logName);
    }

    public void appendLog(String text, String classname)
    {
        if(!MainLibrary.dateLog.equals(MainLibrary.getDateToday())) {
            fileLog.delete();
        }

        if (!fileLog.exists())
        {
            try
            {
                fileLog.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try
        {
            String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();

            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(fileLog, true));
            String msg = "[" + MainLibrary.getDateTime() + "][" + classname + "][" + methodName + "][" + lineNumber + "][" + MainLibrary.getDeviceName() + "][" + MainLibrary.GetDeviceOsVersion() + "-" + MainLibrary.GetApiLevelDevice() + "]" + MainLibrary.versionCode + "-" + SQLiteDB.DATABASE_VERSION + ": " + text + "\n";
            Log.e(classname, msg);
            buf.append(msg);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
