package com.example.xiaocaiweather.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

//用于处理和服务器的交互
public class HttpUtil {
    public static void sendOkHttpRequest(String address, okhttp3.Callback callback){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                .url(address)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
