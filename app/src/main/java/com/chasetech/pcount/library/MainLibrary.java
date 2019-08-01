package com.chasetech.pcount.library;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.DatePicker;

import com.chasetech.pcount.Assortment.Assortment;
import com.chasetech.pcount.Promo.Promo;
import com.chasetech.pcount.StoresActivity;
import com.chasetech.pcount.ErrorLog.ErrorLog;
import com.chasetech.pcount.MKL.PCount;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Vinid on 10/25/2015.
 */
public class MainLibrary extends Application {

    public static boolean BETA = false;
    public static boolean DAVIES_VER = true;

    public static String dateLog = "";

    public static final String API_URL = "http://ulp-projectsos.com";
    public static final String API_URL_BETA = "http://test.ulp-projectsos.com";
    public static final String DAVIES = "http://daviespcount.chasetech.com";

    public static final String API_URL_ASSORTMENT_IMAGE = MainLibrary.API_URL + "/api/uploadassortmentimage";
    public static final String API_URL_ASSORTMENT_POSTING = MainLibrary.API_URL + "/api/uploadassortment";

    public static final String API_URL_PROMO_IMAGE = MainLibrary.API_URL + "/api/uploadpromoimage";
    public static final String API_URL_PROMO_POSTING = MainLibrary.API_URL + "/api/uploadpromo";

    public static final String API_URL_PROMO_IMAGE_BETA = MainLibrary.API_URL_BETA + "/api/uploadpromoimage";
    public static final String API_URL_PROMO_POSTING_BETA = MainLibrary.API_URL_BETA + "/api/uploadpromo";

    public static final String API_URL_ASSORTMENT_IMAGE_BETA = MainLibrary.API_URL_BETA + "/api/uploadassortmentimage";
    public static final String API_URL_ASSORTMENT_POSTING_BETA = MainLibrary.API_URL_BETA + "/api/uploadassortment";

    public static final String API_URL_MKL_IMAGE = MainLibrary.API_URL + "/api/uploadimage";
    public static final String API_URL_MKL_POSTING = MainLibrary.API_URL + "/api/uploadpcount";

    public static final String API_URL_MKL_IMAGE_BETA = MainLibrary.API_URL_BETA + "/api/uploadimage";
    public static final String API_URL_MKL_POSTING_BETA = MainLibrary.API_URL_BETA + "/api/uploadpcount";

    public static final String API_GET_PRNFILENAMES = MainLibrary.API_URL + "/api/prnlist";
    public static final String API_GET_PRNFILENAMES_BETA = MainLibrary.API_URL_BETA + "/api/prnlist";
    public static final String API_GET_PRNFILES = MainLibrary.API_URL + "/api/downloadprn/";
    public static final String API_GET_PRNFILES_BETA = MainLibrary.API_URL_BETA + "/api/downloadprn/";

    public static final String API_UPLOAD_BACKUP = MainLibrary.API_URL + "/api/uploadbackup";
    public static final String API_UPLOAD_BACKUP_BETA = MainLibrary.API_URL_BETA + "/api/uploadbackup";

    public static final String API_CHECK_BACKUP_LIST = MainLibrary.API_URL + "/api/backuplist";
    public static final String API_CHECK_BACKUP_LIST_BETA = MainLibrary.API_URL_BETA + "/api/backuplist";

    public static final String API_DOWNLOAD_BACKUP = MainLibrary.API_URL + "/api/downloadbackup";
    public static final String API_DOWNLOAD_BACKUP_BETA = MainLibrary.API_URL_BETA + "/api/downloadbackup";

    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
    public static final SimpleDateFormat monthFormatter = new SimpleDateFormat("MM", Locale.US);

    public static String CLEAR_USER_PASSWORD = "00000";
    public static DecimalFormat priceDec = new DecimalFormat("###,###,###.00");
    public static String CompanyCode = "";
    public static File ROOT_SDCARD = Environment.getExternalStorageDirectory();
    public static String DOWNLOAD_FOLDER = "Download";
    public static File PRN_FOLDER = null;
    public static StoresActivity selContext;
    public static String ShowDateReturn;
    public static String typefacename = "fonts/fontawesome-webfont.ttf";
    public static Boolean isMasterfileUpdated = true;
    public static Stores gSelectedStores;
    public static String gStrCurrentDate = "";
    public static String selectedMonth = "";
    public static int gStrCurrentUserID;
    public static String gStrCurrentUserName = "";
    public static String gStrDeviceId = "";
    public static boolean isAssortmentMode = false;
    public static boolean isPromoMode = false;
    public static int versionCode = 0;
    public static String versionName = "";
    public static String errlogFile = "errorlogs.txt";
    public static ErrorLog errorLog;

