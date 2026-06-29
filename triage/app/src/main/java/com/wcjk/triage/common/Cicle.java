package com.wcjk.triage.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.wcjk.triage.R;

/**
 * Created by hyc on 2019/2/19
 */

public class Cicle extends View {

    //    定义画笔
    Paint paint;

    public Cicle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    //    重写draw方法
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

//        实例化画笔对象
        paint = new Paint();
//        给画笔设置颜色
        paint.setColor(getResources().getColor(R.color.text4));
//        设置画笔属性
//        paint.setStyle(Paint.Style.FILL);//画笔属性是实心圆
        paint.setStyle(Paint.Style.STROKE);//画笔属性是空心圆

        /*四个参数：
                参数一：圆心的x坐标
                参数二：圆心的y坐标
                参数三：圆的半径
                参数四：定义好的画笔
                */

        float w = (float) (0.8 * getWidth()/2);
        float h = (float) (0.8 * getHeight()/2);
        float r = w > h ? h : w;
        paint.setStrokeWidth((float) (r * 0.1));//设置画笔粗细
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, r, paint);

    }


}