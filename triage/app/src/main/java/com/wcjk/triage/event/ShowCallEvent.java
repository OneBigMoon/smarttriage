package com.wcjk.triage.event;

/**
 * Created by hyc on 2019/4/29
 */

public class ShowCallEvent {
    private String callText;

    public ShowCallEvent(String callText) {
        this.callText = callText;
    }

    public String getCallText() {
        return callText;
    }

    public void setCallText(String callText) {
        this.callText = callText;
    }
}
