package com.chasetech.pcount.Settings;


import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chasetech.pcount.R;
import com.chasetech.pcount.database.DatabaseFile;
import com.chasetech.pcount.database.DatabaseFileAdapter;
import com.chasetech.pcount.database.SQLLib;
import com.chasetech.pcount.database.SQLPortable;
import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.library.MainLibrary;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the osaStatus of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            SettingsActivity.this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();


    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || PrinterPreferenceFragment.class.getName().equals(fragmentName)
                || DatabasePreferenceFragment.class.getName().equals(fragmentName)
                || AboutPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PrinterPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_printer);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("printer_list"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DatabasePreferenceFragment extends PreferenceFragment {

        SQLPortable sqlPortable;
        ProgressDialog progressDialog;
        SQLLib sqlMainDatabase;
        private String TAG;
        private ArrayList<DatabaseFile> arrDbFile;
        private String importedBackupFileID;
        private String importedBackupFileName;
        private DatabaseFileAdapter adapterDatabase;
        private String importedDBVersion;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_database);

            TAG = DatabasePreferenceFragment.this.getClass().getSimpleName();
            sqlPortable = new SQLPortable(getActivity());
            progressDialog = new ProgressDialog(getActivity());
            //sqlMainDatabase = new SQLLib(getActivity());

            arrDbFile = new ArrayList<>();

            Preference prefExport = (Preference) findPreference("db_export_key");
            Preference prefDeviceId = (Preference) findPreference("pref_device_id_key");
            Preference prefDBVersion = (Preference) findPreference("pref_db_version");
            Preference preferenceInitialize = findPreference("db_initialize_hash");

            prefDeviceId.setSummary(MainLibrary.gStrDeviceId);
            prefDBVersion.setSummary(String.valueOf(SQLiteDB.DATABASE_VERSION));

            prefExport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setCancelable(false)
                            .setTitle("Export Database")
                            .setMessage("Do you want to export main database to web server?")
                            .setPositiveButton("Export", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    new CheckInternet(1).execute();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                    return false;
                }
            });

            preferenceInitialize.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setCancelable(false)
                            .setTitle("Initialize Hash")
                            .setMessage("Are you sure you want to Initialize Hash? It will delete all saved records.")
                            .setPositiveButton("Initialize", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    sqlMainDatabase = new SQLLib(getActivity());

                                    String fuckChase = "Update " +SQLiteDB.TABLE_USER + " SET " + SQLiteDB.COLUMN_USER_HASH +  " = ' '";
                                    sqlMainDatabase.ExecSQLWrite(fuckChase);
                                    MainLibrary.savedHashKey = "";
                                    MainLibrary.InitializedHash = true;

                                    SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
                                    spEditor.putString(getString(R.string.pref_hash), " ");
                                    spEditor.putBoolean(getString(R.string.logged_pref_key), false);
                                    spEditor.apply();



                                    getActivity().finish();



                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                    return false;
                }
            });


            Preference prefImport = (Preference) findPreference("db_import_key");
            prefImport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    new AlertDialog.Builder(getActivity())
                            .setCancelable(false)
                            .setTitle("Import Database")
                            .setMessage("Do you check database backup lists for import?")
                            .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    new CheckInternet(2).execute();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();

                    return false;
                }
            });
        }

        void BeginTransferData() throws Exception {

            Cursor cursorPortableTransOsa = sqlPortable.GetDataCursor(SQLiteDB.TABLE_TRANSACTION);
            Cursor cursorPortableTransAssort = sqlPortable.GetDataCursor(SQLiteDB.TABLE_TRANSACTION_ASSORT);

            if(cursorPortableTransOsa.moveToFirst()) {
                sqlMainDatabase = new SQLLib(getActivity());

                // get osa transactions of main database
                Cursor cursorMainTransOsa = sqlMainDatabase.GetDataCursor(SQLiteDB.TABLE_TRANSACTION);
                if(cursorMainTransOsa.moveToFirst()) {
                    String[] aFields = new String[]{ };
                    SQLiteDB.aTransOsaColumns.remove(SQLiteDB.COLUMN_TRANSACTION_ID);
                    SQLiteDB.aTransOsaColumns.toArray(aFields);
                    String strQuery = MainLibrary.createInsertBulkQuery(SQLiteDB.TABLE_TRANSACTION, aFields);
                    sqlPortable.BeginBulkInsert(strQuery, aFields, cursorMainTransOsa);
                }
            }
        }



        class ExportDatabase extends AsyncTask<Void, Void, Boolean> {
            String errMsg = "";
            private String strResponse;

            @Override
            protected void onPreExecute() {
                progressDialog = ProgressDialog.show(getActivity(), "", "Exporting database to web server. Please wait.");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;

                String attachmentName = "data";
                String crlf = "\r\n";
                String twoHyphens = "--";
                String boundary =  "*****";

                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1024 * 1024;

                String attachmentFileName = sqlPortable.getExportedDBName();

                try {
                    String strUrl = MainLibrary.API_UPLOAD_BACKUP;
                    if(MainLibrary.BETA)
                        strUrl = MainLibrary.API_UPLOAD_BACKUP_BETA;

                    URL url = new URL(strUrl + "?device_id=" + MainLibrary.gStrDeviceId + "&username=" + MainLibrary.gStrCurrentUserName + "&database_version=" + String.valueOf(SQLiteDB.DATABASE_VERSION));

                    File dbFileBackup = new File(MainLibrary.dbFolder, attachmentFileName);

                    FileInputStream fileInputStream = new FileInputStream(dbFileBackup); // text file to upload

                    HttpURLConnection httpUrlConnection = null;
                    httpUrlConnection = (HttpURLConnection) url.openConnection();

                    httpUrlConnection.setUseCaches(false);
                    httpUrlConnection.setDoOutput(true);

                    httpUrlConnection.setRequestMethod("POST");
                    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                    httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                    httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

                    request.writeBytes(twoHyphens + boundary + crlf);
                    request.writeBytes("Content-Disposition: form-data; name=\"" + attachmentName + "\";filename=\"" + attachmentFileName + "\"" + crlf);
                    request.writeBytes(crlf);

                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // Read file
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    int i = 0;

                    while (bytesRead > 0) {
                        request.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        i++;
                    }

                    request.writeBytes(crlf);
                    request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
                    request.flush();
                    request.close();

                    int status = httpUrlConnection.getResponseCode();
                    if(status >= HttpStatus.SC_BAD_REQUEST) {
                        //inputStream = httpUrlConnection.getErrorStream();
                        errMsg = "ERROR " + String.valueOf(status) + "\nPlease try again.";
                        return false;
                    }

                    InputStream responseStream = new BufferedInputStream(httpUrlConnection.getInputStream());

                    BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

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
                        dbFileBackup.delete();
                        result = true;
                    }
                    else
                        errMsg = jsonResponse.getString("msg");
                }
                catch (FileNotFoundException ex) {
                    errMsg = "File not found error exception. Please try again.";
                    String exErr = ex.getMessage() != null ? ex.getMessage() : errMsg;
                    MainLibrary.errorLog.appendLog(exErr, TAG);
                    ex.printStackTrace();
                }
                catch (IOException ex) {
                    errMsg = "Slow or unstable internet connection. Please try again.";
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

                return result;
            }

            @Override
            protected void onPostExecute(Boolean res) {
                progressDialog.dismiss();
                if(!res) {
                    MainLibrary.messageBox(getActivity(), "Export error", errMsg);
                    return;
                }


                MainLibrary.messageBox(getActivity(), strResponse, "Database exported successfully.");
            }
        }

        class CheckBackupList extends AsyncTask<Void, Void, Boolean> {
            private String strResponse;

            @Override
            protected void onPreExecute() {
                progressDialog = ProgressDialog.show(getActivity(), "", "Checking database backup in web server. Please wait.");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;

                try {

                    arrDbFile.clear();

                    String urlBackupList = MainLibrary.API_CHECK_BACKUP_LIST;
                    if(MainLibrary.BETA)
                        urlBackupList = MainLibrary.API_CHECK_BACKUP_LIST_BETA;

                    String urlCheckList = urlBackupList + "/" + MainLibrary.gStrCurrentUserName;

                    URL url = new URL(urlCheckList);
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
                        strResponse = "Web server is down. Try again later.";
                        return false;
                    }

                    JSONObject jsonObject = new JSONObject(response);
                    if(!jsonObject.isNull("msg")) {
                        String msg = jsonObject.getString("msg");
                        strResponse = msg.trim();

                        JSONArray jFileList = jsonObject.getJSONArray("files");
                        if(jFileList.length() > 0) {

                            for(int i = 0; i < jFileList.length(); i++) {
                                JSONObject jFileObject = jFileList.getJSONObject(i);

                                if(jFileObject != null) {
                                    int dbID = jFileObject.getInt("id");
                                    int dbBackupID = jFileObject.getInt("device_backup_id");
                                    String strFileName = jFileObject.getString("filename");
                                    String strCreatedAt = jFileObject.getString("created_at");
                                    String strUpdatedAt = jFileObject.getString("updated_at");
                                    String strDBVersion = jFileObject.getString("database_version");
                                    arrDbFile.add(new DatabaseFile(dbID, dbBackupID, strFileName, strCreatedAt, strUpdatedAt, strDBVersion));

                                    result = true;
                                }
                                else {
                                    result = false;
                                    strResponse = "Can't load all database backups.";
                                    break;
                                }
                            }
                        }
                        else strResponse = "No backup database file found.";
                    }
                    else strResponse = "Web server is down. Try again later.";
                }
                catch (FileNotFoundException ex) {
                    strResponse = "Database backup not found for this device. ";
                    String exErr = ex.getMessage() != null ? ex.getMessage() : strResponse;
                    MainLibrary.errorLog.appendLog(exErr, TAG);
                }
                catch (IOException ex) {
                    strResponse = "Slow or unstable internet connection. Please try again.";
                    String exErr = ex.getMessage() != null ? ex.getMessage() : strResponse;
                    MainLibrary.errorLog.appendLog(exErr, TAG);
                }
                catch (JSONException ex) {
                    strResponse = "Data response error. Please try again.";
                    String exErr = ex.getMessage() != null ? ex.getMessage() : strResponse;
                    MainLibrary.errorLog.appendLog(exErr, TAG);
                }

                return result;
            }

            @Override
            protected void onPostExecute(Boolean bResult) {
                progressDialog.dismiss();
                if(!bResult) {
                    MainLibrary.messageBox(getActivity(), "Import Database", strResponse);
                    return;
                }

                Toast.makeText(getActivity(), strResponse, Toast.LENGTH_LONG).show();

                final Dialog filterDialog = new Dialog(getActivity());
                filterDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                filterDialog.setCancelable(true);
                filterDialog.setContentView(R.layout.layout_dialog_filter);

                ListView lvwDatabase = (ListView) filterDialog.findViewById(R.id.lvwDatabase);
                TextView tvwFilterTitle = (TextView) filterDialog.findViewById(R.id.tvwTitle);
                Button btnBack = (Button) filterDialog.findViewById(R.id.btnDatabaseBack);
                tvwFilterTitle.setText("Select database");

                adapterDatabase = new DatabaseFileAdapter(getActivity(), arrDbFile);
                lvwDatabase.setAdapter(adapterDatabase);
                adapterDatabase.notifyDataSetChanged();

                btnBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        filterDialog.dismiss();
                    }
                });

                lvwDatabase.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        final int index = position;

                        new AlertDialog.Builder(getActivity())
                                .setCancelable(false)
                                .setTitle("Import Database")
                                .setMessage("Are you sure to import this database? This will overwrite your existing data and replaces imported database.")
                                .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        filterDialog.dismiss();
                                        importedBackupFileID = String.valueOf(arrDbFile.get(index).ID);
                                        importedBackupFileName = arrDbFile.get(index).fileName;
                                        importedDBVersion = arrDbFile.get(index).databaseVersion;
                                        new CheckInternet(3).execute();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create().show();
                    }
                });

                filterDialog.show();
            }
        }

        private class ImportDatabase extends AsyncTask<Void, String, Boolean> {

            private String errMsg;

            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setTitle("");
                progressDialog.setMessage("Importing database backup. Please wait.");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected void onProgressUpdate(String... values) {
                progressDialog.setMessage("Downloading " + values[1] + ". " + String.format(Locale.getDefault(), "%.2f", Double.valueOf(values[0])) + "Kb downloaded.");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;

                try {

                    String urlDownloadBackup = MainLibrary.API_DOWNLOAD_BACKUP;
                    if(MainLibrary.BETA)
                        urlDownloadBackup = MainLibrary.API_DOWNLOAD_BACKUP_BETA;

                    urlDownloadBackup = urlDownloadBackup + "/" + importedBackupFileID;

                    URL url = new URL(urlDownloadBackup);
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                    httpConn.setReadTimeout(10000 /* milliseconds */);
                    httpConn.setConnectTimeout(15000 /* milliseconds */);
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
                            fileName = importedBackupFileName;
                        }

                        InputStream inputStream = null;
                        inputStream = httpConn.getInputStream();

                        File fSavePath = new File(MainLibrary.dbFolder, fileName);
                        if(fSavePath.exists()) fSavePath.delete();
                        String saveFilePath = fSavePath.getPath();

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

                    } else {
                        errMsg = "Error " + String.valueOf(responseCode) + ". Please check web server.";
                    }
                }
                catch (FileNotFoundException ex) {
                    errMsg = "File not found error exception. Please try again.";
                    String exErr = ex.getMessage() != null ? ex.getMessage() : errMsg;
                    MainLibrary.errorLog.appendLog(exErr, TAG);
                    ex.printStackTrace();
                }
                catch (IOException ex) {
                    errMsg = "Slow or unstable internet connection. Please try again.";
                    String exErr = ex.getMessage() != null ? ex.getMessage() : errMsg;
                    MainLibrary.errorLog.appendLog(exErr, TAG);
                    ex.printStackTrace();
                }

                return result;
            }

            @Override
            protected void onPostExecute(Boolean bResult) {
                progressDialog.dismiss();
                if(!bResult) {
                    MainLibrary.messageBox(getActivity(), "Error importing", errMsg);
                    return;
                }

                if(sqlPortable.copyImportedDBToMain(importedBackupFileName)) {

                    new File(MainLibrary.dbFolder, importedBackupFileName).delete();

                    try {
                        int backupDBVersion = Integer.valueOf(importedDBVersion);
                        if (SQLiteDB.DATABASE_VERSION < backupDBVersion) {
                            SQLiteDB sqLiteDB = new SQLiteDB(getActivity());
                            SQLiteDatabase database = sqLiteDB.getReadableDatabase();
                            sqLiteDB.onUpgrade(database, SQLiteDB.DATABASE_VERSION, backupDBVersion);

                            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(getString(R.string.pcount_sharedprefKey), Context.MODE_PRIVATE);
                            sharedPreferences.edit().putInt(getString(R.string.pref_db_backup_version), backupDBVersion).apply();

                            SQLiteDB.DATABASE_BACKUP_VERSION = backupDBVersion;

                            MainLibrary.errorLog.appendLog("Database is updated from " + String.valueOf(SQLiteDB.DATABASE_VERSION) + " to " + importedDBVersion, TAG);
                        }
                    }
                    catch (Exception ex) { }

                    new AlertDialog.Builder(getActivity())
                            .setCancelable(false)
                            .setTitle("Successful import")
                            .setMessage("Successfully attached imported database.\n\nRestarting the application is recommended. Do you want to restart the Pcount application?")
                            .setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();

                                    Intent intentRestart = getActivity().getBaseContext().getPackageManager().getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
                                    intentRestart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intentRestart);
                                }
                            })
                            .setNegativeButton("Continue", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                }
                else {
                    errMsg = "Database file not found or corrupted. Please try again.";
                }
            }
        }

        private class CheckInternet extends AsyncTask<Void, Void, Boolean> {
            String errmsg = "";
            private int nMode;


            CheckInternet(int nMode) {
                this.nMode = nMode;
            }

            @Override
            protected void onPreExecute() {
                progressDialog = ProgressDialog.show(getActivity(), "", "Checking internet connection.");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;

                ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if(activeNetwork != null) {
                    if(activeNetwork.isFailover()) errmsg = "Internet connection fail over.";
                    result = activeNetwork.isAvailable() || activeNetwork.isConnectedOrConnecting();
                }
                else errmsg = "No internet connection.";

                return result;
            }

            @Override
            protected void onPostExecute(Boolean bResult) {
                progressDialog.dismiss();
                if(!bResult) {
                    Toast.makeText(getActivity(), errmsg, Toast.LENGTH_SHORT).show();
                    return;
                }

                switch (nMode) {
                    case 1: // export database
                        if(sqlPortable.copyMainDatabaseFileToSd()) {
                            new ExportDatabase().execute();
                        }
                        else
                            Toast.makeText(getActivity(), "Can't backup main database. Please try again.", Toast.LENGTH_SHORT).show();
                        break;
                    case 2: // check backup lists
                        new CheckBackupList().execute();
                        break;
                    case 3: // import database
                        new ImportDatabase().execute();
                    default:
                        break;
                }

            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_abouts);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            Preference prefVersionName = (Preference) findPreference("version_name_key");
            Preference prefVersionCode = (Preference) findPreference("version_code_key");
            Preference prefChangelog = (Preference) findPreference("changelog_key");

            prefVersionName.setSummary(MainLibrary.versionName);
            prefVersionCode.setSummary(String.valueOf(MainLibrary.versionCode));

            prefChangelog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("What's new?")
                            .setMessage(MainLibrary.CHANGE_LOGS)
                            .setCancelable(false)
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                    return false;
                }
            });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}
