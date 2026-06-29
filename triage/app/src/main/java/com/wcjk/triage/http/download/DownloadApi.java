package com.wcjk.triage.http.download;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by hyc on 2019/3/26
 */

public interface DownloadApi {
    @Streaming
    @GET
    Observable<ResponseBody> download(@Url String url);

}
