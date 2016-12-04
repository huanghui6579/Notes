package com.yunxinlink.notes.api.interceptor;

import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.impl.UserApi;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.NoteUtil;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * 当token过期或者不存在时，需要重启请求获取token
 * @author tiger
 * @version 1.0.0
 * @update 2016/12/4 12:21
 */
public class TokenAuthenticator implements Authenticator {
    private static final String TAG = "TokenAuthenticator";

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        //重新获取token
        String token = refreshToken();
        if (TextUtils.isEmpty(token)) {
            throw new IOException("authenticate refresh token but is null");
        }
        return response.request().newBuilder()
                .addHeader("Authorization", token)
                .build();
    }

    /**
     * 重新刷新请求token
     * @return
     */
    private String refreshToken() {
        KLog.e(TAG, "authenticator refresh token");
        NoteApplication app = NoteApplication.getInstance();
        User user = app.getCurrentUser();
        if (user == null || !user.isAvailable()) {
            KLog.d(TAG, "authenticator refresh token but user is null or not available");
            return null;
        }
        final UserDto userDto = NoteUtil.buildLoginParams(app.getApplicationContext(), user, null);
        if (userDto == null) {
            KLog.d(TAG, "authenticator refresh token but user dto param is null");
            return null;
        }
        boolean success = UserApi.login(app.getApplicationContext(), userDto, null);
        user = app.getCurrentUser();
        if (success && user != null && user.isAvailable()) {
            String token = user.getToken();
            KLog.e(TAG, "authenticator refresh token success:" + token);
            return token;
        } else {
            KLog.d(TAG, "authenticator refresh token login failed");
            return null;
        }
    }
}
