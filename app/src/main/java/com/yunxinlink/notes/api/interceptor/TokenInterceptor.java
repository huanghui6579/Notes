package com.yunxinlink.notes.api.interceptor;

import android.text.TextUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/12/4 11:23
 */
public class TokenInterceptor implements Interceptor {
    private String token;

    public TokenInterceptor(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        /**
         * 如果你的 token 是空的，就是还没有请求到 token，比如对于登陆请求，是没有 token 的，
         * 只有等到登陆之后才有 token，这时候就不进行附着上 token。另外，如果你的请求中已经带有验证 header 了，
         * 比如你手动设置了一个另外的 token，那么也不需要再附着这一个 token.
         */
        if (token == null || alreadyHasAuthorizationHeader(originalRequest)) {
            return chain.proceed(originalRequest);
        }
        Request authorised = originalRequest.newBuilder()
                .header("Authorization", token)
                .build();
        return chain.proceed(authorised);
    }

    /**
     * 判断是否有了token的头了
     * @param request
     * @return
     */
    private boolean alreadyHasAuthorizationHeader(Request request) {
        return !TextUtils.isEmpty(request.header("Authorization"));
    }
}
