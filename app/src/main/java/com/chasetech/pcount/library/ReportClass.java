package com.chasetech.pcount.library;

/**
 * Created by Vinid on 10/25/2015.
 */
public class ReportClass {

    public final String name;

    public String otherbarc;

    public int ig;

    public int so;

    public int endinv;

    public int finalso;

    public int multi;

    public int unit;

    public String orderAmount;

    public ReportClass(String name, String barcode) {

        this.name = name;

        this.otherbarc = barcode;

        this.ig = 0;

        this.so = 0;

        this.endinv = 0;

        this.finalso = 0;

        this.multi = 0;

    }


    public ReportClass(String name, int ig, int so, int endinv, int finalso) {

        this.name = name;

        this.ig = ig;

        this.so = so;

        this.endinv = endinv;

        this.finalso = finalso;

        this.multi = multi;
    }

}
