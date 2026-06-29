package com.wcjk.triage.common.recyclerview.fjykfsdeyy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wcjk.triage.R;
import com.wcjk.triage.bean.fjykfsdeyy.WaitPatientInfo;

import java.util.List;

public class WaitPatientAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<WaitPatientInfo> list;

    private Context mContext;

    public WaitPatientAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.item_wait_list, parent, false);

        return new mainViewHolder(mContext, v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        mainViewHolder viewHolder = (mainViewHolder) holder;
        WaitPatientInfo item = list.get(position);
        viewHolder.tvNo.setText(item.getCallno());
        viewHolder.tvName.setText(item.getPatientname());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void SetData(List<WaitPatientInfo> items) {
        list = items;
    }

    public static class mainViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNo;
        public TextView tvName;


        public mainViewHolder(Context context, View itemView) {
            super(itemView);
            tvNo = itemView.findViewById(R.id.tv_no);
            tvName = itemView.findViewById(R.id.tv_name);

        }
    }
}
