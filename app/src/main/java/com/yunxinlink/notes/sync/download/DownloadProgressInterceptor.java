package com.yunxinlink.notes.sync.download;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 下载进度及拦截器
 * @author huanghui-iri
 * @update 2016/11/18 17:49
 * @version: 0.0.1
 */
public class DownloadProgressInterceptor implements Interceptor {
    private DownloadListener downloadListener;

    public DownloadProgressInterceptor(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        return originalResponse.newBuilder()
                .body(new DownloadProgressResponseBody(originalResponse.body(), downloadListener))
                .build();
    }
}
