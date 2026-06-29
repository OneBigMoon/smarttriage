package com.wcjk.triage.bean.fjykfsdeyy;

public class WaitPatientInfo {
    private String callno;
    private String patientname;

    public WaitPatientInfo() {
    }

    public WaitPatientInfo(String no, String name) {
        this.callno = no;
        this.patientname = name;
    }

    public String getCallno() {
        return callno;
    }

    public void setCallno(String callno) {
        this.callno = callno;
    }

    public String getPatientname() {
        return patientname;
    }

    public void setPatientname(String patientname) {
        this.patientname = patientname;
    }
}
