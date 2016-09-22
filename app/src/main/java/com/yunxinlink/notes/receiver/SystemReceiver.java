package com.yunxinlink.notes.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.socks.library.KLog;
import com.yunxinlink.notes.api.impl.UserApiImpl;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.listener.SimpleOnLoadCompletedListener;
import com.yunxinlink.notes.model.AccountType;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformDb;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;

/**
 * 主题切换的广播
 */
public class SystemReceiver extends BroadcastReceiver {
    private static final String TAG = "SystemReceiver";

    /**
     * 主题变化的广播
     */
    public static final String ACTION_THEME_CHANGE = "com.yunxinlink.notes.receiver.ACTION_THEME_CHANGE";

    /**
     * 后台登录，校验权限
     */
    public static final String ACTION_AUTHORITY_VERIFY = "com.yunxinlink.notes.ACTION_AUTHORITY_VERIFY";

    private Context mContext;

    public SystemReceiver(Context context) {
        this.mContext = context;
    }

    public SystemReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_THEME_CHANGE:   //主题变化的广播
                    changeTheme(mContext);
                    break;
                case ACTION_AUTHORITY_VERIFY:   //app启动后的广播
                    doAuthorityVerify(context);
                    break;
            }
        }
    }

    /**
     * 切换主题
     * @param context
     */
    private void changeTheme(Context context) {
        KLog.d(TAG, "on receive a theme change receiver");
        if (context != null && context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.recreate();
        }
    }

    /**
     * 后台进行权限校验
     * @param context
     */
    private void doAuthorityVerify(final Context context) {
        SystemUtil.getThreadPool().execute(new NoteTask() {
            @Override
            public void run() {
                KLog.d(TAG, "doAuthorityVerify invoke");
                final UserDto userDto = buildLoginParams(context);
                if (userDto == null) {
                    KLog.d(TAG, "doAuthorityVerify userDto is null");
                    return;
                }
                UserApiImpl.login(userDto, new SimpleOnLoadCompletedListener<ActionResult<UserDto>>() {
                    @Override
                    public void onLoadSuccess(ActionResult<UserDto> result) {
                        super.onLoadSuccess(result);
                        //保存用户信息
                        if (result != null) {
                            int code = result.getResultCode();
                            UserDto resultUserDto = result.getData();
                            switch (code) {
                                case ActionResult.RESULT_FAILED:    //失败
                                    KLog.d(TAG, "doAuthorityVerify result code failed:" + code + ", reason:" + result);
                                    break;
                                case ActionResult.RESULT_PARAM_ERROR:    //参数错误
                                    KLog.d(TAG, "doAuthorityVerify result code param error:" + code + ", reason:" + result);
                                    break;
                                case ActionResult.RESULT_STATE_DISABLE:    //用户不可用了
                                    updateLocalUser(context, userDto, resultUserDto);
                                    KLog.d(TAG, "doAuthorityVerify result code user state is disable:" + code + ", reason:" + result);
                                    break;
                                case ActionResult.RESULT_SUCCESS:   //成功
                                    updateLocalUser(context, userDto, resultUserDto);
                                    KLog.d(TAG, "doAuthorityVerify result code success:" + code + ", reason:" + result);
                                    break;
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * 从服务器合并用户信息到本地
     * @param target
     * @param src
     */
    private void mergeUserInfo(User target, User src) {
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
     * 更新本地的用户信息
     * @param original 本地的用户信息
     * @param result 服务器返回的结果
     */
    private void updateLocalUser(Context context, UserDto original, UserDto result) {
        if (result != null && result.getUser() != null) {
            KLog.d(TAG, "doAuthorityVerify state disable local user will update");
            User user = original.getUser();
            mergeUserInfo(user, result.getUser());
            
            //保存账号类型
            int accountType = original.getType();
            NoteUtil.saveAccountType(context, accountType);
            
            //添加或更新本地账号信息
            boolean success = UserManager.getInstance().insertOrUpdate(user);
            int localUserId = NoteUtil.getAccountId(context);
            if (success && localUserId <= 0 || localUserId != user.getId()) {  //该id已在本地不存在
                NoteUtil.saveAccountId(context, localUserId);
            }
            KLog.d(TAG, "updateLocalUser result:" + success);
        }
    }

    /**
     * 获取用户的登录的参数
     * @param context
     * @return
     */
    private UserDto buildLoginParams(Context context) {
        int accountType = NoteUtil.getAccountType(context);
        User user = null;
        UserDto userDto = new UserDto();
        userDto.setType(accountType);
        if (accountType >= 0) { //用户有登录过，则校验信息
            //校验以及获取用户信息
            Platform platform = null;
            switch (accountType) {
                case AccountType.TYPE_QQ:   //QQ登录
                    platform = ShareSDK.getPlatform(context, QQ.NAME);
                    break;
                case AccountType.TYPE_WECHAT:   //微信登录
                    platform = ShareSDK.getPlatform(context, Wechat.NAME);
                    break;
                case AccountType.TYPE_WEIBO:   //微博登录
                    platform = ShareSDK.getPlatform(context, SinaWeibo.NAME);
                    break;
            }
            if (platform != null) {
                PlatformDb platDB = platform.getDb();//获取数平台数据DB
                //通过DB获取各种数据
                String token = platDB.getToken();
                long expiresTime = platDB.getExpiresTime();
                String userId = platDB.getUserId();

                userDto.setExpiresTime(expiresTime);
                userDto.setOpenUserId(userId);
                userDto.setToken(token);
                
                user = new User();
                user.setOpenUserId(userId);

                userDto.setUser(user);
            } else {
                return null;
            }
        } else {    //则用默认的账号登录，如果账号存在的话
            KLog.d(TAG, "doAuthorityVerify user not use open api login will use local account login");
            int userId = NoteUtil.getAccountId(context);
            user = UserManager.getInstance().getAccountInfo(userId);
            if (user == null) { //本地没有账号
                KLog.d(TAG, "doAuthorityVerify user local account not exists ");
                return null;
            }
        }
        return userDto;
    }
}
