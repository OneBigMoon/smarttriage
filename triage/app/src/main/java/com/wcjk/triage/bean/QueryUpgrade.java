package com.wcjk.triage.bean;

/**
 * Created by hyc on 2019/3/10
 */

public class QueryUpgrade {

    /**
     * model : model
     * appversion : 1.2.2.343243
     */

    private String model;
    private String appversion;

    public QueryUpgrade(String model, String appversion) {
        this.model = model;
        this.appversion = appversion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAppversion() {
        return appversion;
    }

    public void setAppversion(String appversion) {
        this.appversion = appversion;
    }
}
