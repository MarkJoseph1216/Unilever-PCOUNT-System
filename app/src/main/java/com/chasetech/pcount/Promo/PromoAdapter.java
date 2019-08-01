package com.chasetech.pcount.Promo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chasetech.pcount.Assortment.Assortment;
import com.chasetech.pcount.Assortment.AssortmentViewHolder;
import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.R;
import com.chasetech.pcount.library.MainLibrary;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Chase on 05/02/2018.
 * Chase Technologies Corp.
 */

public class PromoAdapter extends BaseAdapter {

    private Context mContext;

    private ArrayList<Promo> arrPromoList;

    public ArrayList <String> itemsOrdered;
    public ArrayList<Promo> arrPromoResultList;

    public PromoAdapter(Context context, ArrayList<Promo> arrayListPromo)
    {
        this.mContext = context;
        this.arrPromoResultList = arrayListPromo;
        this.arrPromoList = new ArrayList<Promo>();
        this.arrPromoList.addAll(arrayListPromo);
        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(context, MainLibrary.errlogFile));
    }

    @Override
    public int getCount() {
        return arrPromoResultList.size();
    }

    @Override
    public Object getItem(int position) {
        return arrPromoResultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final PromoViewHolder holder;

        if (view == null) {
            holder = new PromoViewHolder();
            view = inflater.inflate(R.layout.se_style_grid_row, parent, false);
            holder.linearLayoutPCount = (LinearLayout) view.findViewById(R.id.linearLayoutPCount);
            holder.tvSku = (TextView) view.findViewById(R.id.se_row_sku);
            holder.tvDesc = (TextView) view.findViewById(R.id.se_row_desc);
            holder.tvIgGoal = (TextView) view.findViewById(R.id.se_row_ig_goal);
            holder.tvSapc = (TextView) view.findViewById(R.id.se_row_sa_qty);
            holder.tvWhpc = (TextView) view.findViewById(R.id.se_row_whpc_qty);
            holder.tvWhcs = (TextView) view.findViewById(R.id.se_row_whcs_qty);
            holder.tvSo = (TextView) view.findViewById(R.id.se_row_so_qty);
            holder.tvFso = (TextView) view.findViewById(R.id.se_row_finso_qty);
            view.setTag(holder);
        } else {
            holder = (PromoViewHolder) view.getTag();
        }

        Promo promo =  arrPromoResultList.get(position);

        holder.Promo = promo ;
        holder.tvSku.setText(promo.barcode);
        holder.tvDesc.setText(promo.desc);
        holder.tvIgGoal.setText(String.valueOf(promo.ig));
        holder.tvSapc.setText(String.valueOf(promo.sapc));
        holder.tvWhpc.setText(String.valueOf(promo.whpc));
        holder.tvWhcs.setText(String.valueOf(promo.whcs));
        holder.tvSo.setText(String.valueOf(promo.so));
        holder.tvFso.setText(String.valueOf(promo.fso));

        holder.linearLayoutPCount.setBackground(mContext.getResources().getDrawable(R.drawable.list_selector));
        if (promo.sapc != 0 || promo.whpc != 0 || promo.whcs != 0 || promo.fso != 0 || promo.updated) {
            holder.linearLayoutPCount.setBackgroundColor(mContext.getResources().getColor(R.color.blue_gray));
        }

        return view;
    }

    public void filter(final int filterCode, String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        arrPromoResultList.clear();

        if (charText.length() == 0) {
            arrPromoResultList.addAll(arrPromoList);
        }
        else
        {
            for (Promo promo : arrPromoList)
            {
                boolean lvalid = false;

                switch (filterCode) {
                    case 0:
                        lvalid = promo.category.toLowerCase(Locale.getDefault()).contains(charText);
                        break;
                    case 1:
                        lvalid = promo.subcate.toLowerCase(Locale.getDefault()).contains(charText);
                        break;
                    case 2:
                        lvalid = promo.brand.toLowerCase(Locale.getDefault()).contains(charText);
                        break;
                    case 3:
                        lvalid = promo.division.toLowerCase(Locale.getDefault()).contains(charText);
                        break;
                    case 4:
//                        lvalid = promo.sapc != 0 || promo.whpc != 0 || promo.whcs != 0;
                        lvalid = promo.so != 0;
                        break;
                    case 5:
//                        lvalid = promo.sapc == 0 && promo.whpc == 0 && promo.whcs == 0;
                        lvalid = promo.fso < 1;
                        break;
                    default:
                        lvalid = promo.category.toLowerCase(Locale.getDefault()).contains(charText) ||
                                promo.brand.toLowerCase(Locale.getDefault()).contains(charText) ||
                                promo.subcate.toLowerCase(Locale.getDefault()).contains(charText) ||
                                promo.division.toLowerCase(Locale.getDefault()).contains(charText) ||
                                promo.desc.toLowerCase(Locale.getDefault()).contains(charText) ||
                                promo.barcode.toLowerCase(Locale.getDefault()).contains(charText);
                }

                if (lvalid) {
                    arrPromoResultList.add(promo);
                }

            }
        }
        notifyDataSetChanged();
    }
}
