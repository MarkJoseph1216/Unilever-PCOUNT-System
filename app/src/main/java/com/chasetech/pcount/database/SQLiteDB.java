package com.chasetech.pcount.database;

/**
 * Created by ULTRABOOK on 10/14/2015.
 */
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.chasetech.pcount.ErrorLog.FixBugTask;
import com.chasetech.pcount.R;
import com.chasetech.pcount.library.MainLibrary;

import java.io.File;
import java.util.ArrayList;

public class SQLiteDB extends SQLiteOpenHelper {

    Context mContext;

    public static ArrayList<String> aTransOsaColumns;

    public static final String DATABASE_NAME  = "UPcDb";
    public static final String PORTABLE_DATABASE_NAME  = "pcountDB";
    private static final String TAG = "SettingsProvider";
    public static int DATABASE_VERSION = 20;
    //19 LIVE
    public static int DATABASE_BACKUP_VERSION = -1;

    //Pcount MainTable
    public static final String TABLE_PCOUNT = "pcount";
    public static final String COLUMN_PCOUNT_ID = "id";
    public static final String COLUMN_PCOUNT_BARCODE = "barcode";
    public static final String COLUMN_PCOUNT_DESC = "desc";
    public static final String COLUMN_PCOUNT_IG = "ig";
    public static final String COLUMN_PCOUNT_SAPC = "sapc";
    public static final String COLUMN_PCOUNT_WHPC = "whpc";
    public static final String COLUMN_PCOUNT_WHCS = "whcs";
    public static final String COLUMN_PCOUNT_CONVERSION = "conversion";
    public static final String COLUMN_PCOUNT_SO = "so";
    public static final String COLUMN_PCOUNT_FSO = "fso";
    public static final String COLUMN_PCOUNT_CATEGORYID = "categoryid";
    public static final String COLUMN_PCOUNT_BRANDID = "brandid";
    public static final String COLUMN_PCOUNT_DIVISIONID = "divisionid";
    public static final String COLUMN_PCOUNT_SUBCATEGORYID = "subcategoryid";
    public static final String COLUMN_PCOUNT_STOREID = "storeid";
    public static final String COLUMN_PCOUNT_FSOVALUE = "fsovalue";
    public static final String COLUMN_PCOUNT_WEBID = "webid";
    public static final String COLUMN_PCOUNT_MULTI = "multi";
    public static final String COLUMN_PCOUNT_OTHERBARCODE = "otherbarcode"; // new column v2.4
    public static final String COLUMN_PCOUNT_MINSTOCK = "minstock"; // new column v2.9
    public static final String COLUMN_PCOUNT_CATEGORY = "category"; // new column v.2.9.12
    public static final String COLUMN_PCOUNT_DESCLONG = "description_long"; // new column v.2.9.12
    public static final String COLUMN_PCOUNT_OLDIG = "old_ig"; // new column db v.18
    public static final String COLUMN_PCOUNT_OSATAG = "osa_tag"; // new column db v.18
    public static final String COLUMN_PCOUNT_NPITAG = "npi_tag"; // new column db v.18

    public static final String DATABASE_CREATE_TABLE_PCOUNT = "CREATE TABLE IF NOT EXISTS " + TABLE_PCOUNT + "("
            + COLUMN_PCOUNT_ID + " integer PRIMARY KEY, "
            + COLUMN_PCOUNT_BARCODE + " text, "
            + COLUMN_PCOUNT_DESC + " text, "
            + COLUMN_PCOUNT_IG + " integer, "
            + COLUMN_PCOUNT_SAPC + " integer, "
            + COLUMN_PCOUNT_WHPC + " integer, "
            + COLUMN_PCOUNT_WHCS + " integer, "
            + COLUMN_PCOUNT_CONVERSION + " integer, "
            + COLUMN_PCOUNT_SO + " integer, "
            + COLUMN_PCOUNT_FSO + " integer,"
            + COLUMN_PCOUNT_CATEGORYID + " text,"
            + COLUMN_PCOUNT_BRANDID + " text,"
            + COLUMN_PCOUNT_DIVISIONID + " text,"
            + COLUMN_PCOUNT_SUBCATEGORYID + " text, "
            + COLUMN_PCOUNT_STOREID + " integer, "
            + COLUMN_PCOUNT_FSOVALUE + " real, "
            + COLUMN_PCOUNT_WEBID + " text, "
            + COLUMN_PCOUNT_MULTI + " integer, "
            + COLUMN_PCOUNT_OTHERBARCODE + " text, "
            + COLUMN_PCOUNT_MINSTOCK + " numeric, "
            + COLUMN_PCOUNT_CATEGORY + " text, "
            + COLUMN_PCOUNT_DESCLONG + " text, "
            + COLUMN_PCOUNT_OLDIG + " numeric, "
            + COLUMN_PCOUNT_OSATAG + " integer, "
            + COLUMN_PCOUNT_NPITAG + " integer)";

