package com.wcjk.triage.common.recyclerview.fjykfsdeyy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wcjk.triage.R;
import com.wcjk.triage.bean.fjykfsdeyy.KeShiInfo;
import com.wcjk.triage.bean.fjykfsdeyy.WaitPatientInfo;
import com.wcjk.triage.common.recyclerview.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class KeShiAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<KeShiInfo> list;

    private Context mContext;

    public KeShiAdapter(List<KeShiInfo> list, Context mContext) {
        this.list = list;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.item_keshi_first, parent, false);
        return new mainViewHolder(mContext, v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        mainViewHolder viewHolder = (mainViewHolder) holder;
        KeShiInfo item = list.get(position);
        viewHolder.ShowItem(item.getWaitPatientInfoList());
        viewHolder.tvKeshi.setText(item.getOfficename());
        viewHolder.tvDoctor.setText("医生:" + item.getDoctorname());
//        viewHolder.tvCallNo.setText(item.getCallno());
//        viewHolder.tvCallName.setText(item.getPatientname());
    }

    /**
     * 提供给Activity刷新数据
     *
     * @param list
     */
    public void updateList(List<KeShiInfo> list) {
        if (list != null) {
            this.list = list;
            notifyDataSetChanged();
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class mainViewHolder extends RecyclerView.ViewHolder {
        public TextView tvKeshi;
        public TextView tvDoctor;
        public TextView tvCallNo;
        public TextView tvCallName;
        public RecyclerView rcl_item;
        private WaitPatientAdapter itemAdapter;
        private GridLayoutManager layoutManager;

        public void ShowItem(List<WaitPatientInfo> items) {
            if (items == null) {
                items = new ArrayList<>();
            }
            itemAdapter.SetData(items);

            //填充数据
            rcl_item.setAdapter(itemAdapter);
        }

        public mainViewHolder(Context context, View itemView) {
            super(itemView);

            tvKeshi = itemView.findViewById(R.id.tv_keshi);
            tvDoctor = itemView.findViewById(R.id.tv_ys);
//            tvCallNo = itemView.findViewById(R.id.tv_call_no);
//            tvCallName = itemView.findViewById(R.id.tv_call_name);
            rcl_item = itemView.findViewById(R.id.rv_wait_list);

            itemAdapter = new WaitPatientAdapter(context);
            layoutManager = new GridLayoutManager(context, 1);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            rcl_item.setLayoutManager(layoutManager);

        }
    }
}
