package com.yunxinlink.notes.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.api.impl.UserApi;
import com.yunxinlink.notes.listener.SimpleOnLoadCompletedListener;
import com.yunxinlink.notes.listener.SimpleTextWatcher;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import static com.yunxinlink.notes.util.NoteUtil.checkEmail;

/**
 * 修改密码的界面
 */
public class PasswordModifyActivity extends BaseActivity implements View.OnClickListener {
    private static final int MSG_SEND_MAIL_SUCCESS = 11;
    private static final int MSG_SEND_MAIL_FAILED = 12;
    
    private EditText mEtOldPwd;
    private EditText mEtNewPwd;
    private EditText mEtConfirmPwd;
    
    private Button mBtnModify;
    
    private ProgressDialog mProgressDialog;
    
    private Handler mHandler = new MyHandler(this);

    @Override
    protected int getContentView() {
        return R.layout.activity_password_modify;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {
        mEtOldPwd = (EditText) findViewById(R.id.et_old_password);
        mEtNewPwd = (EditText) findViewById(R.id.et_password);
        mEtConfirmPwd = (EditText) findViewById(R.id.et_password_confirm);
        mBtnModify = (Button) findViewById(R.id.btn_modify);
        TextView tvPwdTip = (TextView) findViewById(R.id.tv_password_tip);

        mBtnModify.setOnClickListener(this);
        tvPwdTip.setOnClickListener(this);

        mEtOldPwd.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SystemUtil.setViewEnable(mBtnModify, canSubmit());
            }
        });

        mEtNewPwd.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SystemUtil.setViewEnable(mBtnModify, canSubmit());
            }
        });

        mEtConfirmPwd.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SystemUtil.setViewEnable(mBtnModify, canSubmit());
            }
        });
    }

    /**
     * 找回密码
     */
    private void resetPassword() {
        
        User user = getCurrentUser();
        if (user == null) {
            SystemUtil.makeShortToast(R.string.account_no_account);
            finish();
            return;
        }

        final EditText editText = new EditText(mContext);
        editText.setHint(R.string.authority_account_tip);

        String email = user.getEmail();
        if (!SystemUtil.isEmail(email)) {
            email = null;
        }
        editText.setText(email);

        int space = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);

        AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
        builder.setTitle(R.string.authority_reset_password)
                .setView(editText, space, space, space, space)
                .setPositiveButton(R.string.authority_reset_password, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CharSequence textEmail = editText.getText();
                        boolean isEmail = checkEmail(textEmail);
                        if (isEmail) {
                            mProgressDialog = showLoadingDialog(R.string.authority_sending);
                            UserApi.resetPasswordAsync(textEmail.toString(), new SimpleOnLoadCompletedListener<ActionResult<Void>>() {
                                @Override
                                public void onLoadSuccess(ActionResult<Void> result) {
                                    dismissDialog(mProgressDialog);
                                    super.onLoadSuccess(result);
                                    mHandler.sendEmptyMessage(MSG_SEND_MAIL_SUCCESS);
                                }

                                @Override
                                public void onLoadFailed(int errorCode, String reason) {
                                    dismissDialog(mProgressDialog);
                                    super.onLoadFailed(errorCode, reason);
                                    Message msg = mHandler.obtainMessage();
                                    msg.what = MSG_SEND_MAIL_FAILED;
                                    msg.arg1 = errorCode;
                                    mHandler.sendMessage(msg);
                                }
                            });
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 是否可以提交
     * @return
     */
    private boolean canSubmit() {
        CharSequence oldPassword = mEtOldPwd.getText();
        CharSequence newPassword = mEtNewPwd.getText();
        CharSequence confirmPassword = mEtConfirmPwd.getText();
        boolean hasOldPassword = !TextUtils.isEmpty(oldPassword);
        boolean hasNewPassword = !TextUtils.isEmpty(newPassword);
        boolean hasConfirmPassword = !TextUtils.isEmpty(confirmPassword);
        if (hasOldPassword && hasNewPassword && hasConfirmPassword) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_modify:   //修改
                String newPassword = mEtNewPwd.getText().toString();
                String confirmPassword = mEtConfirmPwd.getText().toString();
                if (!newPassword.equals(confirmPassword)) { //两次输入密码不相等
                    SystemUtil.makeShortToast(R.string.tip_confirm_pwd_error);
                    return;
                }
                String oldPassword = mEtOldPwd.getText().toString();
                mProgressDialog = showLoadingDialog(R.string.account_modify_tip_ing);
                new ModifyTask().execute(oldPassword, newPassword, confirmPassword);
                break;
            case R.id.tv_password_tip:  //忘记密码
                resetPassword();
                break;
        }
    }

    /**
     * 发送邮件的结果
     * @param resultCode 结果码
     */
    private void mailResult(int resultCode) {
        int tipRes = 0;
        switch (resultCode) {
            case ActionResult.RESULT_SUCCESS:   //成功
                tipRes = R.string.authority_send_mail_success;
                break;
            case ActionResult.RESULT_STATE_DISABLE: //用户被禁用
                tipRes = R.string.authority_login_state_disable;
                break;
            case ActionResult.RESULT_DATA_NOT_EXISTS:   //用户存在
                tipRes = R.string.authority_account_not_exists;
                break;
            default:
                tipRes = R.string.authority_send_mail_failed;
                break;
        }
        SystemUtil.makeShortToast(tipRes);
    }

    /**
     * 修改密码的后台任务
     */
    private class ModifyTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            if (params == null || params.length == 0) {
                return 0;
            }
            return UserApi.modifyPassword(mContext, params);
        }

        @Override
        protected void onPostExecute(Integer value) {
            dismissDialog(mProgressDialog);
            int resultCode = value == null ? 0 : value;
            int resId = 0;
            boolean success = false;
            switch (resultCode) {
                case ActionResult.RESULT_SUCCESS:   //成功
                    resId = R.string.account_modify_success;
                    success = true;
                    break;
                case ActionResult.RESULT_PARAM_ERROR:   //参数错误
                    resId = R.string.tip_post_param_error;
                    break;
                case ActionResult.RESULT_NOT_EQUALS:    //两次输入的密码不等
                    resId = R.string.account_modify_pwd_not_equals;
                    break;
                default:
                    resId = R.string.account_modify_failed;
                    break;
                
            }
            SystemUtil.makeShortToast(resId);
            if (success) {
                finish();
            }
        }
    }
    
    private static class MyHandler extends BaseHandler<PasswordModifyActivity> {
        public MyHandler(PasswordModifyActivity target) {
            super(target);
        }

        @Override
        public void handleMessage(Message msg) {
            PasswordModifyActivity target = getTarget();
            if (target != null) {
                switch (msg.what) {
                    case MSG_SEND_MAIL_SUCCESS: //邮件发送成功
                        target.mailResult(ActionResult.RESULT_SUCCESS);
                        break;
                    case MSG_SEND_MAIL_FAILED:  //邮件发送成功
                        target.mailResult(msg.arg1);
                }
            }
        }
    }
}
