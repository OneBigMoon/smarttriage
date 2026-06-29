package com.wcjk.triage.bean.fjykfsdeyy;

import java.util.List;

public class KeShiInfo {
    private String officename;
    private String doctorname;
    private String waitno;
    private String callno;
    private String patientname;
    private List<WaitPatientInfo> waitPatientInfoList;

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

    public String getWaitno() {
        return waitno;
    }

    public void setWaitno(String waitno) {
        this.waitno = waitno;
    }

    public List<WaitPatientInfo> getWaitPatientInfoList() {
        return waitPatientInfoList;
    }

    public void setWaitPatientInfoList(List<WaitPatientInfo> waitPatientInfoList) {
        this.waitPatientInfoList = waitPatientInfoList;
    }

    public String getOfficename() {
        return officename;
    }

    public void setOfficename(String officename) {
        this.officename = officename;
    }

    public String getDoctorname() {
        return doctorname;
    }

    public void setDoctorname(String doctorname) {
        this.doctorname = doctorname;
    }
}
