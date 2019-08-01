package com.chasetech.pcount.Promo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Printer;
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

import com.chasetech.pcount.Assortment.Assortment;
import com.chasetech.pcount.Assortment.AssortmentActivity;
import com.chasetech.pcount.Assortment.AssortmentAdapter;
import com.chasetech.pcount.Assortment.AssortmentViewHolder;
import com.chasetech.pcount.BuildConfig;
import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.MainActivity;
import com.chasetech.pcount.PostingActivity;
import com.chasetech.pcount.R;
import com.chasetech.pcount.TSC.BPrinter;
import com.chasetech.pcount.Woosim.WoosimPrinter;
import com.chasetech.pcount.adapter.ReportListViewAdapter;
import com.chasetech.pcount.adapter.ReportWithSoAdapter;
import com.chasetech.pcount.database.SQLLib;
import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.library.MainLibrary;
import com.chasetech.pcount.library.ReportClass;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class PromoActivity extends AppCompatActivity {


    private String selectedPrinter = "";

    private ArrayList<Promo> arrPromo = new ArrayList<>();
    private HashMap<String, Promo> mHashmapPromoAll = new HashMap<>();
    private HashMap<String, Promo> hmPromo = new HashMap<>();
    private Promo promoUpdated = null;
    ProgressDialog pDL;
    private SQLLib db;
    private ArrayList<String> itemOrdered;
    private PromoAdapter mPromoAdapter;
    private ListView lvwPromo = null;
    private TextView tvwTotalItems;
    private WoosimPrinter woosimPrinter = null;
    private boolean printEnabled;
    private Boolean Filtered = false;
    private Boolean lprintwithso = false;
    private BPrinter Printer;
    private EditText editTextSearch = null;

    private AlertDialog mAlertDialog;

    private double len  = 0;
    private int numItems = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo);
        db = new SQLLib(PromoActivity.this);


        getSupportActionBar().setTitle(MainLibrary.gSelectedStores.storeName + " - PROMO");
        lvwPromo = (ListView) findViewById(R.id.lvwPromo);
        tvwTotalItems = (TextView) findViewById(R.id.tvwTotalItems);
        editTextSearch = (EditText) findViewById(R.id.enter_search);

        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(this, MainLibrary.errlogFile));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selectedPrinter = prefs.getString("printer_list", "2");

        new TaskProcessData().execute();


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
                mPromoAdapter.filter(99, search);
            }
        });

        lvwPromo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PromoViewHolder viewHolder = (PromoViewHolder) view.getTag();

                final Promo promo = viewHolder.Promo;

                final Dialog dialog = new Dialog(PromoActivity.this, R.style.Transparent);
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

                textViewDesc.setText(promo.desc);
                editTextPcs.setText("");
                editTextWhPcs.setText("");
                editTextWhCs.setText("");

                if (promo.sapc != 0 || promo.whpc != 0 || promo.whcs !=0 ) {
                    editTextPcs.setText(String.valueOf(promo.sapc));
                    editTextWhPcs.setText(String.valueOf(promo.whpc));
                    editTextWhCs.setText(String.valueOf(promo.whcs));
                }

                btnQty.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();

                        promoUpdated = null;

                        String inputPcs = editTextPcs.getText().toString();
                        String inputWhPcs = editTextWhPcs.getText().toString();
                        String inputWhcs = editTextWhCs.getText().toString();
                        String inputIg = txtInventorygoal.getText().toString().trim();


                        if (inputPcs.isEmpty()) {
                            inputPcs = "0";
                        }
                        if (inputWhPcs.isEmpty()) {
                            inputWhPcs = "0";
                        }
                        if (inputWhcs.isEmpty()) {
                            inputWhcs = "0";
                        }

                        int so = promo.ig - Integer.parseInt(inputPcs) - Integer.parseInt(inputWhPcs) - (Integer.parseInt(inputWhcs) * promo.conversion);;


                        int fso = 0;

                        if ((so % promo.multi) == 0) {
                            fso = so;
                        }
                        else{
                            fso = so - (so % promo.multi) + promo.multi;
                        }

                        if (so <= 0) {    //10/27 for negative values
                            so = 0;
                            fso = 0;
                        }

                        promo.sapc = Integer.parseInt(inputPcs);
                        promo.whpc = Integer.parseInt(inputWhPcs);
                        promo.whcs = Integer.parseInt(inputWhcs);
                        promo.so = so;
                        promo.fso = fso;
                        promo.updated = true;


                        Promo promoAll = mHashmapPromoAll.get(promo.id);
                        if(promoAll != null) {
                            promoAll.sapc = Integer.parseInt(inputPcs);
                            promoAll.whpc = Integer.parseInt(inputWhPcs);
                            promoAll.whcs = Integer.parseInt(inputWhcs);
                            promoAll.so = so;
                            promoAll.fso = fso;
                            //assortmentAll.ig = Integer.parseInt(inputIg);
                            promoAll.updated = true;
                            mHashmapPromoAll.put(promo.barcode, promoAll);
                        }

                        promoUpdated = promo;

                        new TaskSaveData().execute();
                        mPromoAdapter.notifyDataSetChanged();
                    }
                });

                dialog.show();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PromoActivity.this);
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
                woosimPrinter = new WoosimPrinter(PromoActivity.this);
                woosimPrinter.SetUpWoosim();
            }
        }
        catch (Exception ex) {
            String errmsg = "Can't connect to printer. Please check printer.";
            String errException = ex.getMessage() != null ? ex.getMessage() : errmsg;
            MainLibrary.errorLog.appendLog(errException, "DEBUGGING");
            Toast.makeText(PromoActivity.this, "Failed Connection: " + errmsg, Toast.LENGTH_LONG).show();
        }
    }

    private class TaskProcessData extends AsyncTask<Void, String, Boolean> {

        private String errorMessage;
        private boolean itemsEqual = false;

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(PromoActivity.this, "", "Updating Masterfile. Please wait.", true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            boolean result = false;
            itemsEqual = false;

            try {
                arrPromo.clear();
                mHashmapPromoAll.clear();

                String strTransQuery = "SELECT * FROM " + SQLiteDB.TABLE_TRANSACTION_PROMO + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_PROMO_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "' AND " + SQLiteDB.COLUMN_TRANSACTION_PROMO_DATE + " = '" + MainLibrary.gStrCurrentDate + "' AND " + SQLiteDB.COLUMN_TRANSACTION_PROMO_USERID + " = '" + MainLibrary.gStrCurrentUserID + "'";
                String strItemsQuery = "SELECT * FROM " + SQLiteDB.TABLE_PROMO + " WHERE " + SQLiteDB.COLUMN_PROMO_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "'";

                Cursor cursTrans = db.queryData(strTransQuery);
                cursTrans.moveToFirst();

                Cursor cursItems = db.queryData(strItemsQuery);
                cursItems.moveToFirst();


                if (cursTrans.getCount() == 0) {

                    int nItemsTotal = cursItems.getCount();

                    if (nItemsTotal == 0) {
                        errorMessage = "No items found in this store.";
                        itemsEqual = true;
                        return false;
                    }

                    while (!cursItems.isAfterLast()) {

                        int id = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_ID));
                        int ig = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_IG));
                        String barcode = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_BARCODE)).trim();
                        String sapc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_SAPC)).trim();
                        String whpc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_WHPC)).trim();
                        String whcs = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_WHCS)).trim();
                        String so = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_SO)).trim();
                        String fso = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_FSO)).trim();
                        String desc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_DESC)).trim();
                        String category = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_CATEGORY)).trim();
                        String descLong = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_DESCLONG)).trim();
                        String categoryid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_CATEGORYID)).trim();
                        String brandid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_BRANDID)).trim();
                        String divisionid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_DIVISIONID)).trim();
                        String subcategoryid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_SUBCATEGORYID)).trim();
                        int conversion = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_CONVERSION));
                        double fsovalue = cursItems.getDouble(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_FSOVALUE));
                        int webid = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_WEBID));
                        int multi = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_MULTI));
                        String otherBarcode = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_OTHERBARCODE));
                        int minstock = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PROMO_MINSTOCK));

                        Promo promoItem = new Promo(
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

                        arrPromo.add(promoItem);
                        mHashmapPromoAll.put(promoItem.barcode.trim(), promoItem);

                        String[] aFields = new String[]{
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_DATE,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_STOREID,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_BARCODE,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_IG,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_SAPC,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_WHPC,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_WHCS,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_CONVERSION,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_SO,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_FSO,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_FSOVALUE,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_LPOSTED,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_WEBID,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_USERID,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_MULTI,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_OTHERBARCODE,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_MINSTOCK,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_CATEGORY,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_DESCLONG,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_DESC,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_CATEGORYID,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_BRANDID,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_DIVISIONID,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_SUBCATEGORYID
                        };

                        String[] aValues = new String[]{
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

                        db.AddRecord(SQLiteDB.TABLE_TRANSACTION_PROMO, aFields, aValues);
                        cursItems.moveToNext();
                    }

                    if (nItemsTotal != arrPromo.size()) {
                        errorMessage = "Items copied are not equal. Please reload items. Do you want to proceed?";
                        itemsEqual = false;
                        return false;
                    }

                    itemsEqual = true;
                    result = true;

                    cursItems.close();
                } else {

                    while (!cursTrans.isAfterLast()) {

                        int id = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_ID));
                        int ig = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_IG));
                        String barcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_BARCODE)).trim();
                        String desc = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_DESC)).trim();
                        String categoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_CATEGORYID)).trim();
                        String brandid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_BRANDID)).trim();
                        String divisionid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_DIVISIONID)).trim();
                        String subcategoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_SUBCATEGORYID)).trim();
                        int conversion = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_CONVERSION));
                        double fsovalue = cursTrans.getDouble(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_FSOVALUE));
                        int webid = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_WEBID));
                        int multi = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_MULTI));
                        String otherBarcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_OTHERBARCODE));
                        int minstock = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_MINSTOCK));
                        int sapc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_SAPC));
                        int whpc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_WHPC));
                        int whcs = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_WHCS));
                        int so = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_SO));
                        int fso = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_FSO));
                        boolean isUpdated = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_PROMO_UPDATED)) == 1;

                        Promo promoItem = new Promo(
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
                            promoItem.sapc = sapc;
                            promoItem.whpc = whpc;
                            promoItem.whcs = whcs;
                            promoItem.so = so;
                            promoItem.fso = fso;
                            promoItem.updated = true;
                            hmPromo.put(promoItem.barcode, promoItem);
                        }

                        arrPromo.add(promoItem);
                        mHashmapPromoAll.put(promoItem.barcode.trim(), promoItem);
                        cursTrans.moveToNext();
                    }

                    result = true;
                }

                cursTrans.close();

                itemOrdered = new ArrayList<>(mHashmapPromoAll.keySet()); // barcodes.

                if (MainLibrary.gSelectedStores.channelArea.equals("MDC")) {
                    Collections.sort(itemOrdered, new Comparator<String>() {
                        @Override
                        public int compare(String lhs, String rhs) {
                            return Integer.parseInt(lhs) - Integer.parseInt(rhs);
                        }
                    });
                }


            } catch (Exception e) {
                errorMessage = "Error in loading items. Please send error report";
                String errmsg = e.getMessage() != null ? e.getMessage() : errorMessage;
                MainLibrary.errorLog.appendLog(errmsg, "DEBUGGING");
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            pDL.dismiss();

            if (!aBoolean) {
                AlertDialog promptDialog = new AlertDialog.Builder(PromoActivity.this).create();
                promptDialog.setCancelable(false);
                promptDialog.setTitle("Loading Items");

                String msg = errorMessage;

                if (!itemsEqual) {

                    String strTransDeleteQuery = "DELETE FROM " + SQLiteDB.TABLE_TRANSACTION_PROMO
                            + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_PROMO_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId)
                            + "' AND " + SQLiteDB.COLUMN_TRANSACTION_PROMO_DATE + " = '" + MainLibrary.gStrCurrentDate + "'";

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
                } else {
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

            mPromoAdapter = new PromoAdapter(PromoActivity.this, arrPromo);
            lvwPromo.setAdapter(mPromoAdapter);
            mPromoAdapter.notifyDataSetChanged();

            String totalItemMsg = "Total Items: " + String.valueOf(hmPromo.size()) + " / " + String.valueOf(arrPromo.size());
            tvwTotalItems.setText(totalItemMsg);
        }
    }

    private class TaskSaveData extends AsyncTask<String, Void, Boolean> {

        private String errmsg;

        @Override
        protected Boolean doInBackground(String... params) {

            boolean result = false;

            try {

                if (promoUpdated != null) {

                    Promo promo = promoUpdated;

                    if (promo.sapc != 0 || promo.whpc != 0 || promo.whcs != 0 || promo.fso != 0 || promo.updated) {

                        Cursor cursTrans = db.GetDataCursor(SQLiteDB.TABLE_TRANSACTION_PROMO,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_BARCODE + " = '" + promo.barcode + "' AND "
                                        + SQLiteDB.COLUMN_TRANSACTION_PROMO_DATE + " = '" + MainLibrary.gStrCurrentDate + "' AND "
                                        + SQLiteDB.COLUMN_TRANSACTION_PROMO_STOREID + " = '" + MainLibrary.gSelectedStores.webStoreId + "' AND "
                                        + SQLiteDB.COLUMN_TRANSACTION_PROMO_USERID + " = '" + MainLibrary.gStrCurrentUserID + "'");
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
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_MONTH,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_LPOSTED,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_UPDATED
                        };

                        String[] avalues = {MainLibrary.gStrCurrentDate
                                , String.valueOf(MainLibrary.gSelectedStores.webStoreId)
                                , promo.barcode
                                , String.valueOf(promo.ig)
                                , String.valueOf(promo.sapc)
                                , String.valueOf(promo.whpc)
                                , String.valueOf(promo.whcs)
                                , String.valueOf(promo.conversion)
                                , String.valueOf(promo.so)
                                , String.valueOf(promo.fso)
                                , String.valueOf(promo.fsovalue)
                                , String.valueOf(promo.webid)
                                , String.valueOf(MainLibrary.gStrCurrentUserID)
                                , String.valueOf(promo.multi)
                                , MainLibrary.selectedMonth.trim()
                                , "0"
                                , "1"
                        };

                        hmPromo.put(promo.barcode, promo);

                        if (cursTrans.getCount() == 0) {
                            db.AddRecord(SQLiteDB.TABLE_TRANSACTION_PROMO, afields, avalues);
                            return true;
                        }

                        String[] whereFields = new String[]{
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_BARCODE,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_DATE,
                                SQLiteDB.COLUMN_TRANSACTION_PROMO_STOREID,
                        };
                        String[] whereValues = new String[]{
                                promo.barcode,
                                MainLibrary.gStrCurrentDate,
                                String.valueOf(MainLibrary.gSelectedStores.webStoreId).trim(),
                        };

                        db.UpdateRecord(SQLiteDB.TABLE_TRANSACTION_PROMO, whereFields, whereValues, afields, avalues, PromoActivity.this);

                        cursTrans.close();
                    }
                    result = true;
                }
                else errmsg = "Data error.";
            }
            catch (Exception ex) {
                errmsg = "Saving transaction failed. Please send error to dev team.";
                String errException = ex.getMessage() != null ? ex.getMessage() : errmsg;
                MainLibrary.errorLog.appendLog(errException, "DEBUGGING");
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean bResult) {
            pDL.dismiss();
            if(!bResult) {
                Toast.makeText(PromoActivity.this, errmsg, Toast.LENGTH_SHORT).show();
                return;
            }

            mPromoAdapter.notifyDataSetChanged();
            lvwPromo.requestLayout();

            String totalItemMsg = "Total Items: " + String.valueOf(hmPromo.size()) + " / " + String.valueOf(arrPromo.size());
            tvwTotalItems.setText(totalItemMsg);
        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(PromoActivity.this, "", "Saving Transaction dated " + MainLibrary.gStrCurrentDate + ". Please Wait...", true);
        }
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
                    } else
                        Toast.makeText(PromoActivity.this, "Printer is already connected.", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(PromoActivity.this, "Printer not connected.", Toast.LENGTH_SHORT).show();
                break;

            case R.id.action_logout:
                AlertDialog.Builder logoutdialog = new AlertDialog.Builder(PromoActivity.this);
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
                mPromoAdapter.filter(0, "");
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
                    for (HashMap.Entry<String, Promo> entry : mHashmapPromoAll.entrySet()) {
                        Promo promo = entry.getValue();
                        if (promo.sapc == 0 && promo.whpc == 0 && promo.whcs == 0) {
                            linvalid = true;
                            break;
                        }
                    }
                    if (linvalid) {
                        Toast.makeText(PromoActivity.this, "Cannot Post Transaction.", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }

                if(MainLibrary.ValidatedRepostingDate() || !MainLibrary.PROMO_validateDatePosting) {
                    if(MainLibrary.PROMO_allItemsReqForPosting) {
                        new CheckRequiredItems(true).execute();
                    }
                    else {
                        Intent intent = new Intent(PromoActivity.this, PostingActivity.class);
                        intent.putExtra("location", MainLibrary.gSelectedStores.webStoreId);
                        intent.putExtra("datepick", MainLibrary.gStrCurrentDate);
                        startActivity(intent);
                    }
                }
                else {
                    new AlertDialog.Builder(PromoActivity.this)
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
                    new AlertDialog.Builder(PromoActivity.this)
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
                    Toast.makeText(PromoActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;

            case R.id.action_print_all_nobarcode:
                if(!printEnabled) {
                    new AlertDialog.Builder(PromoActivity.this)
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
                    Toast.makeText(PromoActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_withso_barcode:

                if(!printEnabled) {
                    new AlertDialog.Builder(PromoActivity.this)
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

                    if(MainLibrary.PROMO_allItemsReqForPrinting)
                        new CheckRequiredItems(false, false, true).execute();
                    else {
                        lprintwithso = true;
                        new TaskPrintData(true).execute(); // PRINT DATA
                    }
                }
                else
                    Toast.makeText(PromoActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_withso_nobarcode:
                if(!printEnabled) {
                    new AlertDialog.Builder(PromoActivity.this)
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

                    if(MainLibrary.PROMO_allItemsReqForPrinting)
                        new CheckRequiredItems(false, false, false).execute();
                    else {
                        lprintwithso = true;
                        new TaskPrintData(false).execute(); // PRINT DATA
                    }
                }
                else
                    Toast.makeText(PromoActivity.this, "Please enable yur bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;


            case android.R.id.home:
                finish();
                break;


        }
        return super.onOptionsItemSelected(item);
    }


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
                        Log.e("DEBUGGING", ex.getMessage());
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
                Toast.makeText(PromoActivity.this, "Error Printing. " + errmsg, Toast.LENGTH_SHORT).show();
                return;
            }


            AlertDialog printdialog = new AlertDialog.Builder(PromoActivity.this).create();
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
            pDL = ProgressDialog.show(PromoActivity.this, "", "Printing. Please Wait...", true);
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

        osascore = MainLibrary.GetOsaScorePromo(mHashmapPromoAll);

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
                    Toast.makeText(PromoActivity.this, "Printer connection is interrupted. Print cancelled.", Toast.LENGTH_SHORT).show();
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

        osascore = MainLibrary.GetOsaScorePromo(mHashmapPromoAll);

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
                    Toast.makeText(PromoActivity.this, "Printer connection is interrupted. Print cancelled.", Toast.LENGTH_SHORT).show();
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

            ArrayList<Promo> Barcodes = new ArrayList<>();
            if(Filtered)
            {
                //Barcodes = filteredItems;

                Barcodes =  mPromoAdapter.arrPromoResultList;
            }
            else
            {

                Barcodes = arrPromo;
            }


            //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())
            for (Promo promo : Barcodes)
            {

                // for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet()) {
                //   Assortment assortment = entry.getValue();
                //for (String barcodeKey :  Barcodes) {

                //  Assortment assortment = mHashmapAssortmentAll.get(barcodeKey);

                if (lprintwithso) {
                    if (promo.so == 0) {
                        continue;
                    }
                }

                int totig = promo.sapc + promo.whpc + (promo.whcs * promo.conversion);
                double orderAmt = promo.fsovalue * promo.fso;


                String itemDesc = StringUtils.rightPad(promo.desc + " " + promo.barcode, 20, "");
                if(!woosimPrinter.printText(itemDesc, false, false, 1)) return false;

                String strValues = StringUtils.rightPad("", 23, "")
                        + StringUtils.leftPad(String.valueOf(promo.ig), 4) + StringUtils.center(" ", 8)
                        + StringUtils.leftPad(String.valueOf(totig), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(String.valueOf(promo.fso), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(MainLibrary.priceDec.format(orderAmt), 11);

                if(!woosimPrinter.printText(strValues, true, false, 1)) return false;

                if(hasBarcode) {
                    String barcodeType = MainLibrary.GetBarcodeType(promo.itembarcode);
                    if(!promo.itembarcode.equals(""))
                        woosimPrinter.print1DBarcode(barcodeType, promo.itembarcode);
                }

                if(!woosimPrinter.printText(" ", false, false, 1)) return false;

                if (promo.so > 0) {
                    totsku = totsku + 1;
                }

                totfso = totfso + promo.fso;
                totfsoval = totfsoval + (promo.fsovalue * promo.fso) ;
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
            Log.e("DEBUGGING", ex.getMessage());
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

                Promo promo = mHashmapPromoAll.get(barcodeKey);

                if (lprintwithso) {
                    if (promo.so == 0) {
                        continue;
                    }
                }



                int totig = promo.sapc + promo.whpc + (promo.whcs * promo.conversion);
                double orderAmt = promo.fsovalue * promo.fso;

                int unit = 0;
                try {
                    unit = promo.fso / promo.multi;
                }
                catch (Exception ex) { Log.e("DEBUGGING", ex.getMessage()); }

                String itemDesc = StringUtils.rightPad(promo.desc + " " + promo.barcode, 20, "");
                if(!woosimPrinter.printText(itemDesc, false, false, 1)) return false;

                String strValues = StringUtils.rightPad("", 20, "")
                        + StringUtils.leftPad(String.valueOf(promo.ig), 4) + StringUtils.center(" ", 6)
                        + StringUtils.leftPad(String.valueOf(totig), 4) + StringUtils.center(" ", 7)
                        + StringUtils.leftPad(String.valueOf(promo.fso), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(String.valueOf(unit), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(MainLibrary.priceDec.format(orderAmt), 10, "");

                if(!woosimPrinter.printText(strValues, true, false, 1)) return false;

                if(hasBarcode) {
                    String barcodeType = MainLibrary.GetBarcodeType(promo.itembarcode);
                    if(!promo.itembarcode.equals(""))
                        woosimPrinter.print1DBarcode(barcodeType, promo.itembarcode);
                }

                if(!woosimPrinter.printText(" ", false, false, 1)) return false;

                if (promo.so > 0) {
                    totsku = totsku + 1;
                }

                totfso = totfso + promo.fso;
                totfsoval = totfsoval + (promo.fsovalue * promo.fso) ;
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
            Log.e("DEBUGGING", ex.getMessage());
        }

        return result;
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
            pDL = ProgressDialog.show(PromoActivity.this, "", "Checking required items.", true);
            strError = "";
            nAllItems = mHashmapPromoAll.size();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bReturn = true;

            if(hmPromo.size() == 0) {
                bReturn = false;
                strError = "No transactions found.";
            }
            else {
                for (HashMap.Entry<String, Promo> entry : mHashmapPromoAll.entrySet()) {
                    Promo promo = entry.getValue();
                    if (!hmPromo.containsKey(promo.barcode)) {
                        bReturn = false;
                        strError = hmPromo.size() + " / " + nAllItems + ". Some required items not transacted.";
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
                Toast.makeText(PromoActivity.this, strError, Toast.LENGTH_SHORT).show();
                return;
            }

            // FOR POSTING
            Intent intentpost = new Intent(PromoActivity.this, PostingActivity.class);
            intentpost.putExtra("location", MainLibrary.gSelectedStores.webStoreId);
            intentpost.putExtra("datepick", MainLibrary.gStrCurrentDate);

            // FOR PRINTING
            mAlertDialog = new AlertDialog.Builder(PromoActivity.this).create();
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
                        new AlertDialog.Builder(PromoActivity.this)
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

//                    new TaskPrintData(bHasBarcode).execute();
                }
            });

            if(bPostMode) startActivity(intentpost);
            else {
                if (bPrintAll) mAlertDialog.show();
                else {
                    if(!printEnabled) {
                        new AlertDialog.Builder(PromoActivity.this)
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

//                    new TaskPrintData(bHasBarcode).execute(); // PRINT DATA
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

        osascore = MainLibrary.GetOsaScorePromo(mHashmapPromoAll);

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

        ArrayList<Promo> Barcodes = new ArrayList<>();
        if(Filtered)
        {
            //Barcodes = filteredItems;

            Barcodes =  mPromoAdapter.arrPromoResultList;
        }
        else
        {

            Barcodes = arrPromo;
        }


        //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())

        //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())
        for (Promo promo : Barcodes)
        {
            //Assortment assortment = entry.getValue();


            //for (String barcodeKey :  Barcodes) {

            //   Assortment assortment = mHashmapAssortmentAll.get(barcodeKey);

            if (lprintwithso) {
                if (promo.so == 0) {
                    continue;
                }
            }

            int lensku = 50 - promo.barcode.length();
            int lenig = 18 - String.valueOf(promo.ig).length();
            int totig = promo.sapc + promo.whpc + (promo.whcs * promo.conversion);
            int lenei = 14 - String.valueOf(totig).length();
            int lenfso = 12 - String.valueOf(promo.fso).length(); // 18
            int lenfsoval = 12 - String.valueOf(promo.fsovalue * promo.fso).length(); // 18

            String barcodeType = MainLibrary.GetBarcodeType(promo.itembarcode);
            String barcodeCmd = "";
            String endlines = "";
            if(hasBarcode) {
                barcodeCmd = "BARCODE ;\"" + barcodeType + "\",50,2,0,2,2,\"" + promo.itembarcode + "\"" + "\n";
            }
            else {
                endlines += "\n";
            }

            toPrint += StringUtils.rightPad(promo.desc + " " + promo.barcode, 20, "") + "\n"
                    + barcodeCmd
                    + endlines
                    + "\nINFO ;"
                    + StringUtils.rightPad(" ", 47,"")
                    + StringUtils.rightPad(String.valueOf(promo.ig), lenig)
                    + StringUtils.rightPad(String.valueOf(totig),lenei)
                    + "*"
                    + StringUtils.rightPad(String.valueOf(promo.fso), lenfso, "")
                    + StringUtils.rightPad(String.format(Locale.getDefault(), "%.2f", promo.fsovalue * promo.fso), lenfsoval, "")
                    + "*"
                    + StringUtils.rightPad("       ", lensku,"")
                    + "\n";

            if (promo.so > 0) {
                totsku = totsku + 1;
            }

            numItems++;
            len += 0.70;

            totfso = totfso + promo.fso;
            totfsoval = totfsoval + (promo.fsovalue * promo.fso) ;
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

        osascore = MainLibrary.GetOsaScorePromo(mHashmapPromoAll);

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


        ArrayList<Promo> Barcodes = new ArrayList<>();
        if(Filtered)
        {
            //Barcodes = filteredItems;

            Barcodes =  mPromoAdapter.arrPromoResultList;
        }
        else
        {

            Barcodes = arrPromo;
        }


        for (String barcodeKey : itemOrdered) {
            Promo promo = mHashmapPromoAll.get(barcodeKey);
            //for (Assortment assortment : Barcodes)
            //{
            if (lprintwithso) {
                if (promo.so == 0) {
                    continue;
                }
            }

            int lensku = 50 - promo.barcode.length();
            int lenig = 15 - String.valueOf(promo.ig).length();

            int totig = promo.sapc + promo.whpc + (promo.whcs * promo.conversion);

            int lenei = 15 - String.valueOf(totig).length();
            int lenfso = 16 - String.valueOf(promo.fso).length(); // 18

            int unit = 0;
            try {
                unit = promo.fso / promo.multi;
            }
            catch (Exception ex) { Log.e("DEBUGGING", ex.getMessage()); }

            int lenUnit = 14 - String.valueOf(unit).length();
            int lenfsoval = 12 - String.valueOf(promo.fsovalue * promo.fso).length(); // 18

            String barcodeType = MainLibrary.GetBarcodeType(promo.itembarcode);
            String barcodeCmd = "";
            String endlines = "";
            if(hasBarcode) {
                barcodeCmd = "BARCODE ;\"" + barcodeType + "\",70,2,0,2,2,\"" + promo.itembarcode + "\"" + "\n";
            }
            else {
                endlines += "\n";
            }

            toPrint += StringUtils.rightPad(promo.desc + " " + promo.barcode, 20, "") + "\n"
                    + barcodeCmd
                    + endlines
                    + "\nINFO ;"
                    + StringUtils.rightPad(" ", 29,"")
                    + StringUtils.rightPad(String.valueOf(promo.ig), lenig, "")
                    + StringUtils.rightPad(String.valueOf(totig),lenei, "")
                    + "*"
                    + StringUtils.rightPad(" ", 12,"")
                    + StringUtils.rightPad(String.valueOf(promo.fso), lenfso, "")
                    + StringUtils.rightPad(String.valueOf(unit), lenUnit, "")
                    + StringUtils.rightPad(MainLibrary.priceDec.format(promo.fsovalue * promo.fso), lenfsoval, "")
                    + "*"
                    + StringUtils.rightPad("       ", lensku,"")
                    + "\n";

            if (promo.so > 0) {
                totsku = totsku + 1;
            }

            numItems++;
            len += 1;

            totfso = totfso + promo.fso;
            totfsoval = totfsoval + (promo.fsovalue * promo.fso) ;

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


    private void ViewReports(int reportType) {

        Boolean lvalid = false;
        Boolean lwso = false;
        Cursor cursorGroup = null;

        final Dialog dialog = new Dialog(PromoActivity.this, R.style.Transparent);
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

        cursorGroup = db.queryData("select " + filterId + " as name, " + SQLiteDB.COLUMN_PROMO_BARCODE + " from " + SQLiteDB.TABLE_PROMO + " where storeid = " + String.valueOf(MainLibrary.gSelectedStores.webStoreId) +
                " group by " + filterId);

        cursorGroup.moveToFirst();

        while (!cursorGroup.isAfterLast()) {

            arrayListReport.add(new ReportClass(cursorGroup.getString(cursorGroup.getColumnIndex("name")).trim(), cursorGroup.getString(cursorGroup.getColumnIndex(SQLiteDB.COLUMN_PROMO_BARCODE)).trim()));

            cursorGroup.moveToNext();
        }

        for (HashMap.Entry<String, Promo> entry : mHashmapPromoAll.entrySet()) {
            Promo promo = entry.getValue();


            for (ReportClass reportClass : arrayListReport) {

                switch (reportType) {

                    case 0:
                        if (!reportClass.name.contains(promo.category)) {
                            continue;
                        }
                        break;
                    case 1:
                        if (!reportClass.name.contains(promo.subcate)) {
                            continue;
                        }
                        break;
                    case 2:
                        if (!reportClass.name.contains(promo.brand)) {
                            continue;
                        }
                        break;
                    case 3:
                        if (!reportClass.name.contains(promo.division)) {
                            continue;
                        }
                        break;
                    case 4:
 /*                       lvalid = pCount.sapc != 0 || pCount.whpc != 0 || pCount.whcs != 0;
                        if (!lvalid) {
                            continue;
                        }*/
                        if (!reportClass.name.contains(promo.desc)) {
                            continue;
                        }
                        break;
                    default:
                        if (!reportClass.name.contains(promo.desc)) {
                            continue;
                        }
                }

                reportClass.ig = reportClass.ig + promo.ig;
                reportClass.so = reportClass.so + promo.so;
                reportClass.endinv = reportClass.endinv + (promo.sapc + promo.whpc + (promo.whcs * promo.conversion));
                reportClass.finalso = reportClass.finalso + promo.fso;
                reportClass.multi = promo.multi;
                try {
                    reportClass.unit = promo.conversion / promo.fso;
                }
                catch (Exception ex) { reportClass.unit = 0; }
                reportClass.orderAmount = String.format(Locale.getDefault(), "%.2f", promo.fsovalue * promo.fso);
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

        if ((reportType == 4)) {
            listViewReport.setAdapter(new ReportWithSoAdapter(PromoActivity.this, arrayListReport2));
        }else{
            listViewReport.setAdapter(new ReportListViewAdapter(PromoActivity.this, arrayListReport));
        }

        dialog.show();

    }


    private void FilterChanged(final int filterCode) {

        final Dialog dialog = new Dialog(PromoActivity.this, R.style.Transparent);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.activity_branch2);



        TextView lvCaption = (TextView) dialog.findViewById(R.id.textViewBranchName);

        if (filterCode > 3) {
            mPromoAdapter.filter(filterCode, "xxx");
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

            Cursor tmpCategory = db.GetGroupby(filterId, SQLiteDB.TABLE_PROMO);
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

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(PromoActivity.this, R.layout.activity_items_filtering,
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
                    Toast.makeText(PromoActivity.this, name, Toast.LENGTH_SHORT).show();
                    mPromoAdapter.filter(filterCode, name);

                }
            });

            dialog.show();
            Filtered = true;

        }

    }

    private class UserLogout extends AsyncTask<Void, Void, Boolean> {

        String response;
        String errmsg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDL = ProgressDialog.show(PromoActivity.this, "", "Logging out. Please Wait...", true);
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
            Intent intentMain = new Intent(PromoActivity.this, MainActivity.class);
            if(!success) {
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
                Toast.makeText(PromoActivity.this, msg, Toast.LENGTH_SHORT).show();
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