    // SETTINGS
    public static boolean allowIG = true;
    public static boolean MKL_allItemsReqForPosting = true;
    public static boolean MKL_allItemsReqForPrinting = false;
    public static boolean ASSORT_allItemsReqForPosting = true;
    public static boolean ASSORT_allItemsReqForPrinting = false;
    public static boolean PROMO_allItemsReqForPosting = false;
    public static boolean PROMO_allItemsReqForPrinting = false;

    public static boolean FIX_IG_BUG = true;

    public static boolean MKL_validateDatePosting = true;
    public static boolean ASSORT_validateDatePosting = true;
    public static boolean PROMO_validateDatePosting = false;

    public static ProgressDialog mProgressDL;

    public static ArrayList<PCount> arrPcountEditedIg = new ArrayList<>();

    public static SharedPreferences spSettings;
    public static String masterfileDateRealese = "";

    public static File appFolder = null;
    public static File dbFolder = null;
    public static int Counter;

    public static boolean InitializedHash = false;

    public static String ICON_STAR_PENDING = "\uf006";
    public static String ICON_STAR_PARTIAL = "\uf123";
    public static String ICON_STAR_COMPLETE = "\uf005";
    public static String ICON_PASSED = "\uf00c";
    public static String ICON_FAILED = "\uf00d";
    public static String savedHashKey = "";

    public static boolean ValidateStoreIfFourInch() {
        return MainLibrary.gSelectedStores.channelArea.toUpperCase().equals("MDC")
                || MainLibrary.gSelectedStores.channelArea.toUpperCase().equals("SOUTH STAR DRUG")
                || MainLibrary.gSelectedStores.channelArea.toUpperCase().equals("ROSE PHARMACY")
                || MainLibrary.gSelectedStores.channelArea.toUpperCase().equals("ST. JOSEPH DRUG")
                || MainLibrary.gSelectedStores.channelArea.toUpperCase().equals("360 PHARMACY");
    }

    public enum PRINTER {
        TSC,
        WOOSIM
    }

    public static PRINTER mSelectedPrinter = PRINTER.TSC;

    public static void LoadAllFolders(Context mContext) {
        appFolder = new File(mContext.getExternalFilesDir(null), "");
        dbFolder = new File(appFolder, "Database");
        if(!dbFolder.exists()) {
            dbFolder.mkdirs();
        }
    }

    private static final String[] PRN_FILES = new String[] {
            "DEFAULT.PRN",
            "FAMILY.PRN",
            "LAWSON.PRN",
            "ALFAMART.PRN",
            "MERCURY.PRN",
            "711.PRN",
            "MINISTOP.PRN"
    };

    public static final String[] aModules = new String[] {
            "SO / OSA",
            "ASSORTMENT",
            "PROMO"
    };

    public static final String MKL_TEXTFILE = "mkl.txt";
    public static final String ASSORTMENT_TXTFILE = "assortment.txt";
    public static final String SETTINGS_TXTFILE = "settings.txt";
    public static final String STORES_TXTFILE = "stores.txt";
    public static final String PROMO_TEXTFILE = "promo.txt";

    public enum STORE_TYPES {
        DEFAULT,
        SEVEN_ELEVEN,
        MINISTOP,
        FAMILY_MART,
        MERCURY_DRUG,
        LAWSON,
        ALFAMART,
        ROSE_PHARMACY,
        ST_JOSEPH_DRUG,
        SOUTH_STAR_DRUG,
        THREESIXTY_PHARMACY
    }

    public static STORE_TYPES eStore = STORE_TYPES.DEFAULT;

