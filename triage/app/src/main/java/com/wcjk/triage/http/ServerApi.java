package com.wcjk.triage.http;

import com.wcjk.triage.bean.QueryDataSources;
import com.wcjk.triage.bean.QueryStyles;
import com.wcjk.triage.bean.QueryUpgrade;
import com.wcjk.triage.bean.ResponseUpgrade;
import com.wcjk.triage.bean.ServerResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

/**
 * Created by hyc on 2018/8/3
 */
public interface ServerApi {
    String BASE_URL = "/api/v1/";

    @POST("terminal/querystyles")
    Call<QueryStyles> getStyles();

    @POST("terminal/querydatasources")
    Call<QueryDataSources> getDataSources();

    @POST("upgrade")
    Observable<ResponseUpgrade> getUpgradeVersion(@Body  QueryUpgrade parmas);

    @GET("upgrade/download")
    Observable<ResponseBody> download( @Query("name") String apkName);

    @Multipart
    @POST("upload?type=logfile")
    Observable<ServerResponse> uploadLog(@PartMap Map<String, RequestBody> map, @Part List<MultipartBody.Part> parts);
}