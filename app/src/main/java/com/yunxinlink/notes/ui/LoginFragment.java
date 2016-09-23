package com.yunxinlink.notes.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.yunxinlink.notes.util.SystemUtil;

import retrofit2.Call;

/**
 * 登录的界面
 */
public class LoginFragment extends BaseFragment implements View.OnClickListener {

    private OnLoginFragmentInteractionListener mListener;
    
    private EditText mEtAccount;
    private EditText mEtPassword;
    private Button mBtnLogin;
    
    private ProgressDialog mProgressDialog;
    
    //网络请求工具
    private Call<?> mCall;

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
                    + " must implement OnFragmentInteractionListener");
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
    public void onClick(View v) {
        UserDto userDto = null;
        User user = null;
        switch (v.getId()) {
            case R.id.qq_layout:    //QQ登录
                userDto = new UserDto();
                user = new User();
                userDto.setUser(user);
                userDto.setType(AccountType.TYPE_QQ);
                break;
            case R.id.weibo_layout:    //微博登录
                userDto = new UserDto();
                user = new User();
                userDto.setUser(user);
                userDto.setType(AccountType.TYPE_WEIBO);
                break;
        }
        if (userDto != null) {
            if (SystemUtil.isNetworkAvailable(getContext())) {  //网络可用
                doLogin(userDto);
            } else {    //网络不可用
                SystemUtil.makeShortToast(R.string.tip_network_not_available);
            }
        }
    }

    /**
     * 显示对话框
     * @param msg 提示的消息
     */
    private void showLoadingDialog(String msg) {
        mProgressDialog = ProgressDialog.show(getContext(), null, msg);
    }

    /**
     * 取消显示加载对话框
     */
    private void dismissLoadingDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * 取消登录
     */
    private void cancelLogin() {
        dismissLoadingDialog();
        if (mCall != null && !mCall.isCanceled()) {
            KLog.d(TAG, "cancelLogin invoke cancel call");
            mCall.cancel();
        }
    }

    /**
     * 处理登录请求
     * @param userDto 参数
     */
    private void doLogin(UserDto userDto) {
        showLoadingDialog(getString(R.string.authority_login_ing));
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
    }
}
