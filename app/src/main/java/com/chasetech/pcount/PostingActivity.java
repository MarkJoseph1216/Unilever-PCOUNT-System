package com.chasetech.pcount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.os.Environment;

import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.MKL.PCount;
import com.chasetech.pcount.database.SQLLib;
import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.library.HomeWatcher;
import com.chasetech.pcount.library.MainLibrary;
import com.simplify.ink.InkView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.chasetech.pcount.R.color.white;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ULTRABOOK on 10/21/2015.
 */
public class PostingActivity extends AppCompatActivity{

    private static final String SAMPLE_DB_NAME = "UPcDb";
    private static final String SUCCESS_RESPONSE = "Success";
    private SQLLib db;
    String urlConnect;
    private ProgressDialog progressDL;
    private AlertDialog mAlertDialog;
    File dlPath;
    File appFolder;
    String fileImage = "";
    String fileCsv = "";
    String[] filetoSend = { fileImage, fileCsv };

    String mImageName = "";

    private String currentDateSelected;
    private String DcurrentDateSelected;

    String TAG = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_signature);

        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(this, MainLibrary.errlogFile));

        HomeWatcher mHomeWatcher = new HomeWatcher(this);
        mHomeWatcher.setOnHomePressedListener(new HomeWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                NavUtils.navigateUpFromSameTask(PostingActivity.this);
            }

            @Override
            public void onHomeLongPressed() {
            }
        });
        mHomeWatcher.startWatch();

        TAG = PostingActivity.this.getLocalClassName();

/*        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(false);*/

        //exportDb();

        getSupportActionBar().setTitle(MainLibrary.gSelectedStores.storeName + " - SIGN CAPTURE");

        currentDateSelected = getIntent().getExtras().getString("datepick");
        DcurrentDateSelected = currentDateSelected.replace("-", "");

        db = new SQLLib(PostingActivity.this);
        db.open();

