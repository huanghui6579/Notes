package com.yunxinlink.notes.api.impl;

import com.yunxinlink.notes.api.interceptor.UserAgentInterceptor;
import com.yunxinlink.notes.sync.download.DownloadListener;
import com.yunxinlink.notes.sync.download.DownloadProgressInterceptor;
import com.yunxinlink.notes.util.SystemUtil;

import okhttp3.Interceptor;
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
    private static final String BASE_URL = "http://192.168.0.5:8080/noteapi/";
//    private static final String BASE_URL = "http://192.168.0.4:8080/noteapi/";
//    private static final String BASE_URL = "http://10.78.48.29:8080/noteapi/";
//    private static final String BASE_URL = "http://www.yunxinlink.com:8888/noteapi/";

    /**
     * 创建Retrofit
     * @return
     */
    protected static Retrofit buildRetrofit() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        UserAgentInterceptor agentInterceptor = new UserAgentInterceptor();

        return buildRetrofit(httpLoggingInterceptor, agentInterceptor);
    }

    /**
     * 构建Retrofit
     * @param interceptors
     * @return
     */
    private static Retrofit buildRetrofit(Interceptor... interceptors) {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        for (Interceptor interceptor : interceptors) {
            builder.addInterceptor(interceptor);
        }
        
        OkHttpClient okHttpClient = builder.build();
        return new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * 构建Retrofit
     * @param downloadListener
     * @return
     */
    protected static Retrofit buildDownloadRetrofit(DownloadListener downloadListener) {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        DownloadProgressInterceptor downloadInterceptor = new DownloadProgressInterceptor(downloadListener);

        UserAgentInterceptor agentInterceptor = new UserAgentInterceptor();

        return buildRetrofit(httpLoggingInterceptor, agentInterceptor, downloadInterceptor);
    }

    /**
     * 后台处理任务
     * @param runnable
     */
    protected static void doInbackground(Runnable runnable) {
        SystemUtil.getThreadPool().execute(runnable);
    }
    
}
