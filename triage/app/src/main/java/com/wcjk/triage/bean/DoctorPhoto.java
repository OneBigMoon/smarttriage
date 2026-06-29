package com.wcjk.triage.bean;

/**
 * Created by hyc on 2019/4/4
 */

public class DoctorPhoto {
    /**
     * type : Buffer
     * data : [255,216]
     */

    private String type;
    private byte[] data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
