package com.wcjk.triage.http.download;

/**
 * Created by hyc on 2019/3/10
 */

public interface DownloadCallBack {

    void onProgress(int progress);

    void onCompleted();

    void onError(String msg);

}