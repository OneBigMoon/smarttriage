package com.wcjk.triage.http;

import com.wcjk.triage.BuildConfig;
import com.wcjk.triage.bean.QueryDataSources;
import com.wcjk.triage.bean.QueryStyles;
import com.wcjk.triage.bean.QueryUpgrade;
import com.wcjk.triage.bean.ResponseUpgrade;
import com.wcjk.triage.bean.ServerResponse;
import com.wcjk.triage.common.Config;
import com.wcjk.triage.common.utils.AppUtils;
import com.wcjk.triage.common.utils.Log;
import com.wcjk.triage.global.Global;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by hyc on 2018/8/3
 */
public class ApiManager {
    private Log log = Log.getLogger(this.getClass());
    private ServerApi api;
    private static final long TIME_OUT = 30000;
    private static ApiManager instance;
    public static ApiManager getInstance() {
        if (instance == null) {
            synchronized (ApiManager.class) {
                if (instance == null) {
                    instance = new ApiManager();
                }
            }
        }
        return instance;
    }

    public synchronized void resetApi() {
        log.w("reset api");
        api = null;
    }

    private synchronized ServerApi createApi() {
        if (api == null) {
            OkHttpClient okHttpClient = new OkHttpClient
                    .Builder()
                    .connectTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
                    .readTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
                    //添加应用拦截器
                    .addInterceptor(new Retry(3))
                    .build();

            String path = AppUtils.getUrl(Config.getConfig(Global.ServerIpKey,Global.ServerIp),
                    Config.getConfig(Global.ServerPortKey,Global.ServerPort), ServerApi.BASE_URL);
            log.w("path :" + path);

            Retrofit retrofit = new Retrofit
                    .Builder()
                    .client(okHttpClient)
                    .baseUrl(path)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();

            api = retrofit.create(ServerApi.class);
        }
        return api;
    }

    /**
     * 获取样式列表
     *
     * @param
     * @param callBack
     */
    public void getStyles(NetCallBack<QueryStyles> callBack) {
        createApi()
                .getStyles()
                .enqueue(callBack);
    }

    /**
     * 获取数据源
     *
     * @param
     * @param callBack
     */
    public void getDataSources(NetCallBack<QueryDataSources> callBack) {
        createApi()
                .getDataSources()
                .enqueue(callBack);
    }

    public Observable<ResponseUpgrade> getUpgradeVersion(String local_version) {
        QueryUpgrade parmas = new QueryUpgrade(BuildConfig.FLAVOR,local_version);
        return createApi().getUpgradeVersion(parmas);
    }

    public Observable<ServerResponse> uploadLogs(String no, List<String> list_log) {
        Map<String,RequestBody> params = new HashMap<>();
        params.put("no",convertToRequestBody(no));
        return createApi().uploadLog(params,filesToMultipartBodyParts(list_log));
    }

    private RequestBody convertToRequestBody(String param){
        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), param);
        return requestBody;
    }

    private List<MultipartBody.Part> filesToMultipartBodyParts(List<String> files) {
        if (files == null || files.size() <= 0) return null;
        List<MultipartBody.Part> parts = new ArrayList<>(files.size());
        for (String filename : files) {
            File file = new File(filename);
            RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
            parts.add(part);
        }
        return parts;
    }
}
