package com.chasetech.pcount;



import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.ErrorLog.ErrorLog;
import com.chasetech.pcount.autoupdate.AutoUpdate;
import com.chasetech.pcount.database.SQLLib;
import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.library.MainLibrary;
import com.novoda.merlin.MerlinsBeard;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private PowerManager.WakeLock wlStayAwake;

    private String password;
    private String username = "";
    private String urlDownload;
    private String urlGet;
    private String urlLogout;
    private String urlConnect;

    private String urlDownloadFile;

    private ProgressDialog progressDL;
    private AlertDialog mAlertDialog;
    private SQLLib sqlLibrary;

    private int SETTINGS_TXT = 1;
    private int STORE_TXT = 2;
    private int ITEMS_TXT = 3;
    private int ASSORTMENT_TXT = 4;
    private int PROMO_TXT =6;
    private int[] ARRAY_LISTS = {SETTINGS_TXT, STORE_TXT, ITEMS_TXT, ASSORTMENT_TXT, PROMO_TXT};
    private static final int BUFFER_SIZE = 4096;

    private boolean isSendError = false;

    private Integer nUserid = 0;
    private String strUsername = "";

    private EditText editUsername;
    private EditText editPassword;

    private int versionCode = 0;
    private String versionName = "";

    private AutoUpdate au;
    private String TAG = "";
    private String hashLogged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TAG = MainActivity.this.getLocalClassName();

        MainLibrary.gStrDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        MainLibrary.errlogFile = MainLibrary.gStrDeviceId + ".txt";
        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(this, MainLibrary.errlogFile));

        PackageManager pm = this.getPackageManager();
        String packageName = this.getPackageName();
        int flags = PackageManager.GET_PERMISSIONS;
        PackageInfo pmInfo;

        MainLibrary.LoadAllFolders(this);

        try {
            pmInfo = pm.getPackageInfo(packageName, flags);
            versionName = pmInfo.versionName;
            versionCode = pmInfo.versionCode;
            MainLibrary.versionCode = versionCode;
            MainLibrary.versionName = versionName;
        }
        catch (PackageManager.NameNotFoundException nex) {
            nex.printStackTrace();
            Log.e("NameNotFoundException", nex.getMessage());
        }

        MainLibrary.errorLog = new ErrorLog(MainLibrary.errlogFile, this);

        MainLibrary.LoadFolders();

        sqlLibrary = new SQLLib(this);

        urlConnect = MainLibrary.API_URL;//uniliver
