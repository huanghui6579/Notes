package com.yunxinlink.notes.sync.download;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.api.impl.DeviceApi;
import com.yunxinlink.notes.api.model.VersionInfo;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.lang.ref.WeakReference;

import retrofit2.Call;

/**
 * 软件下载器
 * @author tiger
 * @version 1.0.0
 * @update 2016/11/20 10:28
 */
public class AppDownloader {
    private static final String TAG = "AppDownloader";

    private static final int MSG_DOWNLOAD_START = 11;
    private static final int MSG_DOWNLOAD_PROGRESS = 12;
    private static final int MSG_DOWNLOAD_CANCEL = 13;
    private static final int MSG_DOWNLOAD_SUCCESS = 14;
    private static final int MSG_DOWNLOAD_FAILED = 15;

    private Context mContext;

    private Handler mHandler;

    private Call<?> mDownloadCall;

    private ProgressBar mProgressBar;

    private TextView mTvPercent;

    //显示版本信息的对话框
    private Dialog mVersionDialog;

    public AppDownloader(Context context) {
        this.mContext = context;
        this.mHandler = new MyHandler(this, Looper.getMainLooper());
    }

    /**
     * 开始下载app
     * @param versionInfo
     */
    public void downloadApp(VersionInfo versionInfo) {
        SystemUtil.doInbackground(new NoteTask(versionInfo) {
            @Override
            public void run() {
                VersionInfo info = (VersionInfo) params[0];
                mDownloadCall = DeviceApi.downloadApp(info, new AppDownloadListener());
            }
        });

    }

    /**
     * 显示下载的对话框
     * @param context
     * @return
     */
    private Dialog showDownloadDialog(Context context) {
        AlertDialog.Builder builder = NoteUtil.buildDialog(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.layout_dialog_download, null);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        mTvPercent = (TextView) view.findViewById(R.id.tv_percent);
        TextView tvMsg = (TextView) view.findViewById(android.R.id.message);
        tvMsg.setText(R.string.version_download_msg);
        mTvPercent.setText(context.getString(R.string.version_download_percent, 0));
        AlertDialog dialog = builder.setTitle(R.string.version_download_title)
                .setView(view)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelDownload(null);
                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
        mVersionDialog = dialog;
        return dialog;
    }

    /**
     * 取消下载
     * @param downloadTask
     */
    private void cancelDownload(DownloadTask downloadTask) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_DOWNLOAD_CANCEL;
        msg.obj = downloadTask;
        mHandler.sendMessage(msg);
    }

    /**
     * 校验自定的view
     * @return
     */
    private boolean validateView() {
        return mProgressBar != null && mTvPercent != null;
    }

    /**
     * 让对话框消失
     * @param dialog
     */
    private void dismissDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * 软件下载的监听器
     */
    private class AppDownloadListener extends SimpleDownloadListener {

        @Override
        public void onStart(DownloadTask downloadTask) {
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_DOWNLOAD_START;
            msg.obj = downloadTask;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onCompleted(DownloadTask downloadTask) {
            super.onCompleted(downloadTask);
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_DOWNLOAD_SUCCESS;
            msg.obj = downloadTask;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onError(DownloadTask downloadTask) {
            super.onError(downloadTask);
            mHandler.sendEmptyMessage(MSG_DOWNLOAD_FAILED);
        }

        @Override
        public void onProgress(long bytesRead, long contentLength, boolean done) {
            super.onProgress(bytesRead, contentLength, done);
            if (contentLength <= 0) {
                KLog.d(TAG, "down load app but content length is 0");
                return;
            }
            int percent = (int) (bytesRead * 100 / contentLength);
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_DOWNLOAD_PROGRESS;
            msg.arg1 = percent;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onCanceled(DownloadTask downloadTask) {
            super.onCanceled(downloadTask);
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_DOWNLOAD_CANCEL;
            msg.obj = downloadTask;
            mHandler.sendMessage(msg);
        }
    }

    /**
     * 更新进度条
     * @param percent
     */
    private void updateProgress(int percent) {
        if (validateView()) {
            KLog.d(TAG, "update dialog progress:" + percent);
            mProgressBar.setProgress(percent);
            mTvPercent.setText(mContext.getString(R.string.version_download_percent, percent));
        }
    }
    /**
     * 下载完毕
     * @param downloadTask
     */
    public void downloadSuccess(DownloadTask downloadTask) {
        if (downloadTask != null && !TextUtils.isEmpty(downloadTask.getSavePath())) {
            SystemUtil.installApp(mContext, downloadTask.getSavePath());
        } else {
            KLog.d(TAG, "download app success but download task is null");
        }
    }

    /**
     * 取消下载
     * @param downloadTask
     */
    private void doCancelDownload(DownloadTask downloadTask) {
        if (mDownloadCall != null) {
            mDownloadCall.cancel();
        }
        if (downloadTask != null) {
            String filePath = downloadTask.getSavePath();
            FileUtil.deleteFile(filePath);
        }
    }

    private static class MyHandler extends Handler {

        private final WeakReference<AppDownloader> mTarget;

        public MyHandler(AppDownloader target, Looper looper) {
            super(looper);
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            AppDownloader target = mTarget.get();
            if (target == null) {
                return;
            }
            DownloadTask downloadTask = null;
            switch (msg.what) {
                case MSG_DOWNLOAD_START:    //开始下载软件包
                    target.dismissDialog(target.mVersionDialog);
                    target.showDownloadDialog(target.mContext);
                    break;
                case MSG_DOWNLOAD_PROGRESS: //更新进度条
                    int percent = msg.arg1;
                    target.updateProgress(percent);
                    break;
                case MSG_DOWNLOAD_CANCEL:
                    downloadTask = (DownloadTask) msg.obj;
                    target.dismissDialog(target.mVersionDialog);
                    target.doCancelDownload(downloadTask);
                    break;
                case MSG_DOWNLOAD_FAILED:
                    target.dismissDialog(target.mVersionDialog);
                    SystemUtil.makeShortToast(R.string.version_download_failed);
                    break;
                case MSG_DOWNLOAD_SUCCESS:
                    downloadTask = (DownloadTask) msg.obj;
                    target.dismissDialog(target.mVersionDialog);
                    target.downloadSuccess(downloadTask);
                    break;
            }
        }
    }
}
