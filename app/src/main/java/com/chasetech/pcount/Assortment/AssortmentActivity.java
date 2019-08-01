package com.chasetech.pcount.Assortment;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chasetech.pcount.BuildConfig;
import com.chasetech.pcount.MKL.PCount;
import com.chasetech.pcount.MKL.PCountActivity;
import com.chasetech.pcount.PostingActivity;
import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.MainActivity;
import com.chasetech.pcount.R;
import com.chasetech.pcount.Woosim.WoosimPrinter;
import com.chasetech.pcount.adapter.ReportListViewAdapter;
import com.chasetech.pcount.adapter.ReportWithSoAdapter;
import com.chasetech.pcount.database.SQLLib;
import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.TSC.BPrinter;
import com.chasetech.pcount.library.MainLibrary;
import com.chasetech.pcount.library.ReportClass;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class AssortmentActivity extends AppCompatActivity {

    private ProgressDialog pDL;
    private AlertDialog mAlertDialog;
    private SQLLib db;

    private final String TAG = "DEBUGGING";

    //GOAL : CREATE NEW HASHMAP AND ARRAYLIST
    // POPULATE THEM AFTER FILTERING. KAHIT ITEMORDERED NALANG.

    private ArrayList<Assortment> arrAssortment = new ArrayList<>();
    private HashMap<String, Assortment> mHashmapAssortmentAll = new HashMap<>();
    private HashMap<String, Assortment> hmAssortment = new HashMap<>();
    private Assortment assortmentUpdated = null;

    private ListView lvwAssortment = null;

    private AssortmentAdapter mAssortmentAdapter;


    private EditText editTextSearch = null;

    private BPrinter Printer;
    private Boolean lprintwithso = false;

    private double len  = 0;
    private int numItems = 0;

    private String selectedPrinter = "";
    private WoosimPrinter woosimPrinter = null;
    private boolean printEnabled;
    private TextView tvwTotalItems;
    private ArrayList<String> itemOrdered;
    private ArrayList<String> filteredItems;
    private Boolean Filtered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.assortment_activity);

        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(this, MainLibrary.errlogFile));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selectedPrinter = prefs.getString("printer_list", "2");

        //GET PRINTER TYPE
        if(selectedPrinter.equals("1")) MainLibrary.mSelectedPrinter = MainLibrary.PRINTER.WOOSIM;
        else if(selectedPrinter.equals("2")) MainLibrary.mSelectedPrinter = MainLibrary.PRINTER.TSC;

        final TextView lblfso = (TextView) findViewById(R.id.lblfso);
//        String fsolbl = MainLibrary.gStrCurrentUserName.substring(3,6) + " Unit";
        String fsolbl = MainLibrary.gStrCurrentUserName + " Unit";

        lblfso.setText(fsolbl);

        db = new SQLLib(AssortmentActivity.this);
        db.open();

        Printer = new BPrinter(this);

/*        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setTitle(MainLibrary.gCurrentBranchNameSelected);
        if(MainLibrary.isAssortmentMode) getActionBar().setTitle(MainLibrary.gCurrentBranchNameSelected + " - " + " ASSORTMENT");*/
//        lupdate = getIntent().getExtras().getBoolean("lupdate");

        //HEADER
        getSupportActionBar().setTitle(MainLibrary.gSelectedStores.storeName + " - ASSORTMENT");

        new TaskProcessData().execute();

        editTextSearch = (EditText) findViewById(R.id.enter_search);
        tvwTotalItems = (TextView) findViewById(R.id.tvwTotalItems);

        //SEARCH SKU
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String search = editTextSearch.getText().toString();
                mAssortmentAdapter.filter(99, search);
            }
        });

        lvwAssortment = (ListView) findViewById(R.id.lvwAssortment);


        // INPUT COUNT
        lvwAssortment.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final AssortmentViewHolder viewHolder = (AssortmentViewHolder) view.getTag();

                final Assortment assortment = viewHolder.assortment;

                final Dialog dialog = new Dialog(AssortmentActivity.this, R.style.Transparent);
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.activity_sku_details);





                // LOAD COUNT
                final TextView textViewDesc = (TextView) dialog.findViewById(R.id.textViewDesc);
                final EditText editTextPcs = (EditText) dialog.findViewById(R.id.pcs);
                final EditText editTextWhPcs = (EditText) dialog.findViewById(R.id.whpcs);
                final EditText editTextWhCs = (EditText) dialog.findViewById(R.id.whcs);
                final EditText txtInventorygoal = (EditText) dialog.findViewById(R.id.txtInveGoal);
                final TextView tvwInventoryGoal = (TextView) dialog.findViewById(R.id.tvwIg);
                final Button btnQty = (Button) dialog.findViewById(R.id.btnQtyOk);

                txtInventorygoal.setVisibility(View.GONE);
                tvwInventoryGoal.setVisibility(View.GONE);

                textViewDesc.setText(assortment.desc);
                editTextPcs.setText("");
                editTextWhPcs.setText("");
                editTextWhCs.setText("");



                if (assortment.sapc != 0 || assortment.whpc != 0 || assortment.whcs !=0 ) {
                    editTextPcs.setText(String.valueOf(assortment.sapc));
                    editTextWhPcs.setText(String.valueOf(assortment.whpc));
                    editTextWhCs.setText(String.valueOf(assortment.whcs));
                }

                btnQty.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();

                        assortmentUpdated = null;

                        String inputPcs = editTextPcs.getText().toString();
                        String inputWhPcs = editTextWhPcs.getText().toString();
                        String inputWhcs = editTextWhCs.getText().toString();
                        String inputIg = txtInventorygoal.getText().toString().trim();

/*                        if (inputPcs.isEmpty() && inputWhPcs.isEmpty() && inputWhcs.isEmpty()) {
                            return;
                        }*/

                        //assortment.ig = Integer.parseInt(inputIg);


                        if (inputPcs.isEmpty()) {
                            inputPcs = "0";
                        }
                        if (inputWhPcs.isEmpty()) {
                            inputWhPcs = "0";
                        }
                        if (inputWhcs.isEmpty()) {
                            inputWhcs = "0";
                        }

                        int so = assortment.ig - Integer.parseInt(inputPcs) - Integer.parseInt(inputWhPcs) - (Integer.parseInt(inputWhcs) * assortment.conversion);;


                        /*// COMPUTATION FOR MDC
                        String channelname = MainLibrary.gSelectedStores.channelArea.toUpperCase();
                        if(channelname.equals("MDC")|| channelname.equals("SOUTH STAR DRUG") || channelname.equals("ROSE PHARMACY") || channelname.equals("ST. JOSEPH DRUG") || channelname.equals("ST. JOSEPH DRUG")) {
                            String channel = MainLibrary.gSelectedStores.channelDesc.trim().toUpperCase();
                            //if(channel.contains("EXTRA SMALL") || channel.contains("SMALL") || channel.contains("MEDIUM")) {
                            double endInv = Double.parseDouble(inputPcs) + Double.parseDouble(inputWhPcs) + (Double.parseDouble(inputWhcs) * assortment.conversion);

                            //if(selectedPcount.multi >= 12) { // 60%
                            double stuff = (assortment.ig * 3) / 5;
                            //if(endInv <= ((selectedPcount.ig * 3) / 5))
                            if(endInv <= stuff)
                            {
                                so = assortment.ig - Integer.parseInt(inputPcs) - Integer.parseInt(inputWhPcs) - (Integer.parseInt(inputWhcs) * assortment.conversion);
                            }
                            else so = 0;
                            //}
                            //}
                        } */

                        int fso = 0;

                        if ((so % assortment.multi) == 0) {
                            fso = so;
                        }
                        else{
                            fso = so - (so % assortment.multi) + assortment.multi;
                        }

                        if (so <= 0) {    //10/27 for negative values
                            so = 0;
                            fso = 0;
                        }
                        /*

                        assortment.sapc =
                        assortment.so = so;
                        assortment.fso = fso;
                        assortment.updated = true; */

                        assortment.sapc = Integer.parseInt(inputPcs);
                        assortment.whpc = Integer.parseInt(inputWhPcs);
                        assortment.whcs = Integer.parseInt(inputWhcs);
                        assortment.so = so;
                        assortment.fso = fso;
                        assortment.updated = true;


                        Assortment assortmentAll = mHashmapAssortmentAll.get(assortment.id);
                        if(assortmentAll != null) {
                            assortmentAll.sapc = Integer.parseInt(inputPcs);
                            assortmentAll.whpc = Integer.parseInt(inputWhPcs);
                            assortmentAll.whcs = Integer.parseInt(inputWhcs);
                            assortmentAll.so = so;
                            assortmentAll.fso = fso;
                            //assortmentAll.ig = Integer.parseInt(inputIg);
                            assortmentAll.updated = true;
                            mHashmapAssortmentAll.put(assortment.barcode, assortmentAll);
                        }

                        assortmentUpdated = assortment;
