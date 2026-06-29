package com.wcjk.triage.common.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public abstract class QuickAdapter<T> extends RecyclerView.Adapter<QuickAdapter.VH>  {

    private List<T> mList;
    private OnItemClickListener onItemClickListener;
    public QuickAdapter(List<T> datas){
        this.mList = datas;
    }

    public abstract int getLayoutId(int viewType);

    public interface OnItemClickListener{
        void onClick(int position);
        void onLongClick(int position);
    }
    public void setOnItemClickListener(OnItemClickListener onItemClickListener ){
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return VH.get(parent,getLayoutId(viewType));
    }

    @Override
    public void onBindViewHolder(VH holder, final int position) {
        convert(holder, mList.get(position), position);
        if (onItemClickListener != null){
            holder.itemView.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onItemClickListener.onClick(position);
                }
            });

            holder.itemView.setOnLongClickListener( new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemClickListener.onLongClick(position);
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    /**
     * 提供给Activity刷新数据
     * @param list
     */
    public void updateList(List<T> list){
        if (list != null) {
            this.mList = list;
            notifyDataSetChanged();
        }
    }

    public void updateList(){
        notifyDataSetChanged();
    }

    public abstract void convert(VH holder, T data, int position);

    protected static class VH extends RecyclerView.ViewHolder{
        private SparseArray<View> mViews;
        private View mConvertView;

        private VH(View v){
            super(v);
            mConvertView = v;
            mViews = new SparseArray<>();
        }

        public static VH get(ViewGroup parent, int layoutId){
            View convertView = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            return new VH(convertView);
        }

        public <T extends View> T getView(int id){
            View v = mViews.get(id);
            if(v == null){
                v = mConvertView.findViewById(id);
                mViews.put(id, v);
            }
            return (T)v;
        }

        public void setText(int id, String value){
            TextView view = getView(id);
            view.setText(value);
        }

        public <T extends View> T getView(String viewTag) {
            View view = mConvertView.findViewWithTag(viewTag);
            return (T) view;
        }

        public void setText(String viewTag, String text) {
            if (viewTag == null || text == null) return;
            TextView textView = getView(viewTag);
            if (textView != null) {
                textView.setText(text);
            }
        }

        public void setImageResource(int viewId, int resId)
        {
            ImageView view = getView(viewId);
            view.setImageResource(resId);
        }

        public void setBackGround(int viewId, int resId)
        {
            View view = getView(viewId);
            view.setBackgroundResource(resId);
        }

        public void setOnClickListener(int viewId, View.OnClickListener listener)
        {
            View view = getView(viewId);
            view.setOnClickListener(listener);
        }

    }
}
