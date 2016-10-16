package com.yunxinlink.notes.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * 登录或者注册的界面
 * @author tiger
 * @update 2016/9/25 18:42
 * @version 1.0.0
 */
public class AuthorityActivity extends BaseActivity implements LoginFragment.OnLoginFragmentInteractionListener, RegisterFragment.OnRegisterFragmentInteractionListener {

    private static final int MSG_SHOW_LOADING_DIALOG = 10;
    private static final int MSG_DISMISS_LOADING_DIALOG = 11;

    public static final String ARG_RELOGIN = "relogin";

    private ProgressDialog mProgressDialog;

    /**
     * 是否是注销账户后再进入的登录界面
     */
    private boolean mIsRelogin;

    private Handler mHandler = new MyHandler(this);

    @Override
    protected boolean hasLockedController() {
        return false;
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_authority;
    }

    @Override
    protected void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            mIsRelogin = intent.getBooleanExtra(ARG_RELOGIN, false);
        }
    }

    @Override
    protected void initView() {
        initAction(true);
    }

    /**
     * 初始化对应的界面
     * @param isLogin 是否是登录界面
     */
    private void initAction(boolean isLogin) {
        Fragment fragment = null;
        if (isLogin) {
            setTitle(R.string.authority_menu_login_text);
            fragment = LoginFragment.newInstance();
        } else {
            setTitle(R.string.authority_menu_register_text);
            fragment = RegisterFragment.newInstance();
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_container, fragment, fragment.getClass().getSimpleName());
        fragmentTransaction.commit();
    }

    /**
     * 跳转到主界面
     */
    private void toMainActivity() {
        if (mIsRelogin) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onAuthoritySuccess() {
        SystemUtil.makeShortToast(R.string.authority_login_success);
        toMainActivity();
    }

    @Override
    public void dismissDialog() {
        dismissLoadingDialog();
    }

    @Override
    public void showDialog(String tip) {
        showLoadingDialog(tip);
    }

    @Override
    public void registerSuccess(User user) {
        SystemUtil.makeShortToast(R.string.authority_register_success);
        toMainActivity();
    }

    @Override
    public void actionRegister() {
        initAction(false);
    }

    /**
     * 显示对话框，主要是发送显示的消息
     * @param tip
     */
    protected ProgressDialog showLoadingDialog(String tip) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_SHOW_LOADING_DIALOG;
        msg.obj = tip;
        mHandler.sendMessage(msg);
        return null;
    }


    /**
     * 显示对话框，主要是发送显示的消息
     */
    public void dismissLoadingDialog() {
        mHandler.sendEmptyMessage(MSG_DISMISS_LOADING_DIALOG);
    }

    /**
     * 显示对话框
     * @param msg 提示的消息
     */
    private void doLoadingDialog(String msg) {
        mProgressDialog = ProgressDialog.show(mContext, null, msg, true, true);
    }

    @Override
    public void actionLogin() {
        initAction(true);
    }

    /**
     * handler
     */
    private static class MyHandler extends BaseHandler<AuthorityActivity> {
        public MyHandler(AuthorityActivity target) {
            super(target);
        }

        @Override
        public void handleMessage(Message msg) {
            AuthorityActivity target = getTarget();
            if (target != null) {
                switch (msg.what) {
                    case MSG_SHOW_LOADING_DIALOG:  //显示加载对话框
                        String tip = (String) msg.obj;
                        if (tip != null) {
                            target.doLoadingDialog(tip);
                        }
                        break;
                    case MSG_DISMISS_LOADING_DIALOG:    //消失对话框
                        target.dismissDialog(target.mProgressDialog);
                        break;
                }

            }
        }
    }
}
