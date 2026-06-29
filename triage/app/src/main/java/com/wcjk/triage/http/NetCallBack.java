package com.wcjk.triage.http;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by hyc on 2018/8/9
 */

public abstract class NetCallBack<T> implements Callback<T> {
    @Override
    public void onResponse(Call<T> call, retrofit2.Response<T> response) {
        if (response == null || !response.isSuccessful()) {
            String message;
            if (response == null) {
                message = "响应为空";
            } else {
                message = "请求失败，HTTP " + response.code();
            }
            onFailed(message);
            return;
        }
        onSucess(response);
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (t == null){
            onFailed("请求失败");
            return;
        }
        if (t instanceof SocketTimeoutException){
        }else if (t instanceof ConnectException){
        }else if (t instanceof RuntimeException){
        }
        onFailed(t.getMessage());
    }

    public abstract void onSucess( retrofit2.Response<T> response);
    public abstract void onFailed(String msg);
}
