package com.yunxinlink.notes.api.interceptor;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Version;

/**
 * This interceptor adds a custom User-Agent.
 * @author tiger
 * @version 1.0.0
 * @update 2016/10/2 10:34
 */
public class UserAgentInterceptor implements Interceptor {
    private static final String TAG = "UserAgentInterceptor";

    private String userAgent;

    public UserAgentInterceptor() {}

    public UserAgentInterceptor(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        //agent-->"okhttp/3.4.1 (Linux; Android 6.0.1; 1505-A02 Build/MMB29M)"
        if (userAgent == null) {
            userAgent = Version.userAgent() + " (Linux; Android " + SystemUtil.getBuildVersion() + "; " + SystemUtil.getPhoneModel() + " Build/" + SystemUtil.getBuildNumber() + ")";
        }
        KLog.d(TAG, "userAgent:" + userAgent);
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build();
        return chain.proceed(requestWithUserAgent);
    }
}
