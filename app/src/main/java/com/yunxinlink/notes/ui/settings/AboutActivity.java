package com.yunxinlink.notes.ui.settings;

import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.view.View;
import android.widget.TextView;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.ui.BaseActivity;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * 关于界面
 * @author tiger
 * @update 2016/11/19 17:23
 * @version 1.0.0
 */
public class AboutActivity extends BaseActivity implements View.OnClickListener {

    private TextView mTvVersion;
    private TextView mBtnCheckVersion;
    private TextView mBtnShare;
    private TextView mBtnWebsite;

    private ProgressDialog mProgressDialog;

    @Override
    protected int getContentView() {
        return R.layout.activity_about;
    }

    @Override
    protected void initData() {
        PackageInfo packageInfo = SystemUtil.getPackageInfo(mContext);
        String versionName = packageInfo.versionName;
        mTvVersion.setText(getString(R.string.about_version_name, versionName));
    }

    @Override
    protected void initView() {
        mTvVersion = (TextView) findViewById(R.id.tv_version);
        mBtnCheckVersion = (TextView) findViewById(R.id.tv_check_version);
        mBtnShare = (TextView) findViewById(R.id.tv_share);
        mBtnWebsite = (TextView) findViewById(R.id.tv_website);

        mBtnCheckVersion.setOnClickListener(this);
        mBtnShare.setOnClickListener(this);
        mBtnWebsite.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_check_version: //检查版本更新
                break;
        }
    }

    private void checkVersion() {
        mProgressDialog = showLoadingDialog(R.string.about_version_check_ing);
    }
}
