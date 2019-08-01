package com.chasetech.pcount.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.R;
import com.chasetech.pcount.library.Stores;
import com.chasetech.pcount.library.MainLibrary;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Vinid on 10/25/2015.
 */

public class BranchListViewAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Stores> arrayListBranch;
    private ArrayList<Stores> arrStoreListForSearch;
    private Typeface menuFontIcon;

    public BranchListViewAdapter(Context context, ArrayList<Stores> arrayListBranch )
    {
        this.mContext = context;
        this.arrayListBranch = arrayListBranch;
        this.arrStoreListForSearch = new ArrayList<>();
        this.arrStoreListForSearch.addAll(arrayListBranch);
        this.menuFontIcon = Typeface.createFromAsset(mContext.getAssets(), MainLibrary.typefacename);
        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(context, MainLibrary.errlogFile));
    }

    private class ViewHolder {
        TextView textViewBranchName;
        TextView tvwIconAssort;
        TextView tvwIconPromo;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final ViewHolder holder;

        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.activity_store_layout_row, parent, false);
            holder.textViewBranchName = (TextView) view.findViewById(R.id.textViewBranchName);
            holder.tvwIconAssort = (TextView) view.findViewById(R.id.tvwIconAssort);
            holder.tvwIconPromo = (TextView) view.findViewById(R.id.tvwIconPromo);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Stores currentStores = arrayListBranch.get(position);

        holder.textViewBranchName.setText(currentStores.storeName);
        holder.textViewBranchName.setTag(currentStores.webStoreId);
        holder.tvwIconAssort.setTypeface(menuFontIcon);
        holder.tvwIconPromo.setTypeface(menuFontIcon);

        holder.tvwIconAssort.setText(MainLibrary.ICON_STAR_PENDING);

        holder.tvwIconPromo.setText(MainLibrary.ICON_STAR_PENDING);
        holder.tvwIconAssort.setTextColor(view.getResources().getColor(R.color.black_overlay));
        holder.tvwIconPromo.setTextColor(view.getResources().getColor(R.color.black_overlay));

        view.setBackground(mContext.getResources().getDrawable(R.drawable.list_selector));
        if (currentStores.osaStatus == 1) {
            holder.textViewBranchName.setTextColor(mContext.getResources().getColor(R.color.white));
            view.setBackgroundColor(mContext.getResources().getColor(R.color.light_red));
        } else if (currentStores.osaStatus == 2) {
            holder.textViewBranchName.setTextColor(mContext.getResources().getColor(R.color.white));
            view.setBackgroundColor(mContext.getResources().getColor(R.color.light_green));
        }
        else holder.textViewBranchName.setTextColor(mContext.getResources().getColor(R.color.textPrimary));


        if (currentStores.assortStatus == 1) {
            holder.tvwIconAssort.setText(MainLibrary.ICON_STAR_PARTIAL);
            holder.tvwIconAssort.setTextColor(view.getResources().getColor(R.color.yellow_orange));
        } else if (currentStores.assortStatus == 2) {
            holder.tvwIconAssort.setText(MainLibrary.ICON_STAR_COMPLETE);
            holder.tvwIconAssort.setTextColor(view.getResources().getColor(R.color.colorAccent_pressed));
        }
        else holder.tvwIconAssort.setText(MainLibrary.ICON_STAR_PENDING);


        //PROMO SAMPLE ADD ANOTHER STAR
        if (currentStores.PromoStatus == 1) {
            holder.tvwIconPromo.setText(MainLibrary.ICON_STAR_PARTIAL);
            holder.tvwIconPromo.setTextColor(view.getResources().getColor(R.color.yellow_orange));
        } else if (currentStores.PromoStatus == 2) {
            holder.tvwIconPromo.setText(MainLibrary.ICON_STAR_COMPLETE);
            holder.tvwIconPromo.setTextColor(view.getResources().getColor(R.color.colorAccent_pressed));
        }
        else holder.tvwIconPromo.setText(MainLibrary.ICON_STAR_PENDING);

        return view;
    }

    public void filter(String charText) {

        charText = charText.toLowerCase(Locale.getDefault());
        arrayListBranch.clear();

        if(charText.length() == 0) {
            arrayListBranch.addAll(arrStoreListForSearch);
        }
        else {
            for (Stores store : arrStoreListForSearch)
            {
                if (store.storeName.toLowerCase(Locale.getDefault()).contains(charText))
                {
                    arrayListBranch.add(store);
                }
            }
        }

        notifyDataSetChanged();
    }



    @Override
    public int getCount() {
        return arrayListBranch.size();
    }

    @Override
    public Object getItem(int position) {
        return arrayListBranch.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
