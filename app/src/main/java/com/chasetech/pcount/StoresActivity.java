package com.chasetech.pcount;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.chasetech.pcount.Assortment.AssortmentActivity;
import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.ErrorLog.ErrorLog;
import com.chasetech.pcount.ErrorLog.FixBugTask;
import com.chasetech.pcount.MKL.PCountActivity;
import com.chasetech.pcount.Promo.PromoActivity;
import com.chasetech.pcount.Settings.SettingsActivity;
import com.chasetech.pcount.adapter.BranchListViewAdapter;
import com.chasetech.pcount.database.SQLLib;
import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.library.HomeWatcher;
import com.chasetech.pcount.library.Stores;
import com.chasetech.pcount.library.MainLibrary;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ULTRABOOK on 10/27/2015.
 */
public class StoresActivity extends AppCompatActivity {

    private ProgressDialog pDL;
    private ArrayList<Stores> myArrayListStores = new ArrayList<>();
    private ArrayList<Stores> arrStoreLoader = new ArrayList<>();
    private BranchListViewAdapter mBranchListViewAdapter;
    private String mStrCurrentDate = "";
    private String strSelectedMonth = "";
    private String TAG = "";
    private SQLLib sqlLibrary;
    private Calendar mCurrentDate;
    private EditText editTextDate;
    private AlertDialog mAlertDialog;
    private boolean isLoggedIn;
    //private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(this, MainLibrary.errlogFile));
        TAG = StoresActivity.this.getLocalClassName();
        MainLibrary.errlogFile = MainLibrary.gStrDeviceId + ".txt";
        MainLibrary.errorLog = new ErrorLog(MainLibrary.errlogFile, this);

        MainLibrary.LoadAllFolders(this);
        sqlLibrary = new SQLLib(this);

