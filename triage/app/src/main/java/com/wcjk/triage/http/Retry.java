package com.wcjk.triage.http;

/**
 * Created by hyc on 2019/2/14
 */

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 自定义的，重试N次的拦截器
 * 通过：addInterceptor 设置
 */
public  class Retry implements Interceptor {
    public int maxRetry;//最大重试次数
    private int retryNum = 0;//假如设置为3次重试的话，则最大可能请求4次（默认1次+3次重试）
    public Retry(int maxRetry) {
        this.maxRetry = maxRetry;
    }
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request newRequest = chain.request().newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:0.9.4)")
                .addHeader("Content-Type","application/json")
                .build();

//        Request request = chain.request();
        Response response = chain.proceed(newRequest);
        Log.i("Retry","num:"+retryNum);
        while (!response.isSuccessful() && retryNum < maxRetry) {
            retryNum++;
            Log.i("Retry","num:"+retryNum);
            response = chain.proceed(newRequest);
        }
        return response;
    }
}
