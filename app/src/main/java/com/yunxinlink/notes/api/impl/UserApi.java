package com.yunxinlink.notes.api.impl;

import android.content.Context;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.IUserApi;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.listener.OnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.State;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
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
public class UserApi extends BaseApi {
    private static final String TAG = "UserApi";

    /**
     * 用户登录--同步
     * @param userDto
     * @return 是否登录成功
     */
    public static boolean login(Context context, UserDto userDto, OnLoadCompletedListener<ActionResult<UserDto>> listener) {
        if (userDto == null) {
            KLog.d(TAG, "login failed params is null");
            return false;
        }
        Retrofit retrofit = buildRetrofit();
        IUserApi repo = retrofit.create(IUserApi.class);
        
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
            success = handleLoginResult(context, userDto, result, false);
        }
        if (listener != null) {
            if (success) {
                listener.onLoadSuccess(result);
            } else {
                listener.onLoadFailed(code, null);
            }
        }
        return success;
    }

    /**
     * 用户登录--异步
     * @param userDto
     * @param listener
     */
    public static void loginAsync(final Context context, final UserDto userDto, final OnLoadCompletedListener<ActionResult<UserDto>> listener) {
        KLog.d(TAG, "login async invoke...");
        if (userDto == null) {
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_PARAM_ERROR, "params is null");
            }
            KLog.d(TAG, "login async failed params is null");
            return;
        }
        doInbackground(new NoteTask(context, userDto, listener) {
            @Override
            public void run() {
                Context ctx = (Context) params[0];
                UserDto userParam = (UserDto) params[1];
                OnLoadCompletedListener<ActionResult<UserDto>> onListener = (OnLoadCompletedListener<ActionResult<UserDto>>) params[2];
                login(ctx, userParam, onListener);
            }
        });
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
        IUserApi repo = retrofit.create(IUserApi.class);
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
     * 同步用户信息，如果本地比较新，则上传本地用户信息，如果本地比较旧，则从服务器更新本地信息，新旧的判断标准是根据syncState来的
     * @param user
     */
    public static void syncUserInfo(final User user) {
        KLog.d(TAG, "sync user info invoke and user:" + user);
        if (user == null || TextUtils.isEmpty(user.getSid())) {
            KLog.d(TAG, "sync user info failed params is null or sid is empty:" + user);
            return;
        }
        boolean isUp = user.checkSyncUp();
        Retrofit retrofit = buildRetrofit();
        final String sid = user.getSid();
        final int id = user.getId();
        final String oldAvatarHash = user.getAvatarHash();
        if (isUp) { //需要上传本地信息到服务器
            KLog.d(TAG, "sync user info upload to server");
            IUserApi repoUp = retrofit.create(IUserApi.class);
            Map<String, RequestBody> map = new HashMap<>();
            String nickname = user.getNickname();
            if (nickname != null) {
                map.put("user.nickname", RequestBody.create(null, nickname));
            }
            String avatar = user.getAvatar();
            if (avatar != null) {
                File file = new File(avatar);
                if (file.exists()) {
                    String filename = file.getName();
                    String mime = FileUtil.getWebMime(file);
                    if (mime == null) {
                        mime = "image/png";
                    }
                    RequestBody img = RequestBody.create(MediaType.parse(mime), file);
                    //avatarFile: 与服务器端的参数名相同
                    map.put("avatarFile\"; filename=\"" + filename + "", img);
                }
            }
            String avatarHash = user.getAvatarHash();
            if (avatarHash != null) {
                map.put("user.avatarHash", RequestBody.create(null, avatarHash));
            }
            Call<ActionResult<Void>> callUp = repoUp.modify(sid, map);
            callUp.enqueue(new Callback<ActionResult<Void>>() {
                @Override
                public void onResponse(Call<ActionResult<Void>> call, Response<ActionResult<Void>> response) {
                    KLog.d(TAG, "sync up user info response success");
                    if (response == null) {
                        KLog.d(TAG, "sync up user info response success but response is null so up failed");
                        return;
                    }
                    ActionResult<Void> actionResult = response.body();
                    if (actionResult == null) {
                        KLog.d(TAG, "sync up user info response success but action result is null so up failed");
                        return;
                    }
                    KLog.d(TAG, "sync up user info response success and action result is:" + actionResult);
                    if (actionResult.isSuccess()) { //成功
                        KLog.d(TAG, "sync up user info response success and will save change sync state to done in local");
                        //将同步的状态修改到本地
                        User u = new User();
                        u.setSid(sid);
                        u.setId(id);
                        u.setSyncState(SyncState.SYNC_DONE.ordinal());
                        updateLocalUserAsync(u);
                    } else {
                        KLog.d(TAG, "sync up user info response failed.");
                    }
                }

                @Override
                public void onFailure(Call<ActionResult<Void>> call, Throwable t) {
                    KLog.d(TAG, "sync up user info on failed no response:" + t);
                }
            });
        } else {    //从服务器上更新信息到本地
            KLog.d(TAG, "sync user info download from server");
            final IUserApi repoDown = retrofit.create(IUserApi.class);
            Call<ActionResult<User>> call = repoDown.downInfo(sid);
            call.enqueue(new Callback<ActionResult<User>>() {
                @Override
                public void onResponse(Call<ActionResult<User>> call, Response<ActionResult<User>> response) {
                    KLog.d(TAG, "sync down user info response success");
                    if (response == null) {
                        KLog.d(TAG, "sync down user info response success but response is null so down failed");
                        return;
                    }
                    ActionResult<User> actionResult = response.body();
                    if (actionResult == null) {
                        KLog.d(TAG, "sync down user info response success but action result is null so down failed");
                        return;
                    }
                    KLog.d(TAG, "sync down user info response success and action result is:" + actionResult);
                    if (actionResult.isSuccess() && actionResult.getData() != null) { //成功
                        KLog.d(TAG, "sync down user info response success and will save info to local");
                        User resultUser = actionResult.getData();
                        //将同步的状态修改到本地
                        User u = new User();
                        u.setSid(sid);
                        u.setId(id);
                        u.setSyncState(SyncState.SYNC_DONE.ordinal());
                        u.setOpenUserId(resultUser.getOpenUserId());
                        String newAvatarHash = resultUser.getAvatarHash();
                        //头像是否改变，若改变，则需要下载头像
                        boolean avatarChanged = !SystemUtil.equalsStr(newAvatarHash, oldAvatarHash);
                        u.setAvatarHash(newAvatarHash);
                        u.setEmail(resultUser.getEmail());
                        u.setGender(resultUser.getGender());
                        u.setMobile(resultUser.getMobile());
                        u.setNickname(resultUser.getNickname());
                        u.setState(resultUser.getState());
                        if (avatarChanged) {    //需要下载头像
                            KLog.d(TAG, "sync down user info response success and avatar changed and will download avatar from server");
                            downloadAvatarAsync(u);
                        } else {
                            KLog.d(TAG, "sync down user info response success and avatar not changed and will save user info to local");
                            updateLocalUserAsync(u);
                        }
                    } else {
                        KLog.d(TAG, "sync up user info response failed.");
                    }
                }

                @Override
                public void onFailure(Call<ActionResult<User>> call, Throwable t) {
                    KLog.d(TAG, "sync down user info on failed no response:" + t);
                }
            });
        }
//        IUserApi repo = retrofit.create(IUserApi.class);
//        Call<ActionResult<UserDto>> call = repo.register(buildRegisterParams(userDto, confirmPassword));
    }

    /**
     * 异步下载头像
     * @param user
     */
    public static void downloadAvatarAsync(User user) {
        doInbackground(new NoteTask(user) {
            @Override
            public void run() {
                try {
                    downloadAvatar((User) params[0]);
                } catch (IOException e) {
                    KLog.d(TAG, "download avatar async error:" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 异步发送重置密码的请求，该方法异步执行，不需要额外的创建线程
     * @param account 收件人的邮箱
     * @param listener 加载完毕的监听器
     * @return
     */
    public static Call<?> resetPasswordAsync(String account, final OnLoadCompletedListener<ActionResult<Void>> listener) {
        if (TextUtils.isEmpty(account)) {
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_PARAM_ERROR, null);
            }
            return null;
        }
        Retrofit retrofit = buildRetrofit();
        final IUserApi repo = retrofit.create(IUserApi.class);
        Call<ActionResult<Void>> call = repo.resetPassword(account);
        call.enqueue(new Callback<ActionResult<Void>>() {
            @Override
            public void onResponse(Call<ActionResult<Void>> call, Response<ActionResult<Void>> response) {
                int actionCode = 0;
                if (response == null || !response.isSuccessful() || response.body() == null) {
                    actionCode = ActionResult.RESULT_FAILED;
                    KLog.d(TAG, "reset password response is null or not successful");
                } else {
                    ActionResult<Void> actionResult = response.body();
                    actionCode = actionResult.getResultCode();
                }
                if (listener != null) {
                    if (actionCode == ActionResult.RESULT_SUCCESS) {    //成功
                        KLog.d(TAG, "user api reset password success");
                        listener.onLoadSuccess(null);
                    } else {
                        KLog.d(TAG, "user api reset password not success");
                        listener.onLoadFailed(actionCode, null);
                    }
                }
            }

            @Override
            public void onFailure(Call<ActionResult<Void>> call, Throwable t) {
                KLog.e(TAG, "user api reset password on failed:" + t);
                if (listener != null) {
                    listener.onLoadFailed(ActionResult.RESULT_FAILED, null);
                }
            }
        });
        return call;
    }

    /**
     * 下载用户的头像，该方法同步在主线程中进行
     * @param user
     */
    private static boolean downloadAvatar(User user) throws IOException {
        String sid = user.getSid();
        Retrofit retrofit = buildRetrofit();
        final IUserApi repo = retrofit.create(IUserApi.class);
        Call<ResponseBody> call = repo.downAvatar(sid);
        Response<ResponseBody> response = call.execute();
        KLog.d(TAG, "download avatar invoke...");
        if (response == null || response.body() == null) {
            KLog.d(TAG, "download avatar response is null or body is null so down failed");
            return false;
        }
        Headers headers = response.headers();
        String filename = SystemUtil.getFilename(headers);
        if (filename == null) {
            filename = sid + ".png";
        }
        boolean saveResult = false;
        String filePath = null;
        try {
            File saveFile = SystemUtil.getAvatarFileByName(filename);
            if (saveFile != null) {
                filePath = saveFile.getAbsolutePath();
                saveResult = FileUtil.writeResponseBodyToDisk(response.body(), saveFile);
                KLog.d(TAG, "download avatar save file result:" + saveResult + ", and path:" + filePath);
            }
        } catch (Exception e) {
            KLog.e(TAG, "download avatar save file error:" + e.getMessage());
        }
        if (saveResult) {   //将数据保存到数据库中
            user.setAvatar(filePath);
            KLog.d(TAG, "download avatar success and will save avatar info to local db:" + filePath);
            saveResult = UserManager.getInstance().update(user);
        }
        return saveResult;
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
            user = ((NoteApplication) context.getApplicationContext()).getCurrentUser();
            if (user != null && success && user.getId() > 0) {  //该id已在本地不存在
                NoteUtil.saveAccountId(context, user.getId());
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
        String srcAvatar = src.getAvatar();
        if (srcAvatar != null) {
            target.setAvatar(src.getAvatar());
            target.setAvatarHash(src.getAvatarHash());
        }
        target.setCreateTime(src.getCreateTime());
        target.setGender(src.getGender());
        target.setMobile(src.getMobile());
        String pwd = src.getPassword();
        if (pwd != null) {
            target.setPassword(src.getPassword());
        }
        target.setSid(src.getSid());
        Integer state = src.getState();
        if (state == null) {
            state = State.NORMAL;
        }
        target.setState(state);
        target.setUsername(src.getUsername());
        target.setEmail(src.getEmail());
        String nickname = src.getNickname();
        if (nickname != null) {
            target.setNickname(src.getNickname());
        }
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

    /**
     * 本地更新用户，同步的方法，非异步
     * @param user
     */
    private static void updateLocalUser(final User user) {
        boolean success = UserManager.getInstance().update(user);
        KLog.d(TAG, "update local user result:" + success + ", user:" + user);
    }

    /**
     * 本地更新用户，同步的方法，非异步
     * @param user
     */
    private static void updateLocalUserAsync(final User user) {
        KLog.d(TAG, "update local user async:" + user);
        doInbackground(new NoteTask(user) {
            @Override
            public void run() {
                updateLocalUser((User) params[0]);
            }
        });
    }
}
