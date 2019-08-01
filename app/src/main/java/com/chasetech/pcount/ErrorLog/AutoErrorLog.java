package com.chasetech.pcount.ErrorLog;

import android.content.Context;
import android.util.Log;

import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.library.MainLibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ULTRABOOK on 5/20/2016.
 */
public class AutoErrorLog implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultUEH;
    private Context app;
    private String className;
    private File fileLog;

    public AutoErrorLog(Context app, String fileLogName) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.app = app;
        this.fileLog = new File(app.getExternalFilesDir(null), fileLogName);
        this.className = this.getClass().getSimpleName();
    }

    public void uncaughtException(Thread t, Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
        String report = "";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        report += "-----------CAUSE------------------\n\n";

        Throwable cause = e.getCause();
        if (cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i = 0; i < arr.length; i++)
                report += "    " + arr[i].toString() + "\n";
        }

        report += "Datetime: " + MainLibrary.getDateTime() + "\n";
        report += "Model: " + MainLibrary.getDeviceName() + "\n";
        report += "SQLite Version: " + String.valueOf(SQLiteDB.DATABASE_VERSION) + "\n";
        report += "Version code: " + String.valueOf(MainLibrary.versionCode) + "\n";
        report += "Version name: " + String.valueOf(MainLibrary.versionName) + "\n";
        report += "Device OS: " + MainLibrary.GetDeviceOsVersion() + "\n";
        report += "Device API level: " + MainLibrary.GetApiLevelDevice() + "\n";
        report += "\n-------------------------------\n\n";

        try {
            FileOutputStream trace = new FileOutputStream(fileLog);
            trace.write(report.getBytes());
            trace.close();
        } catch (IOException ioe) {
            String err = ioe.getMessage() != null ? ioe.getMessage() : "Error in logs.";
            Log.e(className, err);
        }

        defaultUEH.uncaughtException(t, e);
    }
}
