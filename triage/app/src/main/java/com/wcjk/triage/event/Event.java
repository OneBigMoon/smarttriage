package com.wcjk.triage.event;

/**
 * Created by hyc on 2018/8/8
 */
public class Event {
    public static final int TYPE_SETTING_ROTATE = 1;
    public static final int TYPE_RESTART = 2;
    public static final int TYPE_SCREENON = 3;
    public static final int TYPE_SCREENOFF = 4;
    public static final int TYPE_SETTING_VOLUMN = 6;
    public static final int TYPE_SETTING_STYLE = 7;
    public static final int TYPE_SETTING_SOURCE = 8;
    public static final int TYPE_SERVER_ERROR = 12;
    public static final int TYPE_HORSELAMP = 13;
    public static final int TYPE_UPGRADE= 14;
    public static final int TYPE_INSTALL= 15;
    public static final int TYPE_UPLOAD_LOG= 16;
    public static final int TYPE_TITLE = 17;
    public static final int TYPE_CLEAR = 18;

    public Event(int type){
        this.type = type;
    }

    public Event(int type, String value) {
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private int type;

    private String value;
}
