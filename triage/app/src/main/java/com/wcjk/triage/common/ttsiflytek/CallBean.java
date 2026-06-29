package com.wcjk.triage.common.ttsiflytek;

/**
 * Created by hyc on 2019/4/29
 */

public class CallBean {
    private String callText;
    private String showText;

    public CallBean(String callText, String showText) {
        this.callText = callText;
        this.showText = showText;
    }

    public String getCallText() {
        return callText;
    }

    public void setCallText(String callText) {
        this.callText = callText;
    }

    public String getShowText() {
        return showText;
    }

    public void setShowText(String showText) {
        this.showText = showText;
    }
}
