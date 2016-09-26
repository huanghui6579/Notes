package com.yunxinlink.notes.ui;

import android.content.Context;
import android.os.Bundle;
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

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.api.impl.UserApiImpl;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.listener.SimpleOnLoadCompletedListener;
import com.yunxinlink.notes.listener.SimpleTextWatcher;
import com.yunxinlink.notes.model.AccountType;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import retrofit2.Call;

/**
 * 注册界面
 * @author tiger
 * @update 2016/9/25 18:34
 * @version 1.0.0
 */
public class RegisterFragment extends BaseFragment implements View.OnClickListener {
    private OnRegisterFragmentInteractionListener mListener;
    
    //账号
    private EditText mEtAccount;
    //密码
    private EditText mEtPassword;
    //确认面
    private EditText mEtPasswordConfirm;
    
    //注册
    private Button mBtnRegister;
    
    //网络请求的句柄
    private Call<?> mCall;

    public RegisterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RegisterFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RegisterFragment newInstance() {
        RegisterFragment fragment = new RegisterFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //初始化控件
        initView(view);
    }

    /**
     * 初始化界面
     * @param view
     */
    private void initView(View view) {
        mEtAccount = (EditText) view.findViewById(R.id.et_account);
        mEtPassword = (EditText) view.findViewById(R.id.et_password);
        mEtPasswordConfirm = (EditText) view.findViewById(R.id.et_password_confirm);
        mBtnRegister = (Button) view.findViewById(R.id.btn_register);

        mBtnRegister.setOnClickListener(this);

        mEtAccount.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SystemUtil.setViewEnable(mBtnRegister, canRegister());
            }
        });

        mEtPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SystemUtil.setViewEnable(mBtnRegister, canRegister());
            }
        });

        mEtPasswordConfirm.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                SystemUtil.setViewEnable(mBtnRegister, canRegister());
            }
        });
    }

    /**
     * 是否可以注册
     * @return
     */
    private boolean canRegister() {
        boolean hasAccount = !TextUtils.isEmpty(mEtAccount.getText());
        if (hasAccount) {
            boolean isEmail = SystemUtil.isEmail(mEtAccount.getText());
            boolean isPhone = SystemUtil.isPhoneNumber(mEtAccount.getText().toString());
            hasAccount = isEmail || isPhone;
        }
        CharSequence password = mEtPassword.getText();
        CharSequence confirmPassword = mEtPasswordConfirm.getText();
        boolean hasPassword = !TextUtils.isEmpty(password);
        boolean hasConfirmPassword = !TextUtils.isEmpty(confirmPassword);
        if (hasAccount && hasPassword && hasConfirmPassword && 
                password.toString().equals(confirmPassword.toString())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnRegisterFragmentInteractionListener) {
            mListener = (OnRegisterFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnRegisterFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.authority_login, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_login: //登录
                if (mListener != null) {
                    mListener.actionLogin();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_register: //注册
                break;
        }
    }

    /**
     * 消失对话框
     */
    private void dismissLoadingDialog() {
        if (mListener != null) {
            mListener.dismissDialog();
        }
    }

    /**
     * 显示加载的对话框
     */
    private void showLoadingDialog() {
        if (mListener != null) {
            mListener.showDialog(getString(R.string.authority_register_ing));
        }
    }
    

    /**
     * 处理注册
     * @param context
     * @param user
     */
    private void doRegister(Context context, final User user) {
        final UserDto userDto = NoteUtil.buildLoginParams(getContext(), user, AccountType.TYPE_LOCAL);
        showLoadingDialog();
        String confirmPassword = mEtPasswordConfirm.getText().toString();
        mCall = UserApiImpl.register(context, userDto, confirmPassword, new SimpleOnLoadCompletedListener<ActionResult<UserDto>>() {
            @Override
            public void onLoadSuccess(ActionResult<UserDto> result) {
                super.onLoadSuccess(result);
                dismissLoadingDialog();
                if (mListener != null) {
                    User user = null;
                    if (result != null && result.getData() != null) {
                        user = result.getData().getUser();
                    }
                    KLog.d(TAG, "do regist success user:" + user);
                    mListener.registerSuccess(user);
                }
            }

            @Override
            public void onLoadFailed(int errorCode, String reason) {
                super.onLoadFailed(errorCode, reason);
                dismissLoadingDialog();
                int tipRes = 0;
                switch (errorCode) {
                    case ActionResult.RESULT_NOT_EQUALS:   //两次输入的密码不相等
                        tipRes = R.string.authority_register_failed;
                        break;
                    case ActionResult.RESULT_DATA_REPEAT:   //该账号已存在
                        tipRes = R.string.authority_register_account_repeat;
                        break;
                    default:
                        tipRes = R.string.authority_register_failed;
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
    public interface OnRegisterFragmentInteractionListener {
        /**
         * 加载登录界面
         */
        void actionLogin();

        void dismissDialog();

        void showDialog(String tip);

        /**
         * 注册成功
         * @param user
         */
        void registerSuccess(User user);
    }
}
