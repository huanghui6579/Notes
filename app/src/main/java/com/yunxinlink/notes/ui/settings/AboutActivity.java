package com.yunxinlink.notes.ui.settings;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.api.impl.DeviceApi;
import com.yunxinlink.notes.api.model.VersionInfo;
import com.yunxinlink.notes.listener.SimpleOnLoadCompletedListener;
import com.yunxinlink.notes.share.ShareInfo;
import com.yunxinlink.notes.sync.download.AppDownloader;
import com.yunxinlink.notes.ui.BaseActivity;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.lang.ref.WeakReference;

import cn.sharesdk.framework.Platform;

/**
 * 关于界面
 * @author tiger
 * @update 2016/11/19 17:23
 * @version 1.0.0
 */
public class AboutActivity extends BaseActivity implements View.OnClickListener {

    private static final int MSG_NO_NEW_VERSION = 10;

    private TextView mTvVersion;

    private ProgressDialog mProgressDialog;

    private Handler mHandler = new MyHandler(this);

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
        TextView btnCheckVersion = (TextView) findViewById(R.id.tv_check_version);
        TextView btnShare = (TextView) findViewById(R.id.tv_share);
        TextView btnWebsite = (TextView) findViewById(R.id.tv_website);

        if (btnCheckVersion != null) {
            btnCheckVersion.setOnClickListener(this);
        }
        if (btnShare != null) {
            btnShare.setOnClickListener(this);
        }
        if (btnWebsite != null) {
            btnWebsite.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_check_version: //检查版本更新
                checkVersion();
                break;
            case R.id.tv_share: //推荐给好友
                shareApp();
                break;
        }
    }

    /**
     * 分享给好友
     */
    private void shareApp() {
        ShareInfo shareInfo = new ShareInfo();
        String url = Constants.WEB_SITE_URL;
        String title = SystemUtil.getAppName(mContext);
        String text = getString(R.string.about_share_text);
        shareInfo.setShareType(Platform.SHARE_WEBPAGE);
        shareInfo.setSite(title);
        shareInfo.setSiteUrl(url);
        shareInfo.setText(text);
        shareInfo.setTitle(title);
        shareInfo.setTitleUrl(url);
        shareInfo.setUrl(url);

        NoteUtil.showShare(mContext, shareInfo, true);
    }

    /**
     * 检查版本更新
     */
    private void checkVersion() {
        mProgressDialog = showLoadingDialog(R.string.about_version_check_ing);
        DeviceApi.checkVersionAsync(mContext, new MyOnLoadCompletedListener());
    }

    /**
     * 显示新版本信息的对话框
     * @param versionInfo
     * @return
     */
    private Dialog showNewVersionDialog(final VersionInfo versionInfo) {
        String title = getString(R.string.about_check_version_new_title, "V" + versionInfo.getVersionName());
        String pkgSize = getString(R.string.about_check_version_new_size, SystemUtil.formatFileSize(versionInfo.getSize()));
        String content = pkgSize + Constants.TAG_NEXT_LINE + getString(R.string.version_new_info, versionInfo.getContent());
        AlertDialog.Builder builder = NoteUtil.buildDialog(this);
        AlertDialog dialog = builder.setTitle(title)
                .setMessage(content)
                .setPositiveButton(R.string.about_check_version_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        new AppDownloader(mContext).downloadApp(versionInfo);
                    }
                })
                .setNegativeButton(R.string.about_check_version_btn_cancel, null)
                .setCancelable(false)
                .create();
        dialog.show();
        return dialog;
    }

    /**
     * 检查更新后的回调
     */
    private class MyOnLoadCompletedListener extends SimpleOnLoadCompletedListener<VersionInfo> {

        @Override
        public void onLoadSuccess(VersionInfo versionInfo) {
            super.onLoadSuccess(versionInfo);
            if (versionInfo == null) {   //http请求成功了，但是没有版本更新
                mHandler.sendEmptyMessage(MSG_NO_NEW_VERSION);
            } else {
                Message msg = mHandler.obtainMessage();
                msg.what = Constants.MSG_SUCCESS;
                msg.obj = versionInfo;
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public void onLoadFailed(int errorCode, String reason) {
            super.onLoadFailed(errorCode, reason);
            mHandler.sendEmptyMessage(Constants.MSG_FAILED);
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<AboutActivity> mTarget;

        public MyHandler(AboutActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            AboutActivity target = mTarget.get();
            if (target == null) {
                return;
            }
            target.dismissDialog(target.mProgressDialog);
            switch (msg.what) {
                case Constants.MSG_SUCCESS: //检查更新成功，且有新版本
                    VersionInfo versionInfo = (VersionInfo) msg.obj;
                    if (versionInfo != null) {
                        target.showNewVersionDialog(versionInfo);
                    }
                    break;
                case MSG_NO_NEW_VERSION:    //检查更新成功，但没有新版本
                    SystemUtil.makeShortToast(R.string.about_check_version_none);
                    break;
                case Constants.MSG_FAILED:  //http请求失败
                    SystemUtil.makeShortToast(R.string.about_check_version_failed);
                    break;
            }
        }
    }
}
