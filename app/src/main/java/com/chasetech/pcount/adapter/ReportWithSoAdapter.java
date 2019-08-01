package com.chasetech.pcount.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.chasetech.pcount.ErrorLog.AutoErrorLog;
import com.chasetech.pcount.R;
import com.chasetech.pcount.library.MainLibrary;
import com.chasetech.pcount.library.ReportClass;

import java.util.ArrayList;

/**
 * Created by ULTRABOOK on 4/21/2016.
 */
public class ReportWithSoAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<ReportClass> mArrayListReport;

    public ReportWithSoAdapter(Context context, ArrayList<ReportClass> arrayListReport )
    {
        this.context = context;
        this.mArrayListReport = arrayListReport;
        Thread.setDefaultUncaughtExceptionHandler(new AutoErrorLog(context, MainLibrary.errlogFile));
    }

    public class ViewHolder {
        TextView tvwSKU;
        TextView textViewName;
        TextView textViewIG;
        TextView textViewEndinv;
        TextView textViewFinalSO;
        TextView tvwUnit;
        TextView tvwOrderamt;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final ViewHolder holder;

        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.report_row_layout_withso, parent, false);
            holder.textViewName = (TextView) view.findViewById(R.id.se_row_desc);
            holder.tvwSKU = (TextView) view.findViewById(R.id.se_row_sku);
            holder.textViewIG = (TextView) view.findViewById(R.id.se_row_ig_goal);
            holder.textViewEndinv = (TextView) view.findViewById(R.id.se_row_endinv);
            holder.textViewFinalSO = (TextView) view.findViewById(R.id.se_row_finalso);
            holder.tvwUnit = (TextView) view.findViewById(R.id.se_row_unit);
            holder.tvwOrderamt = (TextView) view.findViewById(R.id.se_row_orderamt);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        ReportClass reportClass = mArrayListReport.get(position);

        holder.textViewName.setText(reportClass.name);
        holder.tvwSKU.setText(reportClass.otherbarc);
        holder.textViewIG.setText(String.valueOf(reportClass.ig));
        holder.textViewFinalSO.setText(String.valueOf(reportClass.finalso));
        holder.textViewEndinv.setText(String.valueOf(reportClass.endinv));
        holder.tvwUnit.setText(String.valueOf(reportClass.unit));
        holder.tvwOrderamt.setText(String.valueOf(reportClass.orderAmount));

        return view;
    }

    @Override
    public int getCount() {
        return mArrayListReport.size();
    }

    @Override
    public Object getItem(int position) {
        return mArrayListReport.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
