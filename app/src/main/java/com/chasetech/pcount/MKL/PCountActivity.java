package com.chasetech.pcount.MKL;

import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import android.widget.SimpleCursorAdapter;

import com.chasetech.pcount.Assortment.Assortment;
import com.chasetech.pcount.BuildConfig;
import com.chasetech.pcount.PostingActivity;
import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.MainActivity;
import com.chasetech.pcount.R;
import com.chasetech.pcount.Woosim.WoosimPrinter;
import com.chasetech.pcount.adapter.PCountListViewAdapter;
import com.chasetech.pcount.adapter.ReportListViewAdapter;
import com.chasetech.pcount.adapter.ReportWithSoAdapter;
import com.chasetech.pcount.database.SQLLib;

import com.chasetech.pcount.database.SQLiteDB;
import com.chasetech.pcount.TSC.BPrinter;
import com.chasetech.pcount.library.MainLibrary;
import com.chasetech.pcount.library.ReportClass;
import com.chasetech.pcount.viewholder.PCountViewHolder;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ULTRABOOK on 9/28/2015.
 */
public class PCountActivity extends AppCompatActivity {

    private ProgressDialog pDL;
    private AlertDialog mAlertDialog;
    private SQLLib db;

    private String TAG;

    private ArrayList<PCount> mArrayListPcount = new ArrayList<>();
    private ArrayList<PCount> arrayListForAdapter;
    private HashMap<String, PCount> mHashmapPcountAll = new HashMap<>();
    private HashMap<String, PCount> hmPcountTransacted = new HashMap<>();
    private PCount pCountUpdated = null;

    private ListView lvwOsaItems = null;

    private PCountListViewAdapter mPCountListViewAdapter;

    private EditText editTextSearch = null;

    private BPrinter Printer;
    private Boolean lprintwithso = false;

    private double len  = 0;
    private int numItemsToPrint = 0;

    private String selectedPrinter = "";
    private WoosimPrinter woosimPrinter;
    private boolean printEnabled = false;
    private TextView tvwTotalItems;
    private ArrayList<String> itemOrdered;

    private Boolean Filtered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcount);

        TAG = PCountActivity.this.getLocalClassName();

        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(this, MainLibrary.errlogFile));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        selectedPrinter = prefs.getString("printer_list", "2");

        if(selectedPrinter.equals("1")) MainLibrary.mSelectedPrinter = MainLibrary.PRINTER.WOOSIM;
        else MainLibrary.mSelectedPrinter = MainLibrary.PRINTER.TSC;

        final TextView lblfso = (TextView) findViewById(R.id.lblfso);
//        String fsolbl = MainLibrary.gStrCurrentUserName.substring(3,6) + " Unit";
        String fsolbl = MainLibrary.gStrCurrentUserName + " Unit";
        lblfso.setText(fsolbl);

        db = new SQLLib(PCountActivity.this);
        db.open();

        Printer = new BPrinter(this);

        arrayListForAdapter = new ArrayList<>();

