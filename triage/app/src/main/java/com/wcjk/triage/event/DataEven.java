package com.wcjk.triage.event;

import org.json.JSONObject;

/**
 * Created by hyc on 2018/8/18
 */
public class DataEven {
    public static final int TYPE_PARAMS = 1;
    public static final int TYPE_OTHER = 2;

    private int type;
    private JSONObject data;

    public DataEven(int type,JSONObject data){
        setType(type);
        setData(data);
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
