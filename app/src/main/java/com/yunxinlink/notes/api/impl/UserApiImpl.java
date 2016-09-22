package com.yunxinlink.notes.api.impl;

import com.socks.library.KLog;
import com.yunxinlink.notes.api.UserApi;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.listener.OnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 用户信息的一些API接口
 * @author huanghui1
 * @update 2016/9/21 17:51
 * @version: 0.0.1
 */
public class UserApiImpl extends BaseApi {
    private static final String TAG = "UserApiImpl";

    /**
     * 用户登录
     * @param userDto
     * @param listener
     */
    public static void login(UserDto userDto, OnLoadCompletedListener<ActionResult<UserDto>> listener) {
        if (userDto == null) {
            KLog.d(TAG, "login failed params is null");
            return;
        }
        Retrofit retrofit = buildRetrofit();
        UserApi repo = retrofit.create(UserApi.class);
        User user = userDto.getUser();
        Map<String, String> params = new HashMap<>();
        params.put("user.mobile", user.getMobile());
        params.put("user.password", user.getPassword());
        params.put("user.username", user.getUsername());
        params.put("user.sid", user.getSid());
        params.put("token", userDto.getToken());
        params.put("openUserId", userDto.getOpenUserId());
        params.put("type", String.valueOf(userDto.getType()));
        params.put("expiresTime", String.valueOf(userDto.getExpiresTime()));
        Integer type = userDto.getType();
        int autoCreate = 0;
        if (type != null && type > 0) { //若账号不存在，则自动创建
            autoCreate = 1;
        }
        params.put("autoCreate", String.valueOf(autoCreate));
        Call<ActionResult<UserDto>> call = repo.login(params);
        boolean success = false;
        ActionResult<UserDto> result = null;
        try {
            //同步执行
            Response<ActionResult<UserDto>> response = call.execute();
            if (response != null) {
                success = response.isSuccessful();
                if (success) {  //成功,更新本地用户信息
                    result = response.body();
                    KLog.d(TAG, "login success:" + result);
                } else {
                    KLog.d(TAG, "login failed code:" + response.code() + ", msg:" + response.message());
                }
            } else {
                KLog.d(TAG, "login failed response is null");
            }
        } catch (IOException e) {
            KLog.e(TAG, "login error:" + e.getMessage());
        }
        if (listener != null) {
            if (success) {
                listener.onLoadSuccess(result);
            } else {
                listener.onLoadFailed(null);
            }
        }
    }
}
