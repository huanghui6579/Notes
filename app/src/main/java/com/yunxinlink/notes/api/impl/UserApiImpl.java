package com.yunxinlink.notes.api.impl;

import android.content.Context;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.UserApi;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.listener.OnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
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
     * 用户登录--同步
     * @param userDto
     * @param listener
     */
    public static Call<?> login(Context context, UserDto userDto, OnLoadCompletedListener<ActionResult<UserDto>> listener) {
        if (userDto == null) {
            KLog.d(TAG, "login failed params is null");
            return null;
        }
        Retrofit retrofit = buildRetrofit();
        UserApi repo = retrofit.create(UserApi.class);
        
        Call<ActionResult<UserDto>> call = repo.login(buildLoginParams(userDto));
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
        int code = ActionResult.RESULT_ERROR;
        //保存用户信息
        if (result != null) {
            code = result.getResultCode();
            success = handleLoginResult(context, userDto, result, true);
        }
        if (listener != null) {
            if (success) {
                listener.onLoadSuccess(result);
            } else {
                listener.onLoadFailed(code, null);
            }
        }
        return call;
    }

    /**
     * 用户登录--异步
     * @param userDto
     * @param listener
     */
    public static Call<?> loginAsync(final Context context, final UserDto userDto, final OnLoadCompletedListener<ActionResult<UserDto>> listener) {
        KLog.d(TAG, "login async invoke...");
        if (userDto == null) {
            KLog.d(TAG, "login async failed params is null");
            return null;
        }
        Retrofit retrofit = buildRetrofit();
        UserApi repo = retrofit.create(UserApi.class);

        if (userDto.getType() >= 0 && TextUtils.isEmpty(userDto.getOpenUserId())) {   //第三方账号登录,且没有获取第三方账号
            
        }
        
        Call<ActionResult<UserDto>> call = repo.login(buildLoginParams(userDto));
        call.enqueue(new Callback<ActionResult<UserDto>>() {
            @Override
            public void onResponse(Call<ActionResult<UserDto>> call, Response<ActionResult<UserDto>> response) {
                boolean success = false;
                ActionResult<UserDto> result = null;
                if (response != null) {
                    success = response.isSuccessful();
                    if (success) {  //成功,更新本地用户信息
                        result = response.body();
                        KLog.d(TAG, "login async success:" + result);
                    } else {
                        KLog.d(TAG, "login async failed code:" + response.code() + ", msg:" + response.message());
                    }
                } else {
                    KLog.d(TAG, "login async failed response is null");
                }
                success = false;
                int code = ActionResult.RESULT_ERROR;
                //保存用户信息
                if (result != null) {
                    code = result.getResultCode();
                    success = handleLoginResult(context, userDto, result, true);
                }
                if (listener != null) {
                    if (success) {  //成功
                        listener.onLoadSuccess(result);

                    } else {
                        listener.onLoadFailed(code, null);
                    }
                }
                
            }

            @Override
            public void onFailure(Call<ActionResult<UserDto>> call, Throwable t) {

            }
        });
        return call;
    }

    /**
     * 处理登录后的本地数据操作
     * @param context
     * @param userDto 登录的参数，条件
     * @param result 服务器返回的结果
     * @param async 是否异步处理  
     * @return 请求结果是否成功             
     */
    private static boolean handleLoginResult(Context context, UserDto userDto, ActionResult<UserDto> result, boolean async) {
        boolean success = false;
        //保存用户信息
        int code = result.getResultCode();
        UserDto resultUserDto = result.getData();
        switch (code) {
            case ActionResult.RESULT_FAILED:    //失败
                KLog.d(TAG, "handleLoginResult result code failed:" + code + ", reason:" + result);
                break;
            case ActionResult.RESULT_PARAM_ERROR:    //参数错误
                KLog.d(TAG, "handleLoginResult result code param error:" + code + ", reason:" + result);
                break;
            case ActionResult.RESULT_STATE_DISABLE:    //用户不可用了
                if (async) {
                    updateLocalUserAsync(context, userDto, resultUserDto);
                } else {
                    updateLocalUser(context, userDto, resultUserDto);
                }
                KLog.d(TAG, "handleLoginResult result code user state is disable:" + code + ", reason:" + result);
                break;
            case ActionResult.RESULT_SUCCESS:   //成功
                success = true;
                if (async) {
                    updateLocalUserAsync(context, userDto, resultUserDto);
                } else {
                    updateLocalUser(context, userDto, resultUserDto);
                }
                KLog.d(TAG, "handleLoginResult result code success:" + code + ", reason:" + result);
                break;
        }
        return success;
    }

    /**
     * 更新本地的用户信息--同步处理
     * @param original 本地的用户信息
     * @param result 服务器返回的结果
     */
    private static void updateLocalUser(Context context, final UserDto original, final UserDto result) {
        if (result != null && result.getUser() != null) {

            KLog.d(TAG, "login async state disable local user will update");
            User user = original.getUser();
            mergeUserInfo(user, result.getUser());

            if (context == null) {  //该界面可能已经销毁
                KLog.d(TAG, "login async update local user context is null will use app context");
                context = NoteApplication.getInstance();
            }

            //保存账号类型
            int accountType = original.getType();
            NoteUtil.saveAccountType(context, accountType);

            //添加或更新本地账号信息
            boolean success = UserManager.getInstance().insertOrUpdate(user);
            int localUserId = NoteUtil.getAccountId(context);
            if (success && localUserId <= 0 || localUserId != user.getId()) {  //该id已在本地不存在
                NoteUtil.saveAccountId(context, localUserId);
            }
            KLog.d(TAG, "login async updateLocalUser result:" + success);
        }
    }
    
    /**
     * 更新本地的用户信息--异步处理
     * @param original 本地的用户信息
     * @param result 服务器返回的结果
     */
    private static void updateLocalUserAsync(Context context, final UserDto original, final UserDto result) {
        if (result != null && result.getUser() != null) {
            
            doInbackground(new NoteTask(context, original, result) {
                @Override
                public void run() {
                    KLog.d(TAG, "updateLocalUserAsync invoke");
                    updateLocalUser((Context) params[0], (UserDto) params[1], (UserDto) params[2]);
                }
            });
            
        }
    }

    /**
     * 从服务器合并用户信息到本地
     * @param target
     * @param src
     */
    private static void mergeUserInfo(User target, User src) {
        target.setAvatar(src.getAvatar());
        target.setCreateTime(src.getCreateTime());
        target.setGender(src.getGender());
        target.setMobile(src.getMobile());
        target.setPassword(src.getPassword());
        target.setSid(src.getSid());
        target.setState(src.getState());
        target.setUsername(src.getUsername());
        target.setEmail(src.getEmail());
    }

    /**
     * 构建登录的请求参数
     * @param userDto
     * @return
     */
    private static Map<String, String> buildLoginParams(UserDto userDto) {
        User user = userDto.getUser();
        Map<String, String> params = new HashMap<>();
        if (user != null) {
            params.put("user.mobile", user.getMobile());
            params.put("user.password", user.getPassword());
            params.put("user.username", user.getUsername());
            params.put("user.sid", user.getSid());
        }
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
        return params;
    }
}
