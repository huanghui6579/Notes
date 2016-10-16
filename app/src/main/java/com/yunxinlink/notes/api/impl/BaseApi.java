package com.yunxinlink.notes.api.impl;

import com.yunxinlink.notes.api.interceptor.UserAgentInterceptor;
import com.yunxinlink.notes.util.SystemUtil;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author huanghui1
 * @update 2016/9/20 14:58
 * @version: 0.0.1
 */
public abstract class BaseApi {
//    private static final String BASE_URL = "http://192.168.0.5:8080/noteapi/";
    private static final String BASE_URL = "http://192.168.0.4:8080/noteapi/";
//    private static final String BASE_URL = "http://10.100.80.138:8080/noteapi/";
//    private static final String BASE_URL = "http://www.yunxinlink.com:8888/noteapi/";

    /**
     * 创建Retrofit
     * @return
     */
    protected static Retrofit buildRetrofit() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(new UserAgentInterceptor())
                .build();
        return new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * 后台处理任务
     * @param runnable
     */
    protected static void doInbackground(Runnable runnable) {
        SystemUtil.getThreadPool().execute(runnable);
    }
    
}