/*                        for (Assortment assortmentall : arrAssortmentAll) {
                            if(assortmentall.id == assortment.id) {
                                assortmentall.sapc = Integer.parseInt(inputPcs);
                                assortmentall.whpc = Integer.parseInt(inputWhPcs);
                                assortmentall.whcs = Integer.parseInt(inputWhcs);
                                assortmentall.so = so;
                                assortmentall.fso = fso;
                                assortmentall.updated = true;
                                break;
                            }
                        }*/
                        new TaskSaveData().execute();
                        mAssortmentAdapter.notifyDataSetChanged();
                    }
                });

                dialog.show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AssortmentActivity.this);
        printEnabled = prefs.getBoolean("switch_printer_enabled", false);

        if(printEnabled) CheckWoosimPrinter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(woosimPrinter != null) {
            if (MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM)
                woosimPrinter.Close();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(woosimPrinter != null) {
            if (MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM)
                woosimPrinter.Close();
        }
    }

    private void CheckWoosimPrinter() {
        try {
            if (MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM) {
                woosimPrinter = new WoosimPrinter(AssortmentActivity.this);
                woosimPrinter.SetUpWoosim();
            }
        }
        catch (Exception ex) {
            String errmsg = "Can't connect to printer. Please check printer.";
            String errException = ex.getMessage() != null ? ex.getMessage() : errmsg;
            MainLibrary.errorLog.appendLog(errException, TAG);
            Toast.makeText(AssortmentActivity.this, "Failed Connection: " + errmsg, Toast.LENGTH_LONG).show();
        }
    }

    /** DATA PROCESSING *************************************************************/
    // PUTTING IN HASHMAP
    private class TaskProcessData extends AsyncTask<String, Void, Boolean> {

        private String errorMessage;
        private boolean itemsEqual = false;


        @Override
        protected Boolean doInBackground(String... params) {

            boolean result = false;
            itemsEqual = false;

            try {

                arrAssortment.clear();
                mHashmapAssortmentAll.clear();

                String strTransQuery = "SELECT * FROM " + SQLiteDB.TABLE_TRANSACTION_ASSORT + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "' AND " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_DATE + " = '" + MainLibrary.gStrCurrentDate + "' AND " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_USERID + " = '" + MainLibrary.gStrCurrentUserID + "'";
                String strItemsQuery = "SELECT * FROM " + SQLiteDB.TABLE_ASSORTMENT + " WHERE " + SQLiteDB.COLUMN_ASSORTMENT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "'";

                // SELECTING ASSORTMENT MASTERFILE
                Cursor cursTrans = db.queryData(strTransQuery);
                cursTrans.moveToFirst();

                Cursor cursItems = db.queryData(strItemsQuery);
                cursItems.moveToFirst();

                if(cursTrans.getCount() == 0) {
//                if(cursItems.getCount() != 0) {

                    int nItemsTotal = cursItems.getCount();

                    if(nItemsTotal == 0) {
                        errorMessage = "No items found in this store.";
                        itemsEqual = true;
                        return false;
                    }

                    // SELECTING MKL MASTERFILE
                    while (!cursItems.isAfterLast()) {

                        int id = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_ID));
                        int ig = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_IG));
                        String barcode = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_BARCODE)).trim();
                        String sapc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_SAPC)).trim();
                        String whpc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_WHPC)).trim();
                        String whcs = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_WHCS)).trim();
                        String so = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_SO)).trim();
                        String fso = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_FSO)).trim();
                        String desc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_DESC)).trim();
                        String category = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_CATEGORY)).trim();
                        String descLong = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_DESCLONG)).trim();
                        String categoryid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_CATEGORYID)).trim();
                        String brandid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_BRANDID)).trim();
                        String divisionid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_DIVISIONID)).trim();
                        String subcategoryid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_SUBCATEGORYID)).trim();
                        int conversion = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_CONVERSION));
                        double fsovalue = cursItems.getDouble(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_FSOVALUE));
                        int webid = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_WEBID));
                        int multi = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_MULTI));
                        String otherBarcode = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_OTHERBARCODE));
                        int minstock = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_MINSTOCK));

                        Assortment assortmentItem = new Assortment(
                                id,
                                barcode,
                                desc,
                                categoryid,
                                brandid,
                                divisionid,
                                subcategoryid,
                                ig,
                                conversion,
                                fsovalue,
                                webid,
                                multi,
                                false,
                                otherBarcode,
                                minstock
                        );

                        arrAssortment.add(assortmentItem);
                        mHashmapAssortmentAll.put(assortmentItem.barcode.trim(), assortmentItem);

                        String[] aFields = new String[] {
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_DATE,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_BARCODE,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_IG,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_SAPC,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_WHPC,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_WHCS,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_CONVERSION,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_SO,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_FSO,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_FSOVALUE,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_LPOSTED,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_WEBID,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_USERID,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_MULTI,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_OTHERBARCODE,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_MINSTOCK,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_CATEGORY,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_DESCLONG,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_DESC,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_CATEGORYID,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_BRANDID,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_DIVISIONID,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_SUBCATEGORYID
                        };

                        String[] aValues = new String[] {
                                MainLibrary.gStrCurrentDate,
                                String.valueOf(MainLibrary.gSelectedStores.webStoreId),
                                barcode,
                                String.valueOf(ig),
                                sapc,
                                whpc,
                                whcs,
                                String.valueOf(conversion),
                                so,
                                fso,
                                String.valueOf(fsovalue),
                                "0",
                                String.valueOf(webid),
                                String.valueOf(MainLibrary.gStrCurrentUserID),
                                String.valueOf(multi),
                                otherBarcode,
                                String.valueOf(minstock),
                                category,
                                descLong,
                                desc,
                                categoryid,
                                brandid,
                                divisionid,
                                subcategoryid
                        };

                        db.AddRecord(SQLiteDB.TABLE_TRANSACTION_ASSORT, aFields, aValues);
                        cursItems.moveToNext();
                    }

                    if(nItemsTotal != arrAssortment.size()) {
                        errorMessage = "Items copied are not equal. Please reload items. Do you want to proceed?";
                        itemsEqual = false;
                        return false;
                    }

                    itemsEqual = true;
                    result = true;

                    cursItems.close();
                }
                else {

                    while (!cursTrans.isAfterLast()) {

                        int id = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_ID));
                        int ig = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_IG));
                        String barcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_BARCODE)).trim();
                        String desc = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_DESC)).trim();
                        String categoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_CATEGORYID)).trim();
                        String brandid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_BRANDID)).trim();
                        String divisionid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_DIVISIONID)).trim();
                        String subcategoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_SUBCATEGORYID)).trim();
                        int conversion = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_CONVERSION));
                        double fsovalue = cursTrans.getDouble(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_FSOVALUE));
                        int webid = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_WEBID));
                        int multi = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_MULTI));
                        String otherBarcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_OTHERBARCODE));
                        int minstock = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_MINSTOCK));
                        int sapc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_SAPC));
                        int whpc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_WHPC));
                        int whcs = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_WHCS));
                        int so = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_SO));
                        int fso = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_FSO));
                        boolean isUpdated = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_UPDATED)) == 1;

                       Assortment assortmentItem = new Assortment(
                                id,
                                barcode,
                                desc,
                                categoryid,
                                brandid,
                                divisionid,
                                subcategoryid,
                                ig,
                                conversion,
                                fsovalue,
                                webid,
                                multi,
                                false,
                                otherBarcode,
                                minstock
                        );

                        if (isUpdated) {
                            assortmentItem.sapc = sapc;
                            assortmentItem.whpc = whpc;
                            assortmentItem.whcs = whcs;
                            assortmentItem.so = so;
                            assortmentItem.fso = fso;
                            assortmentItem.updated = true;
                            hmAssortment.put(assortmentItem.barcode, assortmentItem);
                        }

                        arrAssortment.add(assortmentItem);
                        mHashmapAssortmentAll.put(assortmentItem.barcode.trim(), assortmentItem);
                        cursTrans.moveToNext();
                    }

                    result = true;
                }

                cursTrans.close();

                itemOrdered = new ArrayList<>(mHashmapAssortmentAll.keySet()); // barcodes.

                if(MainLibrary.gSelectedStores.channelArea.equals("MDC")) {
                    Collections.sort(itemOrdered, new Comparator<String>() {
                        @Override
                        public int compare(String lhs, String rhs) {
                            return Integer.parseInt(lhs) - Integer.parseInt(rhs);
                        }
                    });
                }

                //ProcessOrderedItems();
            }
            catch (Exception e) {
                errorMessage = "Error in loading items. Please send error report";
                String errmsg = e.getMessage() != null ? e.getMessage() : errorMessage;
                MainLibrary.errorLog.appendLog(errmsg, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            pDL.dismiss();

            if(!result) {
                AlertDialog promptDialog = new AlertDialog.Builder(AssortmentActivity.this).create();
                promptDialog.setCancelable(false);
                promptDialog.setTitle("Loading Items");

                String msg = errorMessage;

                if(!itemsEqual) {

                    String strTransDeleteQuery = "DELETE FROM " + SQLiteDB.TABLE_TRANSACTION_ASSORT
                            + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId)
                            + "' AND " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_DATE + " = '" + MainLibrary.gStrCurrentDate + "'";

                    db.ExecSQLWrite(strTransDeleteQuery);
                    promptDialog.setButton(DialogInterface.BUTTON_POSITIVE, "RELOAD", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            new TaskProcessData().execute();
                        }
                    });
                    promptDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            onBackPressed();
                        }
                    });
                }
                else {
                    promptDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            onBackPressed();
                        }
                    });
                }

                promptDialog.setMessage(msg);
                promptDialog.show();
                return;
            }

            mAssortmentAdapter = new AssortmentAdapter(AssortmentActivity.this, arrAssortment);
            lvwAssortment.setAdapter(mAssortmentAdapter);
            mAssortmentAdapter.notifyDataSetChanged();

            String totalItemMsg = "Total Items: " + String.valueOf(hmAssortment.size()) + " / " + String.valueOf(arrAssortment.size());
            tvwTotalItems.setText(totalItemMsg);
        }

        @Override
        protected void onPreExecute() {
            //Toast.makeText(PCountActivity.this, "Current Stores Selected " + String.valueOf(MainLibrary.gCurrentBranchSelected), Toast.LENGTH_SHORT).show();
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Updating Masterfile. Please wait.", true);
        }
    }

    private void ProcessOrderedItems() {

        mHashmapAssortmentAll.clear();

        String strTransQuery = "SELECT * FROM " + SQLiteDB.TABLE_TRANSACTION_ASSORT + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "' AND " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_DATE + " = '" + MainLibrary.gStrCurrentDate + "'";

        // ORDERED TRANS ITEMS
        if(MainLibrary.gSelectedStores.channelArea.equals("MDC")) {
            strTransQuery = "SELECT * FROM " + SQLiteDB.TABLE_TRANSACTION_ASSORT + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "' AND " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_DATE + " = '" + MainLibrary.gStrCurrentDate + "' ORDER BY " + SQLiteDB.COLUMN_TRANSACTION_ASSORT_BARCODE;
        }

        Cursor cursTrans = db.queryData(strTransQuery);

        if(cursTrans.moveToFirst()) {

            while (!cursTrans.isAfterLast()) {

                int id = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_ID));
                int ig = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_IG));
                String barcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_BARCODE)).trim();
                String desc = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_DESC)).trim();
                String categoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_CATEGORYID)).trim();
                String brandid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_BRANDID)).trim();
                String divisionid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_DIVISIONID)).trim();
                String subcategoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_SUBCATEGORYID)).trim();
                int conversion = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_CONVERSION));
                double fsovalue = cursTrans.getDouble(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_FSOVALUE));
                int webid = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_WEBID));
                int multi = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_MULTI));
                String otherBarcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_OTHERBARCODE));
                int minstock = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_MINSTOCK));
                int sapc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_SAPC));
                int whpc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_WHPC));
                int whcs = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_WHCS));
                int so = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_SO));
                int fso = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_FSO));
                boolean isUpdated = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_UPDATED)) == 1;

                Assortment assortmentItem = new Assortment(
                        id,
                        barcode,
                        desc,
                        categoryid,
                        brandid,
                        divisionid,
                        subcategoryid,
                        ig,
                        conversion,
                        fsovalue,
                        webid,
                        multi,
                        false,
                        otherBarcode,
                        minstock
                );

                if (isUpdated) {
                    assortmentItem.sapc = sapc;
                    assortmentItem.whpc = whpc;
                    assortmentItem.whcs = whcs;
                    assortmentItem.so = so;
                    assortmentItem.fso = fso;
                    assortmentItem.updated = true;
                }

                mHashmapAssortmentAll.put(assortmentItem.barcode, assortmentItem);
                cursTrans.moveToNext();
            }

            cursTrans.close();

            itemOrdered = new ArrayList<String>(mHashmapAssortmentAll.keySet());

            if(MainLibrary.gSelectedStores.channelArea.equals("MDC")) {
                Collections.sort(itemOrdered, new Comparator<String>() {
                    @Override
                    public int compare(String lhs, String rhs) {
                        return Integer.parseInt(lhs) - Integer.parseInt(rhs);
                    }
                });
            }
        }
    }

    /** DATA PROCESSING *************************************************************/
    private class TaskSaveData extends AsyncTask<String, Void, Boolean> {

        private String errmsg;

        @Override
        protected Boolean doInBackground(String... params) {

            boolean result = false;

            try {

                if (assortmentUpdated != null) {

                    Assortment assortment = assortmentUpdated;

                    if (assortment.sapc != 0 || assortment.whpc != 0 || assortment.whcs != 0 || assortment.fso != 0 || assortment.updated) {

                        Cursor cursTrans = db.GetDataCursor(SQLiteDB.TABLE_TRANSACTION_ASSORT,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_BARCODE + " = '" + assortment.barcode + "' AND "
                                        + SQLiteDB.COLUMN_TRANSACTION_ASSORT_DATE + " = '" + MainLibrary.gStrCurrentDate + "' AND "
                                        + SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID + " = '" + MainLibrary.gSelectedStores.webStoreId + "' AND "
                                        + SQLiteDB.COLUMN_TRANSACTION_ASSORT_USERID + " = '" + MainLibrary.gStrCurrentUserID + "'");
                        cursTrans.moveToFirst();

                        String[] afields = {
                                "date",
                                "storeid",
                                "barcode",
                                "ig",
                                "sapc",
                                "whpc",
                                "whcs",
                                "conversion",
                                "so",
                                "fso",
                                "fsovalue",
                                "webid",
                                "userid",
                                "multi",
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_MONTH,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_LPOSTED,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_UPDATED
                        };

                        String[] avalues = {MainLibrary.gStrCurrentDate
                                , String.valueOf(MainLibrary.gSelectedStores.webStoreId)
                                , assortment.barcode
                                , String.valueOf(assortment.ig)
                                , String.valueOf(assortment.sapc)
                                , String.valueOf(assortment.whpc)
                                , String.valueOf(assortment.whcs)
                                , String.valueOf(assortment.conversion)
                                , String.valueOf(assortment.so)
                                , String.valueOf(assortment.fso)
                                , String.valueOf(assortment.fsovalue)
                                , String.valueOf(assortment.webid)
                                , String.valueOf(MainLibrary.gStrCurrentUserID)
                                , String.valueOf(assortment.multi)
                                , MainLibrary.selectedMonth.trim()
                                , "0"
                                , "1"
                        };

                        hmAssortment.put(assortment.barcode, assortment);
                        //db.ExecSQLWrite("UPDATE " + SQLiteDB.TABLE_ASSORTMENT + " SET " + SQLiteDB.COLUMN_ASSORTMENT_IG + " = '" + assortment.ig + "' WHERE " + SQLiteDB.COLUMN_ASSORTMENT_BARCODE + " = '" + String.valueOf(assortment.barcode).trim() + "' AND " + SQLiteDB.COLUMN_ASSORTMENT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId).trim() + "'");

                        if (cursTrans.getCount() == 0) {
                            db.AddRecord(SQLiteDB.TABLE_TRANSACTION_ASSORT, afields, avalues);
                            return true;
                        }

                        String[] whereFields = new String[]{
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_BARCODE,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_DATE,
                                SQLiteDB.COLUMN_TRANSACTION_ASSORT_STOREID,
                        };
                        String[] whereValues = new String[]{
                                assortment.barcode,
                                MainLibrary.gStrCurrentDate,
                                String.valueOf(MainLibrary.gSelectedStores.webStoreId).trim(),
                        };

                        db.UpdateRecord(SQLiteDB.TABLE_TRANSACTION_ASSORT, whereFields, whereValues, afields, avalues, AssortmentActivity.this);

                        cursTrans.close();
                    }
                    result = true;
                }
                else errmsg = "Data error.";
            }
            catch (Exception ex) {
                errmsg = "Saving transaction failed. Please send error to dev team.";
                String errException = ex.getMessage() != null ? ex.getMessage() : errmsg;
                MainLibrary.errorLog.appendLog(errException, TAG);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            pDL.dismiss();
            if(!bResult) {
                Toast.makeText(AssortmentActivity.this, errmsg, Toast.LENGTH_SHORT).show();
                return;
            }

            mAssortmentAdapter.notifyDataSetChanged();
            lvwAssortment.requestLayout();

            String totalItemMsg = "Total Items: " + String.valueOf(hmAssortment.size()) + " / " + String.valueOf(arrAssortment.size());
            tvwTotalItems.setText(totalItemMsg);
        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Saving Transaction dated " + MainLibrary.gStrCurrentDate + ". Please Wait...", true);
        }
    }

    /** DATA PROCESSING ***************************       Boolean lwithbarcode = true;**********************************/
    private class TaskPrintData extends AsyncTask<String, Void, Boolean> {

        String print = "";
        Boolean lwithbarcode = true;
        String errmsg = "";

        TaskPrintData(boolean hasBarcode) {
            this.lwithbarcode = hasBarcode;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            Boolean lsuccess = false;

            switch (MainLibrary.mSelectedPrinter) {
                case WOOSIM:
                    try {

                        if (MainLibrary.ValidateStoreIfFourInch()) {
                            if (PrintFormatWoosim4inch(lwithbarcode))
                                lsuccess = true;
                            else errmsg = "Woosim printer not connected or paired.";

                        } else {
                            if (PrintFormatWoosim3inch(lwithbarcode))
                                lsuccess = true;
                            else errmsg = "Woosim printer not connected or paired.";
                        }
                    }
                    catch (Exception ex) {
                        Log.e(TAG, ex.getMessage());
                        errmsg = ex.getMessage() + ". Please check printer bluetooth connection";
                    }
                    break;

                case TSC:

                    if (MainLibrary.ValidateStoreIfFourInch()) {
                        print = Printer.GenerateStringTSCPrint(PrintFormat4L(lwithbarcode), len, numItems, 1, true);

                        if(Printer.Open()) {
                            String basfile = "DEFAULT.PRN";

                            switch (MainLibrary.eStore) {
                                case MERCURY_DRUG:
                                    basfile = "MERCURY_4L.PRN";
                                    break;
                                case SOUTH_STAR_DRUG:
                                    break;
                                case ST_JOSEPH_DRUG:
                                    break;
                                case ROSE_PHARMACY:
                                    break;
                                case THREESIXTY_PHARMACY:
                                    break;
                                default:
                                    break;
                            }

                            File fbas = new File(Environment.getExternalStorageDirectory(), "Download/" + basfile);
                            if(!fbas.exists()) {
                                if(!new File(Environment.getExternalStorageDirectory(), "Download/DEFAULT.PRN").exists()) {
                                    errmsg = "No downloaded logo. Please re-log to download the required files.";
                                    return false;
                                }
                                else basfile = "DEFAULT.PRN";
                            }

                            try {
                                Printer.sendcommand("SIZE 4,1\n");
                                Printer.sendcommand("GAP 0,0\n");
                                Printer.sendcommand("DIRECTION 1\n");
                                Printer.sendcommand("SET TEAR ON\n");
                                Printer.sendcommand("CLS\n");
                                Printer.sendfile(basfile);
                                Printer.PrintString(print);
                                lsuccess = true;
                            }
                            catch (Exception ex) {
                                errmsg = ex.getMessage();
                            }
                        }
                        else errmsg =  "Can't connect to printer. Please check if printer is paired in this device";
                    }
                    else {

                        print = Printer.GenerateStringTSCPrint(PrintFormat3R(lwithbarcode), len, numItems, 1, false);

                        if (Printer.Open()) {

                            String basfile = "DEFAULT.PRN";
                            switch (MainLibrary.eStore) {
                                case SEVEN_ELEVEN:
                                    basfile = "711.PRN";
                                    break;
                                case MERCURY_DRUG:
                                    basfile = "MERCURY.PRN";
                                    break;
                                case MINISTOP:
                                    basfile = "MINISTOP.PRN";
                                    break;
                                case FAMILY_MART:
                                    basfile = "FAMILY.PRN";
                                    break;
                                case LAWSON:
                                    basfile = "LAWSON.PRN";
                                    break;
                                case ALFAMART:
                                    basfile = "ALFAMART.PRN";
                                    break;
                                case ROSE_PHARMACY:
                                    break;
                                case ST_JOSEPH_DRUG:
                                    break;
                                case SOUTH_STAR_DRUG:
                                    break;
                                case THREESIXTY_PHARMACY:
                                    break;
                                default:
                                    break;
                            }

                            File fbas = new File(Environment.getExternalStorageDirectory(), "Download/" + basfile);
                            if (!fbas.exists()) {
                                if (!new File(Environment.getExternalStorageDirectory(), "Download/DEFAULT.PRN").exists()) {
                                    errmsg = "No downloaded logo. Please re-log to download the required files.";
                                    return false;
                                } else basfile = "DEFAULT.PRN";
                            }

                            try {
                                Printer.sendcommand("SIZE 4,1\n");
                                Printer.sendcommand("GAP 0,0\n");
                                Printer.sendcommand("DIRECTION 1\n");
                                Printer.sendcommand("SET TEAR ON\n");
                                Printer.sendcommand("CLS\n");
                                Printer.sendfile(basfile);
                                Printer.clearbuffer();
                                Printer.PrintString(print);
                                lsuccess = true;
                            } catch (Exception ex) {
                                errmsg = ex.getMessage();
                            }
                        } else
                            errmsg = "Can't connect to printer. Please check if printer is paired in this device";
                    }
                    break;
                default:
                    break;
            }

            return lsuccess;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            pDL.dismiss();
            if (!result) {
                if (woosimPrinter != null) {
                    woosimPrinter.isConnected = false;
                }
                Toast.makeText(AssortmentActivity.this, "Error Printing. " + errmsg, Toast.LENGTH_SHORT).show();
                return;
            }


            AlertDialog printdialog = new AlertDialog.Builder(AssortmentActivity.this).create();
            printdialog.setTitle("Print");
            printdialog.setMessage("Print successful.");

            DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Printer.Close();
                }
            };

            DialogInterface.OnCancelListener cancelListerner = new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Printer.Close();
                }
            };

            if(MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM) {
                okListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                };
                cancelListerner = new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                };
            }

            printdialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", okListener);
            printdialog.setOnCancelListener(cancelListerner);

            printdialog.show();

        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Printing. Please Wait...", true);
        }
    }

    private boolean PrintFormatWoosim3inch(boolean hasBarcode) throws Exception {
        boolean result = false;
        String toPrint = "";

        // PRINT LOGO ------------------------------
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_default, options);

        switch (MainLibrary.eStore) {
            case SEVEN_ELEVEN:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_seveneleven, options);
                break;
            case MERCURY_DRUG:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mercurydrug, options);
                break;
            case MINISTOP:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_ministop, options);
                break;
            case FAMILY_MART:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_familymart, options);
                break;
            case LAWSON:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_lawson, options);
                break;
            case ALFAMART:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alfamart, options);
                break;
            case ROSE_PHARMACY:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_rosepharmacy, options);
                break;
            case ST_JOSEPH_DRUG:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_stjoseph, options);
                break;
            case SOUTH_STAR_DRUG:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_southstardrug, options);
                break;
            case THREESIXTY_PHARMACY:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_threesixtydrug, options);
                break;
            default:
                break;
        };

        if(!woosimPrinter.printBMPImage(bmpStore, 0, 0, 580, 180)) {
            return false;
        }
        // -------------------------------

        toPrint += "Store: " + MainLibrary.gSelectedStores.storeName + "\n";

        String osascore = "";

        osascore = MainLibrary.GetOsaScoreAssortment(mHashmapAssortmentAll);

        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += StringUtils.rightPad("SKU",25,"") +
                StringUtils.rightPad("IG",10,"") +
                StringUtils.rightPad("Invty",8,"") +
                StringUtils.rightPad("Order qty", 12,"") +
                StringUtils.rightPad("Order amt", 5, "") + "\n";
        toPrint += Printer.woosimLines;

        if(!woosimPrinter.printText(toPrint, false, false, 1)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AssortmentActivity.this, "Printer connection is interrupted. Print cancelled.", Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }


        result = PrintDetailsWoosim_3inch(hasBarcode);

        return result;
    }

    private boolean PrintFormatWoosim4inch(boolean hasBarcode) throws Exception {
        boolean result = false;
        String toPrint = "";

        // PRINT LOGO ------------------------------
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_default, options);

        switch (MainLibrary.eStore) {
            case SEVEN_ELEVEN:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_seveneleven_fourinch, options);
                break;
            case MERCURY_DRUG:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mercurydrug_fourinch, options);
                break;
            case MINISTOP:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_ministop_fourinch, options);
                break;
            case FAMILY_MART:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_familymart_fourinch, options);
                break;
            case LAWSON:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_lawson_fourinch, options);
                break;
            case ALFAMART:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_alfamart_fourinch, options);
                break;
            case ROSE_PHARMACY:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_rosepharmacy_fourinch, options);
                break;
            case ST_JOSEPH_DRUG:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_stjoseph_fourinch, options);
                break;
            case SOUTH_STAR_DRUG:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_southstardrug_fourinch, options);
                break;
            case THREESIXTY_PHARMACY:
                bmpStore = BitmapFactory.decodeResource(getResources(), R.drawable.ic_threesixtydrug_fourinch, options);
                break;
            default:
                break;
        };

        if(!woosimPrinter.printBMPImage(bmpStore, 0, 0, 800, 180)) {
            return false;
        }
        // -------------------------------

        toPrint += "Store: " + MainLibrary.gSelectedStores.storeName + "\n";


        String osascore = "";

        osascore = MainLibrary.GetOsaScoreAssortment(mHashmapAssortmentAll);

        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += StringUtils.rightPad("SKU",22,"") +
                StringUtils.rightPad("IG",8,"") +
                StringUtils.rightPad("Invty",9,"") +
                StringUtils.rightPad("Final SO",12,"") +
                StringUtils.rightPad("Unit",9,"") +
                StringUtils.rightPad("Order amt", 9, "") + "\n";
        toPrint += Printer.woosimLines_4inch;

        if(!woosimPrinter.printText(toPrint, false, false, 1)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AssortmentActivity.this, "Printer connection is interrupted. Print cancelled.", Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }

        result = PrintDetailsWoosim_4inch(hasBarcode);

        return result;
    }

    private boolean PrintDetailsWoosim_3inch(boolean hasBarcode) {

        boolean result = false;
        int totsku = 0, totfso = 0;
        double totfsoval = 0;
        try {

            ArrayList<Assortment> Barcodes = new ArrayList<>();
            if(Filtered)
            {
                //Barcodes = filteredItems;

                Barcodes =  mAssortmentAdapter.arrAssortmentResultList;
            }
            else
            {

                Barcodes = arrAssortment;
            }


            //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())
            for (Assortment assortment : Barcodes)
            {

           // for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet()) {
             //   Assortment assortment = entry.getValue();
            //for (String barcodeKey :  Barcodes) {

              //  Assortment assortment = mHashmapAssortmentAll.get(barcodeKey);

                if (lprintwithso) {
                    if (assortment.so == 0) {
                        continue;
                    }
                }

                int totig = assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion);
                double orderAmt = assortment.fsovalue * assortment.fso;


                String itemDesc = StringUtils.rightPad(assortment.desc + " " + assortment.barcode, 20, "");
                if(!woosimPrinter.printText(itemDesc, false, false, 1)) return false;

                String strValues = StringUtils.rightPad("", 23, "")
                        + StringUtils.leftPad(String.valueOf(assortment.ig), 4) + StringUtils.center(" ", 8)
                        + StringUtils.leftPad(String.valueOf(totig), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(String.valueOf(assortment.fso), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(MainLibrary.priceDec.format(orderAmt), 11);

                if(!woosimPrinter.printText(strValues, true, false, 1)) return false;

                if(hasBarcode) {
                    String barcodeType = MainLibrary.GetBarcodeType(assortment.itembarcode);
                    if(!assortment.itembarcode.equals(""))
                        woosimPrinter.print1DBarcode(barcodeType, assortment.itembarcode);
                }

                if(!woosimPrinter.printText(" ", false, false, 1)) return false;

                if (assortment.so > 0) {
                    totsku = totsku + 1;
                }

                totfso = totfso + assortment.fso;
                totfsoval = totfsoval + (assortment.fsovalue * assortment.fso) ;
            }

            // FOOTER
            String footer = "";

            footer += Printer.woosimLines;
            footer += "Total: " + StringUtils.rightPad(String.valueOf(totsku),26/*76*/) + StringUtils.leftPad(String.valueOf(totfso),15)
                    + StringUtils.rightPad(" ", 6) + StringUtils.leftPad(MainLibrary.priceDec.format(totfsoval), 10) + "\n";
            footer += "\n" + "\n" + "\n" + "\n" + "\n";
            footer += StringUtils.center(Printer.woosimLines2, 64);
            footer += StringUtils.center("Acknowledged by", 64);
            footer += "\n" + "\n"+ "\n";

            if(!woosimPrinter.printText(footer, true, false, 1)) return false;

            result = true;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }

        return result;
    }

    private boolean PrintDetailsWoosim_4inch(boolean hasBarcode) {

        boolean result = false;
        int totsku = 0, totfso = 0;
        double totfsoval = 0;
        try {


            /*ArrayList<Assortment> Barcodes = new ArrayList<>();
            if(Filtered)
            {
                //Barcodes = filteredItems;

                Barcodes =  mAssortmentAdapter.arrAssortmentResultList;
            }
            else
            {

                Barcodes = arrAssortment;
            }*/

            //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())


            //for (Assortment assortment : mAssortmentAdapter.arrAssortmentResultList)
            //{



            for (String barcodeKey :  itemOrdered) {

               Assortment assortment = mHashmapAssortmentAll.get(barcodeKey);

                if (lprintwithso) {
                    if (assortment.so == 0) {
                        continue;
                    }
                }



                int totig = assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion);
                double orderAmt = assortment.fsovalue * assortment.fso;

                int unit = 0;
                try {
                    unit = assortment.fso / assortment.multi;
                }
                catch (Exception ex) { Log.e(TAG, ex.getMessage()); }

                String itemDesc = StringUtils.rightPad(assortment.desc + " " + assortment.barcode, 20, "");
                if(!woosimPrinter.printText(itemDesc, false, false, 1)) return false;

                String strValues = StringUtils.rightPad("", 20, "")
                        + StringUtils.leftPad(String.valueOf(assortment.ig), 4) + StringUtils.center(" ", 6)
                        + StringUtils.leftPad(String.valueOf(totig), 4) + StringUtils.center(" ", 7)
                        + StringUtils.leftPad(String.valueOf(assortment.fso), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(String.valueOf(unit), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(MainLibrary.priceDec.format(orderAmt), 10, "");

                if(!woosimPrinter.printText(strValues, true, false, 1)) return false;

                if(hasBarcode) {
                    String barcodeType = MainLibrary.GetBarcodeType(assortment.itembarcode);
                    if(!assortment.itembarcode.equals(""))
                        woosimPrinter.print1DBarcode(barcodeType, assortment.itembarcode);
                }

                if(!woosimPrinter.printText(" ", false, false, 1)) return false;

                if (assortment.so > 0) {
                    totsku = totsku + 1;
                }

                totfso = totfso + assortment.fso;
                totfsoval = totfsoval + (assortment.fsovalue * assortment.fso) ;
            }

            // FOOTER
            String footer = "";

            footer += Printer.woosimLines_4inch;
            footer += "Total: " + StringUtils.rightPad(String.valueOf(totsku),20/*76*/) + StringUtils.leftPad(String.valueOf(totfso), 18)
                    + StringUtils.rightPad(" ", 14) + StringUtils.leftPad(MainLibrary.priceDec.format(totfsoval), 10) + "\n";
            footer += "\n" + "\n" + "\n" + "\n" + "\n";
            footer += StringUtils.center(Printer.woosimLines2_4inch, 64);
            footer += StringUtils.center("Acknowledged by", 84);
            footer += "\n" + "\n"+ "\n";

            if(!woosimPrinter.printText(footer, true, false, 1)) return false;

            result = true;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }

        return result;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if(MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM)
            getMenuInflater().inflate(R.menu.menu_main_woosim, menu);
        else
            getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_check_printer:
                if (woosimPrinter != null) {
                    if (!woosimPrinter.isConnected) {
                        woosimPrinter.SetUpWoosim();
                    }
                    else Toast.makeText(AssortmentActivity.this, "Printer is already connected.", Toast.LENGTH_SHORT).show();
                }
                else Toast.makeText(AssortmentActivity.this, "Printer not connected.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_logout:
//              NavUtils.navigateUpFromSameTask(this);
                AlertDialog.Builder logoutdialog = new AlertDialog.Builder(AssortmentActivity.this);
                logoutdialog.setTitle("Log Out");
                logoutdialog.setMessage("Are you sure you want to log out?");
                logoutdialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                logoutdialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor spEditor = MainLibrary.spSettings.edit();
                        spEditor.putBoolean(getString(R.string.logged_pref_key), false);
                        spEditor.apply();

                        dialog.dismiss();
                        new UserLogout().execute();
                    }
                });

                logoutdialog.show();
                break;
            case R.id.action_submenu_category:
                FilterChanged(0);
                break;
            case R.id.action_submenu_subcateg:
                FilterChanged(1);
                break;
            case R.id.action_submenu_brand:
                FilterChanged(2);
                break;
            case R.id.action_submenu_division:
                FilterChanged(3);
                break;
            case R.id.action_submenu_withso:
                FilterChanged(4);
                break;
            case R.id.action_submenu_woso:
                FilterChanged(5);
                break;
            case R.id.action_submenu_all:
                mAssortmentAdapter.filter(0, "");
                break;
            case R.id.action_detail_summary:
                ViewReports(-1);
                break;
            case R.id.action_category_report:
                ViewReports(0);
                break;
            case R.id.action_subcate_report:
                ViewReports(1);
                break;
            case R.id.action_brand_report:
                ViewReports(2);
                break;
            case R.id.action_division_report:
                ViewReports(3);
                break;
            case R.id.action_withso_report:
                ViewReports(4);
                break;
            case R.id.action_post:
                if (!BuildConfig.DEBUG) {
                    Boolean linvalid = false;
                    for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet()) {
                        Assortment assortment = entry.getValue();
                        if (assortment.sapc == 0 && assortment.whpc == 0 && assortment.whcs == 0) {
                            linvalid = true;
                            break;
                        }
                    }
                    if (linvalid) {
                        Toast.makeText(AssortmentActivity.this, "Cannot Post Transaction.", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }

                if(MainLibrary.ValidatedRepostingDate() || !MainLibrary.ASSORT_validateDatePosting) {
                    if(MainLibrary.ASSORT_allItemsReqForPosting) {
                        new CheckRequiredItems(true).execute();
                    }
                    else {
                        Intent intent = new Intent(AssortmentActivity.this, PostingActivity.class);
                        intent.putExtra("location", MainLibrary.gSelectedStores.webStoreId);
                        intent.putExtra("datepick", MainLibrary.gStrCurrentDate);
                        startActivity(intent);
                    }
                }
                else {
                    new AlertDialog.Builder(AssortmentActivity.this)
                            .setTitle("Transaction date expired")
                            .setMessage("Current transaction date has expired.")
                            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
                break;
            case R.id.action_save_current:
                new TaskSaveData().execute();
                break;
            case R.id.action_print_all_barcode:
                if(!printEnabled) {
                    new AlertDialog.Builder(AssortmentActivity.this)
                            .setCancelable(true)
                            .setTitle("Printer")
                            .setMessage("Printer function is disabled. Please enable it in settings.")
                            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                    break;
                }
                if(MainLibrary.CheckBluetooth()) {
                    lprintwithso = false;
                    new TaskPrintData(true).execute();
                }
                else
                    Toast.makeText(AssortmentActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_all_nobarcode:
                if(!printEnabled) {
                    new AlertDialog.Builder(AssortmentActivity.this)
                            .setCancelable(true)
                            .setTitle("Printer")
                            .setMessage("Printer function is disabled. Please enable it in settings.")
                            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                    break;
                }
                if(MainLibrary.CheckBluetooth()) {
                    lprintwithso = false;
                    new TaskPrintData(false).execute();
                }
                else
                    Toast.makeText(AssortmentActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_withso_barcode:

                if(!printEnabled) {
                    new AlertDialog.Builder(AssortmentActivity.this)
                            .setCancelable(true)
                            .setTitle("Printer")
                            .setMessage("Printer function is disabled. Please enable it in settings.")
                            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                    break;
                }

                if(MainLibrary.CheckBluetooth()) {

                    if(MainLibrary.ASSORT_allItemsReqForPrinting)
                        new CheckRequiredItems(false, false, true).execute();
                    else {
                        lprintwithso = true;
                        new TaskPrintData(true).execute(); // PRINT DATA
                    }
                }
                else
                    Toast.makeText(AssortmentActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_withso_nobarcode:
                if(!printEnabled) {
                    new AlertDialog.Builder(AssortmentActivity.this)
                            .setCancelable(true)
                            .setTitle("Printer")
                            .setMessage("Printer function is disabled. Please enable it in settings.")
                            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();
                    break;
                }
                if(MainLibrary.CheckBluetooth()) {

                    if(MainLibrary.ASSORT_allItemsReqForPrinting)
                        new CheckRequiredItems(false, false, false).execute();
                    else {
                        lprintwithso = true;
                        new TaskPrintData(false).execute(); // PRINT DATA
                    }
                }
                else
                    Toast.makeText(AssortmentActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private class CheckRequiredItems extends AsyncTask<Void, Void, Boolean> {

        String strError;
        int nAllItems;
        boolean bPostMode = false;
        boolean bPrintAll = false;
        boolean bHasBarcode = false;

        CheckRequiredItems(boolean isPostMode) {
            this.bPostMode = isPostMode;
        }

        CheckRequiredItems(boolean isPostMode, boolean isPrintAll, boolean hasBarcode) {
            this.bPostMode = isPostMode;
            this.bPrintAll = isPrintAll;
            this.bHasBarcode = hasBarcode;
        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Checking required items.", true);
            strError = "";
            nAllItems = mHashmapAssortmentAll.size();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bReturn = true;

            if(hmAssortment.size() == 0) {
                bReturn = false;
                strError = "No transactions found.";
            }
            else {
                for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet()) {
                    Assortment assortment = entry.getValue();
                    if (!hmAssortment.containsKey(assortment.barcode)) {
                        bReturn = false;
                        strError = hmAssortment.size() + " / " + nAllItems + ". Some required items not transacted.";
                        break;
                    }
                }
            }

            return bReturn;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            pDL.dismiss();
            if(!aBoolean) {
                Toast.makeText(AssortmentActivity.this, strError, Toast.LENGTH_SHORT).show();
                return;
            }

            // FOR POSTING
            Intent intentpost = new Intent(AssortmentActivity.this, PostingActivity.class);
            intentpost.putExtra("location", MainLibrary.gSelectedStores.webStoreId);
            intentpost.putExtra("datepick", MainLibrary.gStrCurrentDate);

            // FOR PRINTING
            mAlertDialog = new AlertDialog.Builder(AssortmentActivity.this).create();
            mAlertDialog.setTitle("Print all items");

            if(bHasBarcode)
                mAlertDialog.setMessage("Do you want to print all items with barcode ?");
            else
                mAlertDialog.setMessage("Do you want to print all items without barcode ?");

            mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAlertDialog.dismiss();
                }
            });
            mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Print", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAlertDialog.dismiss();

                    if(!printEnabled) {
                        new AlertDialog.Builder(AssortmentActivity.this)
                                .setCancelable(true)
                                .setTitle("Printer")
                                .setMessage("Printer function is disabled. Please enable it in settings.")
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create().show();
                        return;
                    }

                    new TaskPrintData(bHasBarcode).execute();
                }
            });

            if(bPostMode) startActivity(intentpost);
            else {
                if (bPrintAll) mAlertDialog.show();
                else {
                    if(!printEnabled) {
                        new AlertDialog.Builder(AssortmentActivity.this)
                                .setCancelable(true)
                                .setTitle("Printer")
                                .setMessage("Printer function is disabled. Please enable it in settings.")
                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create().show();
                        return;
                    }

                    lprintwithso = true;

                    new TaskPrintData(bHasBarcode).execute(); // PRINT DATA
                }
            }
        }
    }

    private String PrintFormat3R(boolean hasBarcode) {

        String toPrint = "";

        numItems = 0;
        len = 0;

        toPrint += "\n";
        toPrint += "Store: " + MainLibrary.gSelectedStores.storeName + "\n";

        String osascore = "";

        osascore = MainLibrary.GetOsaScoreAssortment(mHashmapAssortmentAll);

        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n" + "\n" ;
        toPrint += StringUtils.rightPad("SKU",45,"") +
                StringUtils.rightPad("IG",14,"") +
                StringUtils.rightPad("Invty",14,"") +
                StringUtils.rightPad("Order qty", 14,"") +
                StringUtils.rightPad("Order amt", 14, "") + "\n";
        toPrint += Printer.tsclines;

        len += 1.5;

        int totsku = 0, totfso = 0;
        double totfsoval = 0;

        ArrayList<Assortment> Barcodes = new ArrayList<>();
        if(Filtered)
        {
            //Barcodes = filteredItems;

            Barcodes =  mAssortmentAdapter.arrAssortmentResultList;
        }
        else
        {

            Barcodes = arrAssortment;
        }


        //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())

       //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())
        for (Assortment assortment : Barcodes)
        {
            //Assortment assortment = entry.getValue();


        //for (String barcodeKey :  Barcodes) {

         //   Assortment assortment = mHashmapAssortmentAll.get(barcodeKey);

            if (lprintwithso) {
                if (assortment.so == 0) {
                    continue;
                }
            }

            int lensku = 50 - assortment.barcode.length();
            int lenig = 18 - String.valueOf(assortment.ig).length();
            int totig = assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion);
            int lenei = 14 - String.valueOf(totig).length();
            int lenfso = 12 - String.valueOf(assortment.fso).length(); // 18
            int lenfsoval = 12 - String.valueOf(assortment.fsovalue * assortment.fso).length(); // 18

            String barcodeType = MainLibrary.GetBarcodeType(assortment.itembarcode);
            String barcodeCmd = "";
            String endlines = "";
            if(hasBarcode) {
                barcodeCmd = "BARCODE ;\"" + barcodeType + "\",50,2,0,2,2,\"" + assortment.itembarcode + "\"" + "\n";
            }
            else {
                endlines += "\n";
            }

            toPrint += StringUtils.rightPad(assortment.desc + " " + assortment.barcode, 20, "") + "\n"
                    + barcodeCmd
                    + endlines
                    + "\nINFO ;"
                    + StringUtils.rightPad(" ", 47,"")
                    + StringUtils.rightPad(String.valueOf(assortment.ig), lenig)
                    + StringUtils.rightPad(String.valueOf(totig),lenei)
                    + "*"
                    + StringUtils.rightPad(String.valueOf(assortment.fso), lenfso, "")
                    + StringUtils.rightPad(String.format(Locale.getDefault(), "%.2f", assortment.fsovalue * assortment.fso), lenfsoval, "")
                    + "*"
                    + StringUtils.rightPad("       ", lensku,"")
                    + "\n";

            if (assortment.so > 0) {
                totsku = totsku + 1;
            }

            numItems++;
            len += 0.70;

            totfso = totfso + assortment.fso;
            totfsoval = totfsoval + (assortment.fsovalue * assortment.fso) ;
        }

        toPrint += Printer.tsclines;
        toPrint += "Total: " + StringUtils.rightPad(String.valueOf(totsku),32/*76*/) + StringUtils.rightPad(String.valueOf(totfso),11)
                + StringUtils.rightPad(String.format(Locale.getDefault(), "%.2f", totfsoval),12) + "\n";
        toPrint += "\n" + "\n" + "\n" + "\n" + "\n";
        toPrint += StringUtils.center(Printer.tsclines2,80);
        toPrint += StringUtils.center("Acknowledge by",80);

        toPrint += "\n";
        toPrint += "\n";
        toPrint += "\n";
        toPrint += "\n";
        toPrint += "\n";

        lprintwithso = false;

        len += 1.60;

        return toPrint;
    }

    private String PrintFormat4L(boolean hasBarcode) {

        len = 0;
        numItems = 0;

        String toPrint = "";

        toPrint += "\n";
        toPrint += "Store: " + MainLibrary.gSelectedStores.storeName + "\n";

        String osascore = "";

        osascore = MainLibrary.GetOsaScoreAssortment(mHashmapAssortmentAll);

        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += StringUtils.rightPad("SKU",26,"") +
                StringUtils.rightPad("IG",12,"") +
                StringUtils.rightPad("Invty",14,"") +
                StringUtils.rightPad("Final SO",14,"") +
                StringUtils.rightPad("Unit",10,"") +
                StringUtils.rightPad("Order amt", 14, "") + "\n";
        toPrint += Printer.tsclines;

        len += 1.5;

        int totsku = 0, totfso = 0;
        double totfsoval = 0;


        ArrayList<Assortment> Barcodes = new ArrayList<>();
        if(Filtered)
        {
            //Barcodes = filteredItems;

            Barcodes =  mAssortmentAdapter.arrAssortmentResultList;
        }
        else
        {

            Barcodes = arrAssortment;
        }


        for (String barcodeKey : itemOrdered) {
         Assortment assortment = mHashmapAssortmentAll.get(barcodeKey);
        //for (Assortment assortment : Barcodes)
        //{
            if (lprintwithso) {
                if (assortment.so == 0) {
                    continue;
                }
            }

            int lensku = 50 - assortment.barcode.length();
            int lenig = 15 - String.valueOf(assortment.ig).length();

            int totig = assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion);

            int lenei = 15 - String.valueOf(totig).length();
            int lenfso = 16 - String.valueOf(assortment.fso).length(); // 18

            int unit = 0;
            try {
                unit = assortment.fso / assortment.multi;
            }
            catch (Exception ex) { Log.e(TAG, ex.getMessage()); }

            int lenUnit = 14 - String.valueOf(unit).length();
            int lenfsoval = 12 - String.valueOf(assortment.fsovalue * assortment.fso).length(); // 18

            String barcodeType = MainLibrary.GetBarcodeType(assortment.itembarcode);
            String barcodeCmd = "";
            String endlines = "";
            if(hasBarcode) {
                barcodeCmd = "BARCODE ;\"" + barcodeType + "\",70,2,0,2,2,\"" + assortment.itembarcode + "\"" + "\n";
            }
            else {
                endlines += "\n";
            }

            toPrint += StringUtils.rightPad(assortment.desc + " " + assortment.barcode, 20, "") + "\n"
                    + barcodeCmd
                    + endlines
                    + "\nINFO ;"
                    + StringUtils.rightPad(" ", 29,"")
                    + StringUtils.rightPad(String.valueOf(assortment.ig), lenig, "")
                    + StringUtils.rightPad(String.valueOf(totig),lenei, "")
                    + "*"
                    + StringUtils.rightPad(" ", 12,"")
                    + StringUtils.rightPad(String.valueOf(assortment.fso), lenfso, "")
                    + StringUtils.rightPad(String.valueOf(unit), lenUnit, "")
                    + StringUtils.rightPad(MainLibrary.priceDec.format(assortment.fsovalue * assortment.fso), lenfsoval, "")
                    + "*"
                    + StringUtils.rightPad("       ", lensku,"")
                    + "\n";

            if (assortment.so > 0) {
                totsku = totsku + 1;
            }

            numItems++;
            len += 1;

            totfso = totfso + assortment.fso;
            totfsoval = totfsoval + (assortment.fsovalue * assortment.fso) ;

            if(hasBarcode) toPrint += "ENDLINE\n";
        }

        toPrint += Printer.tsclines;
        toPrint += "Total: " + StringUtils.rightPad(String.valueOf(totsku),52/*76*/) + StringUtils.rightPad(String.valueOf(totfso),25)
                + StringUtils.rightPad(MainLibrary.priceDec.format(totfsoval),12) + "\n";
        toPrint += "\n" + "\n" + "\n" + "\n" + "\n";
        toPrint += StringUtils.center(Printer.tsclines2, 70);
        toPrint += StringUtils.center("Acknowledged by", 75);

        lprintwithso = false;

        len += 1.60;

        return toPrint;
    }

    private void FilterChanged(final int filterCode) {

        final Dialog dialog = new Dialog(AssortmentActivity.this, R.style.Transparent);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.activity_branch2);

        TextView lvCaption = (TextView) dialog.findViewById(R.id.textViewBranchName);

        if (filterCode > 3) {
            mAssortmentAdapter.filter(filterCode, "xxx");
            Filtered = false;
        } else {

            final String filterId, filterTitle;

            switch (filterCode) {

                case 0:
                    filterId = "categoryid";
                    filterTitle = "Category";
                    break;
                case 1:
                    filterId = "subcategoryid";
                    filterTitle = "Subcategory";
                    break;
                case 2:
                    filterId = "brandid";
                    filterTitle = "Brand";
                    break;
                case 3:
                    filterId = "divisionid";
                    filterTitle = "Division";
                    break;
                default:
                    filterId = "";
                    filterTitle = "";

            }

            //filteredItems.clear();

            Cursor tmpCategory = db.GetGroupby(filterId, SQLiteDB.TABLE_ASSORTMENT);
          /*  int i = 0;
            while(tmpCategory.isAfterLast()) {
                String barcode = tmpCategory.getString(tmpCategory.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ASSORT_BARCODE)).trim();

                i++;
                filteredItems.add(barcode);
            }

            String sht = String.valueOf(i); */



            String[] from = new String[] {
                    filterId
            };
            int[] to = new int[] { R.id.itemTextView };

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(AssortmentActivity.this, R.layout.activity_items_filtering,
                    tmpCategory, from, to, 0);
            final ListView lv = (ListView) dialog.findViewById(R.id.listViewBranch);
            lv.setAdapter(adapter);
            String strCaption = "Select " + filterTitle;
            lvCaption.setText(strCaption);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    dialog.dismiss();

                    TextView c = (TextView) view.findViewById(R.id.itemTextView);
                    String name = c.getText().toString();
                    Toast.makeText(AssortmentActivity.this, name, Toast.LENGTH_SHORT).show();
                    mAssortmentAdapter.filter(filterCode, name);

                }
            });

            dialog.show();
            Filtered = true;

        }

    }

    private void ViewReports(int reportType) {

        Boolean lvalid = false;
        Boolean lwso = false;
        Cursor cursorGroup = null;

        final Dialog dialog = new Dialog(AssortmentActivity.this, R.style.Transparent);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        if(reportType == 4) // with so
            dialog.setContentView(R.layout.activity_report_withso);
        else
            dialog.setContentView(R.layout.activity_report);

        final TextView textViewTitle = (TextView) dialog.findViewById(R.id.textViewTitle);
        final TextView textViewColumnTitle = (TextView) dialog.findViewById(R.id.textViewColumnTitle);
        final ListView listViewReport = (ListView) dialog.findViewById(R.id.listViewReport);

        final String reportTitle, columnTitle, filterId;

        switch (reportType) {

            case 0:
                reportTitle = "Per Category Report";
                columnTitle = "Category";
                filterId = "categoryid";
                break;
            case 1:
                reportTitle = "Per Subcategory Report";
                columnTitle = "Subcategory";
                filterId = "subcategoryid";
                break;
            case 2:
                reportTitle = "Per Brand Report";
                columnTitle = "Brand";
                filterId = "brandid";
                break;
            case 3:
                reportTitle = "Per Division Report";
                columnTitle = "Division";
                filterId = "divisionid";
                break;
            case 4:
                reportTitle = "With SO Report";
                columnTitle = "With SO";
                filterId = "[desc]";
                lwso = true;
                break;
            default:
                reportTitle = "Items Summary Report";
                columnTitle = "Items";
                filterId = "[desc]";
        }

        textViewTitle.setText(reportTitle);
        textViewColumnTitle.setText(columnTitle);

        ArrayList<ReportClass> arrayListReport = new ArrayList<>();

        cursorGroup = db.queryData("select " + filterId + " as name, " + SQLiteDB.COLUMN_ASSORTMENT_BARCODE + " from " + SQLiteDB.TABLE_ASSORTMENT + " where storeid = " + String.valueOf(MainLibrary.gSelectedStores.webStoreId) +
                " group by " + filterId);

        cursorGroup.moveToFirst();

        while (!cursorGroup.isAfterLast()) {

            arrayListReport.add(new ReportClass(cursorGroup.getString(cursorGroup.getColumnIndex("name")).trim(), cursorGroup.getString(cursorGroup.getColumnIndex(SQLiteDB.COLUMN_ASSORTMENT_BARCODE)).trim()));

            cursorGroup.moveToNext();
        }

        for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet()) {
            Assortment assortment = entry.getValue();

 /*           if (reportType >= 4){
                switch (reportType){
                    case 4:
                        lvalid = pCount.sapc != 0 || pCount.whpc != 0 || pCount.whcs != 0;
                        if (!lvalid) {
                            continue;
                        }
                        break;
                }
            }*/

            for (ReportClass reportClass : arrayListReport) {

                switch (reportType) {

                    case 0:
                        if (!reportClass.name.contains(assortment.category)) {
                            continue;
                        }
                        break;
                    case 1:
                        if (!reportClass.name.contains(assortment.subcate)) {
                            continue;
                        }
                        break;
                    case 2:
                        if (!reportClass.name.contains(assortment.brand)) {
                            continue;
                        }
                        break;
                    case 3:
                        if (!reportClass.name.contains(assortment.division)) {
                            continue;
                        }
                        break;
                    case 4:
 /*                       lvalid = pCount.sapc != 0 || pCount.whpc != 0 || pCount.whcs != 0;
                        if (!lvalid) {
                            continue;
                        }*/
                        if (!reportClass.name.contains(assortment.desc)) {
                            continue;
                        }
                        break;
                    default:
                        if (!reportClass.name.contains(assortment.desc)) {
                            continue;
                        }
                }

                reportClass.ig = reportClass.ig + assortment.ig;
                reportClass.so = reportClass.so + assortment.so;
                reportClass.endinv = reportClass.endinv + (assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion));
                reportClass.finalso = reportClass.finalso + assortment.fso;
                reportClass.multi = assortment.multi;
                try {
                    reportClass.unit = assortment.conversion / assortment.fso;
                }
                catch (Exception ex) { reportClass.unit = 0; }
                reportClass.orderAmount = String.format(Locale.getDefault(), "%.2f", assortment.fsovalue * assortment.fso);
            }

        }

        ArrayList<ReportClass> arrayListReport2 = new ArrayList<>();

        if (reportType == 4){
            Iterator i = arrayListReport.iterator();
            while(i.hasNext()){
                ReportClass reportClass = (ReportClass) i.next();
                if (reportClass.so > 0){
//                    reportClass.finalso = reportClass.finalso; //- (reportClass.so % reportClass.multi) + reportClass.multi;
                    arrayListReport2.add(reportClass);
                }else{
                    i.remove();
                }
            }
        }
