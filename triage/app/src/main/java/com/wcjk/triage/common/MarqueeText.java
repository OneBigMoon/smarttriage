package com.wcjk.triage.common;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by hyc on 2019/1/13
 */

public class MarqueeText extends android.support.v7.widget.AppCompatTextView {

    public MarqueeText(Context context) {
        super(context);
        init();
    }

    public MarqueeText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarqueeText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        setMarqueeRepeatLimit(-1);
    }
    //TextView默认设置是第一个获取到的光标，
    //如果想让所有的TextView都有跑马灯效果,则让所有的TextView都获取到光标就行了
    //这里return true 就是让所有的TextView都获取到光标
    @Override
    public boolean isFocused() {
        return true;
    }
}