package com.wcjk.triage.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

/**
 * Created by hyc on 2018/9/24
 */

public class AutoScrollTextView extends android.support.v7.widget.AppCompatTextView {
    /** 是否停止滚动 */
    private boolean mStopMarquee;
    private String mText;
    private float mCoordinateX;
    private float mTextWidth;
    private float windowWith;
    private float y;

    public AutoScrollTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setText(String text) {
        this.mText = text;
        mTextWidth = getPaint().measureText(mText);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        windowWith = displayMetrics.widthPixels;
        if (mHandler.hasMessages(0))
            mHandler.removeMessages(0);
        mHandler.sendEmptyMessageDelayed(0, 2000);
    }


    @SuppressLint("NewApi")
    @Override
    protected void onAttachedToWindow() {
        mStopMarquee = false;
        if (!(mText == null || mText.isEmpty()))
            mHandler.sendEmptyMessageDelayed(0, 2000);
        super.onAttachedToWindow();
    }


    @Override
    protected void onDetachedFromWindow() {
        mStopMarquee = true;
        if (mHandler.hasMessages(0))
            mHandler.removeMessages(0);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        y = getTextSize() + getPaddingTop();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!(mText == null || mText.isEmpty()))
            canvas.drawText(mText, mCoordinateX, y, getPaint());
    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if(mCoordinateX < 0 && Math.abs(mCoordinateX) > mTextWidth){
                        mCoordinateX = windowWith;
                    }else{
                        mCoordinateX -= 1;
                    }
                    invalidate();
                    sendEmptyMessageDelayed(0,30);
                    break;
            }
        }
    };

}

