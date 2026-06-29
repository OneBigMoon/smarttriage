package com.wcjk.triage.common.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateUtils {

    private static final long INTERVAL_IN_MILLISECONDS = 30 * 1000;

    public static boolean compareBefore(String source, String target, String format){
        boolean isBefore = false;
        if (source != null && target != null){
            Date d_source = StringToDate(source,format);
            Date d_target = StringToDate(target,format);
            if (d_target.before(d_source)) {
                isBefore = true;
            }
        }
        return isBefore;
    }

    public static boolean compareAfter(String source, String target, String format){
        boolean isAfter = false;
        if (source != null && target != null){
            Date d_source = StringToDate(source,format);
            Date d_target = StringToDate(target,format);
            if (d_target.after(d_source)) {
                isAfter = true;
            }
        }
        return isAfter;
    }

    public static String getTimestampString(Date messageDate) {

        boolean isChinese = true;

        String format = null;

        long messageTime = messageDate.getTime();
        if (isSameDay(messageTime)) {
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(messageDate);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            format = "HH:mm";

            if (hour > 17) {
                if(isChinese){
                    format = "晚上 hh:mm";
                }

            }else if(hour >= 0 && hour <= 6){
                if(isChinese){
                    format = "凌晨 hh:mm";
                }
            } else if (hour > 11 && hour <= 17) {
                if(isChinese){
                    format = "下午 hh:mm";
                }

            } else {
                if(isChinese){
                    format = "上午 hh:mm";
                }
            }
        } else if (isYesterday(messageTime)) {
            if(isChinese){
                format = "昨天 HH:mm";
            }else{
                format = "MM-dd HH:mm";
            }

        } else {
            if(isChinese){
                format = "M月d日 HH:mm";
            }else{
                format = "MM-dd HH:mm";
            }
        }

        if(isChinese){
            return new SimpleDateFormat(format, Locale.CHINA).format(messageDate);
        }else{
            return new SimpleDateFormat(format, Locale.US).format(messageDate);
        }
    }

    public static boolean isCloseEnough(long time1, long time2) {
        // long time1 = date1.getTime();
        // long time2 = date2.getTime();
        long delta = time1 - time2;
        if (delta < 0) {
            delta = -delta;
        }
        return delta < INTERVAL_IN_MILLISECONDS;
    }

    public static boolean isSameDay(long inputTime) {

        TimeInfo tStartAndEndTime = getTodayStartAndEndTime();
        if(inputTime>tStartAndEndTime.getStartTime()&&inputTime<tStartAndEndTime.getEndTime())
            return true;
        return false;
    }

    public static boolean isYesterday(long inputTime) {
        TimeInfo yStartAndEndTime = getYesterdayStartAndEndTime();
        if(inputTime>yStartAndEndTime.getStartTime()&&inputTime<yStartAndEndTime.getEndTime())
            return true;
        return false;
    }

    public static Date StringToDate(String dateStr, String formatStr) {
        DateFormat format = new SimpleDateFormat(formatStr);
        Date date = null;
        try {
            date = format.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
    /**
     *
     * @param timeLength Millisecond
     * @return
     */
    public static String toTime(int timeLength) {
        timeLength /= 1000;
        int minute = timeLength / 60;
        int hour = 0;
        if (minute >= 60) {
            hour = minute / 60;
            minute = minute % 60;
        }
        int second = timeLength % 60;
        // return String.format("%02d:%02d:%02d", hour, minute, second);
        return String.format("%02d:%02d", minute, second);
    }
    /**
     *
     * @param timeLength second
     * @return
     */
    public static String toTimeBySecond(int timeLength) {
//      timeLength /= 1000;
        int minute = timeLength / 60;
        int hour = 0;
        if (minute >= 60) {
            hour = minute / 60;
            minute = minute % 60;
        }
        int second = timeLength % 60;
        // return String.format("%02d:%02d:%02d", hour, minute, second);
        return String.format("%02d:%02d", minute, second);
    }



    public static TimeInfo getYesterdayStartAndEndTime() {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -1);
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);

        Date startDate = calendar1.getTime();
        long startTime = startDate.getTime();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.DATE, -1);
        calendar2.set(Calendar.HOUR_OF_DAY, 23);
        calendar2.set(Calendar.MINUTE, 59);
        calendar2.set(Calendar.SECOND, 59);
        calendar2.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar2.getTime();
        long endTime = endDate.getTime();
        TimeInfo info = new TimeInfo();
        info.setStartTime(startTime);
        info.setEndTime(endTime);
        return info;
    }

    public static TimeInfo getTodayStartAndEndTime() {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar1.getTime();
        long startTime = startDate.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S");

        Calendar calendar2 = Calendar.getInstance();
        calendar2.set(Calendar.HOUR_OF_DAY, 23);
        calendar2.set(Calendar.MINUTE, 59);
        calendar2.set(Calendar.SECOND, 59);
        calendar2.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar2.getTime();
        long endTime = endDate.getTime();
        TimeInfo info = new TimeInfo();
        info.setStartTime(startTime);
        info.setEndTime(endTime);
        return info;
    }

    public static TimeInfo getBeforeYesterdayStartAndEndTime() {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -2);
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar1.getTime();
        long startTime = startDate.getTime();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.DATE, -2);
        calendar2.set(Calendar.HOUR_OF_DAY, 23);
        calendar2.set(Calendar.MINUTE, 59);
        calendar2.set(Calendar.SECOND, 59);
        calendar2.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar2.getTime();
        long endTime = endDate.getTime();
        TimeInfo info = new TimeInfo();
        info.setStartTime(startTime);
        info.setEndTime(endTime);
        return info;
    }

    /**
     * endtime为今天
     * @return
     */
    public static TimeInfo getCurrentMonthStartAndEndTime(){
        Calendar calendar1 = Calendar.getInstance();
        calendar1.set(Calendar.DATE, 1);
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar1.getTime();
        long startTime = startDate.getTime();

        Calendar calendar2 = Calendar.getInstance();
//      calendar2.set(Calendar.HOUR_OF_DAY, 23);
//      calendar2.set(Calendar.MINUTE, 59);
//      calendar2.set(Calendar.SECOND, 59);
//      calendar2.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar2.getTime();
        long endTime = endDate.getTime();
        TimeInfo info = new TimeInfo();
        info.setStartTime(startTime);
        info.setEndTime(endTime);
        return info;
    }

    public static TimeInfo getLastMonthStartAndEndTime(){
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.MONTH, -1);
        calendar1.set(Calendar.DATE, 1);
        calendar1.set(Calendar.HOUR_OF_DAY, 0);
        calendar1.set(Calendar.MINUTE, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar1.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar1.getTime();
        long startTime = startDate.getTime();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.MONTH, -1);
        calendar2.set(Calendar.DATE, 1);
        calendar2.set(Calendar.HOUR_OF_DAY, 23);
        calendar2.set(Calendar.MINUTE, 59);
        calendar2.set(Calendar.SECOND, 59);
        calendar2.set(Calendar.MILLISECOND, 999);
        calendar2.roll(Calendar.DATE, -1);
        Date endDate = calendar2.getTime();
        long endTime = endDate.getTime();
        TimeInfo info = new TimeInfo();
        info.setStartTime(startTime);
        info.setEndTime(endTime);
        return info;
    }

    public static String getTimestampStr() {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * 获取当前星期几
     * @return 星期几
     */
    public static String getCurrentWeek(){
        String week = "星期";
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        switch (day){
            case Calendar.SUNDAY:
                week += "日";
                break;
            case Calendar.MONDAY:
                week += "一";
                break;
            case Calendar.TUESDAY:
                week += "二";
                break;
            case Calendar.WEDNESDAY:
                week += "三";
                break;
            case Calendar.THURSDAY:
                week += "四";
                break;
            case Calendar.FRIDAY:
                week += "五";
                break;
            case Calendar.SATURDAY:
                week += "六";
                break;
        }
        return week;
    }

    /**
     * 获取当前时间
     * @return HH:mm
     */
    public static String getCurrentTime(){
        String format = "HH:mm:ss";
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        return new SimpleDateFormat(format).format(curDate);
    }

    /**
     * 获取当前日期
     * @return yyyy-MM-dd
     */
    public static String getCurrentDate(String format){
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        return new SimpleDateFormat(format).format(curDate);
    }

    /**
     * date转换为Format
     */
    public static String converToString(Date date,String strFormat) {
        SimpleDateFormat format = new SimpleDateFormat(strFormat);
        return format.format(date);
    }
    /**
     * Format转换为Date
     */
    public static Date converToDate(String strDate,String strFormat) {
        DateFormat format = new SimpleDateFormat(strFormat);
        Date date = null;
        try{
            date = format.parse(strDate);
        }catch (Exception e){

        }

        return date;
    }

    /**
     * 获取两周内特定时间的周几
     * @param time 毫秒时间
     * @return 周几 下周几  x月x日
     */
    public static String getWeekByTime(long time){

        String strWeek = "";
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTimeInMillis(time);
        int day = calendar1.get(Calendar.DAY_OF_WEEK);
        int week = calendar1.get(Calendar.WEEK_OF_YEAR);

        int curWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        int curDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (curWeek == week){
            //同一周
            strWeek = "周";
            if (curDay == day){
                return "今日";
            }else if (curDay == Calendar.SUNDAY){
                strWeek = "下周";
            }
        }else if(week == curWeek + 1){
            //下周
            strWeek = "下周";
            if (day == Calendar.SUNDAY){
                strWeek = "周";
            }
        }else {
            //显示 x月x日
            return new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(time));
        }

        switch (day) {
            case Calendar.SUNDAY:
                strWeek += "日";
                break;
            case Calendar.MONDAY:
                strWeek += "一";
                break;
            case Calendar.TUESDAY:
                strWeek += "二";
                break;
            case Calendar.WEDNESDAY:
                strWeek += "三";
                break;
            case Calendar.THURSDAY:
                strWeek += "四";
                break;
            case Calendar.FRIDAY:
                strWeek += "五";
                break;
            case Calendar.SATURDAY:
                strWeek += "六";
                break;
        }

        return strWeek;
    }

    /**
     * 时间信息
     */
    /**
     * 时间信息
     */
    public static class TimeInfo {
        private long startTime;
        private long endTime;

        public TimeInfo() {
        }

        public long getStartTime() {
            return this.startTime;
        }

        public void setStartTime(long var1) {
            this.startTime = var1;
        }

        public long getEndTime() {
            return this.endTime;
        }

        public void setEndTime(long var1) {
            this.endTime = var1;
        }
    }
}