//        else{
//           for (ReportClass reportClass : arrayListReport) {
//                if (reportClass.so > 0) reportClass.finalso = reportClass.so - (reportClass.so % reportClass.multi) + reportClass.multi;
//            }
//        }

        /*if (reportType >= 4) {
            ArrayList<ReportClass> arrayListReport2 = new ArrayList<>();
            for (ReportClass reportClass : arrayListReport) {
                switch (reportType) {
                    case 4:
                        if (reportClass.so != 0) arrayListReport2.add(reportClass);
                        break;
                }
            }
            listViewReport.setAdapter(new ReportListViewAdapter(PCountActivity.this, arrayListReport2));
        } else {
            listViewReport.setAdapter(new ReportListViewAdapter(PCountActivity.this, arrayListReport));
        }

        dialog.show();*/


        if ((reportType == 4)) {
            listViewReport.setAdapter(new ReportWithSoAdapter(AssortmentActivity.this, arrayListReport2));
        }else{
            listViewReport.setAdapter(new ReportListViewAdapter(AssortmentActivity.this, arrayListReport));
        }

        dialog.show();

    }

    private class UserLogout extends AsyncTask<Void, Void, Boolean> {

        String response;
        String errmsg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDL = ProgressDialog.show(AssortmentActivity.this, "", "Logging out. Please Wait...", true);
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
                try{
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
                }
                catch (MalformedURLException mex) {
                    mex.printStackTrace();
                    Log.e("MalformedURLException", mex.getMessage());
                    errmsg += "\n" + mex.getMessage();
                }

            } catch(Exception e){
                e.printStackTrace();
                Log.e("Exception", e.getMessage(), e);
                errmsg += "\n" + e.getMessage();
            }
            return bReturn;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            pDL.dismiss();
            Intent intentMain = new Intent(AssortmentActivity.this, MainActivity.class);
            if(!success) {
                //Toast.makeText(AssortmentActivity.this, errmsg, Toast.LENGTH_LONG).show();
                startActivity(intentMain);
                MainLibrary.selContext.finish();
                finish();
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

                MainLibrary.selContext.finish();
                Toast.makeText(AssortmentActivity.this, msg, Toast.LENGTH_SHORT).show();
                startActivity(intentMain);
                finish();
            }
            catch (JSONException jex) {
                jex.printStackTrace();
                Log.e("JSONException", jex.getMessage());
            }

        }
    }
}
