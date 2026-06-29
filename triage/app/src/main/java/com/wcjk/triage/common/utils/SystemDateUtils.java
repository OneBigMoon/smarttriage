package com.wcjk.triage.common.utils;

import android.app.AlarmManager;
import android.content.Context;
import android.os.SystemClock;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by hyc on 2019/4/13
 */

public class SystemDateUtils {
    public static void setSysDateAndTime(Context context,Long time){
        if(time / 1000 < Integer.MAX_VALUE){
            DeviceControlResult zcResult = ZcDeviceControl.getInstance().setSystemTime(context, time);
            if (zcResult.isSuccess()) {
                return;
            }
            try {
//                ((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).setTime(time);
                SystemClock.setCurrentTimeMillis(time);
            }catch (Exception e){

            }
        }
    }

    //设置系统日期
    public static void setSysDate(Context context,int year,int month,int day){

        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();

        if(when / 1000 < Integer.MAX_VALUE){
            ((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    //设置系统时间

    public static void setSysTime(Context context,int hour,int minute){

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        long when = c.getTimeInMillis();
        if(when / 1000 < Integer.MAX_VALUE){
            ((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    //设置系统时区
    public static void setTimeZone(String timeZone){

        final Calendar now = Calendar.getInstance();
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        now.setTimeZone(tz);
    }

    //获取系统当前的时区
    public static String getDefaultTimeZone(){
        return TimeZone.getDefault().getDisplayName();
    }

}