    //store Table
    public static final String TABLE_STORE = "branch";
    public static final String COLUMN_STORE_ID = "id";
    public static final String COLUMN_STORE_BID = "bid";
    public static final String COLUMN_STORE_STORECODE = "storeCode"; // new column v.2.6
    public static final String COLUMN_STORE_DESC = "bdesc";
    public static final String COLUMN_STORE_MULTIPLE = "multiple";
    public static final String COLUMN_STORE_CHANNELID = "channelid"; // new column v.2.6
    public static final String COLUMN_STORE_CHANNELDESC = "channeldesc"; // new column v.2.6
    public static final String COLUMN_STORE_CHANNELAREA = "area"; // new column v.2.9.6

//    public static final String COLUMN_STORE_ENROLLMENT = "enrollment"; // new column v.2.9.12
//    public static final String COLUMN_STORE_DISTRIBUTORCODE = "distributor_code"; // new column v.2.9.12
//    public static final String COLUMN_STORE_DISTRIBUTOR = "distributor"; // new column v.2.9.12
//    public static final String COLUMN_STORE_STOREID = "storeid"; // new column v.2.9.12
//    public static final String COLUMN_STORE_STORECODEPSUP = "store_code_psup"; // new column v.2.9.12
//    public static final String COLUMN_STORE_CLIENTCODE = "client_code"; // new column v.2.9.12
//    public static final String COLUMN_STORE_CLIENTNAME = "client_name"; // new column v.2.9.12
//    public static final String COLUMN_STORE_CHANNELCODE = "channel_code"; // new column v.2.9.12
//    public static final String COLUMN_STORE_CUSTOMERCODE = "customer_code"; // new column v.2.9.12
//    public static final String COLUMN_STORE_CUSTOMERNAME = "customer_name"; // new column v.2.9.12
//    public static final String COLUMN_STORE_REGIONCODE = "region_code"; // new column v.2.9.12
//    public static final String COLUMN_STORE_REGIONSHORT = "region_short"; // new column v.2.9.12
//    public static final String COLUMN_STORE_REGION = "region"; // new column v.2.9.12
//    public static final String COLUMN_STORE_AGENCYCODE = "agency_code"; // new column v.2.9.12
//    public static final String COLUMN_STORE_AGENCYNAME = "agency_name"; // new column v.2.9.12

    public static final String DATABASE_CREATE_TABLE_STORE = "CREATE TABLE IF NOT EXISTS " + TABLE_STORE + "("
            + COLUMN_STORE_ID + " integer PRIMARY KEY, "
            + COLUMN_STORE_BID + " integer, "
            + COLUMN_STORE_STORECODE + " text, "
            + COLUMN_STORE_DESC + " text, "
            + COLUMN_STORE_MULTIPLE + " integer, "
            + COLUMN_STORE_CHANNELID + " integer, "
            + COLUMN_STORE_CHANNELDESC + " text, "
            + COLUMN_STORE_CHANNELAREA + " text)";


    //STORE 2 Table
    public static final String TABLE_STORE2 = "tblStore2";
    public static final String COLUMN_STORE2_ID = "id";
    public static final String COLUMN_STORE2_BID = "bid";
    public static final String COLUMN_STORE2_STORECODE = "storeCode"; // new column v.2.6
    public static final String COLUMN_STORE2_DESC = "bdesc";
    public static final String COLUMN_STORE2_MULTIPLE = "multiple";
    public static final String COLUMN_STORE2_CHANNELID = "channelid"; // new column v.2.6
    public static final String COLUMN_STORE2_CHANNELDESC = "channeldesc"; // new column v.2.6
    public static final String COLUMN_STORE2_CHANNELAREA = "area"; // new column v.2.9.6

    public static final String COLUMN_STORE2_ENROLLMENT = "enrollment"; // new column v.2.9.12
    public static final String COLUMN_STORE2_DISTRIBUTORCODE = "distributor_code"; // new column v.2.9.12
    public static final String COLUMN_STORE2_DISTRIBUTOR = "distributor"; // new column v.2.9.12
    public static final String COLUMN_STORE2_STOREID = "storeid"; // new column v.2.9.12
    public static final String COLUMN_STORE2_STORECODEPSUP = "store_code_psup"; // new column v.2.9.12
    public static final String COLUMN_STORE2_CLIENTCODE = "client_code"; // new column v.2.9.12
    public static final String COLUMN_STORE2_CLIENTNAME = "client_name"; // new column v.2.9.12
    public static final String COLUMN_STORE2_CHANNELCODE = "channel_code"; // new column v.2.9.12
    public static final String COLUMN_STORE2_CUSTOMERCODE = "customer_code"; // new column v.2.9.12
    public static final String COLUMN_STORE2_CUSTOMERNAME = "customer_name"; // new column v.2.9.12
    public static final String COLUMN_STORE2_REGIONCODE = "region_code"; // new column v.2.9.12
    public static final String COLUMN_STORE2_REGIONSHORT = "region_short"; // new column v.2.9.12
    public static final String COLUMN_STORE2_REGION = "region"; // new column v.2.9.12
    public static final String COLUMN_STORE2_AGENCYCODE = "agency_code"; // new column v.2.9.12
    public static final String COLUMN_STORE2_AGENCYNAME = "agency_name"; // new column v.2.9.12

    public static final String DATABASE_CREATE_TABLE_STORE2 = "CREATE TABLE IF NOT EXISTS " + TABLE_STORE2 + "("
            + COLUMN_STORE2_ID + " integer PRIMARY KEY, "
            + COLUMN_STORE2_BID + " integer, "
            + COLUMN_STORE2_STORECODE + " text, "
            + COLUMN_STORE2_DESC + " text, "
            + COLUMN_STORE2_MULTIPLE + " integer, "
            + COLUMN_STORE2_CHANNELID + " integer, "
            + COLUMN_STORE2_CHANNELDESC + " text, "
            + COLUMN_STORE2_CHANNELAREA + " text, "
            + COLUMN_STORE2_ENROLLMENT + " text, "
            + COLUMN_STORE2_DISTRIBUTORCODE + " text, "
            + COLUMN_STORE2_DISTRIBUTOR + " text, "
            + COLUMN_STORE2_STOREID + " text, "
            + COLUMN_STORE2_STORECODEPSUP + " text, "
            + COLUMN_STORE2_CLIENTCODE + " text, "
            + COLUMN_STORE2_CLIENTNAME + " text, "
            + COLUMN_STORE2_CHANNELCODE + " text, "
            + COLUMN_STORE2_CUSTOMERCODE + " text, "
            + COLUMN_STORE2_CUSTOMERNAME + " text, "
            + COLUMN_STORE2_REGIONCODE + " text, "
            + COLUMN_STORE2_REGIONSHORT + " text, "
            + COLUMN_STORE2_REGION + " text, "
            + COLUMN_STORE2_AGENCYCODE + " text, "
            + COLUMN_STORE2_AGENCYNAME + " text)";

