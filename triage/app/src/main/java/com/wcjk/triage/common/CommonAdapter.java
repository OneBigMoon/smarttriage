package com.wcjk.triage.common;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by hyc on 2018/7/27
 */
public abstract class CommonAdapter<T> extends BaseAdapter {
    private List<T> mDatas;//数据集合，一般T为javaBean对象
    private LayoutInflater inflater;//布局填充器
    private Context context;
    private int layoutID;//item布局资源id

    public CommonAdapter(Context context, List<T> mDatas, int layoutID) {
        this.mDatas = mDatas;
        this.context = context;
        this.layoutID = layoutID;
        inflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return mDatas.size();
    }
    @Override
    public T getItem(int position) {
        return mDatas.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = ViewHolder.get(context, position, convertView, parent, layoutID);
        convert(holder, getItem(position));
        return holder.getConverView();
    }
    public abstract void convert(ViewHolder holder, T t);

    /**
     * 自定义ViewHolder进行数据绑定
     */
    public static class ViewHolder {
        private SparseArray<View> mViews;//用于存储item中的view对象
        private View mConvertView;//设计用于重用item
        private int position;//对外提供

        public ViewHolder(Context context, int position, ViewGroup parent, int layoutID) {
            mViews = new SparseArray<>();
            mConvertView = LayoutInflater.from(context).inflate(layoutID, parent, false);
            mConvertView.setTag(this);
            this.position = position;
        }
        public int getPosition() {
            return position;
        }
        public View getConverView() {
            return mConvertView;
        }
        public static ViewHolder get(Context context, int position, View convertView, ViewGroup parent, int layoutID) {
            ViewHolder holder;
            if (null == convertView) {
                holder = new ViewHolder(context, position, parent, layoutID);
            } else {
                holder = (ViewHolder) convertView.getTag();
                holder.position = position;
            }
            return holder;
        }

        public <T extends View> T getView(int viewID) {
            View view = mViews.get(viewID);
            if (null == view) {
                view = mConvertView.findViewById(viewID);
                mViews.put(viewID, view);
            }
            return (T) view;
        }

        public <T extends View> T getView(String viewTag) {
            View view = mConvertView.findViewWithTag(viewTag);
            return (T) view;
        }

        public ViewHolder setTextView(int viewID, String text) {
            TextView textView = getView(viewID);
            textView.setText(text);
            return this;
        }

        public ViewHolder setTextView(String viewTag, String text) {
            TextView textView = getView(viewTag);
            if (textView != null) {
                textView.setText(text);
            }
            return this;
        }

        public ViewHolder setImageResource(int viewID, int resourceID) {
            ImageView imageView = getView(viewID);
            imageView.setImageResource(resourceID);
            return this;
        }
    }
}
