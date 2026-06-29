package com.wcjk.triage.common;

import android.support.annotation.LayoutRes;

/**
 * Created by hyc on 2018/7/23
 */
public class CommonDialog extends BaseDialog {
    private ViewConvertListener convertListener;

    public static CommonDialog newInstance() {
        CommonDialog dialog = new CommonDialog();
        return dialog;
    }

    /**
     * 设置Dialog布局
     *
     * @param layoutId
     * @return
     */
    public CommonDialog setLayoutId(@LayoutRes int layoutId) {
        this.mLayoutResId = layoutId;
        return this;
    }

    @Override
    public int setUpLayoutId() {
        return mLayoutResId;
    }

    @Override
    public void convertView(ViewHolder holder, BaseDialog dialog) {
        if (convertListener != null) {
            convertListener.convertView(holder, dialog);
        }
    }

    public CommonDialog setConvertListener(ViewConvertListener convertListener) {
        this.convertListener = convertListener;
        return this;
    }

    public abstract static class ViewConvertListener {
        public abstract void convertView(ViewHolder holder, final BaseDialog dialog);
    }
}
