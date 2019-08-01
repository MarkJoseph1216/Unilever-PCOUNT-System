package com.chasetech.pcount.ErrorLog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import com.chasetech.pcount.R;
import com.chasetech.pcount.database.SQLLib;
import com.chasetech.pcount.library.MainLibrary;

/**
 * Created by Lloyd on 9/19/16.
 */

public class FixBugTask extends AsyncTask<Void, String, Boolean> {

    private String errmsg;
    private ProgressDialog progressDialog;
    private Context mContext;
    private SQLLib sqlLib;

    public FixBugTask(Context mContext) {
        this.mContext = mContext;
        this.sqlLib = new SQLLib(mContext);
    }

    @Override
    protected void onPreExecute() {
        progressDialog = ProgressDialog.show(this.mContext, "", "Executing fix task. Please wait.");
    }

    @Override
    protected void onProgressUpdate(String... values) {
        progressDialog.setMessage(values[0]);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean result = false;

        try {
            publishProgress("Fixing IG field in database. Please wait.");
            sqlLib.SetFixItemIG();
            result = true;
        }
        catch (Exception e) {
            errmsg = e.getMessage() != null ? e.getMessage() : "Error in fix task.";
            MainLibrary.errorLog.appendLog(errmsg, "FixTask");
        }

        return result;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        progressDialog.dismiss();
        if(!aBoolean) {
            MainLibrary.messageBox(this.mContext, "Fix Error", errmsg);
        }

        MainLibrary.FIX_IG_BUG = false;
        SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
        spEditor.putBoolean(this.mContext.getString(R.string.pref_fix_ig), false);
        spEditor.apply();
    }
}
