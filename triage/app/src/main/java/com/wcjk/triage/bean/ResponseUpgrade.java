package com.wcjk.triage.bean;

/**
 * Created by hyc on 2019/3/10
 */

public class ResponseUpgrade {

    /**
     * errcode : 0
     * name : upload_78a56428595ab973d7db5a8afd0648bb.apk
     * originname : *******_v1.0.0.1810151548.apk
     * md5 : dbd0f92d216e0ed4feba18990d0a0647
     * appversion : 2.2.2
     */

    private int errcode;
    private String name;
    private String originname;
    private String md5;
    private String appversion;

    public int getErrcode() {
        return errcode;
    }

    public void setErrcode(int errcode) {
        this.errcode = errcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginname() {
        return originname;
    }

    public void setOriginname(String originname) {
        this.originname = originname;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getAppversion() {
        return appversion;
    }

    public void setAppversion(String appversion) {
        this.appversion = appversion;
    }
}
