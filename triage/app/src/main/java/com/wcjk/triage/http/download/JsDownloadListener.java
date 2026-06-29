package com.wcjk.triage.http.download;

/**
 * Created by hyc on 2019/3/26
 */

public interface JsDownloadListener {
    void onStartDownload(long length);
    void onProgress(int progress);
    void onFail(String errorInfo);
    void onFinishDownload() ;
}