/*        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setTitle(MainLibrary.gCurrentBranchNameSelected);*/
//        lupdate = getIntent().getExtras().getBoolean("lupdate");

        try {
            String actionBarTitle = MainLibrary.gSelectedStores.storeName;
            getSupportActionBar().setTitle(actionBarTitle);
        }
        catch (NullPointerException nex) {
            Log.e(TAG, nex.getMessage());
            nex.printStackTrace();
        }

        MainLibrary.errorLog.appendLog("Start pcount transaction.", TAG);

        editTextSearch = (EditText) findViewById(R.id.enter_search);
        tvwTotalItems = (TextView) findViewById(R.id.tvwTotalItems);

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String search = editTextSearch.getText().toString();
                mPCountListViewAdapter.filter(99, search);
            }
        });

        lvwOsaItems = (ListView) findViewById(R.id.listViewPcount);
        lvwOsaItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PCountViewHolder viewHolder = (PCountViewHolder) view.getTag();

                final PCount selectedPcount = viewHolder.pCount;

                final Dialog dialog = new Dialog(PCountActivity.this, R.style.Transparent);
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.activity_sku_details);

                final TextView textViewDesc = (TextView) dialog.findViewById(R.id.textViewDesc);
                final EditText editTextPcs = (EditText) dialog.findViewById(R.id.pcs);
                final EditText editTextWhPcs = (EditText) dialog.findViewById(R.id.whpcs);
                final EditText editTextWhCs = (EditText) dialog.findViewById(R.id.whcs);
                final EditText txtInventorygoal = (EditText) dialog.findViewById(R.id.txtInveGoal);
                final Button btnQty = (Button) dialog.findViewById(R.id.btnQtyOk);

                txtInventorygoal.setEnabled(MainLibrary.allowIG);

                textViewDesc.setText(selectedPcount.desc);
                editTextPcs.setText("");
                editTextWhPcs.setText("");
                editTextWhCs.setText("");
                txtInventorygoal.setText(String.valueOf(selectedPcount.ig));

                if (selectedPcount.sapc != 0 || selectedPcount.whpc != 0 || selectedPcount.whcs !=0 ) {
                    editTextPcs.setText(String.valueOf(selectedPcount.sapc));
                    editTextWhPcs.setText(String.valueOf(selectedPcount.whpc));
                    editTextWhCs.setText(String.valueOf(selectedPcount.whcs));
                }

                btnQty.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        pCountUpdated = null;

                        String inputPcs = editTextPcs.getText().toString().trim();
                        String inputWhPcs = editTextWhPcs.getText().toString().trim();
                        String inputWhcs = editTextWhCs.getText().toString().trim();
                        String inputIg = txtInventorygoal.getText().toString().trim();
                        inputIg = inputIg.equals("") ? "0" : inputIg;

                        if (inputPcs.length() > 7) {
                            editTextPcs.setError("Invalid value!");
                            return;
                        }

                        if (inputWhPcs.length() > 7) {
                            editTextWhPcs.setError("Invalid WHPC value!");
                            return;
                        }

                        if (inputWhcs.length() > 7) {
                            editTextWhCs.setError("Invalid WHCS value!");
                            return;
                        }

                        if (inputIg.length() > 7) {
                            txtInventorygoal.setError("Invalid IG value!");
                            return;
                        }

                        int nNewIG = Integer.parseInt(inputIg);

                        if(nNewIG < selectedPcount.oldIg) {
                            String errmsg = inputIg + " is less than its old IG of " + String.valueOf(selectedPcount.oldIg);
                            txtInventorygoal.setError(errmsg);
                            Toast.makeText(PCountActivity.this, errmsg, Toast.LENGTH_LONG).show();
                            return;
                        }

                        dialog.dismiss();
                        selectedPcount.ig = nNewIG;

                        if (inputPcs.isEmpty()) {
                            inputPcs = "0";
                        }

                        if (inputWhPcs.isEmpty()) {
                            inputWhPcs = "0";
                        }

                        if (inputWhcs.isEmpty()) {
                            inputWhcs = "0";
                        }

                        int so = selectedPcount.ig - Integer.parseInt(inputPcs) - Integer.parseInt(inputWhPcs) - (Integer.parseInt(inputWhcs) * selectedPcount.conversion);



                        // COMPUTATION FOR MDC
                        String channelname = MainLibrary.gSelectedStores.channelArea.toUpperCase();
                        if(channelname.equals("MDC")|| channelname.equals("SOUTH STAR DRUG") || channelname.equals("ROSE PHARMACY") || channelname.equals("ST. JOSEPH DRUG") || channelname.equals("360 PHARMACY")) {
                            String channel = MainLibrary.gSelectedStores.channelDesc.trim().toUpperCase();
                            //if(channel.contains("EXTRA SMALL") || channel.contains("SMALL") || channel.contains("MEDIUM")) {
                                double endInv = Double.parseDouble(inputPcs) + Double.parseDouble(inputWhPcs) + (Double.parseDouble(inputWhcs) * selectedPcount.conversion);

                                //if(selectedPcount.multi >= 12) { // 60%
                                    double stuff = (selectedPcount.ig * 3.0) / 5.0;

                            if (stuff % 1 != 0) {
                                if (endInv <= stuff) {
                                    so = selectedPcount.ig - Integer.parseInt(inputPcs) - Integer.parseInt(inputWhPcs) - (Integer.parseInt(inputWhcs) * selectedPcount.conversion);
                                } else so = 0;
                            }

                            else {
                                //if(endInv <= ((selectedPcount.ig * 3) / 5))
                                if (endInv < stuff) {
                                    so = selectedPcount.ig - Integer.parseInt(inputPcs) - Integer.parseInt(inputWhPcs) - (Integer.parseInt(inputWhcs) * selectedPcount.conversion);
                                } else so = 0;
                            }
                                //}
                            //}
                        }


                        int fso;

                        if ((so % selectedPcount.multi) == 0) {
                            fso = so;
                        }
                        else {
                            fso = so - (so % selectedPcount.multi) + selectedPcount.multi;
                        }

                        if (so <= 0) {    //10/27 for negative values
                            so = 0;
                            fso = 0;
                        }

                        selectedPcount.sapc = Integer.parseInt(inputPcs);
                        selectedPcount.whpc = Integer.parseInt(inputWhPcs);
                        selectedPcount.whcs = Integer.parseInt(inputWhcs);
                        selectedPcount.so = so;
                        selectedPcount.fso = fso;
                        selectedPcount.fso = fso;
                        selectedPcount.updated = true;

                        PCount pcountAll = mHashmapPcountAll.get(selectedPcount.barcode);
                        if(pcountAll != null) {
                            pcountAll.sapc = Integer.parseInt(inputPcs);
                            pcountAll.whpc = Integer.parseInt(inputWhPcs);
                            pcountAll.whcs = Integer.parseInt(inputWhcs);
                            pcountAll.so = so;
                            pcountAll.fso = fso;
                            pcountAll.ig = Integer.parseInt(inputIg);
                            pcountAll.updated = true;
                            mHashmapPcountAll.put(selectedPcount.barcode, pcountAll);
                        }

                        pCountUpdated = selectedPcount;
                        //hmPcountUpdated.put(selectedPcount.id, selectedPcount);
                        if(selectedPcount.oldIg != selectedPcount.ig)
                            MainLibrary.arrPcountEditedIg.add(selectedPcount);

                        mPCountListViewAdapter.notifyDataSetChanged();
                        new TaskSaveData().execute();
                    }
                });
                dialog.show();
            }
        });

        MainLibrary.arrPcountEditedIg.clear();
        MainLibrary.Counter = 1;
        new TaskProcessData().execute();

    }

    /** DATA PROCESSING *************************************************************/
    private class TaskProcessData extends AsyncTask<String, Void, Boolean> {

        private String errorMessage;
        private boolean itemsEqual = false;

        @Override
        protected Boolean doInBackground(String... params) {

            boolean result = false;
            itemsEqual = false;
            int i = 1;

            try {
                mArrayListPcount.clear();
                mHashmapPcountAll.clear();

                String strTransQuery = "SELECT * FROM " + SQLiteDB.TABLE_TRANSACTION + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "' AND " + SQLiteDB.COLUMN_TRANSACTION_DATE + " = '" + MainLibrary.gStrCurrentDate + "' AND " + SQLiteDB.COLUMN_TRANSACTION_USERID + " = '" + MainLibrary.gStrCurrentUserID + "'";
                String strItemsQuery = "SELECT * FROM " + SQLiteDB.TABLE_PCOUNT + " WHERE " + SQLiteDB.COLUMN_PCOUNT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "'";

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
                        int id = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_ID));
                        int ig = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_IG));
                        String barcode = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_BARCODE)).trim();
                        String sapc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_SAPC)).trim();
                        String whpc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_WHPC)).trim();
                        String whcs = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_WHCS)).trim();
                        String so = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_SO)).trim();
                        String fso = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_FSO)).trim();
                        String desc = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_DESC)).trim();
                        String category = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_CATEGORY)).trim();
                        String descLong = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_DESCLONG)).trim();
                        String categoryid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_CATEGORYID)).trim();
                        String brandid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_BRANDID)).trim();
                        String divisionid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_DIVISIONID)).trim();
                        String subcategoryid = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_SUBCATEGORYID)).trim();
                        int conversion = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_CONVERSION));
                        double fsovalue = cursItems.getDouble(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_FSOVALUE));
                        int webid = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_WEBID));
                        int multi = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_MULTI));
                        String otherBarcode = cursItems.getString(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_OTHERBARCODE));
                        int minstock = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_MINSTOCK));
                        int oldIg = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_OLDIG));
                        int osaTag = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_OSATAG));
                        int npiTag = cursItems.getInt(cursItems.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_NPITAG));

                        PCount pCountItem = new PCount(
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
                                otherBarcode,
                                minstock,
                                false,
                                oldIg,
                                osaTag,
                                npiTag,
                                i
                        );

                        mArrayListPcount.add(pCountItem);
                        mHashmapPcountAll.put(pCountItem.barcode.trim(), pCountItem);

                        if(pCountItem.oldIg != pCountItem.ig)
                            MainLibrary.arrPcountEditedIg.add(pCountItem);

                        String[] aFields = new String[] {
                                SQLiteDB.COLUMN_TRANSACTION_DATE,
                                SQLiteDB.COLUMN_TRANSACTION_STOREID,
                                SQLiteDB.COLUMN_TRANSACTION_BARCODE,
                                SQLiteDB.COLUMN_TRANSACTION_IG,
                                SQLiteDB.COLUMN_TRANSACTION_SAPC,
                                SQLiteDB.COLUMN_TRANSACTION_WHPC,
                                SQLiteDB.COLUMN_TRANSACTION_WHCS,
                                SQLiteDB.COLUMN_TRANSACTION_CONVERSION,
                                SQLiteDB.COLUMN_TRANSACTION_SO,
                                SQLiteDB.COLUMN_TRANSACTION_FSO,
                                SQLiteDB.COLUMN_TRANSACTION_FSOVALUE,
                                SQLiteDB.COLUMN_TRANSACTION_LPOSTED,
                                SQLiteDB.COLUMN_TRANSACTION_WEBID,
                                SQLiteDB.COLUMN_TRANSACTION_USERID,
                                SQLiteDB.COLUMN_TRANSACTION_MULTI,
                                SQLiteDB.COLUMN_TRANSACTION_OTHERBARCODE,
                                SQLiteDB.COLUMN_TRANSACTION_MINSTOCK,
                                SQLiteDB.COLUMN_TRANSACTION_CATEGORY,
                                SQLiteDB.COLUMN_TRANSACTION_DESCLONG,
                                SQLiteDB.COLUMN_TRANSACTION_DESC,
                                SQLiteDB.COLUMN_TRANSACTION_CATEGORYID,
                                SQLiteDB.COLUMN_TRANSACTION_BRANDID,
                                SQLiteDB.COLUMN_TRANSACTION_DIVISIONID,
                                SQLiteDB.COLUMN_TRANSACTION_SUBCATEGORYID,
                                SQLiteDB.COLUMN_TRANSACTION_OLDIG,
                                SQLiteDB.COLUMN_TRANSACTION_OSATAG,
                                SQLiteDB.COLUMN_TRANSACTION_NPITAG
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
                                subcategoryid,
                                String.valueOf(oldIg),
                                String.valueOf(osaTag),
                                String.valueOf(npiTag)
                        };

                        db.AddRecord(SQLiteDB.TABLE_TRANSACTION, aFields, aValues);
                        i++;
                        cursItems.moveToNext();
                    }

                    cursItems.close();

                    if(nItemsTotal != mArrayListPcount.size()) {
                        errorMessage = "Item count copied are not equal. Please reload items. Do you want to proceed?";
                        itemsEqual = false;
                        return false;
                    }

                    itemsEqual = true;
                    result = true;
                }
                else {

                    while (!cursTrans.isAfterLast()) {


                        int id = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ID));
                        int ig = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_IG));
                        String barcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_BARCODE)).trim();
                        String desc = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_DESC)).trim();
                        String categoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_CATEGORYID)).trim();
                        String brandid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_BRANDID)).trim();
                        String divisionid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_DIVISIONID)).trim();
                        String subcategoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_SUBCATEGORYID)).trim();
                        int conversion = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_CONVERSION));
                        double fsovalue = cursTrans.getDouble(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_FSOVALUE));
                        int webid = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_WEBID));
                        int multi = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_MULTI));
                        String otherBarcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_OTHERBARCODE));
                        int minstock = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_MINSTOCK));
                        int sapc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_SAPC));
                        int whpc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_WHPC));
                        int whcs = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_WHCS));
                        int so = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_SO));
                        int fso = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_FSO));
                        boolean isUpdated = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_UPDATED)) == 1;
                        int oldIg = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_OLDIG));
                        int osaTag = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_OSATAG));
                        int npiTag = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_NPITAG));

                        PCount pCountItem = new PCount(
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
                                otherBarcode,
                                minstock,
                                false,
                                oldIg,
                                osaTag,
                                npiTag,
                                i
                        );

                        if (isUpdated) {
                            pCountItem.sapc = sapc;
                            pCountItem.whpc = whpc;
                            pCountItem.whcs = whcs;
                            pCountItem.so = so;
                            pCountItem.fso = fso;
                            pCountItem.updated = true;
                            hmPcountTransacted.put(pCountItem.barcode, pCountItem);
                        }

                        mHashmapPcountAll.put(pCountItem.barcode.trim(), pCountItem);
                        mArrayListPcount.add(pCountItem);

                        if(pCountItem.oldIg != pCountItem.ig)
                            MainLibrary.arrPcountEditedIg.add(pCountItem);

                        MainLibrary.Counter ++;
                        i++;
                        cursTrans.moveToNext();

                    }

                    result = true;
                }

                cursTrans.close();

                itemOrdered = new ArrayList<String>(mHashmapPcountAll.keySet());

                if(MainLibrary.gSelectedStores.channelArea.equals("MDC")) {
                    Collections.sort(itemOrdered, new Comparator<String>() {
                        @Override
                        public int compare(String lhs, String rhs) {
                            return Integer.parseInt(lhs) - Integer.parseInt(rhs);
                        }
                    });
                }

                ProcessOrderedPCountItems(); //TODO TO CHECK
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
                AlertDialog promptDialog = new AlertDialog.Builder(PCountActivity.this).create();
                promptDialog.setCancelable(false);
                promptDialog.setTitle("Loading Items");

                String msg = errorMessage;

                if(!itemsEqual) {

                    String strTransDeleteQuery = "DELETE FROM " + SQLiteDB.TABLE_TRANSACTION
                            + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId)
                            + "' AND " + SQLiteDB.COLUMN_TRANSACTION_DATE + " = '" + MainLibrary.gStrCurrentDate + "'";

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

            arrayListForAdapter.clear();
            arrayListForAdapter.addAll(mArrayListPcount);
            mPCountListViewAdapter = new PCountListViewAdapter(PCountActivity.this, arrayListForAdapter);
            lvwOsaItems.setAdapter(mPCountListViewAdapter);
            mPCountListViewAdapter.notifyDataSetChanged();

            String totalItemMsg = "Total Items: " + String.valueOf(hmPcountTransacted.size()) + " / " + String.valueOf(mArrayListPcount.size());
            tvwTotalItems.setText(totalItemMsg);
        }

        @Override
        protected void onPreExecute() {
            //Toast.makeText(PCountActivity.this, "Current Stores Selected " + String.valueOf(MainLibrary.gCurrentBranchSelected), Toast.LENGTH_SHORT).show();
            pDL = ProgressDialog.show(PCountActivity.this, "", "Updating Masterfile. Please wait.", true);
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }
    }

    private void ProcessOrderedPCountItems() {

        int i =1;
        mHashmapPcountAll.clear();

        String strTransQuery = "SELECT * FROM " + SQLiteDB.TABLE_TRANSACTION + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "' AND " + SQLiteDB.COLUMN_TRANSACTION_DATE + " = '" + MainLibrary.gStrCurrentDate + "'";

        // ORDERED TRANS ITEMS
        if(MainLibrary.gSelectedStores.channelArea.equals("MDC")) {
            strTransQuery = "SELECT * FROM " + SQLiteDB.TABLE_TRANSACTION + " WHERE " + SQLiteDB.COLUMN_TRANSACTION_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "' AND " + SQLiteDB.COLUMN_TRANSACTION_DATE + " = '" + MainLibrary.gStrCurrentDate + "' ORDER BY " + SQLiteDB.COLUMN_TRANSACTION_BARCODE;
        }

        Cursor cursTrans = db.queryData(strTransQuery);

        if(cursTrans.moveToFirst()) {

            while (!cursTrans.isAfterLast()) {

                int id = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_ID));
                int ig = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_IG));
                String barcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_BARCODE)).trim();
                String desc = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_DESC)).trim();
                String categoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_CATEGORYID)).trim();
                String brandid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_BRANDID)).trim();
                String divisionid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_DIVISIONID)).trim();
                String subcategoryid = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_SUBCATEGORYID)).trim();
                int conversion = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_CONVERSION));
                double fsovalue = cursTrans.getDouble(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_FSOVALUE));
                int webid = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_WEBID));
                int multi = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_MULTI));
                String otherBarcode = cursTrans.getString(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_OTHERBARCODE));
                int minstock = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_MINSTOCK));
                int sapc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_SAPC));
                int whpc = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_WHPC));
                int whcs = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_WHCS));
                int so = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_SO));
                int fso = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_FSO));
                boolean isUpdated = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_UPDATED)) == 1;
                int oldIg = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_OLDIG));
                int osaTag = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_OSATAG));
                int npiTag = cursTrans.getInt(cursTrans.getColumnIndex(SQLiteDB.COLUMN_TRANSACTION_NPITAG));

                PCount pCountItem = new PCount(
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
                        otherBarcode,
                        minstock,
                        false,
                        oldIg,
                        osaTag,
                        npiTag,
                        i

                );

                if (isUpdated) {
                    pCountItem.sapc = sapc;
                    pCountItem.whpc = whpc;
                    pCountItem.whcs = whcs;
                    pCountItem.so = so;
                    pCountItem.fso = fso;
                    pCountItem.updated = true;
                }

                mHashmapPcountAll.put(pCountItem.barcode.trim(), pCountItem);
                i++;
                cursTrans.moveToNext();
            }

            cursTrans.close();
        }

    }

    /** DATA PROCESSING *************************************************************/
    private class TaskSaveData extends AsyncTask<String, Void, Boolean> {

        private String errmsg;

        @Override
        protected Boolean doInBackground(String... params) {

            boolean result = false;

            try {

                if (pCountUpdated != null) {

                    PCount pCount = pCountUpdated;

                    Cursor cursTrans = db.GetDataCursor(SQLiteDB.TABLE_TRANSACTION, SQLiteDB.COLUMN_TRANSACTION_BARCODE + " = '" + pCount.barcode + "' AND " + SQLiteDB.COLUMN_TRANSACTION_DATE + " = '" + MainLibrary.gStrCurrentDate + "' AND " + SQLiteDB.COLUMN_TRANSACTION_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId) + "' AND " + SQLiteDB.COLUMN_TRANSACTION_USERID + " = '" + MainLibrary.gStrCurrentUserID + "'");
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
                            "lposted",
                            SQLiteDB.COLUMN_TRANSACTION_UPDATED
                    };

                    String[] avalues = {MainLibrary.gStrCurrentDate
                            , String.valueOf(MainLibrary.gSelectedStores.webStoreId)
                            , pCount.barcode
                            , String.valueOf(pCount.ig)
                            , String.valueOf(pCount.sapc)
                            , String.valueOf(pCount.whpc)
                            , String.valueOf(pCount.whcs)
                            , String.valueOf(pCount.conversion)
                            , String.valueOf(pCount.so)
                            , String.valueOf(pCount.fso)
                            , String.valueOf(pCount.fsovalue)
                            , String.valueOf(pCount.webid)
                            , String.valueOf(MainLibrary.gStrCurrentUserID)
                            , String.valueOf(pCount.multi)
                            , "0"
                            , "1"
                    };

                    hmPcountTransacted.put(pCount.barcode, pCount);
//                    db.ExecSQLWrite("UPDATE " + SQLiteDB.TABLE_PCOUNT + " SET " + SQLiteDB.COLUMN_PCOUNT_IG + " = '" + pCount.ig
//                            + "' WHERE " + SQLiteDB.COLUMN_PCOUNT_BARCODE + " = '" + String.valueOf(pCount.barcode).trim() + "' AND "
//                            + SQLiteDB.COLUMN_PCOUNT_STOREID + " = '" + String.valueOf(MainLibrary.gSelectedStores.webStoreId).trim() + "'");

                    if (cursTrans.getCount() == 0) {
                        db.AddRecord(SQLiteDB.TABLE_TRANSACTION, afields, avalues);
                        return true;
                    }

                    String[] whereFields = new String[]{
                            SQLiteDB.COLUMN_TRANSACTION_BARCODE,
                            SQLiteDB.COLUMN_TRANSACTION_DATE,
                            SQLiteDB.COLUMN_TRANSACTION_STOREID,
                    };
                    String[] whereValues = new String[]{
                            pCount.barcode,
                            MainLibrary.gStrCurrentDate,
                            String.valueOf(MainLibrary.gSelectedStores.webStoreId).trim(),
                    };

                    db.UpdateRecord(SQLiteDB.TABLE_TRANSACTION, whereFields, whereValues, afields, avalues, PCountActivity.this);
                    cursTrans.close();
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
                Toast.makeText(PCountActivity.this, errmsg, Toast.LENGTH_LONG).show();
                return;
            }

            mPCountListViewAdapter.notifyDataSetChanged();
            lvwOsaItems.requestLayout();

            int i = hmPcountTransacted.size();
            int j = mArrayListPcount.size();
            String totalItemMsg = "Total Items: " + String.valueOf(hmPcountTransacted.size()) + " / " + String.valueOf(mArrayListPcount.size());
            tvwTotalItems.setText(totalItemMsg);
        }

        @Override
        protected void onPreExecute() {
            pDL = ProgressDialog.show(PCountActivity.this, "", "Saving Transaction dated " + MainLibrary.gStrCurrentDate + ". Please Wait...", true);
        }
    }

    /** DATA PROCESSING *************************************************************/
    private class TaskPrintData extends AsyncTask<String, Void, Boolean> {

        String print = "";
        Boolean lwithbarcode = true;
        String errorPrintMessage = "";

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
                            else errorPrintMessage = "Woosim printer not connected or paired.";

                        } else {
                            if (PrintFormatWoosim3inch(lwithbarcode))
                                lsuccess = true;
                            else errorPrintMessage = "Woosim printer not connected or paired.";
                        }
                    }
                    catch (Exception e) {
                        String prompt = "Print error. Please check printer connection and try again.";
                        String errmsg = e.getMessage() != null ? e.getMessage() : prompt;
                        MainLibrary.errorLog.appendLog(errmsg, TAG);
                        errorPrintMessage = prompt;
                    }
                    break;
                case TSC:

                    if (MainLibrary.ValidateStoreIfFourInch()) {
                        print = Printer.GenerateStringTSCPrint(PrintFormatTSC4inch(lwithbarcode), len, numItemsToPrint, 1, true);

                        if(Printer.Open()) {
                            String basfile = "DEFAULT.PRN";
                            if(MainLibrary.eStore == MainLibrary.STORE_TYPES.MERCURY_DRUG)
                                basfile = "MERCURY_4L.PRN";

                            File fbas = new File(Environment.getExternalStorageDirectory(), "Download/" + basfile);
                            if(!fbas.exists()) {
                                if(!new File(Environment.getExternalStorageDirectory(), "Download/DEFAULT.PRN").exists()) {
                                    errorPrintMessage = "No downloaded logo. Please re-log to download the required files.";
                                    return lsuccess;
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
                            catch (Exception e) {
                                String prompt = "Print error. Please check printer connection and try again.";
                                String errmsg = e.getMessage() != null ? e.getMessage() : prompt;
                                MainLibrary.errorLog.appendLog(errmsg, TAG);
                                errorPrintMessage = prompt;
                            }
                        }
                        else errorPrintMessage =  "Can't connect to printer. Please check if printer is paired in this device";
                    }
                    else {

                        print = Printer.GenerateStringTSCPrint(PrintFormatTSC3inch(lwithbarcode), len, numItemsToPrint, 1, false);

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
                                    errorPrintMessage = "No downloaded logo. Please re-log to download the required files.";
                                    return lsuccess;
                                } else basfile = "DEFAULT.PRN";
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
                            } catch (Exception e) {
                                String prompt = "Print error. Please check printer connection and try again.";
                                errorPrintMessage = prompt;
                                String errmsg = e.getMessage() != null ? e.getMessage() : prompt;
                                MainLibrary.errorLog.appendLog(errmsg, TAG);
                            }
                        } else
                            errorPrintMessage = "Can't connect to printer. Please check if printer is paired in this device";
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
                if(woosimPrinter != null) woosimPrinter.isConnected = false;
                MainLibrary.messageBox(PCountActivity.this, "Print", "Printing not successful. " + errorPrintMessage);
                //Toast.makeText(PCountActivity.this, "Printing not successful. " + errorPrintMessage, Toast.LENGTH_LONG).show();
                return;
            }

            AlertDialog printdialog = new AlertDialog.Builder(PCountActivity.this).create();
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
            pDL = ProgressDialog.show(PCountActivity.this, "", "Printing. Please Wait...", true);
        }
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

        final AlertDialog.Builder itemDialog = new AlertDialog.Builder(PCountActivity.this);

        itemDialog.setTitle("Print all");
        itemDialog.setMessage("Are you sure you want print all items?");

        switch (item.getItemId()) {
            case R.id.action_check_printer:
                if (woosimPrinter != null) {
                    if (!woosimPrinter.isConnected) {
                        woosimPrinter.SetUpWoosim();
                    }
                    else Toast.makeText(PCountActivity.this, "Printer is already connected.", Toast.LENGTH_SHORT).show();
                }
                else Toast.makeText(PCountActivity.this, "Printer not connected.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_logout:
//                NavUtils.navigateUpFromSameTask(this);
                itemDialog.setTitle("Log Out");
                itemDialog.setMessage("Are you sure you want to log out?");
                itemDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                itemDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        new UserLogout().execute();
                    }
                });

                itemDialog.show();
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
                mPCountListViewAdapter.filter(0, "");
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
                    for (HashMap.Entry<String, PCount> entry : mHashmapPcountAll.entrySet()) {
                        PCount pCount = entry.getValue();
                        if (pCount.sapc == 0 && pCount.whpc == 0 && pCount.whcs == 0) {
                            linvalid = true;
                            break;
                        }
                    }
                    if (linvalid) {
                        Toast.makeText(PCountActivity.this, "Cannot Post Transaction.", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }

                if(MainLibrary.ValidatedRepostingDate() || !MainLibrary.MKL_validateDatePosting) {
                    if (MainLibrary.MKL_allItemsReqForPosting) {
                        new CheckRequiredItems(true).execute();
                    } else {
                        Intent intent = new Intent(PCountActivity.this, PostingActivity.class);
                        intent.putExtra("location", MainLibrary.gSelectedStores.webStoreId);
                        intent.putExtra("datepick", MainLibrary.gStrCurrentDate);
                        startActivity(intent);
                    }
                }
                else {
                    new AlertDialog.Builder(PCountActivity.this)
                            .setTitle("Transaction date expired")
                            .setMessage("Current transaction date has expired.")
                            .setCancelable(false)
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
                    new AlertDialog.Builder(PCountActivity.this)
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
                    itemDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    itemDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            lprintwithso = false;
                            new TaskPrintData(true).execute();
                        }
                    });
                    itemDialog.show();
                }
                else
                    Toast.makeText(PCountActivity.this, "Please enable your bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_all_nobarcode:

                if(!printEnabled) {
                    new AlertDialog.Builder(PCountActivity.this)
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

                    itemDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    itemDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            lprintwithso = false;
                            new TaskPrintData(false).execute();
                        }
                    });
                    itemDialog.show();
                }
                else
                    Toast.makeText(PCountActivity.this, "Please enable your bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_withso_barcode:

                if(!printEnabled) {
                    new AlertDialog.Builder(PCountActivity.this)
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

                    if(MainLibrary.MKL_allItemsReqForPrinting) {
                        //lprintwithso = true;
                        //new TaskPrintData(true).execute(); // PRINT DATA
                        new CheckRequiredItems(false, false, true).execute();
                    }
                    else {
                        lprintwithso = true;
                        new TaskPrintData(true).execute(); // PRINT DATA
                    }
                }
                else
                    Toast.makeText(PCountActivity.this, "Please enable your bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_print_withso_nobarcode:

                if(!printEnabled) {
                    new AlertDialog.Builder(PCountActivity.this)
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

                    if(MainLibrary.MKL_allItemsReqForPrinting)
                        new CheckRequiredItems(false, false, false).execute();
                    else {
                        lprintwithso = true;
                        new TaskPrintData(false).execute(); // PRINT DATA
                    }
                }
                else
                    Toast.makeText(PCountActivity.this, "Please enable your bluetooth to proceed.", Toast.LENGTH_SHORT).show();
                break;
            case android.R.id.home:
                onBackPressed();
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
        boolean bHasBarcode = true;

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
            pDL = ProgressDialog.show(PCountActivity.this, "", "Checking required items.", true);
            strError = "";
            nAllItems = mArrayListPcount.size();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bReturn = true;

            if(hmPcountTransacted.size() == 0) {
                bReturn = false;
                strError = "No transactions found.";
            }
            else {
                for (HashMap.Entry<String, PCount> entry : mHashmapPcountAll.entrySet()) {
                    PCount pcount = entry.getValue();
                    if (!hmPcountTransacted.containsKey(pcount.barcode)) {
                        bReturn = false;
                        strError = hmPcountTransacted.size() + " / " + nAllItems + ". Some required items not transacted.";
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
                Toast.makeText(PCountActivity.this, strError, Toast.LENGTH_SHORT).show();
                return;
            }

            // FOR POSTING
            Intent intentpost = new Intent(PCountActivity.this, PostingActivity.class);
            intentpost.putExtra("location", MainLibrary.gSelectedStores.webStoreId);
            intentpost.putExtra("datepick", MainLibrary.gStrCurrentDate);

            // FOR PRINTING
            mAlertDialog = new AlertDialog.Builder(PCountActivity.this).create();
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
                        new AlertDialog.Builder(PCountActivity.this)
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

            if(bPostMode)
                startActivity(intentpost);
            else {
                if (bPrintAll) {
                    mAlertDialog.show();
                }
                else {
                    lprintwithso = true;

                    if(!printEnabled) {
                        new AlertDialog.Builder(PCountActivity.this)
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

                    new TaskPrintData(bHasBarcode).execute(); // PRINT DATA
                }
            }
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
            return result;
        }
        // -------------------------------

        toPrint += "Store: " + MainLibrary.gSelectedStores.storeName + "\n";

        String osascore = "";

        osascore = MainLibrary.GetOsaScorePcount(mHashmapPcountAll);


        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += StringUtils.rightPad("SKU",25,"") +
                StringUtils.rightPad("IG",10,"") +
                StringUtils.rightPad("Invty",8,"") +
                StringUtils.rightPad("Order qty", 12,"") +
                StringUtils.rightPad("Order amt", 5, "") + "\n";
        toPrint += Printer.woosimLines;




        if(!woosimPrinter.printText(toPrint, false, false, 1))  {
            runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PCountActivity.this, "Printer connection is interrupted. Print cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });
                return result;
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
            return result;
        }
        // -------------------------------

        toPrint += "Store: " + MainLibrary.gSelectedStores.storeName + "\n";

        String osascore = "";

        osascore = MainLibrary.GetOsaScorePcount(mHashmapPcountAll);

        /*toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += StringUtils.rightPad("SKU",22,"") +
                StringUtils.rightPad("IG",8,"") +
                StringUtils.rightPad("Invty",9,"") +
                StringUtils.rightPad("Final SO",12,"") +
                StringUtils.rightPad("Unit",9,"") +
                StringUtils.rightPad("Order amt", 9, "") + "\n";
        toPrint += Printer.woosimLines_4inch;
        */

        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += StringUtils.rightPad("SKU",25,"") + // +3
                StringUtils.rightPad("IG",11,"") +
                //StringUtils.rightPad("Invty",12,"") +
                StringUtils.rightPad("Final SO",15,"") +
                StringUtils.rightPad("Unit",14,"") +
                StringUtils.rightPad("Order amt", 14, "") + "\n";
        toPrint += Printer.woosimLines_4inch;

        if(!woosimPrinter.printText(toPrint, false, false, 1)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PCountActivity.this, "Printer connection is interrupted. Print cancelled.", Toast.LENGTH_SHORT).show();
                }
            });
            return result;
        }

        result = PrintDetailsWoosim_4inch(hasBarcode);

        return result;
    }

    private boolean PrintDetailsWoosim_3inch(boolean hasBarcode) {

        boolean result = false;
        int totsku = 0, totfso = 0;
        double totfsoval = 0;
        try {

            ArrayList<PCount> Barcodes = new ArrayList<>();
            if(Filtered)
            {
                //Barcodes = filteredItems;

                Barcodes = mPCountListViewAdapter.mArrayListPCountResultList;
            }
            else {

                Barcodes = mArrayListPcount;
            }


            //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())
            for (PCount pCount : Barcodes) {

                //for (HashMap.Entry<String, PCount> entry : mHashmapPcountAll.entrySet()) {
                //  PCount pCount = entry.getValue();

                if (lprintwithso) {
                    if (pCount.so == 0) {
                        continue;
                    }
                }


                int totig = pCount.sapc + pCount.whpc + (pCount.whcs * pCount.conversion);
                double orderAmt = pCount.fsovalue * pCount.fso;


                String itemDesc = StringUtils.rightPad(pCount.desc + " " + pCount.barcode, 20, "");
                if(!woosimPrinter.printText(itemDesc, false, false, 1)) return result;

                String strValues = StringUtils.rightPad("", 23, "")
                            + StringUtils.leftPad(String.valueOf(pCount.ig), 4) + StringUtils.center(" ", 8)
                            + StringUtils.leftPad(String.valueOf(totig), 4) + StringUtils.center(" ", 5)
                            + StringUtils.leftPad(String.valueOf(pCount.fso), 4) + StringUtils.center(" ", 5)
                            + StringUtils.leftPad(MainLibrary.priceDec.format(orderAmt), 11);

                if(!woosimPrinter.printText(strValues, true, false, 1)) return result;

                if(hasBarcode) {
                    String barcodeType = MainLibrary.GetBarcodeType(pCount.itembarcode);
                    if(!pCount.itembarcode.equals(""))
                        woosimPrinter.print1DBarcode(barcodeType, pCount.itembarcode);
                }

                if(!woosimPrinter.printText(" ", false, false, 1)) return result;

                if (pCount.so > 0) {
                    totsku = totsku + 1;
                }

                totfso = totfso + pCount.fso;
                totfsoval = totfsoval + (pCount.fsovalue * pCount.fso) ;


            }

             // FOOTER
            String footer = "";

            footer += Printer.woosimLines;
            footer += "Total: " + StringUtils.rightPad(String.valueOf(totsku),26) + StringUtils.leftPad(String.valueOf(totfso),15)
                    + StringUtils.rightPad(" ", 6) + StringUtils.leftPad(MainLibrary.priceDec.format(totfsoval), 10) + "\n";
            footer += "\n" + "\n" + "\n" + "\n" + "\n";
            footer += StringUtils.center(Printer.woosimLines2, 64);
            footer += StringUtils.center("Acknowledged by", 64);
            footer += "\n" + "\n"+ "\n";

            if(!woosimPrinter.printText(footer, true, false, 1)) return result;


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

            ArrayList<PCount> Barcodes = new ArrayList<>();
            if(Filtered)
            {
                //Barcodes = filteredItems;

                Barcodes = mPCountListViewAdapter.mArrayListPCountResultList;
            }
            else {

                Barcodes = mArrayListPcount;
            }


            //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())
            for (PCount pCount : Barcodes)
            {


            //for (String barcodeKey : itemOrdered) {
              //  PCount pCount = mHashmapPcountAll.get(barcodeKey);

                if (lprintwithso) {
                    if (pCount.so == 0) {
                        continue;
                    }
                }

                int totig = pCount.sapc + pCount.whpc + (pCount.whcs * pCount.conversion);
                double orderAmt = pCount.fsovalue * pCount.fso;

                int unit = 0;
                try {
                    unit = pCount.fso / pCount.multi;
                }
                catch (Exception ex) { Log.e(TAG, ex.getMessage()); }

                String itemDesc = StringUtils.rightPad(pCount.desc + " " + pCount.barcode, 20, "");
                if(!woosimPrinter.printText(itemDesc, false, false, 1)) return result;

                String strValues = StringUtils.rightPad("", 22, "")
                        + StringUtils.leftPad(String.valueOf(pCount.ig), 4) + StringUtils.center(" ", 9)
                        + StringUtils.leftPad(String.valueOf(totig), 4) + StringUtils.center(" ", 10)
                        + StringUtils.leftPad(String.valueOf(pCount.fso), 4) + StringUtils.center(" ", 8)
                        // StringUtils.leftPad(String.valueOf(unit), 4) + StringUtils.center(" ", 8)
                        + StringUtils.leftPad(MainLibrary.priceDec.format(orderAmt), 13, "");

                if(!woosimPrinter.printText(strValues, true, false, 1)) return result;

                if(hasBarcode) {
                    String barcodeType = MainLibrary.GetBarcodeType(pCount.itembarcode);
                    if(!pCount.itembarcode.equals(""))
                        woosimPrinter.print1DBarcode(barcodeType, pCount.itembarcode);
                }

                if(!woosimPrinter.printText(" ", false, false, 1)) return result;

                if (pCount.so > 0) {
                    totsku = totsku + 1;
                }

                totfso = totfso + pCount.fso;
                totfsoval = totfsoval + (pCount.fsovalue * pCount.fso) ;
            }

            // FOOTER
            String footer = "";

            footer += Printer.woosimLines_4inch +"\n";
            footer += "Total: " + StringUtils.rightPad(String.valueOf(totsku),23/*76*/) + StringUtils.leftPad(String.valueOf(totfso), 21)
                    + StringUtils.rightPad(" ", 13) + StringUtils.leftPad(MainLibrary.priceDec.format(totfsoval), 13) + "\n";
            footer += "\n" + "\n" + "\n" + "\n" + "\n";
            footer += StringUtils.center(Printer.woosimLines2_4inch, 67);
            footer += StringUtils.center("Acknowledged by", 87);
            footer += "\n" + "\n"+ "\n";

            if(!woosimPrinter.printText(footer, true, false, 1)) return result;

            result = true;


        }
        catch (final IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PCountActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        return result;
    }

    private String PrintFormatTSC3inch(boolean hasBarcode) {

        len = 0;
        numItemsToPrint = 0;

        String toPrint = "";

        toPrint += "\n";
        toPrint += "Store: " + MainLibrary.gSelectedStores.storeName + "\n";


        String osascore = "";

        osascore = MainLibrary.GetOsaScorePcount(mHashmapPcountAll);

        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += StringUtils.rightPad("SKU",45,"") +
                StringUtils.rightPad("IG",14,"") +
                StringUtils.rightPad("Invty",14,"") +
                StringUtils.rightPad("Order qty", 14,"") +
                StringUtils.rightPad("Order amt", 14, "") + "\n";
        toPrint += Printer.tsclines;

        len += 1.5;

        int totsku = 0, totfso = 0;
        double totfsoval = 0;


        ArrayList<PCount> Barcodes = new ArrayList<>();
        if(Filtered)
        {
            //Barcodes = filteredItems;

            Barcodes = mPCountListViewAdapter.mArrayListPCountResultList;
        }
        else {

            Barcodes = mArrayListPcount;
        }

        for (PCount pCount : Barcodes)
        {
        //for (HashMap.Entry<String, PCount> entry : mHashmapPcountAll.entrySet()) {
         //   PCount pCount = entry.getValue();
            if (lprintwithso) {
                if (pCount.so == 0) {
                    continue;
                }
            }

            int lensku = 50 - pCount.barcode.length();
            int lenig = 18 - String.valueOf(pCount.ig).length();

            int totig = pCount.sapc + pCount.whpc + (pCount.whcs * pCount.conversion);

            int lenei = 14 - String.valueOf(totig).length();
            int lenfso = 12 - String.valueOf(pCount.fso).length(); // 18
            int lenfsoval = 12 - String.valueOf(pCount.fsovalue * pCount.fso).length(); // 18

            String barcodeType = MainLibrary.GetBarcodeType(pCount.itembarcode);
            String barcodeCmd = "";
            String endlines = "";
            if(hasBarcode) {
                barcodeCmd = "BARCODE ;\"" + barcodeType + "\",50,2,0,2,2,\"" + pCount.itembarcode + "\"" + "\n";
            }
            else {
                endlines += "\n";
            }

            toPrint += StringUtils.rightPad(pCount.desc + " " + pCount.barcode, 20, "") + "\n"
                    + barcodeCmd
                    + endlines
                    + "\nINFO ;"
                    + StringUtils.rightPad(" ", 47,"")
                    + StringUtils.rightPad(String.valueOf(pCount.ig), lenig)
                    + StringUtils.rightPad(String.valueOf(totig),lenei)
                    + "*"
                    + StringUtils.rightPad(String.valueOf(pCount.fso), lenfso, "")
                    + StringUtils.rightPad(MainLibrary.priceDec.format(pCount.fsovalue * pCount.fso), lenfsoval, "")
                    + "*"
                    + StringUtils.rightPad("       ", lensku,"")
                    + "\n";

            if (pCount.so > 0) {
                totsku = totsku + 1;
            }

            numItemsToPrint++;
            len += 0.70;

            totfso = totfso + pCount.fso;
            totfsoval = totfsoval + (pCount.fsovalue * pCount.fso) ;
        }

        toPrint += Printer.tsclines;
        toPrint += "Total: " + StringUtils.rightPad(String.valueOf(totsku),32/*76*/) + StringUtils.rightPad(String.valueOf(totfso),11)
                + StringUtils.rightPad(MainLibrary.priceDec.format(totfsoval),12) + "\n";
        toPrint += "\n" + "\n" + "\n" + "\n" + "\n";
        toPrint += StringUtils.center(Printer.tsclines2, 80);
        toPrint += StringUtils.center("Acknowledged by", 80);

        lprintwithso = false;

        len += 1.60;

        return toPrint;
    }

    private String PrintFormatTSC4inch(boolean hasBarcode) {

        len = 0;
        numItemsToPrint = 0;

        String toPrint = "";

        toPrint += "\n";
        toPrint += "Store: " + MainLibrary.gSelectedStores.storeName + "\n";

        String osascore = "";

        osascore = MainLibrary.GetOsaScorePcount(mHashmapPcountAll);

        toPrint += "Date: " + MainLibrary.gStrCurrentDate +"\n";
        toPrint += "OSA Score: " + osascore + "\n\n";
        toPrint += StringUtils.rightPad("SKU",26,"") +
                StringUtils.rightPad("IG",12,"") +
                //StringUtils.rightPad("Invty",14,"") +
                StringUtils.rightPad("Final SO",14,"") +
                StringUtils.rightPad("Unit",10,"") +
                StringUtils.rightPad("Order amt", 14, "") + "\n";
        toPrint += Printer.tsclines;

        len += 1.5;

        int totsku = 0, totfso = 0;
        double totfsoval = 0;

        ArrayList<PCount> Barcodes = new ArrayList<>();
        if(Filtered)
        {
            //Barcodes = filteredItems
            Barcodes = mPCountListViewAdapter.mArrayListPCountResultList;
        }
        else {

            Barcodes = mArrayListPcount;
        }

        //for (HashMap.Entry<String, Assortment> entry : mHashmapAssortmentAll.entrySet())
        for (PCount pCount : Barcodes)
        {
        //for (String barcodeKey : itemOrdered) {
            //PCount pCount = mHashmapPcountAll.get(barcodeKey);

            if(pCount == null)
                continue;

            if (lprintwithso) {
                if (pCount.so == 0) {
                    continue;
                }
            }

            int lensku = 50 - pCount.barcode.length();
            int lenig = 15 - String.valueOf(pCount.ig).length();

            int totig = pCount.sapc + pCount.whpc + (pCount.whcs * pCount.conversion);

            int lenei = 15 - String.valueOf(totig).length();
            int lenfso = 16 - String.valueOf(pCount.fso).length(); // 18

            int unit = 0;
            try {
                unit = pCount.fso / pCount.multi;
            }
            catch (Exception ex) { Log.e(TAG, ex.getMessage()); }

            int lenUnit = 14 - String.valueOf(unit).length();
            int lenfsoval = 12 - String.valueOf(pCount.fsovalue * pCount.fso).length(); // 18

            String barcodeType = MainLibrary.GetBarcodeType(pCount.itembarcode);
            String barcodeCmd = "";
            String endlines = "";
            if(hasBarcode) {
                barcodeCmd = "BARCODE ;\"" + barcodeType + "\",70,2,0,2,2,\"" + pCount.itembarcode + "\"" + "\n";
            }
            else {
                endlines += "\n";
            }

            toPrint += StringUtils.rightPad(pCount.desc + " " + pCount.barcode, 20, "") + "\n"
                    + barcodeCmd
                    + endlines
                    + "\nINFO ;"
                    + StringUtils.rightPad(" ", 29,"")
                    + StringUtils.rightPad(String.valueOf(pCount.ig), lenig, "")
                    //+ StringUtils.rightPad(String.valueOf(totig),lenei, "")
                    + "*"
                    //+ StringUtils.rightPad(" ", 12,"")
                    + StringUtils.rightPad(String.valueOf(pCount.fso), lenfso, "")
                    + StringUtils.rightPad(String.valueOf(unit), lenUnit, "")
                    + StringUtils.rightPad(MainLibrary.priceDec.format(pCount.fsovalue * pCount.fso), lenfsoval, "")
                    + "*"
                    + StringUtils.rightPad("       ", lensku,"")
                    + "\n";

            if (pCount.so > 0) {
                totsku = totsku + 1;
            }

            numItemsToPrint++;
            len += 1;

            totfso = totfso + pCount.fso;
            totfsoval = totfsoval + (pCount.fsovalue * pCount.fso) ;

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

        final Dialog dialog = new Dialog(PCountActivity.this, R.style.Transparent);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.activity_branch2);

        TextView lvCaption = (TextView) dialog.findViewById(R.id.textViewBranchName);

        if (filterCode > 3) {

            mPCountListViewAdapter.filter(filterCode, "xxx");
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

            Cursor tmpCategory = db.GetGroupby(filterId, "pcount");

            String[] from = new String[] {
                    filterId
            };
            int[] to = new int[] { R.id.itemTextView };

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(PCountActivity.this, R.layout.activity_items_filtering,
                    tmpCategory, from, to, 0);
            final ListView lv = (ListView) dialog.findViewById(R.id.listViewBranch);
            lv.setAdapter(adapter);
            lvCaption.setText("Select " + filterTitle);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    dialog.dismiss();

                    TextView c = (TextView) view.findViewById(R.id.itemTextView);
                    String name = c.getText().toString();
                    Toast.makeText(PCountActivity.this, name, Toast.LENGTH_SHORT).show();
                    mPCountListViewAdapter.filter(filterCode, name);

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

        final Dialog dialog = new Dialog(PCountActivity.this, R.style.Transparent);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        TextView tvwTotalIG = null;
        TextView tvwTotalInventory = null;
        TextView tvwTotalFSO = null;
        TextView tvwTotalUnit = null;
        TextView tvwGrandTotal = null;
        TextView tvwTotalItems = null;

        if(reportType == 4) { // with so
            dialog.setContentView(R.layout.activity_report_withso);
            tvwTotalIG = (TextView) dialog.findViewById(R.id.tvwTotalIG);
            tvwTotalInventory = (TextView) dialog.findViewById(R.id.tvwTotalInventory);
            tvwTotalFSO = (TextView) dialog.findViewById(R.id.tvwTotalFSO);
            tvwTotalUnit = (TextView) dialog.findViewById(R.id.tvwTotalUnit);
            tvwGrandTotal = (TextView) dialog.findViewById(R.id.tvwGrandTotal);
            tvwTotalItems = (TextView) dialog.findViewById(R.id.tvwTotalItems);
        }
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

        cursorGroup = db.queryData("select " + filterId + " as name, " + SQLiteDB.COLUMN_PCOUNT_BARCODE + " from " + SQLiteDB.TABLE_PCOUNT + " where storeid = " + String.valueOf(MainLibrary.gSelectedStores.webStoreId) +
                    " group by " + filterId);

        cursorGroup.moveToFirst();

        while (!cursorGroup.isAfterLast()) {

            arrayListReport.add(new ReportClass(cursorGroup.getString(cursorGroup.getColumnIndex("name")).trim(),
                    cursorGroup.getString(cursorGroup.getColumnIndex(SQLiteDB.COLUMN_PCOUNT_BARCODE)).trim()));

            cursorGroup.moveToNext();
        }

        for (HashMap.Entry<String, PCount> entry : mHashmapPcountAll.entrySet()) {
            PCount pCount = entry.getValue();

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
                        if (!reportClass.name.contains(pCount.category)) {
                            continue;
                        }
                        break;
                    case 1:
                        if (!reportClass.name.contains(pCount.subcate)) {
                            continue;
                        }
                        break;
                    case 2:
                        if (!reportClass.name.contains(pCount.brand)) {
                            continue;
                        }
                        break;
                    case 3:
                        if (!reportClass.name.contains(pCount.division)) {
                            continue;
                        }
                        break;
                    case 4:
 /*                       lvalid = pCount.sapc != 0 || pCount.whpc != 0 || pCount.whcs != 0;
                        if (!lvalid) {
                            continue;
                        }*/
                        if (!reportClass.name.contains(pCount.desc)) {
                            continue;
                        }
                        break;
                    default:
                        if (!reportClass.name.contains(pCount.desc)) {
                            continue;
                        }
                }

                reportClass.ig = reportClass.ig + pCount.ig;
                reportClass.so = reportClass.so + pCount.so;
                reportClass.endinv = reportClass.endinv + (pCount.sapc + pCount.whpc + (pCount.whcs * pCount.conversion));
                reportClass.finalso = reportClass.finalso + pCount.fso;
                reportClass.multi = pCount.multi;
                try {
                    reportClass.unit = pCount.fso / pCount.multi;
                }
                catch (Exception ex) { reportClass.unit = 0; }
                reportClass.orderAmount = String.format("%.2f", pCount.fsovalue * pCount.fso);
            }
        }

        ArrayList<ReportClass> arrayListReport2 = new ArrayList<>();
        double dGrandTotal = 0;
        int nTotalUnit = 0;
        int nTotalIG = 0;
        int nTotalInventory = 0;
        int nTotalFSO = 0;
        int nTotalItems = 0;

        if (reportType == 4){
            Iterator i = arrayListReport.iterator();
            while(i.hasNext()){
                ReportClass reportClass = (ReportClass) i.next();
                if (reportClass.so > 0){
//                    reportClass.finalso = reportClass.finalso; //- (reportClass.so % reportClass.multi) + reportClass.multi;
                    dGrandTotal += Double.valueOf(reportClass.orderAmount);
                    nTotalIG += reportClass.ig;
                    nTotalUnit += reportClass.unit;
                    nTotalFSO += reportClass.finalso;
                    nTotalInventory += reportClass.endinv;
                    nTotalItems += 1;
                    arrayListReport2.add(reportClass);
                }else{
                    i.remove();
                }
            }
        }

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
            listViewReport.setAdapter(new ReportWithSoAdapter(PCountActivity.this, arrayListReport2));
            String strTotal = MainLibrary.priceDec.format(dGrandTotal);
            String strItems = "Items: " + String.valueOf(nTotalItems);

            tvwGrandTotal.setText(strTotal);
            tvwTotalFSO.setText(String.valueOf(nTotalFSO));
            tvwTotalIG.setText(String.valueOf(nTotalIG));
            tvwTotalInventory.setText(String.valueOf(nTotalInventory));
            tvwTotalUnit.setText(String.valueOf(nTotalUnit));
            tvwTotalItems.setText(strItems);
        }else{
            listViewReport.setAdapter(new ReportListViewAdapter(PCountActivity.this, arrayListReport));
        }

        dialog.show();

    }

    public void ViewReports2(int reportType) {

        final Dialog dialog = new Dialog(PCountActivity.this, R.style.Transparent);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.activity_report);

        final ListView listViewReport = (ListView) dialog.findViewById(R.id.listViewReport);

        ArrayList<PCount> mArrayListPcountAll =
                new ArrayList<>(mHashmapPcountAll.values());

        final PCountListViewAdapter reportAdapter = new PCountListViewAdapter(PCountActivity.this, mArrayListPcountAll);
        listViewReport.setAdapter(reportAdapter);

        if (reportType == 0) {
            reportAdapter.filter(4, "xxx");
        } else {
            reportAdapter.filter(5, "xxx");
        }

        dialog.show();

    }

    private class UserLogout extends AsyncTask<Void, Void, Boolean> {

        String response;
        String errmsg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDL = ProgressDialog.show(PCountActivity.this, "", "Logging out. Please Wait...", true);
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
                    Log.e(TAG, mex.getMessage());
                    errmsg += "\n" + mex.getMessage();
                }

            } catch(Exception e){
                e.printStackTrace();
                Log.e(TAG, e.getMessage(), e);
                errmsg += "\n" + e.getMessage();
            }
            return bReturn;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            pDL.dismiss();
            Intent intentMain = new Intent(PCountActivity.this, MainActivity.class);
            if(!success) {
                //Toast.makeText(PCountActivity.this, errmsg, Toast.LENGTH_LONG).show();
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
                Toast.makeText(PCountActivity.this, msg, Toast.LENGTH_SHORT).show();
                startActivity(intentMain);
                finish();
            }
            catch (JSONException jex) {
                jex.printStackTrace();
                Log.e(TAG, jex.getMessage());
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PCountActivity.this);
        printEnabled = prefs.getBoolean("switch_printer_enabled", false);
        if(printEnabled)
            CheckWoosimPrinter();
    }

    private void CheckWoosimPrinter() {
        try {
            if (MainLibrary.mSelectedPrinter == MainLibrary.PRINTER.WOOSIM) {
                woosimPrinter = new WoosimPrinter(PCountActivity.this);
                woosimPrinter.SetUpWoosim();
            }
        }
        catch (Exception ex) {
            String errmsg = "Can't connect to printer. Please check printer.";
            String errException = ex.getMessage() != null ? ex.getMessage() : errmsg;
            Log.e(TAG, errException);
            MainLibrary.errorLog.appendLog(errException, TAG);
            Toast.makeText(PCountActivity.this, "Failed Connection: " + errmsg, Toast.LENGTH_LONG).show();
        }
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
    }
}
  /* woosim 4 inch
                int totig = pCount.sapc + pCount.whpc + (pCount.whcs * pCount.conversion);
                double orderAmt = pCount.fsovalue * pCount.fso;

                int unit = 0;
                try {
                    unit = pCount.fso / pCount.multi;
                }
                catch (Exception ex) { Log.e(TAG, ex.getMessage()); }

                String itemDesc = StringUtils.rightPad(pCount.desc + " " + pCount.barcode, 20, "");
                if(!woosimPrinter.printText(itemDesc, false, false, 2)) return result;

                String strValues = StringUtils.rightPad("", 20, "")
                        + StringUtils.leftPad(String.valueOf(pCount.ig), 4) + StringUtils.center(" ", 6)
                        + StringUtils.leftPad(String.valueOf(totig), 4) + StringUtils.center(" ", 7)
                        + StringUtils.leftPad(String.valueOf(pCount.fso), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(String.valueOf(unit), 4) + StringUtils.center(" ", 5)
                        + StringUtils.leftPad(MainLibrary.priceDec.format(orderAmt), 10, "");

                if(!woosimPrinter.printText(strValues, true, false, 2)) return result;

                if(hasBarcode) {
                    String barcodeType = MainLibrary.GetBarcodeType(pCount.itembarcode);
                    if(!pCount.itembarcode.equals(""))
                        woosimPrinter.print1DBarcode(barcodeType, pCount.itembarcode);
                }

                if(!woosimPrinter.printText(" ", false, false, 2)) return result;

                if (pCount.so > 0) {
                    totsku = totsku + 1;
                }

                totfso = totfso + pCount.fso;
                totfsoval = totfsoval + (pCount.fsovalue * pCount.fso) ;
            }

            // FOOTER
            String footer = "";

            footer += Printer.woosimLines_4inch;

            footer += "Total: " + StringUtils.rightPad(String.valueOf(totsku),20) + StringUtils.leftPad(String.valueOf(totfso), 18)
                    + StringUtils.rightPad(" ", 14) + StringUtils.leftPad(MainLibrary.priceDec.format(totfsoval), 10) + "\n";
            footer += "\n" + "\n" + "\n" + "\n" + "\n";
            footer += StringUtils.center(Printer.woosimLines2_4inch, 64);
            footer += StringUtils.center("Acknowledged by", 84);
            footer += "\n" + "\n"+ "\n";

            if(!woosimPrinter.printText(footer, true, false, 2)) return result;

            result = true;
            */
