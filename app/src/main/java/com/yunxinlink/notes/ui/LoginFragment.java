package com.yunxinlink.notes.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.api.impl.UserApiImpl;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.listener.SimpleOnLoadCompletedListener;
import com.yunxinlink.notes.listener.SimpleTextWatcher;
import com.yunxinlink.notes.model.AccountType;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.share.SimplePlatformActionListener;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import retrofit2.Call;

/**
 * 登录的界面
 */
public class LoginFragment extends BaseFragment implements View.OnClickListener {

    private static final int MSG_LOGIN = 1;

    private OnLoginFragmentInteractionListener mListener;
    
    private EditText mEtAccount;
    private EditText mEtPassword;
    private Button mBtnLogin;
    
    //网络请求工具
    private Call<?> mCall;

    private Handler mHandler = new MyHandler(this);

    public LoginFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LoginFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
    }

    /**
     * 初始化控件
     * @param view
     */
    private void initView(View view) {
        LinearLayout btnQQLogin = (LinearLayout) view.findViewById(R.id.qq_layout);
        LinearLayout btnWeiboLogin = (LinearLayout) view.findViewById(R.id.weibo_layout);

        mEtAccount = (EditText) view.findViewById(R.id.et_account);
        mEtPassword = (EditText) view.findViewById(R.id.et_password);
        
        mBtnLogin = (Button) view.findViewById(R.id.btn_login);
//
        btnQQLogin.setOnClickListener(this);
        btnWeiboLogin.setOnClickListener(this);

        mBtnLogin.setOnClickListener(this);
        mEtAccount.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SystemUtil.setViewEnable(mBtnLogin, canLogin());
            }
        });

        mEtPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SystemUtil.setViewEnable(mBtnLogin, canLogin());
            }
        });

    }

    /**
     * 是否可以登录
     * @return
     */
    private boolean canLogin() {
        boolean hasAccount = !TextUtils.isEmpty(mEtAccount.getText());
        if (hasAccount) {
            boolean isEmail = SystemUtil.isEmail(mEtAccount.getText());
            boolean isPhone = SystemUtil.isPhoneNumber(mEtAccount.getText().toString());
            hasAccount = isEmail || isPhone;
        }
        boolean hasPassword = !TextUtils.isEmpty(mEtPassword.getText());
        return hasAccount && hasPassword;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnLoginFragmentInteractionListener) {
            mListener = (OnLoginFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnRegisterFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cancelLogin();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        
        inflater.inflate(R.menu.authority_register, menu);
        
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_register:  //注册
                if (mListener != null) {
                    mListener.actionRegister();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        User user = null;
        int loginType = -1;
        switch (v.getId()) {
            case R.id.qq_layout:    //QQ登录
                loginType = AccountType.TYPE_QQ;
                break;
            case R.id.weibo_layout:    //微博登录
                loginType = AccountType.TYPE_WEIBO;
                break;
            case R.id.btn_login:    //手机或者邮箱登录
                loginType = AccountType.TYPE_LOCAL;
                String account = mEtAccount.getText().toString();
                boolean isEmail = SystemUtil.isEmail(account);
                user = new User();
                if (isEmail) {  //邮箱类型
                    user.setEmail(account);
                } else if (SystemUtil.isPhoneNumber(account)) { //手机
                    user.setMobile(account);
                } else {    //用户名
                    user.setUsername(account);
                }
                user.setPassword(mEtPassword.getText().toString());
                break;
        }
        if (loginType != -1) {
            if (SystemUtil.isNetworkAvailable(getContext())) {  //网络可用
                handleLogin(user, loginType);
            } else {    //网络不可用
                SystemUtil.makeShortToast(R.string.tip_network_not_available);
            }
        }
    }
    
    /**
     * 取消登录
     */
    private void cancelLogin() {
        if (mListener != null) {
            mListener.dismissDialog();
        }
        if (mCall != null && !mCall.isCanceled()) {
            KLog.d(TAG, "cancelLogin invoke cancel call");
            mCall.cancel();
        }
    }

    /**
     * 提交登录请求
     * @param context
     * @param userDto
     */
    private void postLogin(Context context, UserDto userDto) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_LOGIN;
        msg.obj = userDto;
        mHandler.sendMessage(msg);
    }

    /**
     * 处理登录请求
     * @param user 参数
     * @param loginType 登录的类型
     */
    private void handleLogin(final User user, final Integer loginType) {
        final UserDto userDto = NoteUtil.buildLoginParams(getContext(), user, loginType);
        if (userDto == null) {  //参数为空
            if (loginType > 0) {    //第三方，则需要进行首次登录
                KLog.d(TAG, "do login user dto params is null loginType is :" + loginType + ", will to authority ui");
                Platform platform = NoteUtil.getPlatform(getContext(), loginType);
                platform.SSOSetting(false); //优先使用客户端
                platform.setPlatformActionListener(new SimplePlatformActionListener() {
                    @Override
                    public void onComplete(Platform platform, int action, HashMap<String, Object> hashMap) {
                        super.onComplete(platform, action, hashMap);
                        UserDto param = NoteUtil.buildLoginParams(getContext(), user, loginType);
                        postLogin(getContext(), param);
                    }
                });
                platform.showUser(null);
            } else {
                KLog.d(TAG, "do login user dto params is null");
                SystemUtil.makeShortToast(R.string.authority_login_error);
            }
            return;
        }
        doLogin(getContext(), userDto);
    }

    /**
     * 执行登录操作
     * @param context
     * @param userDto
     */
    private void doLogin(Context context, UserDto userDto) {
        if (mListener != null) {
            mListener.showDialog(getString(R.string.authority_login_ing));
        }
        mCall = UserApiImpl.loginAsync(getContext(), userDto, new SimpleOnLoadCompletedListener<ActionResult<UserDto>>() {
            @Override
            public void onLoadSuccess(ActionResult<UserDto> result) {
                super.onLoadSuccess(result);
                dismissLoadingDialog();
                if (mListener != null) {
                    KLog.d(TAG, "do login success");
                    mListener.onAuthoritySuccess();
                } else {
                    KLog.d(TAG, "do login success but mListener is null");
                }
            }

            @Override
            public void onLoadFailed(int errorCode, String reason) {
                super.onLoadFailed(errorCode, reason);
                dismissLoadingDialog();
                int tipRes = 0;
                switch (errorCode) {
                    case ActionResult.RESULT_STATE_DISABLE: //用户不可用
                        tipRes = R.string.authority_login_state_disable;
                        break;
                    case ActionResult.RESULT_FAILED:    //用户名或密码不正确
                        tipRes = R.string.authority_login_failed;
                        break;
                    default:
                        tipRes = R.string.authority_login_error;
                        break;
                }
                SystemUtil.makeShortToast(tipRes);
            }
        });
    }

    private void dismissLoadingDialog() {
        if (mListener != null) {
            mListener.dismissDialog();
        }
    }

    private static class MyHandler extends BaseHandler<LoginFragment> {

        public MyHandler(LoginFragment target) {
            super(target);
        }

        @Override
        public void handleMessage(Message msg) {
            LoginFragment target = getTarget();
            if (target != null) {
                switch (msg.what) {
                    case MSG_LOGIN:
                        UserDto userDto = (UserDto) msg.obj;
                        target.doLogin(target.getContext(), userDto);
                        break;
                }
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnLoginFragmentInteractionListener {
        /**
         * 登录或注册成功了
         */
        void onAuthoritySuccess();

        void dismissDialog();

        void showDialog(String tip);

        /**
         * 加载注册界面
         */
        void actionRegister();
    }
}
