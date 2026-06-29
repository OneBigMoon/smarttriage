package com.wcjk.triage.event;

/**
 * Created by hyc on 2018/7/27
 */
public class CallEvent {
    String type;
    String data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public CallEvent(String text){
        this.setData(text);
    }
}
