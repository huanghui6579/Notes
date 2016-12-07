package com.yunxinlink.notes.account;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.api.impl.UserApi;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.util.DigestUtil;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.widget.ClearableEditText;

/**
 * 账户绑定的辅助类
 * @author huanghui-iri
 * @update 2016/11/23 15:52
 * @version: 0.0.1
 */
public class AccountBinder {
    
    private static final String TAG = "AccountBinder";

    /**
     * 获取当前的用户
     * @param context
     * @return
     */
    private User getUser(Context context) {
        NoteApplication app = (NoteApplication) context.getApplicationContext();
        return app.getCurrentUser();
    }
    
    /**
     * 绑定邮箱
     * @param context 
     * @param autoCreate 当本地没有账号时，是否自动注册一个
     * @param listener 确认按钮点击的监听器                  
     */
    public void bindAccount(final Context context, boolean autoCreate, final OnBindPostedListener listener) {
        User user = getUser(context);
        if (user == null && !autoCreate) {
            SystemUtil.makeShortToast(R.string.account_no_account);
            return;
        }

        String email = null;
        if (user != null) {
            email = user.getEmail();
        }

        LinearLayout linearLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final ClearableEditText editText = new ClearableEditText(context);
        editText.setHint(R.string.authority_account_tip);
        editText.setText(email);

        //检查该用户有没有设置登录密码
        int pwdHint = R.string.account_password;
        if (user != null && !TextUtils.isEmpty(user.getPassword())) {    //有设置登录密码
            pwdHint = R.string.account_password_input;
        }

        final AppCompatEditText etPassword = new AppCompatEditText(context);
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setHint(pwdHint);
        LinearLayout.LayoutParams pwdParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pwdParams.topMargin = context.getResources().getDimensionPixelOffset(R.dimen.grid_item_padding);

        linearLayout.addView(editText);
        linearLayout.addView(etPassword, pwdParams);

        int space = context.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);

        AlertDialog.Builder builder = NoteUtil.buildDialog(context);
        builder.setTitle(R.string.account_email)
                .setView(linearLayout, space, space, space, space)
                .setPositiveButton(R.string.account_email_btn_bind, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText() == null ? null : editText.getText().toString();
                        String password = etPassword.getText() == null ? null : etPassword.getText().toString();
                        View focusView = editText.hasFocus() ? editText : etPassword;
                        SystemUtil.hideSoftInput(context, focusView);
                        if (listener != null) {
                            listener.onBindPosted(text, password);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 提交绑定的数据
     * @param context
     * @param param 提交的参数
     * @param listener
     */
    public void postData(final Context context, final User param, final OnBindPostedListener listener) {
        SystemUtil.doInbackground(new NoteTask(param) {
            @Override
            public void run() {
                String pPassword = ((User) params[0]).getPassword();
                String pEmail = ((User) params[0]).getEmail();
                //密码MD5 加密
                String encodePwd = DigestUtil.md5Hex(pPassword);
                User user = getUser(context);
                int resultCode = 0;
                User param = null;
                boolean isLogin = false;
                if (user == null) {   //创建用户，即注册用户
                    param = new User();
                    
                    param.setEmail(pEmail);
                    param.setPassword(encodePwd);

                    UserDto userDto = new UserDto();
                    userDto.setUser(param);
                    resultCode = UserApi.bind(context, userDto);
                    
                    /*if (autoCreate) {   //是否会自动创建用户
                        
                    } else {    //只校验本地的
                        //校验本地的
                        User resultUser = UserManager.getInstance().getAccountInfo(param);
                        if (resultUser != null) {   //本地有该用户，则校验密码
                            success = pPassword.equals(resultUser.getPassword());
                        }
                        if (!success) {
                            resultCode = ActionResult.RESULT_NOT_EQUALS;
                        }
                    }*/
                    KLog.d(TAG, "bind user account match local user result code:" + resultCode);
                } else {
                    param = (User) user.clone();
                    if (param == null) {
                        KLog.d(TAG, "bind user account clone user is null");
                        if (listener != null) {
                            listener.onBindEnd(false, 0);
                        }
                        return;
                    }
                    isLogin = true;
                    //是否没有修改，即输入的邮箱和原始的一样
                    boolean notModify = pEmail.equals(param.getEmail());
                    param.setEmail(pEmail);
                    param.setPassword(encodePwd);
                    boolean pwdOk = true;
                    if (!TextUtils.isEmpty(user.getPassword())) {    //用户之前有登录密码，则校验本地的密码
                        pwdOk = NoteUtil.checkPassword(user, encodePwd);
                    }
                    if (!pwdOk) {
                        resultCode = ActionResult.RESULT_NOT_EQUALS;
                    } else {
                        if (notModify) {
                            resultCode = ActionResult.RESULT_SUCCESS;
                        } else {
                            resultCode = UserApi.syncUpUser(param);
                        }
                    }
                    boolean success = false;
                    if (notModify) {
                        success = true;
                    } else {
                        if (resultCode == ActionResult.RESULT_SUCCESS) {
                            //保存数据到本地
                            success = UserManager.getInstance().update(param);
                        }
                    }
                    KLog.d(TAG, "bind user account sync user info result:" + success);
                }
                
                boolean success = false;
                int resId = 0;
                switch (resultCode) {
                    case ActionResult.RESULT_SUCCESS:    //成功
                        resId = R.string.account_email_bind_success;
                        success = true;
                        break;
                    case ActionResult.RESULT_DATA_NOT_EXISTS:   //用户不存在
                        resId = R.string.authority_account_not_exists;
                        break;
                    case ActionResult.RESULT_STATE_DISABLE:   //用户被禁用
                        resId = R.string.authority_login_state_disable;
                        break;
                    case ActionResult.RESULT_NOT_EQUALS:    //密码不正确，主要用户已经有密码的用户
                        resId = R.string.account_password_not_right;
                        break;
                    case ActionResult.RESULT_VALIDATE_FAILED:   //用户存在，但密码错误
                        if (isLogin) {  //当前用户已登录，则提示密码错误
                            resId = R.string.account_password_not_right;
                        } else {    //当前用户没有登录，则提示该用户已存在
                            resId = R.string.account_exist;
                        }
                        break;
                    default:
                        resId = R.string.account_email_bind_failed;
                        break;
                }
                if (listener != null) {
                    listener.onBindEnd(success, resId);
                }
                
            }
        });
    }

    /**
     * 点击对话框确认按钮后的回调
     */
    public interface OnBindPostedListener {
        /**
         * 点击确定按钮后的回调方法
         * @param account 邮箱或者手机号
         * @param password 密码
         */
        void onBindPosted(String account, String password);

        /**
         * 提交数据结束
         * @param success 是否成功
         * @param resId 提示的字符串资源
         */
        void onBindEnd(boolean success, int resId);
    }
}
