package com.yunxinlink.notes.ui;

import android.support.v4.app.FragmentTransaction;

import com.yunxinlink.notes.R;

/**
 * 登录界面
 */
public class AuthorityActivity extends BaseActivity implements LoginFragment.OnLoginFragmentInteractionListener {

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

    }

    @Override
    protected void initView() {
        LoginFragment fragment = LoginFragment.newInstance();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_container, fragment, fragment.getClass().getSimpleName());
        fragmentTransaction.commit();
    }

    @Override
    public void onAuthoritySuccess() {
        finish();
    }
}