    //User Table
    public static final String TABLE_USER = "user";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USER_UID = "uid";
    public static final String COLUMN_USER_DESC = "udesc";
    public static final String COLUMN_USER_HASH = "hash";

    public static final String DATABASE_CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS " + TABLE_USER + "("
            + COLUMN_USER_ID + " integer PRIMARY KEY, "
            + COLUMN_USER_UID + " integer, "
            + COLUMN_USER_DESC + " text, "
            + COLUMN_USER_HASH + " text)";

    //PCOUNT Transaction
    public static final String TABLE_TRANSACTION = "TRANS";
    public static final String COLUMN_TRANSACTION_ID = "id";
    public static final String COLUMN_TRANSACTION_DATE = "date";
    public static final String COLUMN_TRANSACTION_STOREID = "storeid";
    public static final String COLUMN_TRANSACTION_BARCODE = "barcode";
    public static final String COLUMN_TRANSACTION_IG = "ig";
    public static final String COLUMN_TRANSACTION_SAPC = "sapc";
    public static final String COLUMN_TRANSACTION_WHPC = "whpc";
    public static final String COLUMN_TRANSACTION_WHCS = "whcs";
    public static final String COLUMN_TRANSACTION_CONVERSION = "conversion";
    public static final String COLUMN_TRANSACTION_SO = "so";
    public static final String COLUMN_TRANSACTION_FSO = "fso";
    public static final String COLUMN_TRANSACTION_FSOVALUE = "fsovalue";
    public static final String COLUMN_TRANSACTION_LPOSTED = "lposted";
    public static final String COLUMN_TRANSACTION_WEBID = "webid";
    public static final String COLUMN_TRANSACTION_USERID = "userid";
    public static final String COLUMN_TRANSACTION_MULTI = "multi";
    public static final String COLUMN_TRANSACTION_STORE2ID = "store2_id"; // new column v.2.9.12

    public static final String COLUMN_TRANSACTION_OTHERBARCODE = "otherbarcode"; // new column v.2.9.13
    public static final String COLUMN_TRANSACTION_MINSTOCK = "minstock"; // new column v.2.9.13
    public static final String COLUMN_TRANSACTION_CATEGORY = "category"; // new column v.2.9.13
    public static final String COLUMN_TRANSACTION_DESCLONG = "description_long"; // new column v.2.9.13
    public static final String COLUMN_TRANSACTION_DESC = "desc";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_CATEGORYID = "categoryid";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_BRANDID = "brandid";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_DIVISIONID = "divisionid";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_SUBCATEGORYID = "subcategoryid";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_UPDATED = "updated";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_OLDIG = "old_ig"; // new column db v.18
    public static final String COLUMN_TRANSACTION_OSATAG = "osa_tag"; // new column db v.18
    public static final String COLUMN_TRANSACTION_NPITAG = "npi_tag"; // new column db v.18

    public static final String DATABASE_CREATE_TABLE_TRANSACTION = "CREATE TABLE IF NOT EXISTS " + TABLE_TRANSACTION + "("
            + COLUMN_TRANSACTION_ID + " integer PRIMARY KEY autoincrement, "
            + COLUMN_TRANSACTION_DATE + " text, "
            + COLUMN_TRANSACTION_STOREID + " integer, "
            + COLUMN_TRANSACTION_BARCODE + " text, "
            + COLUMN_TRANSACTION_IG + " integer, "
            + COLUMN_TRANSACTION_SAPC + " integer, "
            + COLUMN_TRANSACTION_WHPC + " integer, "
            + COLUMN_TRANSACTION_WHCS + " integer, "
            + COLUMN_TRANSACTION_CONVERSION + " integer, "
            + COLUMN_TRANSACTION_SO + " integer, "
            + COLUMN_TRANSACTION_FSO + " integer, "
            + COLUMN_TRANSACTION_FSOVALUE + " real, "
            + COLUMN_TRANSACTION_LPOSTED + " integer, "
            + COLUMN_TRANSACTION_WEBID + " text, "
            + COLUMN_TRANSACTION_USERID + " integer, "
            + COLUMN_TRANSACTION_MULTI + " integer, "
            + COLUMN_TRANSACTION_STORE2ID + " numeric, "
            + COLUMN_TRANSACTION_OTHERBARCODE + " text, "
            + COLUMN_TRANSACTION_MINSTOCK + " numeric, "
            + COLUMN_TRANSACTION_CATEGORYID + " text, "
            + COLUMN_TRANSACTION_CATEGORY + " text, "
            + COLUMN_TRANSACTION_DESCLONG + " text, "
            + COLUMN_TRANSACTION_DESC + " text, "
            + COLUMN_TRANSACTION_BRANDID + " text, "
            + COLUMN_TRANSACTION_DIVISIONID + " text, "
            + COLUMN_TRANSACTION_SUBCATEGORYID + " text, "
            + COLUMN_TRANSACTION_UPDATED + " integer, "
            + COLUMN_TRANSACTION_OLDIG + " numeric, "
            + COLUMN_TRANSACTION_OSATAG + " integer, "
            + COLUMN_TRANSACTION_NPITAG + " integer)";

    //ASSORTMENT MASTERFILE - NEW TABLE VERSION 2.0
    public static final String TABLE_ASSORTMENT = "tblAssortment";
    public static final String COLUMN_ASSORTMENT_ID = "id";
    public static final String COLUMN_ASSORTMENT_BARCODE = "barcode";
    public static final String COLUMN_ASSORTMENT_DESC = "desc";
    public static final String COLUMN_ASSORTMENT_IG = "ig";
    public static final String COLUMN_ASSORTMENT_SAPC = "sapc";
    public static final String COLUMN_ASSORTMENT_WHPC = "whpc";
    public static final String COLUMN_ASSORTMENT_WHCS = "whcs";
    public static final String COLUMN_ASSORTMENT_CONVERSION = "conversion";
    public static final String COLUMN_ASSORTMENT_SO = "so";
    public static final String COLUMN_ASSORTMENT_FSO = "fso";
    public static final String COLUMN_ASSORTMENT_CATEGORYID = "categoryid";
    public static final String COLUMN_ASSORTMENT_BRANDID = "brandid";
    public static final String COLUMN_ASSORTMENT_DIVISIONID = "divisionid";
    public static final String COLUMN_ASSORTMENT_SUBCATEGORYID = "subcategoryid";
    public static final String COLUMN_ASSORTMENT_STOREID = "storeid";
    public static final String COLUMN_ASSORTMENT_FSOVALUE = "fsovalue";
    public static final String COLUMN_ASSORTMENT_WEBID = "webid";
    public static final String COLUMN_ASSORTMENT_MULTI = "multi";
    public static final String COLUMN_ASSORTMENT_OTHERBARCODE = "otherbarcode"; // new column v2.4
    public static final String COLUMN_ASSORTMENT_MINSTOCK = "minstock"; // new columnd v2.5
    public static final String COLUMN_ASSORTMENT_CATEGORY = "category"; // new column v.2.9.12
    public static final String COLUMN_ASSORTMENT_DESCLONG = "description_long"; // new column v.2.9.12

    public static final String DATABASE_CREATE_TABLE_ASSORTMENT = "CREATE TABLE IF NOT EXISTS " + TABLE_ASSORTMENT + "("
            + COLUMN_ASSORTMENT_ID + " integer PRIMARY KEY, "
            + COLUMN_ASSORTMENT_BARCODE + " text, "
            + COLUMN_ASSORTMENT_DESC + " text, "
            + COLUMN_ASSORTMENT_IG + " integer, "
            + COLUMN_ASSORTMENT_SAPC + " integer, "
            + COLUMN_ASSORTMENT_WHPC + " integer, "
            + COLUMN_ASSORTMENT_WHCS + " integer, "
            + COLUMN_ASSORTMENT_CONVERSION + " integer, "
            + COLUMN_ASSORTMENT_SO + " integer, "
            + COLUMN_ASSORTMENT_FSO + " integer,"
            + COLUMN_ASSORTMENT_CATEGORYID + " text,"
            + COLUMN_ASSORTMENT_BRANDID + " text,"
            + COLUMN_ASSORTMENT_DIVISIONID + " text,"
            + COLUMN_ASSORTMENT_SUBCATEGORYID + " text, "
            + COLUMN_ASSORTMENT_STOREID + " integer, "
            + COLUMN_ASSORTMENT_FSOVALUE + " real, "
            + COLUMN_ASSORTMENT_WEBID + " text, "
            + COLUMN_ASSORTMENT_MULTI + " integer, "
            + COLUMN_ASSORTMENT_OTHERBARCODE + " text, "
            + COLUMN_ASSORTMENT_MINSTOCK + " numeric, "
            + COLUMN_ASSORTMENT_CATEGORY + " text, "
            + COLUMN_ASSORTMENT_DESCLONG + " text)";

    //ASSORTMENT TRANSACTIONS - NEW TABLE VERSION 2.0
    public static final String TABLE_TRANSACTION_ASSORT = "tblAssortTransaction";
    public static final String COLUMN_TRANSACTION_ASSORT_ID = "id";
    public static final String COLUMN_TRANSACTION_ASSORT_DATE = "date";
    public static final String COLUMN_TRANSACTION_ASSORT_STOREID = "storeid";
    public static final String COLUMN_TRANSACTION_ASSORT_BARCODE = "barcode";
    public static final String COLUMN_TRANSACTION_ASSORT_IG = "ig";
    public static final String COLUMN_TRANSACTION_ASSORT_SAPC = "sapc";
    public static final String COLUMN_TRANSACTION_ASSORT_WHPC = "whpc";
    public static final String COLUMN_TRANSACTION_ASSORT_WHCS = "whcs";
    public static final String COLUMN_TRANSACTION_ASSORT_CONVERSION = "conversion";
    public static final String COLUMN_TRANSACTION_ASSORT_SO = "so";
    public static final String COLUMN_TRANSACTION_ASSORT_FSO = "fso";
    public static final String COLUMN_TRANSACTION_ASSORT_FSOVALUE = "fsovalue";
    public static final String COLUMN_TRANSACTION_ASSORT_LPOSTED = "lposted";
    public static final String COLUMN_TRANSACTION_ASSORT_WEBID = "webid";
    public static final String COLUMN_TRANSACTION_ASSORT_USERID = "userid";
    public static final String COLUMN_TRANSACTION_ASSORT_MULTI = "multi";
    public static final String COLUMN_TRANSACTION_ASSORT_MONTH = "month";
    public static final String COLUMN_TRANSACTION_ASSORT_STORE2ID = "store2_id"; // new column v.2.9.12

    public static final String COLUMN_TRANSACTION_ASSORT_OTHERBARCODE = "otherbarcode"; // new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_MINSTOCK = "minstock"; // new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_CATEGORY = "category"; // new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_DESCLONG = "description_long"; // new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_DESC = "desc";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_CATEGORYID = "categoryid";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_BRANDID = "brandid";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_DIVISIONID = "divisionid";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_SUBCATEGORYID = "subcategoryid";// new column v.2.9.13
    public static final String COLUMN_TRANSACTION_ASSORT_UPDATED = "updated";// new column v.2.9.13

    public static final String DATABASE_CREATE_TABLE_TRANSASSORT = "CREATE TABLE IF NOT EXISTS " + TABLE_TRANSACTION_ASSORT + "("
            + COLUMN_TRANSACTION_ASSORT_ID + " integer PRIMARY KEY autoincrement, "
            + COLUMN_TRANSACTION_ASSORT_DATE + " text, "
            + COLUMN_TRANSACTION_ASSORT_STOREID + " integer, "
            + COLUMN_TRANSACTION_ASSORT_BARCODE + " text, "
            + COLUMN_TRANSACTION_ASSORT_IG + " integer, "
            + COLUMN_TRANSACTION_ASSORT_SAPC + " integer, "
            + COLUMN_TRANSACTION_ASSORT_WHPC + " integer, "
            + COLUMN_TRANSACTION_ASSORT_WHCS + " integer, "
            + COLUMN_TRANSACTION_ASSORT_CONVERSION + " integer, "
            + COLUMN_TRANSACTION_ASSORT_SO + " integer, "
            + COLUMN_TRANSACTION_ASSORT_FSO + " integer, "
            + COLUMN_TRANSACTION_ASSORT_FSOVALUE + " real, "
            + COLUMN_TRANSACTION_ASSORT_LPOSTED + " integer, "
            + COLUMN_TRANSACTION_ASSORT_WEBID + " text, "
            + COLUMN_TRANSACTION_ASSORT_USERID + " integer, "
            + COLUMN_TRANSACTION_ASSORT_MULTI + " integer, "
            + COLUMN_TRANSACTION_ASSORT_MONTH + " text, "
            + COLUMN_TRANSACTION_ASSORT_STORE2ID + " numeric, "
            + COLUMN_TRANSACTION_ASSORT_OTHERBARCODE + " text, "
            + COLUMN_TRANSACTION_ASSORT_MINSTOCK + " numeric, "
            + COLUMN_TRANSACTION_ASSORT_CATEGORYID + " text, "
            + COLUMN_TRANSACTION_ASSORT_CATEGORY + " text, "
            + COLUMN_TRANSACTION_ASSORT_DESCLONG + " text, "
            + COLUMN_TRANSACTION_ASSORT_DESC + " text, "
            + COLUMN_TRANSACTION_ASSORT_BRANDID + " text, "
            + COLUMN_TRANSACTION_ASSORT_DIVISIONID + " text, "
            + COLUMN_TRANSACTION_ASSORT_SUBCATEGORYID + " text, "
            + COLUMN_TRANSACTION_ASSORT_UPDATED + " integer)";



    public static final String TABLE_PROMO = "tblPromo";
    public static final String COLUMN_PROMO_ID = "id";
    public static final String COLUMN_PROMO_BARCODE = "barcode";
    public static final String COLUMN_PROMO_DESC = "desc";
    public static final String COLUMN_PROMO_IG = "ig";
    public static final String COLUMN_PROMO_SAPC = "sapc";
    public static final String COLUMN_PROMO_WHPC = "whpc";
    public static final String COLUMN_PROMO_WHCS = "whcs";
    public static final String COLUMN_PROMO_CONVERSION = "conversion";
    public static final String COLUMN_PROMO_SO = "so";
    public static final String COLUMN_PROMO_FSO = "fso";
    public static final String COLUMN_PROMO_CATEGORYID = "categoryid";
    public static final String COLUMN_PROMO_BRANDID = "brandid";
    public static final String COLUMN_PROMO_DIVISIONID = "divisionid";
    public static final String COLUMN_PROMO_SUBCATEGORYID = "subcategoryid";
    public static final String COLUMN_PROMO_STOREID = "storeid";
    public static final String COLUMN_PROMO_FSOVALUE = "fsovalue";
    public static final String COLUMN_PROMO_WEBID = "webid";
    public static final String COLUMN_PROMO_MULTI = "multi";
    public static final String COLUMN_PROMO_OTHERBARCODE = "otherbarcode";
    public static final String COLUMN_PROMO_MINSTOCK = "minstock";
    public static final String COLUMN_PROMO_CATEGORY = "category";
    public static final String COLUMN_PROMO_DESCLONG = "description_long";

    public static final String DATABASE_CREATE_TABLE_PROMO = "CREATE TABLE IF NOT EXISTS " + TABLE_PROMO + "("
            + COLUMN_PROMO_ID + " integer PRIMARY KEY, "
            + COLUMN_PROMO_BARCODE + " text, "
            + COLUMN_PROMO_DESC + " text, "
            + COLUMN_PROMO_IG + " integer, "
            + COLUMN_PROMO_SAPC + " integer, "
            + COLUMN_PROMO_WHPC + " integer, "
            + COLUMN_PROMO_WHCS + " integer, "
            + COLUMN_PROMO_CONVERSION + " integer, "
            + COLUMN_PROMO_SO + " integer, "
            + COLUMN_PROMO_FSO + " integer,"
            + COLUMN_PROMO_CATEGORYID + " text,"
            + COLUMN_PROMO_BRANDID + " text,"
            + COLUMN_PROMO_DIVISIONID + " text,"
            + COLUMN_PROMO_SUBCATEGORYID + " text, "
            + COLUMN_PROMO_STOREID + " integer, "
            + COLUMN_PROMO_FSOVALUE + " real, "
            + COLUMN_PROMO_WEBID + " text, "
            + COLUMN_PROMO_MULTI + " integer, "
            + COLUMN_PROMO_OTHERBARCODE + " text, "
            + COLUMN_PROMO_MINSTOCK + " numeric, "
            + COLUMN_PROMO_CATEGORY + " text, "
            + COLUMN_PROMO_DESCLONG + " text)";

    //PROMO TRANSACTIONS - DB VERSION 20
    public static final String TABLE_TRANSACTION_PROMO = "tblPromoTransaction";
    public static final String COLUMN_TRANSACTION_PROMO_ID = "id";
    public static final String COLUMN_TRANSACTION_PROMO_DATE = "date";
    public static final String COLUMN_TRANSACTION_PROMO_STOREID = "storeid";
    public static final String COLUMN_TRANSACTION_PROMO_BARCODE = "barcode";
    public static final String COLUMN_TRANSACTION_PROMO_IG = "ig";
    public static final String COLUMN_TRANSACTION_PROMO_SAPC = "sapc";
    public static final String COLUMN_TRANSACTION_PROMO_WHPC = "whpc";
    public static final String COLUMN_TRANSACTION_PROMO_WHCS = "whcs";
    public static final String COLUMN_TRANSACTION_PROMO_CONVERSION = "conversion";
    public static final String COLUMN_TRANSACTION_PROMO_SO = "so";
    public static final String COLUMN_TRANSACTION_PROMO_FSO = "fso";
    public static final String COLUMN_TRANSACTION_PROMO_FSOVALUE = "fsovalue";
    public static final String COLUMN_TRANSACTION_PROMO_LPOSTED = "lposted";
    public static final String COLUMN_TRANSACTION_PROMO_WEBID = "webid";
    public static final String COLUMN_TRANSACTION_PROMO_USERID = "userid";
    public static final String COLUMN_TRANSACTION_PROMO_MULTI = "multi";
    public static final String COLUMN_TRANSACTION_PROMO_MONTH = "month";
    public static final String COLUMN_TRANSACTION_PROMO_STORE2ID = "store2_id";

    public static final String COLUMN_TRANSACTION_PROMO_OTHERBARCODE = "otherbarcode";
    public static final String COLUMN_TRANSACTION_PROMO_MINSTOCK = "minstock";
    public static final String COLUMN_TRANSACTION_PROMO_CATEGORY = "category";
    public static final String COLUMN_TRANSACTION_PROMO_DESCLONG = "description_long";
    public static final String COLUMN_TRANSACTION_PROMO_DESC = "desc";
    public static final String COLUMN_TRANSACTION_PROMO_CATEGORYID = "categoryid";
    public static final String COLUMN_TRANSACTION_PROMO_BRANDID = "brandid";
    public static final String COLUMN_TRANSACTION_PROMO_DIVISIONID = "divisionid";
    public static final String COLUMN_TRANSACTION_PROMO_SUBCATEGORYID = "subcategoryid";
    public static final String COLUMN_TRANSACTION_PROMO_UPDATED = "updated";

    public static final String DATABASE_CREATE_TABLE_TRANSPROMO = "CREATE TABLE IF NOT EXISTS " + TABLE_TRANSACTION_PROMO + "("
            + COLUMN_TRANSACTION_PROMO_ID + " integer PRIMARY KEY autoincrement, "
            + COLUMN_TRANSACTION_PROMO_DATE + " text, "
            + COLUMN_TRANSACTION_PROMO_STOREID + " integer, "
            + COLUMN_TRANSACTION_PROMO_BARCODE + " text, "
            + COLUMN_TRANSACTION_PROMO_IG + " integer, "
            + COLUMN_TRANSACTION_PROMO_SAPC + " integer, "
            + COLUMN_TRANSACTION_PROMO_WHPC + " integer, "
            + COLUMN_TRANSACTION_PROMO_WHCS + " integer, "
            + COLUMN_TRANSACTION_PROMO_CONVERSION + " integer, "
            + COLUMN_TRANSACTION_PROMO_SO + " integer, "
            + COLUMN_TRANSACTION_PROMO_FSO + " integer, "
            + COLUMN_TRANSACTION_PROMO_FSOVALUE + " real, "
            + COLUMN_TRANSACTION_PROMO_LPOSTED + " integer, "
            + COLUMN_TRANSACTION_PROMO_WEBID + " text, "
            + COLUMN_TRANSACTION_PROMO_USERID + " integer, "
            + COLUMN_TRANSACTION_PROMO_MULTI + " integer, "
            + COLUMN_TRANSACTION_PROMO_MONTH + " text, "
            + COLUMN_TRANSACTION_PROMO_STORE2ID + " numeric, "
            + COLUMN_TRANSACTION_PROMO_OTHERBARCODE + " text, "
            + COLUMN_TRANSACTION_PROMO_MINSTOCK + " numeric, "
            + COLUMN_TRANSACTION_PROMO_CATEGORYID + " text, "
            + COLUMN_TRANSACTION_PROMO_CATEGORY + " text, "
            + COLUMN_TRANSACTION_PROMO_DESCLONG + " text, "
            + COLUMN_TRANSACTION_PROMO_DESC + " text, "
            + COLUMN_TRANSACTION_PROMO_BRANDID + " text, "
            + COLUMN_TRANSACTION_PROMO_DIVISIONID + " text, "
            + COLUMN_TRANSACTION_PROMO_SUBCATEGORYID + " text, "
            + COLUMN_TRANSACTION_PROMO_UPDATED + " integer)";

    public SQLiteDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
        aTransOsaColumns = new ArrayList<>();

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.pcount_sharedprefKey), Context.MODE_PRIVATE);
        this.DATABASE_BACKUP_VERSION = sharedPreferences.getInt(context.getString(R.string.pref_db_backup_version), 0);
        if(DATABASE_BACKUP_VERSION > 0 && DATABASE_VERSION <= DATABASE_BACKUP_VERSION)
            DATABASE_VERSION = DATABASE_BACKUP_VERSION;

        LoadColumns();
    }

    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_TABLE_PCOUNT);
        database.execSQL(DATABASE_CREATE_TABLE_STORE);
        database.execSQL(DATABASE_CREATE_TABLE_STORE2);
        database.execSQL(DATABASE_CREATE_TABLE_USER);
        database.execSQL(DATABASE_CREATE_TABLE_TRANSACTION);
        database.execSQL(DATABASE_CREATE_TABLE_ASSORTMENT);
        database.execSQL(DATABASE_CREATE_TABLE_TRANSASSORT);
        database.execSQL(DATABASE_CREATE_TABLE_PROMO);
        database.execSQL(DATABASE_CREATE_TABLE_TRANSPROMO);

        database.execSQL("CREATE INDEX pcountDescIndex ON " + TABLE_PCOUNT + " (desc)");
        database.execSQL("CREATE INDEX pcountCategoryIndex ON " + TABLE_PCOUNT + " (categoryid)");
        database.execSQL("CREATE INDEX pcountBrandIndex ON " + TABLE_PCOUNT + " (brandid)");
        database.execSQL("CREATE INDEX pcountDivisionIndex ON " + TABLE_PCOUNT + " (divisionid)");
        database.execSQL("CREATE INDEX pcountSubCategoryIndex ON " + TABLE_PCOUNT + " (subcategoryid)");

        database.execSQL("CREATE INDEX assortDescIndex ON " + TABLE_ASSORTMENT + " (desc)");
        database.execSQL("CREATE INDEX assortCategoryIndex ON " + TABLE_ASSORTMENT + " (categoryid)");
        database.execSQL("CREATE INDEX assortBrandIndex ON " + TABLE_ASSORTMENT + " (brandid)");
        database.execSQL("CREATE INDEX assortDivisionIndex ON " + TABLE_ASSORTMENT + " (divisionid)");
        database.execSQL("CREATE INDEX assortSubCategoryIndex ON " + TABLE_ASSORTMENT + " (subcategoryid)");

        database.execSQL("CREATE INDEX transactionIndex ON " + TABLE_TRANSACTION + " (date,storeid)");
        database.execSQL("CREATE INDEX transactionBarcodeIndex ON " + TABLE_TRANSACTION + " (barcode)");

        database.execSQL("CREATE INDEX assorttransactionIndex ON " + TABLE_TRANSACTION_ASSORT + " (date,storeid)");
        database.execSQL("CREATE INDEX assorttransactionBarcodeIndex ON " + TABLE_TRANSACTION_ASSORT + " (barcode)");

        database.execSQL("CREATE INDEX promoDescIndex ON " + TABLE_PROMO + " (desc)");
        database.execSQL("CREATE INDEX promoCategoryIndex ON " + TABLE_PROMO + " (categoryid)");
        database.execSQL("CREATE INDEX promoBrandIndex ON " + TABLE_PROMO + " (brandid)");
        database.execSQL("CREATE INDEX promoDivisionIndex ON " + TABLE_PROMO + " (divisionid)");
        database.execSQL("CREATE INDEX promoSubCategoryIndex ON " + TABLE_PROMO + " (subcategoryid)");

        database.execSQL("CREATE INDEX promotransactionIndex ON " + TABLE_TRANSACTION_PROMO + " (date,storeid)");
        database.execSQL("CREATE INDEX promotransactionBarcodeIndex ON " + TABLE_TRANSACTION_PROMO + " (barcode)");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {

        ProgressDialog progressDialog = ProgressDialog.show(mContext, "", "Updating database from " + String.valueOf(oldVersion) + " to " + String.valueOf(currentVersion));
        progressDialog.setCancelable(false);

        if(currentVersion > oldVersion && oldVersion < 12) {
            CheckSchemaChanges(db);
        }
        else {

            if (currentVersion > oldVersion && oldVersion < 13) { // version 13
                db.execSQL("ALTER TABLE " + TABLE_STORE + " ADD COLUMN " + COLUMN_STORE_CHANNELDESC + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_STORE + " ADD COLUMN " + COLUMN_STORE_CHANNELAREA + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_STORE + " ADD COLUMN " + COLUMN_STORE_STORECODE + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_STORE + " ADD COLUMN " + COLUMN_STORE_CHANNELID + " INTEGER");
            }

            if (currentVersion > oldVersion && oldVersion < 14) { // version 14
                db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_PCOUNT_MINSTOCK + " NUMERIC DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_ASSORTMENT + " ADD COLUMN " + COLUMN_ASSORTMENT_MINSTOCK + " NUMERIC DEFAULT 0");
            }

            if(currentVersion > oldVersion && oldVersion < 15) { // version 15
                // no db updates, check and re-create not existing tables and columns
                CheckSchemaChanges(db);
            }

            if(currentVersion > oldVersion && oldVersion < 16) { // version 16

                db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_PCOUNT_CATEGORY + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_PCOUNT_DESCLONG + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_STORE2ID + " NUMERIC");

                db.execSQL("ALTER TABLE " + TABLE_ASSORTMENT + " ADD COLUMN " + COLUMN_ASSORTMENT_CATEGORY + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_ASSORTMENT + " ADD COLUMN " + COLUMN_ASSORTMENT_DESCLONG + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_STORE2ID + " NUMERIC");

                db.execSQL(DATABASE_CREATE_TABLE_STORE2);
            }

            if(currentVersion > oldVersion && oldVersion < 17) { // version 17
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_OTHERBARCODE + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_CATEGORY + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_MINSTOCK + " NUMERIC");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_DESCLONG + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_DESC + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_CATEGORYID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_BRANDID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_DIVISIONID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_SUBCATEGORYID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_UPDATED + " INTEGER");

                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_OTHERBARCODE + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_CATEGORY + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_MINSTOCK + " NUMERIC");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_DESCLONG + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_DESC + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_CATEGORYID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_BRANDID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_DIVISIONID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_SUBCATEGORYID + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION_ASSORT + " ADD COLUMN " + COLUMN_TRANSACTION_ASSORT_UPDATED + " INTEGER");

                db.delete(TABLE_TRANSACTION, null, null);
                db.delete(TABLE_TRANSACTION_ASSORT, null, null);
            }

            if(currentVersion > oldVersion && oldVersion < 18) { // version 18
                db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_PCOUNT_OLDIG + " NUMERIC");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_OLDIG + " NUMERIC");
                db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_PCOUNT_OSATAG + " INTEGER");
                db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_PCOUNT_NPITAG + " INTEGER");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_OSATAG + " INTEGER");
                db.execSQL("ALTER TABLE " + TABLE_TRANSACTION + " ADD COLUMN " + COLUMN_TRANSACTION_NPITAG + " INTEGER");
            }

            if(currentVersion > oldVersion && oldVersion < 19) { // version 20
                db.execSQL(DATABASE_CREATE_TABLE_PROMO);
                db.execSQL(DATABASE_CREATE_TABLE_TRANSPROMO);


                db.execSQL("CREATE INDEX promoDescIndex ON " + TABLE_PROMO + " (desc)");
                db.execSQL("CREATE INDEX promoCategoryIndex ON " + TABLE_PROMO + " (categoryid)");
                db.execSQL("CREATE INDEX promoBrandIndex ON " + TABLE_PROMO + " (brandid)");
                db.execSQL("CREATE INDEX promoDivisionIndex ON " + TABLE_PROMO + " (divisionid)");
                db.execSQL("CREATE INDEX promoSubCategoryIndex ON " + TABLE_PROMO + " (subcategoryid)");

                db.execSQL("CREATE INDEX promotransactionIndex ON " + TABLE_TRANSACTION_PROMO + " (date,storeid)");
                db.execSQL("CREATE INDEX promotransactionBarcodeIndex ON " + TABLE_TRANSACTION_PROMO + " (barcode)");
            }
        }

        Log.w(TAG, "Upgrading settings database from version " + oldVersion + " to " + currentVersion);

        progressDialog.dismiss();
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    private void CheckSchemaChanges(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_TABLE_ASSORTMENT);
        db.execSQL(DATABASE_CREATE_TABLE_TRANSASSORT);

        if(!CheckColumnIfExist(TABLE_PCOUNT, COLUMN_PCOUNT_OTHERBARCODE, db))
            db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_PCOUNT_OTHERBARCODE + " TEXT");

        if(!CheckColumnIfExist(TABLE_PCOUNT, COLUMN_PCOUNT_MINSTOCK, db))
            db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_PCOUNT_MINSTOCK + " NUMERIC");

        if(!CheckColumnIfExist(TABLE_ASSORTMENT, COLUMN_ASSORTMENT_OTHERBARCODE, db))
            db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_ASSORTMENT_OTHERBARCODE + " TEXT");

        if(!CheckColumnIfExist(TABLE_ASSORTMENT, COLUMN_ASSORTMENT_MINSTOCK, db))
            db.execSQL("ALTER TABLE " + TABLE_PCOUNT + " ADD COLUMN " + COLUMN_ASSORTMENT_MINSTOCK + " NUMERIC");

        if(!CheckColumnIfExist(TABLE_STORE, COLUMN_STORE_CHANNELDESC, db))
            db.execSQL("ALTER TABLE " + TABLE_STORE + " ADD COLUMN " + COLUMN_STORE_CHANNELDESC + " TEXT");

        if(!CheckColumnIfExist(TABLE_STORE, COLUMN_STORE_CHANNELAREA, db))
            db.execSQL("ALTER TABLE " + TABLE_STORE + " ADD COLUMN " + COLUMN_STORE_CHANNELAREA + " TEXT");

        if(!CheckColumnIfExist(TABLE_STORE, COLUMN_STORE_STORECODE, db))
            db.execSQL("ALTER TABLE " + TABLE_STORE + " ADD COLUMN " + COLUMN_STORE_STORECODE + " TEXT");

        if(!CheckColumnIfExist(TABLE_STORE, COLUMN_STORE_CHANNELID, db))
            db.execSQL("ALTER TABLE " + TABLE_STORE + " ADD COLUMN " + COLUMN_STORE_CHANNELID + " INTEGER");
    }

    private boolean CheckColumnIfExist(String tableName, String columnName, SQLiteDatabase db) {
        boolean result = false;

        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, null);
        int deleteStateColumnIndex = cursor.getColumnIndex(columnName); // -1 if not existing, greater than 0 if existing
        if (deleteStateColumnIndex > 0) {
            result = true;
        }
        cursor.close();

        return result;
    }

    private void LoadColumns() {

        aTransOsaColumns.clear();
        aTransOsaColumns.add(COLUMN_TRANSACTION_ID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_DATE);
        aTransOsaColumns.add(COLUMN_TRANSACTION_STOREID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_BARCODE);
        aTransOsaColumns.add(COLUMN_TRANSACTION_IG);
        aTransOsaColumns.add(COLUMN_TRANSACTION_SAPC);
        aTransOsaColumns.add(COLUMN_TRANSACTION_WHPC);
        aTransOsaColumns.add(COLUMN_TRANSACTION_WHCS);
        aTransOsaColumns.add(COLUMN_TRANSACTION_CONVERSION);
        aTransOsaColumns.add(COLUMN_TRANSACTION_SO);
        aTransOsaColumns.add(COLUMN_TRANSACTION_FSO);
        aTransOsaColumns.add(COLUMN_TRANSACTION_FSOVALUE);
        aTransOsaColumns.add(COLUMN_TRANSACTION_LPOSTED);
        aTransOsaColumns.add(COLUMN_TRANSACTION_WEBID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_USERID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_MULTI);
        aTransOsaColumns.add(COLUMN_TRANSACTION_STORE2ID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_OTHERBARCODE);
        aTransOsaColumns.add(COLUMN_TRANSACTION_MINSTOCK);
        aTransOsaColumns.add(COLUMN_TRANSACTION_CATEGORY);
        aTransOsaColumns.add(COLUMN_TRANSACTION_DESCLONG);
        aTransOsaColumns.add(COLUMN_TRANSACTION_DESC);
        aTransOsaColumns.add(COLUMN_TRANSACTION_CATEGORYID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_BRANDID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_DIVISIONID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_SUBCATEGORYID);
        aTransOsaColumns.add(COLUMN_TRANSACTION_UPDATED);
        aTransOsaColumns.add(COLUMN_TRANSACTION_OLDIG);
        aTransOsaColumns.add(COLUMN_TRANSACTION_OSATAG);
        aTransOsaColumns.add(COLUMN_TRANSACTION_NPITAG);
    }
}