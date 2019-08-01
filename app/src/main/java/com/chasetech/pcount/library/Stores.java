package com.chasetech.pcount.library;

/**
 * Created by Vinid on 10/25/2015.
 */
public class Stores {

    public final int ID;
    public final int webStoreId;
    public final String storeCode;
    public final String storeName;
    public final int multiple;
    public final int channelID;
    public final String channelDesc;
    public final String channelArea;
    public final String enrollment;
    public final String distributorCode;
    public final String distributor;
    public final String storeId;
    public final String storeCodePsup;
    public final String clientCode;
    public final String clientName;
    public final String channelCode;
    public final String customerCode;
    public final String customerName;
    public final String regionCode;
    public final String regionShort;
    public final String region;
    public final String agencyCode;
    public final String agencyName;

    public int osaStatus;
    public int assortStatus;
    public int PromoStatus;

    public Stores(int ID, int webStoreId, String storeCode, String storeName, int multiple, int channelID, String channelDesc, String channelArea, String enrollment, String distributorCode, String distributor, String storeId, String storeCodePsup, String clientCode, String clientName, String channelCode, String customerCode, String customerName, String regionCode, String regionShort, String region, String agencyCode, String agencyName, int osaStatus, int assortStatus,int promoStatus) {
        this.ID = ID;
        this.webStoreId = webStoreId;
        this.storeCode = storeCode;
        this.storeName = storeName;
        this.multiple = multiple;
        this.channelID = channelID;
        this.channelDesc = channelDesc;
        this.channelArea = channelArea;
        this.enrollment = enrollment;
        this.distributorCode = distributorCode;
        this.distributor = distributor;
        this.storeId = storeId;
        this.storeCodePsup = storeCodePsup;
        this.clientCode = clientCode;
        this.clientName = clientName;
        this.channelCode = channelCode;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.regionCode = regionCode;
        this.regionShort = regionShort;
        this.region = region;
        this.agencyCode = agencyCode;
        this.agencyName = agencyName;
        this.osaStatus = osaStatus;
        this.assortStatus = assortStatus;
        this.PromoStatus = promoStatus;
    }

}