        HomeWatcher mHomeWatcher = new HomeWatcher(this);
        mHomeWatcher.setOnHomePressedListener(new HomeWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                NavUtils.navigateUpFromSameTask(StoresActivity.this);
            }
            @Override
            public void onHomeLongPressed() { }
        });
        mHomeWatcher.startWatch();

        MainLibrary.selContext = this;
        SetSharedPreferencesSettings();

        if(isLoggedIn) {
            MainLibrary.gStrCurrentUserID = MainLibrary.spSettings.getInt(getString(R.string.pref_userid), MainLibrary.gStrCurrentUserID);
            MainLibrary.gStrCurrentUserName = MainLibrary.spSettings.getString(getString(R.string.pref_username), MainLibrary.gStrCurrentUserName);
            Cursor cursUser = sqlLibrary.GetDataCursor(SQLiteDB.TABLE_USER);
            if(cursUser.moveToFirst()) {
                MainLibrary.savedHashKey = cursUser.getString(cursUser.getColumnIndex(SQLiteDB.COLUMN_USER_HASH));
            }
            cursUser.close();
        }
        else {
            Intent intent = new Intent(StoresActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        try {
            String activityTitle = "USER: " + MainLibrary.gStrCurrentUserName;
            getSupportActionBar().setTitle(activityTitle);
        }
        catch (Exception ex) {
            String errMsg = ex.getMessage() != null ? ex.getMessage() : "Action bar title is null";
            MainLibrary.errorLog.appendLog(errMsg, TAG);
        }

        MainLibrary.errorLog.appendLog("Start pcount transaction with user: " + MainLibrary.gStrCurrentUserName, TAG);
        sqlLibrary.open();

        mCurrentDate = Calendar.getInstance();
        mStrCurrentDate = MainLibrary.dateFormatter.format(mCurrentDate.getTime());
        strSelectedMonth = MainLibrary.monthFormatter.format(mCurrentDate.getTime());
        editTextDate = (EditText) findViewById(R.id.editTextDate);
        editTextDate.setText(mStrCurrentDate);

        editTextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(StoresActivity.this, dateSetListener, mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH),
                        mCurrentDate.get(Calendar.DAY_OF_MONTH));

                Calendar mCalendar = Calendar.getInstance();
                mCalendar.setTime(new Date());

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { // below latest os
                    datePickerDialog.getDatePicker().getCalendarView().setFirstDayOfWeek(Calendar.MONDAY);
                    datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
                }
                else { // lollipop os and above
                    mCalendar.add(Calendar.DAY_OF_MONTH, 1);
                    datePickerDialog.getDatePicker().setFirstDayOfWeek(Calendar.MONDAY);
                    datePickerDialog.getDatePicker().setMaxDate(mCalendar.getTime().getTime());
                }

                datePickerDialog.show();
            }
        });

        final ListView listView = (ListView) findViewById(R.id.listViewBranch);
        mBranchListViewAdapter = new BranchListViewAdapter(this, myArrayListStores);
        listView.setAdapter(mBranchListViewAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (!mBluetoothAdapter.isEnabled()) {
                    Toast.makeText(StoresActivity.this, "Please turn on the bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                    return;
                }

                final int nPosition = position;

                final AlertDialog.Builder mModuleDialog = new AlertDialog.Builder(StoresActivity.this);
                mModuleDialog.setTitle("Select a module");
                mModuleDialog.setCancelable(true);

                mModuleDialog.setItems(MainLibrary.aModules, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        switch (which) {
                            case 0: // NORMAL
                                Toast.makeText(StoresActivity.this, "TEST", Toast.LENGTH_SHORT).show();

                                MainLibrary.isAssortmentMode = false;
                                MainLibrary.isPromoMode = false;
                                Stores loc = myArrayListStores.get(nPosition);
                                Intent intent = new Intent(StoresActivity.this, PCountActivity.class);

                                MainLibrary.gStrCurrentDate = mStrCurrentDate;
                                MainLibrary.gSelectedStores = loc;

                                if(loc.storeName.toUpperCase().contains("711")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.SEVEN_ELEVEN;
                                }
                                else if(loc.storeName.toUpperCase().contains("MINISTOP")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.MINISTOP;
                                }
                                else if(loc.storeName.toUpperCase().contains("MERCURY") || loc.storeName.toUpperCase().contains("MDC")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.MERCURY_DRUG;
                                }
                                else if(loc.storeName.toUpperCase().contains("FAMILY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.FAMILY_MART;
                                }
                                else if(loc.storeName.toUpperCase().contains("LAWSON")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.LAWSON;
                                }
                                else if(loc.storeName.toUpperCase().contains("ALFAMART")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ALFAMART;
                                }
                                else if(loc.channelArea.toUpperCase().contains("SOUTH STAR DRUG")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.SOUTH_STAR_DRUG;
                                }
                                else if(loc.channelArea.toUpperCase().contains("ST. JOSEPH DRUG")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ST_JOSEPH_DRUG;
                                }
                                else if(loc.channelArea.toUpperCase().contains("ROSE PHARMACY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ROSE_PHARMACY;
                                }
                                else if(loc.channelArea.toUpperCase().contains("360 PHARMACY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.THREESIXTY_PHARMACY;
                                }
                                else MainLibrary.eStore = MainLibrary.STORE_TYPES.DEFAULT;

                                startActivityForResult(intent, 999);
                                break;

                            case 1: // ASSORTMENT
                                MainLibrary.isAssortmentMode = true;
                                MainLibrary.isPromoMode = false;
                                Stores stores = myArrayListStores.get(nPosition);
                                Intent assortintent = new Intent(StoresActivity.this, AssortmentActivity.class);

                                MainLibrary.gStrCurrentDate = mStrCurrentDate;
                                MainLibrary.selectedMonth = strSelectedMonth;
                                MainLibrary.gSelectedStores = stores;

                                MainLibrary.eStore = MainLibrary.STORE_TYPES.DEFAULT;

                                if(stores.storeName.toUpperCase().contains("711")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.SEVEN_ELEVEN;
                                }
                                if(stores.storeName.toUpperCase().contains("MINISTOP")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.MINISTOP;
                                }
                                if(stores.storeName.toUpperCase().contains("MERCURY")  || stores.storeName.toUpperCase().contains("MDC")){
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.MERCURY_DRUG;
                                }
                                if(stores.storeName.toUpperCase().contains("FAMILY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.FAMILY_MART;
                                }
                                if(stores.storeName.toUpperCase().contains("LAWSON")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.LAWSON;
                                }
                                if(stores.storeName.toUpperCase().contains("ALFAMART")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ALFAMART;
                                }
                                if(stores.channelArea.toUpperCase().contains("SOUTH STAR DRUG")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.SOUTH_STAR_DRUG;
                                }
                                if(stores.channelArea.toUpperCase().contains("ST. JOSEPH DRUG")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ST_JOSEPH_DRUG;
                                }
                                if(stores.channelArea.toUpperCase().contains("ROSE PHARMACY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ROSE_PHARMACY;
                                }
                                if(stores.channelArea.toUpperCase().contains("360 PHARMACY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.THREESIXTY_PHARMACY;
                                }

                                startActivityForResult(assortintent, 999);
                                break;


                            case 2 : //PROMO
                                MainLibrary.isPromoMode = true;
                                MainLibrary.isAssortmentMode = false;

                                Stores promostore = myArrayListStores.get(nPosition);
                                Intent intentpromo = new Intent(StoresActivity.this, PromoActivity.class);

                                MainLibrary.gStrCurrentDate = mStrCurrentDate;
                                MainLibrary.selectedMonth = strSelectedMonth;
                                MainLibrary.gSelectedStores = promostore;

                                MainLibrary.eStore = MainLibrary.STORE_TYPES.DEFAULT;

                                if(promostore.storeName.toUpperCase().contains("711")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.SEVEN_ELEVEN;
                                }
                                if(promostore.storeName.toUpperCase().contains("MINISTOP")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.MINISTOP;
                                }
                                if(promostore.storeName.toUpperCase().contains("MERCURY")  || promostore.storeName.toUpperCase().contains("MDC")){
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.MERCURY_DRUG;
                                }
                                if(promostore.storeName.toUpperCase().contains("FAMILY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.FAMILY_MART;
                                }
                                if(promostore.storeName.toUpperCase().contains("LAWSON")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.LAWSON;
                                }
                                if(promostore.storeName.toUpperCase().contains("ALFAMART")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ALFAMART;
                                }
                                if(promostore.channelArea.toUpperCase().contains("SOUTH STAR DRUG")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.SOUTH_STAR_DRUG;
                                }
                                if(promostore.channelArea.toUpperCase().contains("ST. JOSEPH DRUG")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ST_JOSEPH_DRUG;
                                }
                                if(promostore.channelArea.toUpperCase().contains("ROSE PHARMACY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.ROSE_PHARMACY;
                                }
                                if(promostore.channelArea.toUpperCase().contains("360 PHARMACY")) {
                                    MainLibrary.eStore = MainLibrary.STORE_TYPES.THREESIXTY_PHARMACY;
                                }

                                startActivityForResult(intentpromo, 999);
                                break;

                            default:
                                MainLibrary.isAssortmentMode = false;
                                break;
                        }


                    }
                });

                if(!CheckAssortmentOnSelectedMonth(myArrayListStores.get(nPosition))) {
                    mAlertDialog = new AlertDialog.Builder(StoresActivity.this).create();
                    mAlertDialog.setTitle("Assortment");
                    mAlertDialog.setMessage("You didn't transact for assortment.\nDo you want to perform assortment transaction?");
                    mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mModuleDialog.show();
                        }
                    });
                    mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            MainLibrary.isAssortmentMode = true;
                            Stores stores = myArrayListStores.get(nPosition);
                            Intent assortintent = new Intent(StoresActivity.this, AssortmentActivity.class);

                            MainLibrary.gStrCurrentDate = mStrCurrentDate;
                            MainLibrary.gSelectedStores = stores;
                            MainLibrary.selectedMonth = strSelectedMonth;

                            if(stores.storeName.contains("711")){
                                MainLibrary.eStore = MainLibrary.STORE_TYPES.SEVEN_ELEVEN;
                            }
                            if(stores.storeName.contains("MINISTOP")){
                                MainLibrary.eStore = MainLibrary.STORE_TYPES.MINISTOP;
                            }
                            if(stores.storeName.contains("MERCURY")){
                                MainLibrary.eStore = MainLibrary.STORE_TYPES.MERCURY_DRUG;
                            }
                            if(stores.storeName.contains("FAMILY")){
                                MainLibrary.eStore = MainLibrary.STORE_TYPES.FAMILY_MART;
                            }

                            startActivityForResult(assortintent, 999);
                        }
                    });
                    mAlertDialog.show();
                    return;
                }
                mModuleDialog.show();
            }
        });

        GetLocationArrayList();

        if(MainLibrary.FIX_IG_BUG && SQLiteDB.DATABASE_VERSION == 19) {
            new FixBugTask(StoresActivity.this).execute();
        }
    }

    private void SetSharedPreferencesSettings() {
        MainLibrary.spSettings = getSharedPreferences(getString(R.string.pcount_sharedprefKey), Context.MODE_PRIVATE);
        isLoggedIn = MainLibrary.spSettings.getBoolean(getString(R.string.logged_pref_key), false);
        MainLibrary.allowIG = MainLibrary.spSettings.getBoolean(getString(R.string.pref_allowIG), true);
        MainLibrary.MKL_allItemsReqForPosting = MainLibrary.spSettings.getBoolean(getString(R.string.pref_mkl_allitemsreqforposting), true);
        MainLibrary.MKL_allItemsReqForPrinting = MainLibrary.spSettings.getBoolean(getString(R.string.pref_mkl_allitemsreqforprinting), true);
        MainLibrary.ASSORT_allItemsReqForPosting = MainLibrary.spSettings.getBoolean(getString(R.string.pref_assort_allitemsreqforposting), true);
        MainLibrary.ASSORT_allItemsReqForPrinting = MainLibrary.spSettings.getBoolean(getString(R.string.pref_assort_allitemsreqforprinting), true);
        MainLibrary.CLEAR_USER_PASSWORD = MainLibrary.spSettings.getString(getString(R.string.pref_userclearpassword), MainLibrary.CLEAR_USER_PASSWORD);
        MainLibrary.masterfileDateRealese = MainLibrary.spSettings.getString(getString(R.string.pref_masterfile_date), MainLibrary.masterfileDateRealese);
        MainLibrary.MKL_validateDatePosting = MainLibrary.spSettings.getBoolean(getString(R.string.pref_mkl_validateDatePosting), MainLibrary.MKL_validateDatePosting);
        MainLibrary.ASSORT_validateDatePosting = MainLibrary.spSettings.getBoolean(getString(R.string.pref_assort_validateDatePosting), MainLibrary.ASSORT_validateDatePosting);
        MainLibrary.FIX_IG_BUG = MainLibrary.spSettings.getBoolean(getString(R.string.pref_fix_ig), MainLibrary.FIX_IG_BUG);

        SetMasterFileDate();
    }

    private void SetMasterFileDate() {
        Typeface menuFontIcon = Typeface.createFromAsset(getAssets(), MainLibrary.typefacename);
        TextView tvwMfDate = (TextView) findViewById(R.id.tvwMasterfileDate);
        tvwMfDate.setTypeface(menuFontIcon);
        String strVal = "\uf00c" + " Masterfile updated: " + MainLibrary.masterfileDateRealese;
        tvwMfDate.setText(strVal);
        if(MainLibrary.masterfileDateRealese.equals("")) {
            tvwMfDate.setVisibility(View.GONE);
        }
    }


    //CHECK IF ASSORTMENT IS MADE ON CURRENT MONTH
    private boolean CheckAssortmentOnSelectedMonth(Stores seletedLoc) {
        boolean bReturn = false;

        Cursor cursCheck = sqlLibrary.GetDataCursor(SQLiteDB.TABLE_TRANSACTION_ASSORT, SQLiteDB.COLUMN_TRANSACTION_ASSORT_MONTH + " = '" + strSelectedMonth + "' AND " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_LPOSTED + " = '1' AND " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID + " = '" + seletedLoc.webStoreId + "'");
        cursCheck.moveToFirst();
        if(cursCheck.getCount() > 0) {
            bReturn = true;
        }
        cursCheck.close();

        return bReturn;
    }

    private DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            mCurrentDate = Calendar.getInstance(Locale.GERMANY);
            mCurrentDate.set(year, monthOfYear, dayOfMonth);
            mStrCurrentDate = MainLibrary.dateFormatter.format(mCurrentDate.getTime());
            strSelectedMonth = MainLibrary.monthFormatter.format(mCurrentDate.getTime());
            editTextDate.setText(mStrCurrentDate);

            GetLocationArrayList();
        }

    };

    private class TaskProcessLocationData extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            sqlLibrary.TruncateTable(SQLiteDB.TABLE_STORE);

            File sdcard = Environment.getExternalStorageDirectory();
            File file = new File(sdcard,"stores.txt");
            try {

                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                Boolean bStart = false;

                while ((line = br.readLine()) != null) {
                    String[] itemRefs = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                    if (!bStart) {
                        bStart = true;
                        continue;
                    }

                    if (itemRefs[0].startsWith("\"")) {
                        continue;
                    }

                    int branchid = Integer.parseInt(itemRefs[0].trim());
                    String storecode = itemRefs[1].trim();
                    String branchdesc = itemRefs[2].replace("\"", "");
                    int channelID = Integer.parseInt(itemRefs[3].trim());
                    String channelDesc = itemRefs[4].replace("\"", "");
                    String channelArea = itemRefs[5].replace("\"", "");

                    sqlLibrary.insertToBranch(branchid, storecode, branchdesc, channelID, channelDesc, channelArea );
                }

                br.close();

            }
            catch (IOException e) {
                Toast.makeText(StoresActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pDL.dismiss();
            GetLocationArrayList();
        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(StoresActivity.this, "", "Updating Stores Masterfile. Please wait...", true);
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }

    private void GetLocationArrayList() {

        arrStoreLoader.clear();

        Cursor cursorStore = sqlLibrary.GetDataCursor(SQLiteDB.TABLE_STORE);
        cursorStore.moveToFirst();

        int osaStatus;
        int assortStatus;
        int promoStatus;

        while (!cursorStore.isAfterLast()) {

            int ID = cursorStore.getInt(cursorStore.getColumnIndex(SQLiteDB.COLUMN_STORE_ID));
            int webStoreId = cursorStore.getInt(cursorStore.getColumnIndex(SQLiteDB.COLUMN_STORE_BID));
            String storeCode = cursorStore.getString(cursorStore.getColumnIndex(SQLiteDB.COLUMN_STORE_STORECODE)).replace("\"", "");
            String storeName = cursorStore.getString(cursorStore.getColumnIndex(SQLiteDB.COLUMN_STORE_DESC)).replace("\"", "");
            int multiple = cursorStore.getInt(cursorStore.getColumnIndex(SQLiteDB.COLUMN_STORE_MULTIPLE));
            int channelId = cursorStore.getInt(cursorStore.getColumnIndex(SQLiteDB.COLUMN_STORE_CHANNELID));
            String channelDesc = cursorStore.getString(cursorStore.getColumnIndex(SQLiteDB.COLUMN_STORE_CHANNELDESC)).replace("\"", "");
            String channelArea = cursorStore.getString(cursorStore.getColumnIndex(SQLiteDB.COLUMN_STORE_CHANNELAREA)).replace("\"", "");


            String osaQuery = "select " + SQLiteDB.COLUMN_TRANSACTION_LPOSTED + "," + SQLiteDB.COLUMN_TRANSACTION_UPDATED + " from " + SQLiteDB.TABLE_TRANSACTION + " where " + SQLiteDB.COLUMN_TRANSACTION_STOREID + " = '" + webStoreId
                    + "' and date = '" + mStrCurrentDate + "' AND " + SQLiteDB.COLUMN_TRANSACTION_UPDATED + " = '1' limit 1";


            String assortQuery = "select " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_LPOSTED + "," + SQLiteDB.COLUMN_TRANSACTION_ASSORT_UPDATED + " from " + SQLiteDB.TABLE_TRANSACTION_ASSORT + " where " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID + " = '" + webStoreId
                    + "' and date = '" + mStrCurrentDate + "' AND " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_UPDATED + " = '1' limit 1";

            String promoQuery = "select " + SQLiteDB.COLUMN_TRANSACTION_PROMO_LPOSTED + "," + SQLiteDB.COLUMN_TRANSACTION_PROMO_UPDATED + " from " + SQLiteDB.TABLE_TRANSACTION_PROMO+ " where " + SQLiteDB.COLUMN_TRANSACTION_PROMO_STOREID + " = '" + webStoreId
                    + "' and date = '" + mStrCurrentDate + "' AND " + SQLiteDB.COLUMN_TRANSACTION_PROMO_UPDATED + " = '1' limit 1";

            Cursor cursorTrans = sqlLibrary.queryData(osaQuery);
            cursorTrans.moveToFirst();

            Cursor cursorAssortTrans = sqlLibrary.queryData(assortQuery);
            cursorAssortTrans.moveToFirst();

            Cursor cursorPromoTrans = sqlLibrary.queryData(promoQuery);
            cursorPromoTrans.moveToFirst();

            osaStatus = 0;
            assortStatus = 0;
            promoStatus = 0;

            // OSA
            if (cursorTrans.getCount() != 0) {
                cursorTrans.moveToFirst();
                osaStatus = 1;
                if(cursorTrans.getCount() > 0) {
                    if (cursorTrans.getInt(cursorTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_LPOSTED)) != 0) {
                        osaStatus = 2;
                    }
                }
            }

            // ASSORTMENT
            if (cursorAssortTrans.getCount() != 0) {
                cursorAssortTrans.moveToFirst();
                assortStatus = 1;
                if(cursorAssortTrans.getCount() > 0) {
                    if (cursorAssortTrans.getInt(cursorAssortTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_LPOSTED)) != 0) {
                        assortStatus = 2;
                    }
                }
            }

            // PROMO
            if (cursorPromoTrans.getCount() != 0) {
                cursorPromoTrans.moveToFirst();
                promoStatus = 1;
                if(cursorPromoTrans.getCount() > 0) {
                    if (cursorPromoTrans.getInt(cursorPromoTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_LPOSTED)) != 0) {
                        promoStatus = 2;
                    }
                }
            }

            arrStoreLoader.add(new Stores(
                    ID,
                    webStoreId,
                    storeCode,
                    storeName,
                    multiple,
                    channelId,
                    channelDesc,
                    channelArea,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    osaStatus,
                    assortStatus,
                    promoStatus
                    ));

            cursorTrans.close();
            cursorAssortTrans.close();

            cursorStore.moveToNext();
        }

        myArrayListStores.clear();
        myArrayListStores.addAll(arrStoreLoader);

        mBranchListViewAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter == null) {
            Toast.makeText(StoresActivity.this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
        }

        GetLocationArrayList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MainLibrary.InitializedHash) {
            MainLibrary.InitializedHash = false;
            MainLibrary.messageBox(StoresActivity.this,"Log out","Hash Initialized. Account will now log out.");
            new UserLogout(StoresActivity.this).execute();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_set_printer:
                Intent intentSettings = new Intent(StoresActivity.this, SettingsActivity.class);
                startActivity(intentSettings);
                break;
            case R.id.action_logout:
                onBackPressed();
                break;
            case R.id.action_send_error:
                new CheckInternet().execute();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private class CheckInternet extends AsyncTask<Void, Void, Boolean> {
        String errmsg = "";

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(StoresActivity.this, "", "Checking internet connection.");
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
            else errmsg = "No internet connection.";

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            pDL.dismiss();
            if(!bResult) {
                Toast.makeText(StoresActivity.this, errmsg, Toast.LENGTH_SHORT).show();
                return;
            }

            new PostErrorReport().execute();
        }
    }

    private class PostErrorReport extends AsyncTask<Void, Void, Boolean> {
        String errMsg = "";
        String response = "";
        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(StoresActivity.this, "", "Sending error report to dev team.");
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
                HttpURLConnection httpUrlConnection;
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

                String line;
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
            catch (IOException | JSONException ex) {
                errMsg = "Slow or unstable internet connection.";
                String err = ex.getMessage() != null ? ex.getMessage() : errMsg;
                MainLibrary.errorLog.appendLog(err, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            pDL.dismiss();
            if(!bResult) {
                Toast.makeText(StoresActivity.this, errMsg, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(StoresActivity.this, response, Toast.LENGTH_SHORT).show();
            MainLibrary.errorLog.fileLog.delete();
        }
    }

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder logoutdialog = new AlertDialog.Builder(StoresActivity.this);
        logoutdialog.setTitle("Log Out");
        logoutdialog.setMessage("Are you sure you want to log out?");
        logoutdialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        logoutdialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                new UserLogout(StoresActivity.this).execute();
            }
        });

        logoutdialog.show();
    }

    private class UserLogout extends AsyncTask<Void, Void, Boolean> {

        String response;
        String errmsg;
        Context mContext;

        UserLogout(Context ctx) {
            this.mContext = ctx;
        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(mContext, "", "Logging out. Please Wait...", true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bReturn = false;

            try {
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

            } catch(IOException ex){
                errmsg = "No internet connection.";
                String errException = ex.getMessage() != null ? ex.getMessage() : errmsg;
                MainLibrary.errorLog.appendLog("LOGOUT, " + errException, TAG);
            }
            return bReturn;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            pDL.dismiss();
            Intent intentMain = new Intent(StoresActivity.this, MainActivity.class);
            if(!success) {
                Toast.makeText(mContext, errmsg, Toast.LENGTH_LONG).show();
/*                startActivity(intentMain);
                finish();*/
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }
}