//        Cursor dbSum = db.queryData("select count(*) as ctr, sum(so) as so, sum(fso) as fso from trans where so > 0 and storeid = "
//                + String.valueOf(MainLibrary.gCurrentBranchSelected) + " and date = '" + MainLibrary.gStrCurrentDate + "'");
        Cursor dbSum = null;

        if(MainLibrary.isAssortmentMode & !MainLibrary.isPromoMode) {
            dbSum = db.queryData("select * from " + SQLiteDB.TABLE_TRANSACTION_ASSORT + " where storeid = "
                    + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + " and date = '" + MainLibrary.gStrCurrentDate + "'");
        }
        else {
            dbSum = db.queryData("select * from " + SQLiteDB.TABLE_TRANSACTION + " where storeid = "
                    + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + " and date = '" + MainLibrary.gStrCurrentDate + "'");
        }

        if(MainLibrary.isPromoMode)
        {
            dbSum = null;
            dbSum = db.queryData("select * from " + SQLiteDB.TABLE_TRANSACTION_PROMO + " where storeid = "
                    + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + " and date = '" + MainLibrary.gStrCurrentDate + "'");
        }


        dbSum.moveToFirst();

        int totso = 0,totfso = 0, totig = 0, totei = 0;

        while (!dbSum.isAfterLast()) {

            int sapc = dbSum.getInt(dbSum.getColumnIndex("sapc"));
            int whpc = dbSum.getInt(dbSum.getColumnIndex("whpc"));
            int whcs = dbSum.getInt(dbSum.getColumnIndex("whcs"));
            int ig = dbSum.getInt(dbSum.getColumnIndex("ig"));
            int conversion = dbSum.getInt(dbSum.getColumnIndex("conversion"));

            int so = dbSum.getInt(dbSum.getColumnIndex("so"));
            int fso = dbSum.getInt(dbSum.getColumnIndex("fso"));

            if (so > 0) {
                totso = totso + 1; //so; &&former SO Count, now Sku count with SO
            }
            //if (sapc == 0 && whpc == 0 && whcs == 0) continue;

            totei = totei + sapc + whpc + (whcs * conversion);
            totig = totig + ig;
            totfso = totfso + fso;

            dbSum.moveToNext();
        }

        final String stotsku = String.valueOf(dbSum.getCount());
        final String stotso = String.valueOf(totso);
        final String stotfso = String.valueOf(totfso);
        final String stotei = String.valueOf(totei);
        final String stotig = String.valueOf(totig);

        urlConnect = MainLibrary.API_URL;

        appFolder = Environment.getExternalStorageDirectory() ; //new File(getExternalFilesDir(null),"");
        dlPath = new File(appFolder, "");

        final InkView inkSign = (InkView) findViewById(R.id.inkSignature);
        inkSign.setColor(getResources().getColor(android.R.color.black));
        inkSign.setMinStrokeWidth(1.5f);
        inkSign.setMaxStrokeWidth(6f);

        final Button btnClear = (Button) findViewById(R.id.btnClear);
        final Button btnSubmitSign = (Button) findViewById(R.id.btnSubmitSign);

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inkSign.clear();
            }
        });

        btnSubmitSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(PostingActivity.this)
                        .setTitle("Transaction Summary")
                        .setMessage("Are you sure you want to post?\nTotal SKUs for Posting = " + stotsku + "\nTotal EI (PC)= " + stotei +
                                "\nTotal SKU with SO = " + stotso + "\nTotal FSO Qty (PC)= " + stotfso)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Bitmap inkSignature = inkSign.getBitmap(getResources().getColor(white));

                                Cursor tmp = db.queryData("select uid from user");
                                tmp.moveToLast();
                                String uid = tmp.getString(tmp.getColumnIndex("uid"));

                                mImageName = "IM_" + String.valueOf(MainLibrary.gSelectedStores.storeCode) + "-" + uid + "-" + currentDateSelected + ".jpg";
                                fileImage = mImageName;
                                File mediastorage = new File(Environment.getExternalStorageDirectory(), "");
                                File pictureFile = new File(mediastorage.getPath() + File.separator + mImageName);

                                try {
                                    FileOutputStream fos = new FileOutputStream(pictureFile);
                                    inkSignature.compress(Bitmap.CompressFormat.PNG, 90, fos);
                                    fos.close();
                                    ExportTabletoCSV();
                                    new CheckInternet().execute();
                                } catch (FileNotFoundException a) {
                                    Toast.makeText(PostingActivity.this, a.getMessage(), Toast.LENGTH_LONG).show();
                                } catch (IOException e) {
                                    Toast.makeText(PostingActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
    }

    public class CheckHash extends AsyncTask<Void, Void, Boolean> {
        String errmsg = "";
        boolean isUpdated = false;
        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(PostingActivity.this, "", "Checking masterfile update.", true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bResult = false;
            isUpdated = false;

            try {

                String urlfinal = MainLibrary.API_URL;
                if(MainLibrary.BETA)
                    urlfinal = MainLibrary.API_URL_BETA;

                String urlComplete = urlfinal + "/api/checkhash";

                URL url = new URL(urlComplete);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();

                String response = stringBuilder.toString().replace("\""," ").replace("\n"," ").trim();

                urlConnection.disconnect();

                String hashExisting = db.GetCurrentHash();
                if(hashExisting.trim().equals(response)) {
                    isUpdated = true;
                }

                bResult = true;
            }
            catch(UnknownHostException e) {
                errmsg = e.getMessage() != null ? e.getMessage() : "Web Host not available. Please check connection.";
                MainLibrary.errorLog.appendLog(errmsg, TAG);
                e.printStackTrace();
                Log.e(TAG, errmsg, e);
            }
            catch(IOException e) {
                errmsg = e.getMessage() != null ? e.getMessage() : "Slow or unstable internet connection. Please try again.";
                MainLibrary.errorLog.appendLog(errmsg, TAG);
                e.printStackTrace();
                Log.e(TAG, errmsg);
            }

            return bResult;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if(!bResult) {
                Toast.makeText(PostingActivity.this, errmsg, Toast.LENGTH_LONG).show();
                return;
            }

            if(!isUpdated) {
                new AlertDialog.Builder(PostingActivity.this)
                        .setTitle("Update Masterfile")
                        .setMessage("New masterfile found. Please re-log this account.")
                        .setPositiveButton("Log out", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                new UserLogout(PostingActivity.this).execute();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                return;
            }

            new PostCsvFiles(false).execute();
        }
    }

    public class CheckMasterfile extends AsyncTask<Void, Void, Boolean> {
        String errmsg = "";
        boolean isUpdated = false;
        String strResponse = "";
        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(PostingActivity.this, "", "Checking masterfile sync.", true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bResult = false;
            isUpdated = false;

            try {
                String urlfinal = MainLibrary.API_URL; // uniliver
//                String urlfinal = MainLibrary.DAVIES; // davies
                if(MainLibrary.BETA)
                    urlfinal = MainLibrary.API_URL_BETA;

                String urlComplete = urlfinal + "/api/lastlogin?device_id=" + MainLibrary.gStrDeviceId + "&email=" + MainLibrary.gStrCurrentUserName;

                URL url = new URL(urlComplete);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();

                String response = stringBuilder.toString().replace("\n"," ").trim();
                urlConnection.disconnect();
                if(response.trim().toLowerCase().contains("be right back")) {
                    errmsg = "Web server is down. Try again later.";
                    return false;
                }

                JSONObject jsonObject = new JSONObject(response);
                if(!jsonObject.isNull("msg")) {
                    int status = jsonObject.getInt("status");
                    String msg = jsonObject.getString("msg");
                    strResponse = msg.trim();

                    if(status == 0) isUpdated = true;
                }

                bResult = true;
            }
            catch(UnknownHostException e) {
                errmsg = e.getMessage() != null ? e.getMessage() : "Web Host not available. Please check connection.";
                MainLibrary.errorLog.appendLog(errmsg, TAG);
                e.printStackTrace();
                Log.e(TAG, errmsg, e);
            }
            catch(IOException e) {
                errmsg = e.getMessage() != null ? e.getMessage() : "Slow or unstable internet connection. Please try again.";
                MainLibrary.errorLog.appendLog(errmsg, TAG);
                e.printStackTrace();
                Log.e(TAG, errmsg);
            }
            catch(JSONException e) {
                errmsg = e.getMessage() != null ? e.getMessage() : "Data Error.";
                MainLibrary.errorLog.appendLog(errmsg, TAG);
                e.printStackTrace();
                Log.e(TAG, errmsg);
            }

            return bResult;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if(!bResult) {
                Toast.makeText(PostingActivity.this, errmsg, Toast.LENGTH_LONG).show();
                return;
            }

            if(!isUpdated) {
                new AlertDialog.Builder(PostingActivity.this)
                        .setTitle("Sync Masterfile")
                        .setMessage(strResponse + " Please re-log this account.")
                        .setPositiveButton("Log out", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                new UserLogout(PostingActivity.this).execute();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                return;
            }
            Toast.makeText(PostingActivity.this, strResponse, Toast.LENGTH_LONG).show();

            new PostCsvFiles(false).execute();
        }
    }

    public class UserLogout extends AsyncTask<Void, Void, Boolean> {

        String response;
        String errmsg;
        Context mContext;

        public UserLogout(Context ctx) {
            this.mContext = ctx;
        }

        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(mContext, "", "Logging out. Please Wait...", true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bReturn = false;

            try{
                String urlLog = MainLibrary.API_URL;
                if(MainLibrary.BETA) urlLog = MainLibrary.API_URL_BETA;

                String urlfinal = urlLog + "/api/logout?email=" + MainLibrary.gStrCurrentUserName + "&device_id=" + MainLibrary.gStrDeviceId;

                URL url = new URL(urlfinal);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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
                bReturn = true;

            } catch(IOException e){
                e.printStackTrace();
                errmsg += "No internet connection.";
            }
            return bReturn;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressDL.dismiss();
            Intent intentMain = new Intent(mContext, MainActivity.class);
            if(!success) {
                Toast.makeText(mContext, errmsg, Toast.LENGTH_LONG).show();
                return;
            }

            try {

                SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
                spEditor.putBoolean(getString(R.string.logged_pref_key), false);
                spEditor.apply();

                JSONObject data = new JSONObject(response);
                String msg = data.getString("msg");

                MainLibrary.gStrCurrentUserID = 0;
                MainLibrary.gStrCurrentUserName = "";

                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                startActivity(intentMain);
                finish();
            }
            catch (JSONException jex) {
                jex.printStackTrace();
                Log.e("JSONException", jex.getMessage());
            }
        }
    }

    public class CheckInternet extends AsyncTask<Void, Void, Boolean> {
        String errmsg = "";

        @Override
        protected void onPreExecute() {
            progressDL = ProgressDialog.show(PostingActivity.this, "", "Checking internet connection.");
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
            else
                errmsg = "No internet connection.";

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if(!bResult) {
                Toast.makeText(PostingActivity.this, errmsg, Toast.LENGTH_SHORT).show();
                return;
            }

            new CheckMasterfile().execute();
        }
    }

    private void exportDb(){
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source=null;
        FileChannel destination=null;
        File dataBaseFile = getDatabasePath("UPcDb");
        String currentDBPath = dataBaseFile.toString();
        String backupDBPath = SAMPLE_DB_NAME;
        File currentDB = dataBaseFile; //new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
        } catch(IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void ExportTabletoCSV(){
        File myFile;

        try {
            String datetimeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

            Cursor tmp = db.queryData("select uid from user");
            tmp.moveToLast();
            String uid = tmp.getString(tmp.getColumnIndex("uid"));
            String mFileName = String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "-" + uid + "-" + currentDateSelected + "-5.csv";
            fileCsv = mFileName;
            myFile = new File (Environment.getExternalStorageDirectory() + File.separator + mFileName);
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
/*            myOutWriter.append("WEBID;SAPC;WHPC;WHCS;SO;FSO;FSOVALUE;OTHERCODE;MULTIPLIER;IG;CONVERSION"); //remove posting date, insert multiplier after othercode
            myOutWriter.append("\n");*/

//            Cursor c = db.queryData("SELECT webid,sapc,whpc,whcs,so,fso,barcode FROM trans where " +
//                    "storeid = " + String.valueOf(currentBranchSelected) + " and date = '" + currentDateSelected + "'" , null);

            Cursor cursExport = null;
            if(MainLibrary.isAssortmentMode & !MainLibrary.isPromoMode) {
                cursExport = db.queryData("SELECT webid,sapc,whpc,whcs,so,fso,barcode,fsovalue,ig,conversion,multi, " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_MINSTOCK + " FROM " + SQLiteDB.TABLE_TRANSACTION_ASSORT + " where storeid = " + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + " and date = '" + currentDateSelected +
                        "' and userid = " + MainLibrary.gStrCurrentUserID , null);
            }
            else {
                cursExport = db.queryData("SELECT webid,sapc,whpc,whcs,so,fso,barcode,fsovalue,ig,conversion,multi," + SQLiteDB.COLUMN_TRANSACTION_OSATAG + "," + SQLiteDB.COLUMN_TRANSACTION_NPITAG + "," + SQLiteDB.COLUMN_TRANSACTION_MINSTOCK + " FROM " + SQLiteDB.TABLE_TRANSACTION + " where storeid = " + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + " and date = '" + currentDateSelected +
                        "' and userid = " + MainLibrary.gStrCurrentUserID , null);
            }

            if(MainLibrary.isPromoMode) {
                cursExport = null;
                cursExport = db.queryData("SELECT webid,sapc,whpc,whcs,so,fso,barcode,fsovalue,ig,conversion,multi, " + SQLiteDB.COLUMN_TRANSACTION_PROMO_MINSTOCK + " FROM " + SQLiteDB.TABLE_TRANSACTION_PROMO + " where storeid = " + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + " and date = '" + currentDateSelected +
                        "' and userid = " + MainLibrary.gStrCurrentUserID , null);
            }

            if (cursExport != null) {
                if (cursExport.moveToFirst()) {
                    do {
                        String webid = cursExport.getString(cursExport.getColumnIndex("webid"));
                        String sapc = cursExport.getString(cursExport.getColumnIndex("sapc"));
                        String whpc = cursExport.getString(cursExport.getColumnIndex("whpc"));
                        String whcs = cursExport.getString(cursExport.getColumnIndex("whcs"));
                        String so = cursExport.getString(cursExport.getColumnIndex("so"));
                        String fso = cursExport.getString(cursExport.getColumnIndex("fso"));
                        String fsovalue = cursExport.getString(cursExport.getColumnIndex("fsovalue"));
                        String fsovalue2 = String.valueOf(Integer.parseInt(fso) * Double.parseDouble(fsovalue));
                        String barcode = cursExport.getString(cursExport.getColumnIndex("barcode"));
                        String ig = cursExport.getString(cursExport.getColumnIndex("ig"));
                        String conv = cursExport.getString(cursExport.getColumnIndex("conversion"));
                        String multi = cursExport.getString(cursExport.getColumnIndex("multi"));

                        if(!MainLibrary.isAssortmentMode & !MainLibrary.isPromoMode) {
                            int osatag = cursExport.getInt(cursExport.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_OSATAG));
                            int npitag = cursExport.getInt(cursExport.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_NPITAG));
                            int minstock = cursExport.getInt(cursExport.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_MINSTOCK));

                            myOutWriter.append(webid + ";" + sapc + ";" + whpc + ";" + whcs + ";" + so + ";" + fso + ";" + fsovalue2 + ";" + barcode + ";" + multi + ";" + ig + ";" + conv + ";" + osatag + ";" + npitag + ";" + minstock); //elapse_Time
                        }
                        else {
                            int minstock = cursExport.getInt(cursExport.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_MINSTOCK));
                            myOutWriter.append(webid + ";" + sapc + ";" + whpc + ";" + whcs + ";" + so + ";" + fso + ";" + fsovalue2 + ";" + barcode + ";" + multi + ";" + ig + ";" + conv + ";" + minstock); //elapse_Time
                        }

                        if(MainLibrary.isPromoMode) {
                            int minstock = cursExport.getInt(cursExport.getColumnIndex(SQLiteDB.COLUMN_PROMO_MINSTOCK));
                            myOutWriter.append(webid + ";" + sapc + ";" + whpc + ";" + whcs + ";" + so + ";" + fso + ";" + fsovalue2 + ";" + barcode + ";" + multi + ";" + ig + ";" + conv + ";" + minstock);

                        }


                        myOutWriter.append("\n");
                    }
                    while (cursExport.moveToNext());
                }

                cursExport.close();
                myOutWriter.close();
                fOut.close();
            }
        } catch (SQLiteException se) {
            Toast.makeText(this, "1" + se.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        } catch (FileNotFoundException e) {
            Toast.makeText(this,"2" + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "3" + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally { }

    }

    public class PostCsvFiles extends AsyncTask<Void, Void, Boolean> {
        String errMsg = "";
        String strResponse = "";
        Boolean bRepost = false;

        public PostCsvFiles(Boolean bRepost) {
            this.bRepost = bRepost;
        }

        @Override
        protected void onPreExecute() {
            String msg = "Posting transactions to web. Please wait.";
            if(bRepost) {
                msg = "Re-posting transactions to web. Please wait";
                bRepost = false;
            }
            progressDL = ProgressDialog.show(PostingActivity.this, "", msg, true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            MainLibrary.errorLog.appendLog("Posting transaction with user: " + MainLibrary.gStrCurrentUserName, TAG);

            String attachmentName = "data";
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary =  "*****";
            boolean bSucccess = false;

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024*1024;

            String attachmentFileName = fileCsv ;

            try {
                File fcsv = new File(dlPath, fileCsv);

                FileInputStream fileInputStream = new FileInputStream(fcsv); // text file to upload

                HttpURLConnection httpUrlConnection = null;
                if (MainLibrary.isAssortmentMode) {
                    URL url = new URL(MainLibrary.API_URL_ASSORTMENT_POSTING);
                    if (MainLibrary.BETA)
                        url = new URL(MainLibrary.API_URL_ASSORTMENT_POSTING_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                } else {
                    URL url = new URL(MainLibrary.API_URL_MKL_POSTING);
                    if (MainLibrary.BETA) url = new URL(MainLibrary.API_URL_MKL_POSTING_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                }

                if(MainLibrary.isPromoMode)
                {
                    URL url = new URL(MainLibrary.API_URL_PROMO_POSTING);
                    if (MainLibrary.BETA) url = new URL(MainLibrary.API_URL_PROMO_POSTING_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                }

                httpUrlConnection.setUseCaches(false);
                httpUrlConnection.setDoOutput(true);

                httpUrlConnection.setRequestMethod("POST");
                httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                httpUrlConnection.setRequestProperty(
                        "Content-Type", "multipart/form-data;boundary=" + boundary);

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

                responseStream.close();
                httpUrlConnection.disconnect();

                strResponse = stringBuilder.toString().trim();

                JSONObject jsonResponse = new JSONObject(strResponse);
                if(jsonResponse.getInt("status") == 0) {
                    strResponse = jsonResponse.getString("msg");
                    bSucccess = true;
                }
                else
                    errMsg = jsonResponse.getString("msg");
            }
            catch (IOException ex) {
                errMsg = "Slow or unstable internet connection. Please re-post";
                String exErr = ex.getMessage() != null ? ex.getMessage() : errMsg;
                MainLibrary.errorLog.appendLog(exErr, TAG);
                ex.printStackTrace();
            }
            catch (JSONException ex) {
                errMsg = "Data response error. Please try again.";
                String exErr = ex.getMessage() != null ? ex.getMessage() : errMsg;
                MainLibrary.errorLog.appendLog(exErr, TAG);
                ex.printStackTrace();
            }

            return bSucccess;

        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            progressDL.dismiss();
            if (!bResult) {
                mAlertDialog = new AlertDialog.Builder(PostingActivity.this).create();
                mAlertDialog.setTitle("Unsuccessful post.");
                mAlertDialog.setCancelable(false);
                mAlertDialog.setMessage("Unsuccessful posting of transaction. Re-posting of transaction is highly recommended.\nError: " + errMsg);
                mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Repost", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        new PostCsvFiles(true).execute();
                    }
                });
                mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                mAlertDialog.show();
                return;
            }

            if(strResponse.equals("file uploaded")) {
                Toast.makeText(PostingActivity.this, "Signature Image successfully posted.", Toast.LENGTH_LONG).show();
                new PostSignatureImage(false).execute();
            }
        }
    }

    public class PostSignatureImage extends AsyncTask<Void, Void, Boolean> {
        String errMsg = "";
        String strResponse = "";
        Boolean bRepost = false;

        public PostSignatureImage(Boolean bRepost) {
            this.bRepost = bRepost;
        }

        @Override
        protected void onPreExecute() {
            String msg = "Sending signature image. Please wait";
            if(bRepost) {
                msg = "Re-sending signature image. Please wait";
                bRepost = false;
            }
            progressDL = ProgressDialog.show(PostingActivity.this, "", msg, true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            boolean bSucccess = false;

            String attachmentName = "data";
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary =  "*****";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024*1024;

            String attachmentFileName = fileImage ;

            try {

                FileInputStream fileInputStream = new FileInputStream(new File(dlPath, fileImage)); // text file to upload

                HttpURLConnection httpUrlConnection = null;
                if (MainLibrary.isAssortmentMode & !MainLibrary.isPromoMode) {
                    URL url = new URL(MainLibrary.API_URL_ASSORTMENT_IMAGE);
                    if (MainLibrary.BETA) url = new URL(MainLibrary.API_URL_ASSORTMENT_IMAGE_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                } else {
                    URL url = new URL(MainLibrary.API_URL_MKL_IMAGE);
                    if (MainLibrary.BETA) url = new URL(MainLibrary.API_URL_MKL_IMAGE_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                }

                if (MainLibrary.isPromoMode) {
                    URL url = new URL(MainLibrary.API_URL_PROMO_IMAGE);
                    if (MainLibrary.BETA) url = new URL(MainLibrary.API_URL_PROMO_IMAGE_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                }

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

                responseStream.close();
                httpUrlConnection.disconnect();

                strResponse = stringBuilder.toString().trim();
                JSONObject jsonResponse = new JSONObject(strResponse);
                if(jsonResponse.getInt("status") == 0) {
                    strResponse = jsonResponse.getString("msg");

                    if(strResponse.equals("file uploaded")) {

                        if (MainLibrary.isAssortmentMode & !MainLibrary.isPromoMode) {
                            db.UpdateRecord(SQLiteDB.TABLE_TRANSACTION_ASSORT, "date = ? and storeid = ?", new String[]{MainLibrary.gStrCurrentDate, String.valueOf(MainLibrary.gSelectedStores.webStoreId)},
                                    new String[]{"lposted"}, new String[]{"1"});
                        }

                        else {
                            db.UpdateRecord(SQLiteDB.TABLE_TRANSACTION, "date = ? and storeid = ?", new String[]{MainLibrary.gStrCurrentDate, String.valueOf(MainLibrary.gSelectedStores.webStoreId)},
                                    new String[]{"lposted"}, new String[]{"1"});
                        }

                        if (MainLibrary.isPromoMode) {
                            db.UpdateRecord(SQLiteDB.TABLE_TRANSACTION_PROMO, "date = ? and storeid = ?", new String[]{MainLibrary.gStrCurrentDate, String.valueOf(MainLibrary.gSelectedStores.webStoreId)},
                                    new String[]{"lposted"}, new String[]{"1"});
                        }

                        File filenew = new File(dlPath, fileImage);
                        if (filenew.exists()) {
                            filenew.delete();
                        }
                        File filenew2 = new File(dlPath, fileCsv);
                        if (filenew2.exists()) {
//                            filenew2.delete();
                        }
                        bSucccess = true;
                    }

                }
            }
            catch (IOException ex) {
                errMsg = ex.getMessage() != null ? ex.getMessage() : "Slow or unstable internet connection. Please re-post transaction.";
                Log.e(TAG, errMsg);
                ex.printStackTrace();
            }
            catch (JSONException ex) {
                errMsg = ex.getMessage() != null ? ex.getMessage() : "Data response error.";
                Log.e(TAG, errMsg);
                ex.printStackTrace();
            }

            return bSucccess;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            progressDL.dismiss();
            if(!aBoolean) {
                mAlertDialog = new AlertDialog.Builder(PostingActivity.this).create();
                mAlertDialog.setTitle("Unsuccessful post.");
                mAlertDialog.setCancelable(false);
                mAlertDialog.setMessage("Unsuccessful posting of signature image. Re-posting of transaction is highly recommended.\n\nError: " + errMsg);
                mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Repost", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        new PostSignatureImage(true).execute();
                    }
                });
                mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                mAlertDialog.show();
                return;
            }

            try {
                SaveAllEditedIg();

                new AlertDialog.Builder(PostingActivity.this)
                        .setTitle("Post Successful")
                        .setMessage("All transaction successfully posted.")
                        .setCancelable(false)
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .create().show();
            }
            catch (Exception e) {
                String exErr = e.getMessage() != null ? e.getMessage() : "Edited IG not saved.";
                e.printStackTrace();
                MainLibrary.errorLog.appendLog(exErr, TAG);
                MainLibrary.messageBox(PostingActivity.this, "Edit IG", exErr);
            }
        }
    }

    public void SaveAllEditedIg() throws Exception {

        boolean isIgEdited = false;

        for (PCount pCount : MainLibrary.arrPcountEditedIg) {

            if(pCount.ig != pCount.oldIg) {
                //jasonmod
                /*db.ExecSQLWrite("UPDATE " + SQLiteDB.TABLE_PCOUNT + " SET " + SQLiteDB.COLUMN_PCOUNT_OLDIG + " = '" + pCount.oldIg
                        + "', " + SQLiteDB.COLUMN_PCOUNT_IG + " = '" + pCount.ig
                        + "' WHERE " + SQLiteDB.COLUMN_PCOUNT_BARCODE + " = '" + String.valueOf(pCount.barcode).trim() + "' AND "
                        + SQLiteDB.COLUMN_PCOUNT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId).trim() + "'");*/

                db.ExecSQLWrite("UPDATE " + SQLiteDB.TABLE_PCOUNT + " SET "
                        + " " + SQLiteDB.COLUMN_PCOUNT_IG + " = '" + pCount.ig
                        + "' WHERE " + SQLiteDB.COLUMN_PCOUNT_BARCODE + " = '" + String.valueOf(pCount.barcode).trim() + "' AND "
                        + SQLiteDB.COLUMN_PCOUNT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId).trim() + "'");


                db.ExecSQLWrite("UPDATE " + SQLiteDB.TABLE_TRANSACTION + " SET " + SQLiteDB.COLUMN_TRANSACTION_OLDIG + " = '" + pCount.ig
                        + "', " + SQLiteDB.COLUMN_TRANSACTION_IG + " = '" + pCount.ig
                        + "' WHERE " + SQLiteDB.COLUMN_TRANSACTION_BARCODE + " = '" + String.valueOf(pCount.barcode).trim() + "' AND "
                        + SQLiteDB.COLUMN_TRANSACTION_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId).trim() + "'");

                isIgEdited = true;
            }
        }

        if(isIgEdited) {
            Toast.makeText(PostingActivity.this, "New item's IG saved.", Toast.LENGTH_LONG).show();
        }

        MainLibrary.arrPcountEditedIg.clear();
    }

    @Override
    public void onBackPressed() {
        setResult(888);
        finish();
    }

    public Boolean uploadFile(String cfile) throws IOException {

        String attachmentName = "data";
        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary =  "*****";
        boolean bSucccess = false;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024*1024;

        String attachmentFileName = cfile ;

            FileInputStream fileInputStream = new FileInputStream(new File(dlPath, cfile)); // text file to upload

            HttpURLConnection httpUrlConnection = null;

            if(MainLibrary.isAssortmentMode & !MainLibrary.isPromoMode) {
                if (cfile.equals(mImageName)) {
                    URL url = new URL(MainLibrary.API_URL_ASSORTMENT_IMAGE);
                    if(MainLibrary.BETA) url = new URL(MainLibrary.API_URL_ASSORTMENT_IMAGE_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                } else {
                    URL url = new URL(MainLibrary.API_URL_ASSORTMENT_POSTING);
                    if(MainLibrary.BETA) url = new URL(MainLibrary.API_URL_ASSORTMENT_POSTING_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                }


            }
            else {
                if (cfile.equals(mImageName)) {
                    URL url = new URL(MainLibrary.API_URL_MKL_IMAGE);
                    if(MainLibrary.BETA) url = new URL(MainLibrary.API_URL_MKL_IMAGE_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                } else {
                    URL url = new URL(MainLibrary.API_URL_MKL_POSTING);
                    if(MainLibrary.BETA) url = new URL(MainLibrary.API_URL_MKL_POSTING_BETA);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                }
            }

        if(MainLibrary.isPromoMode) {
            if (cfile.equals(mImageName)) {
                URL url = new URL(MainLibrary.API_URL_PROMO_IMAGE);
                if(MainLibrary.BETA) url = new URL(MainLibrary.API_URL_PROMO_IMAGE_BETA);
                httpUrlConnection = (HttpURLConnection) url.openConnection();
            } else {
                URL url = new URL(MainLibrary.API_URL_PROMO_POSTING);
                if(MainLibrary.BETA) url = new URL(MainLibrary.API_URL_PROMO_POSTING_BETA);
                httpUrlConnection = (HttpURLConnection) url.openConnection();
            }

        }

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

            while (bytesRead > 0)
            {
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

            responseStream.close();
            httpUrlConnection.disconnect();

        String strResponse = stringBuilder.toString().trim();

        bSucccess = true;


        return bSucccess;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