    public static void LoadFolders() {
        PRN_FOLDER = new File(ROOT_SDCARD, DOWNLOAD_FOLDER);
        if(!PRN_FOLDER.exists())
            PRN_FOLDER.mkdirs();
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static void CopyFile(File fileFrom, File fileTo) throws IOException {

        InputStream in = new FileInputStream(fileFrom);
        OutputStream out = new FileOutputStream(fileTo);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }

    public static String createInsertBulkQuery(String tableName, String[] aFields) {

        String strReturn = "";

        String questionMarks = "";
        String strColumns = "";

        for (int i = 1; i <= aFields.length; i++) {

            questionMarks += "?";
            strColumns += aFields[i-1];

            if(i < aFields.length) {
                questionMarks += ",";
                strColumns += ",";
            }
        }

        strReturn = "INSERT INTO " + tableName + "(" + strColumns + ") VALUES (" + questionMarks + ");";
        return strReturn;
    }

    public static String GetBarcodeType(String barcode) {
        String barcodeType = "128";
        if(barcode.length() == 13) barcodeType = "EAN13";
        if(barcode.length() == 8) barcodeType = "EAN8";

        return barcodeType;
    }

    public void ShowCalendar() {

        // Process to get Current Date
        final Calendar c = Calendar.getInstance();
        final int mYear = c.get(Calendar.YEAR);
        final int mMonth = c.get(Calendar.MONTH);
        final int mDay = c.get(Calendar.DAY_OF_MONTH);

        // Launch Date Picker Dialog
        DatePickerDialog dpd = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {

                        Calendar newDate = Calendar.getInstance();
                        newDate.set(year, monthOfYear, dayOfMonth);
                        String date = MainLibrary.dateFormatter.format(newDate.getTime());

                    }
                }, mYear, mMonth, mDay);
        dpd.show();
    }

    public static boolean CheckBluetooth() {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        return bt.isEnabled();
    }

    public static String GetOsaScorePcount(HashMap<String, PCount> mHashmapPcountAll) {
        int nSkuWithStocks = 0;
        int nTotSku = 0;

        double nOsaScore;

        for (HashMap.Entry<String, PCount> entry : mHashmapPcountAll.entrySet()) {
            PCount pcount = entry.getValue();
            nTotSku++;
            if(pcount.sapc != 0 || pcount.whpc != 0 || pcount.whcs != 0) {
                int totStocks = pcount.sapc + pcount.whpc + (pcount.whcs * pcount.conversion);
                if(totStocks > pcount.minstock)
                    nSkuWithStocks++;
            }
        }


        nOsaScore = (Double.valueOf(nSkuWithStocks) / Double.valueOf(nTotSku)) * 100;
        return String.format(Locale.getDefault(), "%.2f", nOsaScore) + " %";
    }

    public static String GetOsaScoreAssortment(HashMap<String, Assortment> mHashmapAssort) {
        int nSkuWithStocks = 0;
        int nTotSku = 0;

        double nOsaScore;

        for (HashMap.Entry<String, Assortment> entry : mHashmapAssort.entrySet()) {
            Assortment assortment = entry.getValue();
            nTotSku++;
            if(assortment.sapc != 0 || assortment.whpc != 0 || assortment.whcs != 0) {
                int totStocks = assortment.sapc + assortment.whpc + (assortment.whcs * assortment.conversion);
                if(totStocks > assortment.minstock)
                    nSkuWithStocks++;
            }
        }
        nOsaScore = (Double.valueOf(nSkuWithStocks) / Double.valueOf(nTotSku)) * 100;

        return String.format(Locale.getDefault(), "%.2f", nOsaScore) + " %";
    }

    public static String GetOsaScorePromo(HashMap<String, Promo> mhashmapPromo) {
        int nSkuWithStocks = 0;
        int nTotSku = 0;

        double nOsaScore;

        for (HashMap.Entry<String, Promo> entry : mhashmapPromo.entrySet()) {
            Promo promo = entry.getValue();
            nTotSku++;
            if(promo.sapc != 0 || promo.whpc != 0 || promo.whcs != 0) {
                int totStocks = promo.sapc + promo.whpc + (promo.whcs * promo.conversion);
                if(totStocks > promo.minstock)
                    nSkuWithStocks++;
            }
        }
        nOsaScore = (Double.valueOf(nSkuWithStocks) / Double.valueOf(nTotSku)) * 100;

        return String.format(Locale.getDefault(), "%.2f", nOsaScore) + " %";
    }

    public static String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getDateTimeToday() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "MM/dd/yyyy HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getDateToday() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "MM/dd/yyyy", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getDateTodayNoSeperator() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "MMddyyyy", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getTimeToday() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "HH:mm", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        String model = Build.MODEL.toUpperCase();
        if (model.startsWith(manufacturer)) {
            return model;
        }
        return manufacturer + " " + model;
    }

    public static boolean ValidatedRepostingDate() {
        boolean result = false;

        try {

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());

            Calendar mCalendar = Calendar.getInstance(Locale.GERMANY);
            mCalendar.setTime(new Date());

            int todayWeekNumber = mCalendar.get(Calendar.WEEK_OF_YEAR);

            Calendar mCalendarTrans = Calendar.getInstance(Locale.GERMANY);
            mCalendarTrans.setTime(dateFormat.parse(MainLibrary.gStrCurrentDate));

            int transWeekNumber = mCalendarTrans.get(Calendar.WEEK_OF_YEAR);

            if(todayWeekNumber == transWeekNumber) {
                result = true;
            }

        }
        catch (ParseException ex) {
            String err = ex.getMessage() != null ? ex.getMessage() : "Parse date error.";
            Log.e("ValidationPost", err);
            MainLibrary.errorLog.appendLog(err, "ValidationPost");
        }

        return result;
    }

    public static String GetDeviceOsVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    public static String GetApiLevelDevice() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }

    public static void messageBox(Context mContext, String title, String message) {
        new AlertDialog.Builder(mContext)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    public static final String CHANGE_LOGS = "3.0.5\n" +
                    "   * Integrate South star drug, St. Joseph and Rose Pharmacy logos in Woosim printer.\n" +
                    "   * Added total data rows validation in saving masterfile.\n" +
                    "   * Fix some bugs.";
}
