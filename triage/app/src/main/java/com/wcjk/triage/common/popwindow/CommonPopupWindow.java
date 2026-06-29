package com.wcjk.triage.common.popwindow;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

public abstract class CommonPopupWindow {
    protected Context context;
    protected View contentView;
    protected PopupWindow mInstance;

    public CommonPopupWindow(Context c, int layoutRes, int w, int h) {
        context=c;
        contentView= LayoutInflater.from(c).inflate(layoutRes, null);
        initView();

        mInstance=new PopupWindow(contentView, w, h, true);
        initWindow();
        initEvent();
    }

    public CommonPopupWindow(Context c, int layoutRes, int w) {
        context=c;
        contentView= LayoutInflater.from(c).inflate(layoutRes, null);
        initView();
        initEvent();
        mInstance=new PopupWindow(contentView, w, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        initWindow();

    }

    public CommonPopupWindow(Context c, int layoutRes) {
        context=c;
        contentView= LayoutInflater.from(c).inflate(layoutRes, null);
        initView();
        initEvent();
        mInstance=new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,true);
        initWindow();
    }

    public View getContentView() { return contentView; }
    public PopupWindow getPopupWindow() { return mInstance; }

    protected abstract void initView();
    protected abstract void initEvent();


    protected void initWindow() {
        mInstance.setBackgroundDrawable(new BitmapDrawable());
        mInstance.setOutsideTouchable(true);
        mInstance.setTouchable(true);
        mInstance.setFocusable(true);
        //防止PopupWindow被软件盘挡住
//        mInstance.setSoftInputMode(PopupWindow.INPUT_METHOD_NEEDED);
//        mInstance.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
//        mInstance.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public void showBashOfAnchor(View anchor, LayoutGravity layoutGravity, int xmerge, int ymerge) {
        int[] offset=layoutGravity.getOffset(anchor, mInstance);
        mInstance.showAsDropDown(anchor, offset[0]+xmerge, offset[1]+ymerge);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff) {
       mInstance.showAsDropDown(anchor, xoff, yoff);
    }

     public void showAtLocation(View parent, int gravity, int x, int y) {
         mInstance.showAtLocation(parent, gravity, x, y);
     }

     public void dismiss(){
//        mInstance.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mInstance.dismiss();
     }
}