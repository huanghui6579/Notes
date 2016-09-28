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
import com.yunxinlink.notes.persistent.NoteManager;
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
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_PARAM_ERROR, "params is null");
            }
            KLog.d(TAG, "login async failed params is null");
            return null;
        }
        Retrofit retrofit = buildRetrofit();
        UserApi repo = retrofit.create(UserApi.class);

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
                    success = handleLoginResult(context, userDto, result, false);
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
                String reason = t != null ? t.getMessage() : "";
                KLog.d(TAG, "login async error:" + reason);
                if (listener != null) {
                    listener.onLoadFailed(ActionResult.RESULT_ERROR, reason);
                }
            }
        });
        return call;
    }

    /**
     * 用户注册--异步
     * @param context
     * @param userDto
     * @return
     */
    public static Call<?> register(final Context context, final UserDto userDto, String confirmPassword, final OnLoadCompletedListener<ActionResult<UserDto>> listener) {
        KLog.d(TAG, "register invoke...");
        if (userDto == null) {
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_PARAM_ERROR, "params is null");
            }
            KLog.d(TAG, "register failed params is null");
            return null;
        }
        Retrofit retrofit = buildRetrofit();
        UserApi repo = retrofit.create(UserApi.class);
        Call<ActionResult<UserDto>> call = repo.register(buildRegisterParams(userDto, confirmPassword));
        call.enqueue(new Callback<ActionResult<UserDto>>() {
            @Override
            public void onResponse(Call<ActionResult<UserDto>> call, Response<ActionResult<UserDto>> response) {
                boolean success = false;
                ActionResult<UserDto> result = null;
                if (response != null) {
                    success = response.isSuccessful();
                    if (success) {  //成功,更新本地用户信息
                        result = response.body();
                        KLog.d(TAG, "register success:" + result);
                    } else {
                        KLog.d(TAG, "register failed code:" + response.code() + ", msg:" + response.message());
                    }
                } else {
                    KLog.d(TAG, "register failed response is null");
                }
                success = false;
                int code = ActionResult.RESULT_ERROR;
                //保存用户信息
                if (result != null) {
                    code = result.getResultCode();
                }
                success = handleRegisterResult(context, userDto, result);
                if (listener != null) {
                    if (success) {  //成功
                        listener.onLoadSuccess(result);

                    } else {
                        String reason = result == null ? null : result.getReason();
                        KLog.d(TAG, "user api impl register user error:" + result);
                        listener.onLoadFailed(code, reason);
                    }
                }
            }

            @Override
            public void onFailure(Call<ActionResult<UserDto>> call, Throwable t) {
                String reason = t != null ? t.getMessage() : "";
                KLog.d(TAG, "user api impl register error:" + reason);
                if (listener != null) {
                    listener.onLoadFailed(ActionResult.RESULT_ERROR, reason);
                }
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
     * 处理注册后的结果
     * @param context
     * @param userDto
     * @param result
     * @return
     */
    private static boolean handleRegisterResult(Context context, UserDto userDto, ActionResult<UserDto> result) {
        boolean success = false;
        //保存用户信息
        int code = result.getResultCode();
        UserDto resultUserDto = result.getData();
        if (resultUserDto != null && code == ActionResult.RESULT_SUCCESS) {  //成功，则保存用户信息到本地
            updateLocalUserAsync(context, userDto, resultUserDto);
            success = true;
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

            KLog.d(TAG, "update local user state local user will update");
            User user = original.getUser();
            if (user == null) {
                user = new User();
            }
            if (!user.checkId()) {
                int id = NoteUtil.getAccountId(context);
                user.setId(id);
            }
            user.setOpenUserId(original.getOpenUserId());
            mergeUserInfo(user, result.getUser());

            if (context == null) {  //该界面可能已经销毁
                KLog.d(TAG, "update local update local user context is null will use app context");
                context = NoteApplication.getInstance();
            }

            //保存账号类型
            int accountType = original.getType();
            NoteUtil.saveAccountType(context, accountType);

            //添加或更新本地账号信息
            boolean success = UserManager.getInstance().insertOrUpdate(user);
            int localUserId = user.getId();
            if (success && localUserId > 0) {  //该id已在本地不存在
                NoteUtil.saveAccountId(context, localUserId);
            }
            KLog.d(TAG, "update local updateLocalUser result:" + success);
            
            doInbackground(new NoteTask(user) {
                @Override
                public void run() {
                    KLog.d(TAG, "update local completed and will merge local notes");
                    NoteManager.getInstance().mergeLocalNotes((User) params[0], null);
                }
            });
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
            String mobile = user.getMobile();
            if (mobile != null) {
                params.put("user.mobile", mobile);
            }
            String email = user.getEmail();
            if (email != null) {
                params.put("user.email", email);
            }
            String password = user.getPassword();
            if (password != null) {
                params.put("user.password", password);
            }
            String username = user.getUsername();
            if (username != null) {
                params.put("user.username", username);
            }
            String sid = user.getSid();
            if (sid != null) {
                params.put("user.sid", sid);
            }
        }
        String token = userDto.getToken();
        if (token != null) {
            params.put("token", token);
        }
        String openUserId = userDto.getOpenUserId();
        if (openUserId != null) {
            params.put("openUserId", openUserId);
        }
        params.put("expiresTime", String.valueOf(userDto.getExpiresTime()));
        Integer type = userDto.getType();
        int autoCreate = 0;
        if (type != null && type > 0) { //若账号不存在，则自动创建
            params.put("type", String.valueOf(type));
            autoCreate = 1;
        }
        params.put("autoCreate", String.valueOf(autoCreate));
        return params;
    }

    /**
     * 构建注册的参数
     * @param userDto
     * @param confirmPassword 确认密码
     * @return
     */
    private static Map<String, String> buildRegisterParams(UserDto userDto, String confirmPassword) {
        User user = userDto.getUser();
        Map<String, String> params = new HashMap<>();
        String mobile = user.getMobile();
        if (!TextUtils.isEmpty(mobile)) {
            params.put("user.mobile", mobile);
        }
        String email = user.getEmail();
        if (!TextUtils.isEmpty(email)) {
            params.put("user.email", email);
        }
        String password = user.getPassword();
        if (!TextUtils.isEmpty(password)) {
            params.put("user.password", password);
        }
        if (!TextUtils.isEmpty(confirmPassword)) {
            params.put("confirmPassword", confirmPassword);
        }
        return params;
    }
}