//        urlConnect = MainLibrary.DAVIES;//davies
//        if(MainLibrary.BETA) urlConnect = MainLibrary.API_URL_BETA;

        if(MainLibrary.BETA) urlConnect = MainLibrary.API_URL_BETA;

        urlGet =  urlConnect + "/api/auth?";
        urlLogout =  urlConnect + "/api/logout?";
        urlDownload = urlConnect + "/api/download?";

        PowerManager powerManager = (PowerManager) getSystemService(getApplicationContext().POWER_SERVICE);
        wlStayAwake = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "wakelocktag");

        final Button btnLogin = (Button) this.findViewById(R.id.btnLogin);
        editUsername = (EditText) this.findViewById(R.id.edit_username);
        editPassword = (EditText) this.findViewById(R.id.edit_password);
        final TextView tvwVersion = (TextView) findViewById(R.id.tvwVersion);
        String version = "v. " + versionName;
        tvwVersion.setText(version);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(editUsername.getText().toString().trim().equals("")) {
                    editUsername.setError("This field is required!");
                    return;
                }

                if(editPassword.getText().toString().trim().equals("")) {
                    editPassword.setError("This field is required!");
                    return;
                }

                if(au.isUpdating) {
                    Toast.makeText(MainActivity.this, "The application is updating. Please wait.", Toast.LENGTH_SHORT).show();
                    return;
                }

                editUsername.setError(null);
                editPassword.setError(null);

                MerlinsBeard checkConn = MerlinsBeard.from(MainActivity.this);
                if(!checkConn.isConnected()) {
                    Toast.makeText(MainActivity.this, "No internet connection.", Toast.LENGTH_SHORT).show();
                    return;
                }

                View view = MainActivity.this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                wlStayAwake.acquire();

                //new CheckIfHasUpdate().execute();
                new CheckInternet().execute();
            }
        });

        MainLibrary.spSettings = getSharedPreferences(getString(R.string.pcount_sharedprefKey), Context.MODE_PRIVATE);

        boolean isLoggedIn = MainLibrary.spSettings.getBoolean(getString(R.string.logged_pref_key), false);
        MainLibrary.gStrCurrentUserID = MainLibrary.spSettings.getInt(getString(R.string.pref_userid), MainLibrary.gStrCurrentUserID);

        MainLibrary.dateLog = MainLibrary.spSettings.getString(getString(R.string.pref_date_log), MainLibrary.getDateToday());
        MainLibrary.masterfileDateRealese = MainLibrary.spSettings.getString(getString(R.string.pref_masterfile_date), "");
        MainLibrary.savedHashKey = MainLibrary.spSettings.getString(getString(R.string.pref_hash), MainLibrary.savedHashKey);

        MainLibrary.errorLog.appendLog("New application run.", TAG);

        SharedPreferences.Editor spEdit = MainLibrary.spSettings.edit();
        spEdit.putString(getString(R.string.pref_date_log), MainLibrary.getDateToday());
        spEdit.apply();

        MainLibrary.verifyStoragePermissions(this); // grant permissions for other lower devices.

        if(isLoggedIn) {
            String username = MainLibrary.spSettings.getString(getString(R.string.pref_username), "");
            MainLibrary.errorLog.appendLog("User Logged: " + username, TAG);
            Intent intent = new Intent(MainActivity.this, StoresActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        editUsername.setEnabled(true);

        if(MainLibrary.gStrCurrentUserID > 0) {
            String username = MainLibrary.spSettings.getString(getString(R.string.pref_username), "");
            String strUsername = username.trim().toUpperCase();
            editUsername.setText(strUsername);
            editUsername.setEnabled(false);
        }

        // update apk
        au = new AutoUpdate(MainActivity.this);

//        sqlLibrary.ExecSQLWrite("DROP TABLE IF EXISTS "+ SQLiteDB.TABLE_PROMO);
//        sqlLibrary.ExecSQLWrite("DROP TABLE IF EXISTS "+ SQLiteDB.TABLE_TRANSACTION_PROMO);
        if(!sqlLibrary.IfTableExist(SQLiteDB.TABLE_PROMO))
        {
            sqlLibrary.ExecSQLWrite(SQLiteDB.DATABASE_CREATE_TABLE_PROMO);

            sqlLibrary.ExecSQLWrite("CREATE INDEX promoDescIndex ON " + SQLiteDB.TABLE_PROMO + " (desc)");
            sqlLibrary.ExecSQLWrite("CREATE INDEX promoCategoryIndex ON " + SQLiteDB.TABLE_PROMO + " (categoryid)");
            sqlLibrary.ExecSQLWrite("CREATE INDEX promoBrandIndex ON " + SQLiteDB.TABLE_PROMO + " (brandid)");
            sqlLibrary.ExecSQLWrite("CREATE INDEX promoDivisionIndex ON " + SQLiteDB.TABLE_PROMO + " (divisionid)");
            sqlLibrary.ExecSQLWrite("CREATE INDEX promoSubCategoryIndex ON " + SQLiteDB.TABLE_PROMO + " (subcategoryid)");
        }

        if(!sqlLibrary.IfTableExist(SQLiteDB.TABLE_TRANSACTION_PROMO)) {

            sqlLibrary.ExecSQLWrite(SQLiteDB.DATABASE_CREATE_TABLE_TRANSPROMO);
            sqlLibrary.ExecSQLWrite("CREATE INDEX promotransactionIndex ON " + SQLiteDB.TABLE_TRANSACTION_PROMO + " (date,storeid)");
            sqlLibrary.ExecSQLWrite("CREATE INDEX promotransactionBarcodeIndex ON " + SQLiteDB.TABLE_TRANSACTION_PROMO + " (barcode)");

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        urlDownload = urlConnect + "/api/download?";
        if(MainLibrary.InitializedHash)
        {

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private class CheckInternet extends AsyncTask<Void, Void, Boolean> {
        String errmsg = "";

        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(MainActivity.this, "", "Checking internet connection.");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;

            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if(activeNetwork != null) {
                if(activeNetwork.isFailover()) errmsg = "Internet connection fail over.";
                result = activeNetwork.isAvailable() || activeNetwork.isConnectedOrConnecting();
            }
            else {
                errmsg = "No internet connection.";
                MainLibrary.errorLog.appendLog(errmsg, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if(!bResult) {
                Toast.makeText(MainActivity.this, errmsg, Toast.LENGTH_SHORT).show();
                return;
            }

            if(isSendError) {
                isSendError = false;
                new PostErrorReport().execute();
                return;
            }

            new CheckIfHasUpdate().execute();
        }
    }

    private class PostErrorReport extends AsyncTask<Void, Void, Boolean> {
        String errMsg = "";
        String response = "";
        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(MainActivity.this, "", "Sending error report to dev team.");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;

            String urlSend = MainLibrary.API_URL + "/api/uploadtrace";

            if(!MainLibrary.errorLog.fileLog.exists()) {
                errMsg = "No errors to send.";
                return false;
            }

            String attachmentName = "data";
            String attachmentFileName = MainLibrary.errlogFile;
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary =  "*****";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024 * 1024;

            try {

                FileInputStream fileInputStream = new FileInputStream(MainLibrary.errorLog.fileLog); // text file to upload
                HttpURLConnection httpUrlConnection = null;
                URL url = new URL(urlSend); // url to post
                httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setUseCaches(false);
                httpUrlConnection.setDoOutput(true);

                httpUrlConnection.setRequestMethod("POST");
                httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(
                        httpUrlConnection.getOutputStream());

                request.writeBytes(twoHyphens + boundary + crlf);
                request.writeBytes("Content-Disposition: form-data; name=\"" +
                        attachmentName + "\";filename=\"" + attachmentFileName + "\"" + crlf);
                request.writeBytes(crlf);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    request.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                request.writeBytes(crlf);
                request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
                request.flush();
                request.close();

                InputStream responseStream = new
                        BufferedInputStream(httpUrlConnection.getInputStream());

                BufferedReader responseStreamReader =
                        new BufferedReader(new InputStreamReader(responseStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                responseStreamReader.close();

                response = stringBuilder.toString();

                JSONObject jsonResp = new JSONObject(response);
                if(!jsonResp.isNull("msg")) {
                    response = jsonResp.getString("msg");
                }

                responseStream.close();
                httpUrlConnection.disconnect();

                result = true;
            }
            catch (IOException ex) {
                errMsg = "Slow or unstable internet connection.";
                String err = ex.getMessage() != null ? ex.getMessage() : errMsg;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
            }
            catch (JSONException ex) {
                errMsg = "Error in web response data.";
                String err = ex.getMessage() != null ? ex.getMessage() : errMsg;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if(!bResult) {
                Toast.makeText(MainActivity.this, errMsg, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
            MainLibrary.errorLog.fileLog.delete();
        }
    }

    private class AsyncGetUser extends AsyncTask<Void, Void, Boolean> {

        private boolean isLoggedOut = false;
        String response = "";
        String errMsg;

        AsyncGetUser(boolean logout)
        {
            this.isLoggedOut = logout;
        }

        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(MainActivity.this, "", "Logging in. Please Wait...", true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;

            try {

                String urlfinal = urlGet + "email=" + username + "&pwd=" + password + "&device_id=" + MainLibrary.gStrDeviceId + "&version=" + versionName;
                if(isLoggedOut)
                    urlfinal = urlLogout + "email=" + username + "&pwd=" + password + "&device_id=" + MainLibrary.gStrDeviceId;

                URL url = new URL(urlfinal);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 (compatible) ");
//                urlConnection.setRequestProperty("Accept","*/*");
                urlConnection.connect();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                urlConnection.disconnect();
                response = stringBuilder.toString();
                result = true;
            } catch(Exception e){
                errMsg = "Slow or unstable internet connection. Please try again";
                String err = e.getMessage() == null ? e.getMessage() : errMsg;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if(!bResult) {
                Toast.makeText(MainActivity.this, errMsg, Toast.LENGTH_LONG).show();
                return;
            }

            try {
                JSONObject data = new JSONObject(response);
                int status = 0;
                if(data.has("status")) {
                    status = data.getInt("status");
                }
                if(data.has("log_status")) {
                    status = data.getInt("log_status");
                }

                if(status == 1) {
                    String usercode = data.getString("id");
                    String name = data.getString("name");
                    hashLogged = data.getString("hash").trim();

                    String userCodeLogged = "";

                    Cursor cursUser = sqlLibrary.GetDataCursor(SQLiteDB.TABLE_USER);
                    if(cursUser.moveToFirst()) {
                        userCodeLogged = cursUser.getString(cursUser.getColumnIndex(SQLiteDB.COLUMN_USER_UID));
                    }
                    cursUser.close();

                    MainLibrary.isMasterfileUpdated = MainLibrary.savedHashKey.trim().equals(hashLogged) && userCodeLogged.trim().equals(usercode);

                    urlDownload = urlDownload + "id=" + usercode;
                    nUserid = Integer.parseInt(usercode);
                    strUsername = name;

                    MainLibrary.errorLog.appendLog("HASH LOGGED: " + hashLogged + "\nSAVED HASH: " + MainLibrary.savedHashKey.trim(), TAG);

                    if (!MainLibrary.isMasterfileUpdated) { // not same hash or user logged, will download masterfile.
                        new AsyncDownloadFile().execute();
                    }
                    else { // same hash and user are logged again.

                        try {
                            LoadSettings();
                            MainLibrary.gStrCurrentUserID = nUserid;
                            MainLibrary.gStrCurrentUserName = strUsername;

                            SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
                            spEditor.putBoolean(getString(R.string.logged_pref_key), true);
                            spEditor.putInt(getString(R.string.pref_userid), nUserid);
                            spEditor.putString(getString(R.string.pref_username), strUsername);
                            spEditor.putString(getString(R.string.pref_hash), hashLogged);
                            spEditor.apply();

                            sqlLibrary.TruncateTable(SQLiteDB.TABLE_USER);
                            sqlLibrary.insertToUser(nUserid, strUsername, hashLogged);

                            MainLibrary.errorLog.appendLog("User successfully logged: " + MainLibrary.gStrCurrentUserName, TAG);

                            Intent intent = new Intent(MainActivity.this, StoresActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        catch (IOException ex) {
                            String errmsg = "Error in web response data.";
                            String err = ex.getMessage() != null ? ex.getMessage() : errmsg;
                            MainLibrary.errorLog.appendLog(err, TAG);
                            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else {
                    String msg = data.getString("msg").trim();
                    Toast.makeText(MainActivity.this, msg.trim(), Toast.LENGTH_LONG).show();
                    sqlLibrary.TruncateTable("user");
                    MainLibrary.errorLog.appendLog(msg, TAG);
                }

            }
            catch (JSONException e) {
                String prompt = "Data returned from web server error.";
                String errmsg = e.getMessage() != null ? e.getMessage() : prompt;
                MainLibrary.errorLog.appendLog(errmsg, TAG);
                MainLibrary.messageBox(MainActivity.this, "Web server", prompt + " " + errmsg);
            }
        }
    }

    private class GetPrnFiles extends AsyncTask<Void, Void, Boolean> {
        String response;
        private String errMsg;

        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(MainActivity.this, "", "Downloading required items.");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bReturn = false;

            try {
                String urlfinal = MainLibrary.BETA ? MainLibrary.API_GET_PRNFILENAMES_BETA : MainLibrary.API_GET_PRNFILENAMES;
                URL url = new URL(urlfinal);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    response = stringBuilder.toString();
                }
                finally {
                    urlConnection.disconnect();
                }

                try {
                    JSONObject data = new JSONObject(response);
                    if(data != null) {
                        String msg = data.getString("msg");
                        JSONArray jArray = data.getJSONArray("files");

                        String saveDir = Uri.fromFile(MainLibrary.PRN_FOLDER).getPath();

                        // DOWNLOAD PRN FILES
                        URL urlDownload = null;
                        if(jArray != null) {
                            for (int i = 0; i < jArray.length(); i++) {
                                String prnfile = jArray.getString(i);

                                String strUrl = MainLibrary.BETA ? MainLibrary.API_GET_PRNFILES_BETA + prnfile : MainLibrary.API_GET_PRNFILES + prnfile;

                                urlDownload = new URL(strUrl);
                                HttpURLConnection httpConn = (HttpURLConnection) urlDownload.openConnection();
                                final int responseConde = httpConn.getResponseCode();
                                if(responseConde == HttpURLConnection.HTTP_OK) {
                                    String fileName = "";
                                    String disposition = httpConn.getHeaderField("Content-Disposition");

                                    if (disposition != null) {
                                        // extracts file name from header field
                                        int index = disposition.indexOf("filename=");
                                        if (index > 0) {
                                            fileName = disposition.substring(index + 10, disposition.length() - 1);
                                        }
                                    } else {
                                        // extracts file name from URL
                                        fileName = strUrl.substring(strUrl.lastIndexOf("/") + 1,
                                                strUrl.length());
                                    }

                                    String saveFilePath = saveDir + File.separator + fileName;
                                    File fprn = new File(saveFilePath);
                                    if(fprn.exists()) fprn.delete();

                                    InputStream inputStream = httpConn.getInputStream();
                                    FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                                    int bytesRead = -1;
                                    byte[] buffer = new byte[BUFFER_SIZE];

                                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, bytesRead);
                                    }

                                    outputStream.close();
                                    inputStream.close();
                                }
                            }
                        }
                        bReturn = true;
                    }
                    else response = "No response returned.";

                }
                catch (JSONException jex) {
                    errMsg = "Error in web response data.";
                    String err = jex.getMessage() != null ? jex.getMessage() : errMsg;
                    Log.e(TAG, err);
                    MainLibrary.errorLog.appendLog(err, TAG);
                }
            }
            catch(UnknownHostException e) {
                errMsg = "Slow or unstable internet connection. Please try again";
                String err = e.getMessage() != null ? e.getMessage() : errMsg;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
                response = errMsg;
            }
            catch(MalformedURLException e) {
                errMsg = "Can't connect to web server.";
                String err = e.getMessage() != null ? e.getMessage() : errMsg;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
                response = errMsg;
            }
            catch(IOException e) {
                errMsg = "Slow or unstable internet connection. Please try again";
                String err = e.getMessage() != null ? e.getMessage() : errMsg;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
                response = errMsg;
            }

            return bReturn;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            progressDL.dismiss();
            if(!aBoolean) {
                Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
                return;
            }

            new SaveData().execute();
        }
    }

    private class SaveData extends AsyncTask<Void, String, Boolean> {

        String errmsg = "";
        String strTable = "";
        // STORE MKL AND ASSORTMENT RECORDS
        File fTextFileMKL = new File (Environment.getExternalStorageDirectory(), MainLibrary.MKL_TEXTFILE);
        File fTextFileAssort = new File(Environment.getExternalStorageDirectory(), MainLibrary.ASSORTMENT_TXTFILE);
        File fTextFileStores = new File(Environment.getExternalStorageDirectory(), MainLibrary.STORES_TXTFILE);
        File fTextFilePromo = new File(Environment.getExternalStorageDirectory(), MainLibrary.PROMO_TEXTFILE);
        LineNumberReader lnReader;

        @Override
        protected void onPreExecute() {
            progressDL = new ProgressDialog(MainActivity.this);
            progressDL.setCancelable(false);
            progressDL.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDL.setMessage("Saving data. Please wait.");
            progressDL.show();
            int nMaxprogress = 0;

            if(fTextFileMKL.exists()) {
                try{
                    lnReader = new LineNumberReader(new FileReader(fTextFileMKL));
                    while (lnReader.readLine() != null) { }
                    nMaxprogress += lnReader.getLineNumber();
                }
                catch (IOException ie) { MainLibrary.errorLog.appendLog("No data found in MKL.", TAG); }
            }
            if(fTextFileAssort.exists()) {
                try{
                    lnReader = new LineNumberReader(new FileReader(fTextFileAssort));
                    while (lnReader.readLine() != null) { }
                    nMaxprogress += lnReader.getLineNumber();
                }
                catch (IOException ie) { MainLibrary.errorLog.appendLog("No data found in Assortment.", TAG); }
            }

            if(fTextFilePromo.exists()) {
                try{
                    lnReader = new LineNumberReader(new FileReader(fTextFilePromo));
                    while (lnReader.readLine() != null) { }
                    nMaxprogress += lnReader.getLineNumber();
                }
                catch (IOException ie) { MainLibrary.errorLog.appendLog("No data found in Promo.", TAG); }
            }

            if(fTextFileStores.exists()) {
                try{
                    lnReader = new LineNumberReader(new FileReader(fTextFileStores));
                    while (lnReader.readLine() != null) { }
                    nMaxprogress += lnReader.getLineNumber();
                }
                catch (IOException ie) { MainLibrary.errorLog.appendLog("No data found in Stores.", TAG); }
            }

            progressDL.setMax(nMaxprogress);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            progressDL.setMessage(values[0]);
            progressDL.incrementProgressBy(1);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;

            try {
                if (!MainLibrary.isMasterfileUpdated) {

                    String strQuery = "";
                    //TODO ADD PROMO TO VALIDATION
                    if (fTextFileMKL.exists() && fTextFileAssort.exists() && fTextFileStores.exists() && fTextFilePromo.exists()) {

                        strTable = SQLiteDB.TABLE_PCOUNT;
                        sqlLibrary.TruncateTable(SQLiteDB.TABLE_PCOUNT);
                        sqlLibrary.TruncateTable(SQLiteDB.TABLE_TRANSACTION);

                        if(sqlLibrary.GetDataCount(SQLiteDB.TABLE_PCOUNT) == 0)
                        {

                        }
                        BufferedReader brMkl = new BufferedReader(new FileReader(fTextFileMKL));
                        strQuery = sqlLibrary.getStringBulkInsert(25, SQLiteDB.TABLE_PCOUNT);
                        //sqlLibrary.insertBulktoPcount(strQuery, brMkl);

                        // BEGIN INSERT FOR PCOUNT ITEMS
                        SQLiteDB dbHelperMkl = new SQLiteDB(MainActivity.this);
                        SQLiteDatabase dbaseMkl = dbHelperMkl.getWritableDatabase();

                        SQLiteStatement statementMkl = dbaseMkl.compileStatement(strQuery);
                        dbaseMkl.beginTransaction();
                        int idMkl = 0;
                        Boolean fLineMkl = false;
                        String lineMkl;
                        int totalMkl = 0;

                        try {
                            while ((lineMkl = brMkl.readLine()) != null) {

                                // GET TOTAL ROWS COUNT OF EXPECTED DATA
                                if (!fLineMkl) {
                                    fLineMkl = true;
                                    totalMkl = Integer.valueOf(lineMkl.trim().replace("\uFEFF", "").replace("\"", ""));
                                    continue;
                                }

                                String[] itemRefs = lineMkl.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                                String itembarcode = "";
                                if(itemRefs.length == 18) itembarcode = itemRefs[12].trim();

                                idMkl++;

                                statementMkl.clearBindings();
                                statementMkl.bindLong(1, idMkl);
                                String barcode = itemRefs[0].replace("\"", "").toString();
                                statementMkl.bindString(2, itemRefs[0].replace("\"", "")); // barcode
                                String desc = itemRefs[3].replace("\"", "").toString();
                                statementMkl.bindString(3, itemRefs[1].replace("\"","")); // desc
                                statementMkl.bindLong(4, Integer.parseInt(itemRefs[2])); // ig
                                statementMkl.bindLong(5, 0); // sapc
                                statementMkl.bindLong(6, 0); // whpc
                                statementMkl.bindLong(7, 0); // whcs
                                statementMkl.bindLong(8, Integer.parseInt(itemRefs[3])); // conversion
                                statementMkl.bindLong(9, 0); // so
                                statementMkl.bindLong(10, 0); // fso
                                statementMkl.bindString(11, itemRefs[5].replace("\"", "")); // categoryid
                                statementMkl.bindString(12, itemRefs[7].replace("\"","")); // brandid
                                statementMkl.bindString(13, itemRefs[8].replace("\"","")); // divisionid
                                statementMkl.bindString(14, itemRefs[6].replace("\"","")); // subcategoryid
                                statementMkl.bindLong(15, Integer.parseInt(itemRefs[9])); // storeid
                                statementMkl.bindDouble(16, Double.parseDouble(itemRefs[4])); // fsovalue
                                statementMkl.bindDouble(17,Integer.parseInt(itemRefs[10])); //webid
                                statementMkl.bindDouble(18,Integer.parseInt(itemRefs[11])); // multi
                                statementMkl.bindString(19, itembarcode); // otherbarcode
                                statementMkl.bindString(20, itemRefs[13]); // minstock
                                statementMkl.bindString(21, itemRefs[14].trim().replace("\"", "")); //category
                                statementMkl.bindString(22, itemRefs[15].trim().replace("\"", "")); // desc long
                                statementMkl.bindLong(23, Integer.parseInt(itemRefs[2])); // ig = oldig
                                statementMkl.bindString(24, itemRefs[16].trim().replace("\"", ""));// osa tag
                                statementMkl.bindString(25, itemRefs[17].trim().replace("\"", "")); // npi tag
                                statementMkl.execute();

                                publishProgress("Saving mkl data. Please wait.");
                            }

                            brMkl.close();

                            // if data rows is equal to total expected rows
                            if(idMkl != totalMkl) {
                                errmsg = "MKL Items are not downloaded completely. Please try again.";
                                return false;
                            }

                        } catch (IOException e) {
                            String err = "Error in inserting bulk to pcount: " + strQuery + ", " + e.getMessage();
                            errmsg = err;
                            Log.e(TAG, err);
                            MainLibrary.errorLog.appendLog(err, TAG);
                            return false;
                        }
                        finally {
                            try {
                                dbaseMkl.setTransactionSuccessful();
                                dbaseMkl.endTransaction();
                            }
                            catch (Exception e)
                            {

                            }
                        }
                        // END OF MKL BULK INSERT

                        strTable = SQLiteDB.TABLE_ASSORTMENT;
                        sqlLibrary.TruncateTable(SQLiteDB.TABLE_ASSORTMENT);
                        sqlLibrary.TruncateTable(SQLiteDB.TABLE_TRANSACTION_ASSORT);

//                        if(sqlLibrary.GetDataCount(SQLiteDB.TABLE_ASSORTMENT) == 0)
//                        {
//                            Toast.makeText(MainActivity.this,"Hindin nag truncate assort",Toast.LENGTH_LONG).show();
//                        }
                        BufferedReader brAssort = new BufferedReader(new FileReader(fTextFileAssort));
                        strQuery = sqlLibrary.getStringBulkInsert(22, SQLiteDB.TABLE_ASSORTMENT);
                        //sqlLibrary.insertBulktoAssortment(strQuery, brAssort);

                        // BEGIN ASSORTMENT BULK INSERT
                        SQLiteDB dbHelperAssort = new SQLiteDB(MainActivity.this);
                        SQLiteDatabase dbAssort = dbHelperAssort.getWritableDatabase();

                        SQLiteStatement statementAssort = dbAssort.compileStatement(strQuery);
                        dbAssort.beginTransaction();
                        int assortmentID = 0;
                        Boolean fLine = false;
                        String lineAssort;
                        int totalAssormentItems = 0;

                        try {
                            while ((lineAssort = brAssort.readLine()) != null) {

                                if (!fLine) {
                                    fLine = true;
                                    totalAssormentItems = Integer.valueOf(lineAssort.trim().replace("\uFEFF", "").replace("\"", ""));
                                    continue;
                                }

                                String[] itemRefs = lineAssort.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                                String itembarcode = "";
                                if(itemRefs.length == 16) itembarcode = itemRefs[12].trim();

                                assortmentID++;

                                statementAssort.clearBindings();
                                statementAssort.bindLong(1, assortmentID);
                                statementAssort.bindString(2, itemRefs[0].replace("\"", ""));
                                statementAssort.bindString(3, itemRefs[1].replace("\"",""));
                                statementAssort.bindLong(4, Integer.parseInt(itemRefs[2]));
                                statementAssort.bindLong(5, 0);
                                statementAssort.bindLong(6, 0);
                                statementAssort.bindLong(7, 0);
                                statementAssort.bindLong(8, Integer.parseInt(itemRefs[3]));
                                statementAssort.bindLong(9, 0);
                                statementAssort.bindLong(10, 0);
                                statementAssort.bindString(11, itemRefs[5].replace("\"", ""));
                                statementAssort.bindString(12, itemRefs[7].replace("\"",""));
                                statementAssort.bindString(13, itemRefs[8].replace("\"",""));
                                statementAssort.bindString(14, itemRefs[6].replace("\"",""));
                                statementAssort.bindLong(15, Integer.parseInt(itemRefs[9]));
                                statementAssort.bindDouble(16, Double.parseDouble(itemRefs[4]));
                                statementAssort.bindDouble(17,Integer.parseInt(itemRefs[10]));
                                statementAssort.bindDouble(18,Integer.parseInt(itemRefs[11]));
                                statementAssort.bindString(19, itembarcode);
                                statementAssort.bindString(20, itemRefs[13]);
                                statementAssort.bindString(21, itemRefs[14].trim().replace("\"", ""));
                                statementAssort.bindString(22, itemRefs[15].trim().replace("\"", ""));
                                statementAssort.execute();
                                publishProgress("Saving assortment data. Please wait.");
                            }

                            brAssort.close();

                            // if data rows is equal to total expected rows
                            if(assortmentID != totalAssormentItems) {
                                errmsg = "Assortment items are not downloaded completely. Please try again.";
                                return false;
                            }

                        } catch (IOException e) {
                            String err = "Error in inserting bulk to Assortment: " + strQuery + ", " + e.getMessage();
                            errmsg = err;
                            MainLibrary.errorLog.appendLog(err, TAG);
                            return false;
                        }
                        finally {
                            try {
                                dbAssort.setTransactionSuccessful();
                                dbAssort.endTransaction();
                            }
                            catch (Exception e)
                            {}
                        }

                        // END OF ASSORT BULK INSERT

                        //EBEGIN PROMO BULK INSERT

                        strTable = SQLiteDB.TABLE_PROMO;
                        sqlLibrary.TruncateTable(SQLiteDB.TABLE_PROMO);
                        sqlLibrary.TruncateTable(SQLiteDB.TABLE_TRANSACTION_PROMO);

                        BufferedReader brPromo = new BufferedReader(new FileReader(fTextFilePromo));
                        strQuery = sqlLibrary.getStringBulkInsert(22, SQLiteDB.TABLE_PROMO);

                        SQLiteDB dbHelperPromo= new SQLiteDB(MainActivity.this);
                        SQLiteDatabase dbPromo= dbHelperPromo.getWritableDatabase();

                        SQLiteStatement statementPromo = dbPromo.compileStatement(strQuery);
                        dbPromo.beginTransaction();
                        int PromoID = 0;
                        Boolean fLinePrmo = false;
                        String linePromo;
                        int totalPromoItems = 0;

                        try {
                            while ((linePromo = brPromo.readLine()) != null) {

                                if (!fLinePrmo) {
                                    fLinePrmo = true;
                                    totalPromoItems = Integer.valueOf(linePromo.trim().replace("\uFEFF", "").replace("\"", ""));
                                    continue;
                                }

                                String[] itemRefs = linePromo.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                                String itembarcode = "";
                                if(itemRefs.length == 16) itembarcode = itemRefs[12].trim();

                                PromoID++;

                                statementPromo.clearBindings();
                                statementPromo.bindLong(1, PromoID);
                                statementPromo.bindString(2, itemRefs[0].replace("\"", ""));
                                statementPromo.bindString(3, itemRefs[1].replace("\"",""));
                                statementPromo.bindLong(4, Integer.parseInt(itemRefs[2]));
                                statementPromo.bindLong(5, 0);
                                statementPromo.bindLong(6, 0);
                                statementPromo.bindLong(7, 0);
                                statementPromo.bindLong(8, Integer.parseInt(itemRefs[3]));
                                statementPromo.bindLong(9, 0);
                                statementPromo.bindLong(10, 0);
                                statementPromo.bindString(11, itemRefs[5].replace("\"", ""));
                                statementPromo.bindString(12, itemRefs[7].replace("\"",""));
                                statementPromo.bindString(13, itemRefs[8].replace("\"",""));
                                statementPromo.bindString(14, itemRefs[6].replace("\"",""));
                                statementPromo.bindLong(15, Integer.parseInt(itemRefs[9]));
                                statementPromo.bindDouble(16, Double.parseDouble(itemRefs[4]));
                                statementPromo.bindDouble(17,Integer.parseInt(itemRefs[10]));
                                statementPromo.bindDouble(18,Integer.parseInt(itemRefs[11]));
                                statementPromo.bindString(19, itembarcode);
                                statementPromo.bindString(20, itemRefs[13]);
                                statementPromo.bindString(21, itemRefs[14].trim().replace("\"", ""));
                                statementPromo.bindString(22, itemRefs[15].trim().replace("\"", ""));
                                statementPromo.execute();
                                publishProgress("Saving Promo data. Please wait.");
                            }

                            brPromo.close();

                            // if data rows is equal to total expected rows
                            if(PromoID != totalPromoItems) {
                                errmsg = "Promo items are not downloaded completely. Please try again.";
                                return false;
                            }

                        } catch (IOException e) {
                            String err = "Error in inserting bulk to Promo: " + strQuery + ", " + e.getMessage();
                            errmsg = err;
                            MainLibrary.errorLog.appendLog(err, TAG);
                            return false;
                        }
                        finally {

                            try {
                                dbPromo.setTransactionSuccessful();
                                dbPromo.endTransaction();
                            }
                            catch (Exception e)
                            {

                            }

                        }

                        //END OF PROMO BULK INSERT

                        strTable = SQLiteDB.TABLE_STORE;
                        sqlLibrary.TruncateTable(SQLiteDB.TABLE_STORE);
                        sqlLibrary.TruncateTable(SQLiteDB.TABLE_STORE2);

                        try {

                            BufferedReader br = new BufferedReader(new FileReader(fTextFileStores));
                            String lineStore;

                            Boolean bStart = false;
                            int totalStores = 0;
                            int nCounterStores = 0;

                            while ((lineStore = br.readLine()) != null) {
                                String[] itemRefs = lineStore.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                                if (!bStart) {
                                    bStart = true;
                                    totalStores = Integer.valueOf(lineStore.trim().replace("\uFEFF", "").replace("\"", ""));
                                    continue;
                                }

                                if (itemRefs[0].startsWith("\"")) {
                                    continue;
                                }

                                nCounterStores++;

                                int branchid = Integer.parseInt(itemRefs[0].trim());
                                String storecode = itemRefs[1].trim();
                                String branchdesc = itemRefs[2].replace("\"", "");
                                int channelID = Integer.parseInt(itemRefs[3].trim());
                                String channelDesc = itemRefs[4].replace("\"", "");
                                String channelArea = itemRefs[5].replace("\"", "");

                                sqlLibrary.insertToBranch(branchid, storecode, branchdesc, channelID, channelDesc, channelArea );
                                publishProgress("Saving stores data. Please wait.");
                            }

                            br.close();
                            // if data rows is equal to total expected rows
                            if(nCounterStores != totalStores) {
                                errmsg = "Stores are not downloaded completely. Please try again.";
                                return false;
                            }
                        }
                        catch (IOException e) {
                            String err = "Error in inserting bulk to pcount: INSERTING STORE.";
                            String exErr = e.getMessage() != null ? e.getMessage() : err;
                            MainLibrary.errorLog.appendLog(exErr, TAG);
                            return false;
                        }

                        result = true;
                    }
                    else {
                        errmsg = "MKL or Assortment or Promo data textfile is not existing. Please re-log again.";
                    }
                }
            }
            catch (Exception ex) {
                errmsg = "Error in saving data for " + strTable + ". Please try to log again.";
                String err = ex.getMessage() != null ? ex.getMessage() : errmsg;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            progressDL.dismiss();
            if(!aBoolean) {
                MainLibrary.messageBox(MainActivity.this, "Error in saving data", errmsg);
                return;
            }

            try {
                LoadSettings();
                SaveMasterfileReleaseDate();
                MainLibrary.gStrCurrentUserID = nUserid;
                MainLibrary.gStrCurrentUserName = strUsername;

                SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
                spEditor.putBoolean(getString(R.string.logged_pref_key), true);
                spEditor.putInt(getString(R.string.pref_userid), nUserid);
                spEditor.putString(getString(R.string.pref_username), strUsername);
                spEditor.putString(getString(R.string.pref_hash), hashLogged);
                spEditor.apply();

                sqlLibrary.TruncateTable(SQLiteDB.TABLE_USER);
                sqlLibrary.insertToUser(nUserid, strUsername, hashLogged);

                MainLibrary.errorLog.appendLog("User successfully logged: " + MainLibrary.gStrCurrentUserName, TAG);

                Intent intent = new Intent(MainActivity.this, StoresActivity.class);
                startActivity(intent);
                finish();
            }
            catch (IOException ex) {
                errmsg = "Error in web response data.";
                String err = ex.getMessage() != null ? ex.getMessage() : errmsg;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
                Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void SaveMasterfileReleaseDate() {
        SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
        spEditor.putString(getString(R.string.pref_masterfile_date), MainLibrary.getDateTime());
        spEditor.apply();
    }

    private void LoadSettings() throws IOException {
        // LOAD SETTINGS
        File settingsFile = new File (Environment.getExternalStorageDirectory(), MainLibrary.SETTINGS_TXTFILE);
        if(settingsFile.exists()) {
            BufferedReader brSettings = new BufferedReader(new FileReader(settingsFile));
            String sline = "";
            while ((sline = brSettings.readLine()) != null) {
                String[] itemSettings = sline.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                MainLibrary.allowIG = itemSettings[0].trim().replace("\uFEFF", "").equals("1");
                MainLibrary.MKL_allItemsReqForPosting = itemSettings[1].trim().equals("1");
                MainLibrary.MKL_allItemsReqForPrinting = itemSettings[2].trim().equals("1");
                MainLibrary.ASSORT_allItemsReqForPosting = itemSettings[3].trim().equals("1");
                MainLibrary.ASSORT_allItemsReqForPrinting = itemSettings[4].trim().equals("1");
                MainLibrary.CLEAR_USER_PASSWORD = itemSettings[5].trim();

                MainLibrary.MKL_validateDatePosting = itemSettings[6].trim().equals("1");
                MainLibrary.ASSORT_validateDatePosting = itemSettings[7].trim().equals("1");

                MainLibrary.PROMO_allItemsReqForPosting = itemSettings[8].trim().equals("1");
                MainLibrary.PROMO_allItemsReqForPrinting = itemSettings[9].trim().equals("1");
                MainLibrary.PROMO_validateDatePosting = itemSettings[10].trim().equals("1");

                SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
                spEditor.putBoolean(getString(R.string.pref_allowIG), MainLibrary.allowIG);
                spEditor.putBoolean(getString(R.string.pref_mkl_allitemsreqforposting), MainLibrary.MKL_allItemsReqForPosting);
                spEditor.putBoolean(getString(R.string.pref_mkl_allitemsreqforprinting), MainLibrary.MKL_allItemsReqForPrinting);
                spEditor.putBoolean(getString(R.string.pref_assort_allitemsreqforposting), MainLibrary.ASSORT_allItemsReqForPosting);
                spEditor.putBoolean(getString(R.string.pref_assort_allitemsreqforprinting), MainLibrary.ASSORT_allItemsReqForPrinting);
                spEditor.putString(getString(R.string.pref_userclearpassword), MainLibrary.CLEAR_USER_PASSWORD);
                spEditor.putBoolean(getString(R.string.pref_mkl_validateDatePosting), MainLibrary.MKL_validateDatePosting);
                spEditor.putBoolean(getString(R.string.pref_assort_validateDatePosting), MainLibrary.ASSORT_validateDatePosting);
                spEditor.apply();
            }
        }
    }

    // DOWNLOADING FILE
    private class AsyncDownloadFile extends AsyncTask<Void, String, Boolean> {

        String statusMessage = "";
        private DefaultHttpClient httpclient = new DefaultHttpClient();

        @Override
        protected void onPreExecute() {
            progressDL = new ProgressDialog(MainActivity.this);
            progressDL.setTitle("");
            progressDL.setMessage("Storing downloaded data.. Please wait.");
            progressDL.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDL.setCancelable(false);
            progressDL.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            try {
                progressDL.setMessage("Downloading " + values[1] + ". " + String.format(Locale.getDefault(), "%.2f", Double.valueOf(values[0])) + "Kb downloaded.");
            } catch (Exception e){
                e.getMessage();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;
            try {
                String saveDir = Environment.getExternalStorageDirectory() + File.separator; //Uri.fromFile(dlPath).getPath(); //

                for (int type : ARRAY_LISTS) {
                    urlDownloadFile = urlDownload + "&type=" + type;

                    URL url = new URL(urlDownloadFile);
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                    httpConn.setReadTimeout(60000 /* milliseconds */);
                    httpConn.setConnectTimeout(60000 /* milliseconds */);
                    httpConn.setRequestMethod("GET");
                    httpConn.setDoInput(true);
                    httpConn.connect();
                    final int responseCode = httpConn.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        String fileName = "";
                        String disposition = httpConn.getHeaderField("Content-Disposition");
                        String contentType = httpConn.getContentType();
                        int contentLength = httpConn.getContentLength();
                        if (disposition != null) {
                            // extracts file name from header field
                            int index = disposition.indexOf("filename=");
                            if (index > 0) {
                                fileName = disposition.substring(index + 10,
                                        disposition.length() - 1);
                            }
                        } else {
                            // extracts file name from URL
                            fileName = urlDownload.substring(urlDownload.lastIndexOf("/") + 1,
                                    urlDownload.length());
                        }

                        InputStream inputStream = null;
                        inputStream = httpConn.getInputStream();

                        String saveFilePath = saveDir + File.separator + fileName;

                        // opens an output stream to save into file
                        FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                        int bytesRead = -1;
                        byte[] buffer = new byte[4 * 1024];
                        double totalDownloaded = 0;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            totalDownloaded +=  Double.valueOf(bytesRead) / 1000;
                            publishProgress(String.valueOf(totalDownloaded), fileName);
                            outputStream.write(buffer, 0, bytesRead);
                        }

                        outputStream.flush();
                        outputStream.close();

                        inputStream.close();
                        System.gc();
                        httpConn.disconnect();

                        result = true;

                    }else{
                        statusMessage = "Error " + String.valueOf(responseCode) + ". Please check web server.";
                    }
                }
                return result;
            }
            catch(IOException | NullPointerException e) {
                statusMessage = "Download failed due to slow or unstable internet connection. Please try again.";
                String err = e.getMessage() != null ? e.getMessage() : statusMessage;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if(!bResult) {
                Toast.makeText(MainActivity.this, statusMessage, Toast.LENGTH_LONG).show();
                editUsername.setText("");
                editPassword.setText("");
                return;
            }

            if (!MainLibrary.BETA)
                new GetPrnFiles().execute();
            else
                new SaveData().execute();
        }
    }

    private void ClearUser() {

        mAlertDialog = new AlertDialog.Builder(MainActivity.this).create();
        mAlertDialog.setTitle("Clear User");
        mAlertDialog.setMessage("Do you want to clear user " + editUsername.getText().toString() + " ?");
        mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
                spEditor.putInt(getString(R.string.pref_userid), 0);
                spEditor.putString(getString(R.string.pref_username), "");
                spEditor.apply();
                sqlLibrary.TruncateTable(SQLiteDB.TABLE_USER);
                editUsername.setText(null);
                editPassword.setText(null);
                editUsername.setEnabled(true);
            }
        });
        mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog.Builder alertPassword = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.modal_activity_password, null);
        final EditText txtPassword = (EditText) layout.findViewById(R.id.txtPassword);

        alertPassword.setTitle("Clear User").setMessage("Enter password");

        alertPassword.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = txtPassword.getText().toString().trim().toUpperCase();
                if(password.equals(MainLibrary.CLEAR_USER_PASSWORD)) {
                    dialog.dismiss();
                    mAlertDialog.show();
                }
                else Toast.makeText(MainActivity.this, "Wrong password", Toast.LENGTH_SHORT).show();
            }
        });
        alertPassword.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertPassword.setView(layout);
        alertPassword.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_clear_user:
                ClearUser();
                break;
            case R.id.action_send_error:
                isSendError = true;
                new CheckInternet().execute();
                break;
            case R.id.action_clear_updates:
                DeleteAllUpdates();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void DeleteAllUpdates() {
        File fUpdates = new File(getExternalFilesDir(null), "APK Updates");

        if(fUpdates.isDirectory()) {
            String[] files = fUpdates.list();
            for (String file : files) {
                new File(fUpdates, file).delete();
            }
        }

        Toast.makeText(MainActivity.this, "All updates with parse error are cleared.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            au.killTasks();
            unregisterReceiver(au.nReceiver);
        }
        catch (Exception ex) {
            String err = ex.getMessage() != null ? ex.getMessage() : "error";
            Log.e("Unregister", err);
        }
    }

    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    private class CheckIfHasUpdate extends AsyncTask<Void, Void, Boolean> {

        String messages = "";
        private DefaultHttpClient httpclient = new DefaultHttpClient();

       String urlCheck = AutoUpdate.API_URL_APK_CHECK;//uniliver
      //  String urlCheck = AutoUpdate.DAVIES_BETACHECK;//uniliver



        private boolean hasUpdate = false;

        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(MainActivity.this, "", "Checking for new updates.");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;

            try {
                urlCheck = AutoUpdate.API_URL_APK_CHECK;//uniliver
//                urlCheck = AutoUpdate.DAVIES_BETACHECK; //davies
                if(MainLibrary.BETA) {
                    urlCheck = AutoUpdate.API_URL_APK_BETACHECK; //uniliver
//                    urlCheck = AutoUpdate.DAVIES_BETACHECK;
                }

                HttpPost post = new HttpPost(urlCheck);
                long start = System.currentTimeMillis();

                HttpParams httpParameters = new BasicHttpParams();
                int timeoutConnection = 10000;
                HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                int timeoutSocket = 6000;
                HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

                httpclient.setParams(httpParameters);
                String packagename = MainActivity.this.getPackageName();

                StringEntity parameters = new StringEntity("pkgname=" + packagename);

                post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                post.setEntity(parameters);
                String response = EntityUtils.toString( httpclient.execute(post).getEntity(), "UTF-8" );

                if(response.trim().toLowerCase().contains("be right back")) {
                    messages = "Web server is down. Try again later.";
                    MainLibrary.errorLog.appendLog(messages, TAG);
                    return false;
                }

                int vcodeAPI = Integer.valueOf(response.trim());
                if(vcodeAPI > versionCode) {
                    hasUpdate = true;
                }

                result = true;
            }
            catch (Exception ex) {
                messages = "Slow or unstable internet connection. Please try again.";
                String err = ex.getMessage() != null ? ex.getMessage() : messages;
                Log.e(TAG, err);
                MainLibrary.errorLog.appendLog(err, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if(!bResult) {
                MainLibrary.messageBox(MainActivity.this, "No internet", messages);
                return;
            }

            if(hasUpdate) {
                au.StartAutoUpdate();
                Toast.makeText(MainActivity.this, "An update has been released, please update system.", Toast.LENGTH_LONG).show();
                return;
            }

            username = editUsername.getText().toString(); //"PCN10";
            password = editPassword.getText().toString(); //"password"; //

            new AsyncGetUser(false).execute();
        }
    }
}