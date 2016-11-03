package com.yunxinlink.notes.sync.download;

import android.content.Context;

import com.socks.library.KLog;
import com.yunxinlink.notes.api.impl.NoteApi;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 文件下载器
 * @author huanghui-iri
 * @update 2016/11/2 14:13
 * @version: 0.0.1
 */
public class Downloader {
    
    private static final String TAG = "Downloader";
    
    private static Downloader mInstance;
    
    private LaunchTaskPool mPool = new LaunchTaskPool();
    
    private List<DownloadListener> mListeners;
    
    private Downloader() {
        mListeners = new LinkedList<>();
    }
    
    public synchronized static Downloader getInstance() {
        if (mInstance == null) {
            mInstance = new Downloader();
        }
        return mInstance;
    }

    /**
     * 取消、停止所有的任务
     */
    public void cancelAll() {
        mPool.expireAll();
//        mListeners.clear();
        mInstance = null;
    }

    /**
     * 添加监听器
     * @param listener
     */
    public void attachListener(DownloadListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * 移除监听器
     * @param listener
     */
    public void detachListener(DownloadListener listener) {
        mListeners.remove(listener);
    }

    /**
     * 通知下载任务开始了
     * @param task
     */
    public synchronized void notifyStart(DownloadTask task) {
        if (mListeners != null && mListeners.size() > 0) {
            for (DownloadListener listener : mListeners) {
                listener.onStart(task);
            }
        }
    }

    /**
     * 通知下载任务失败了
     * @param task
     */
    public synchronized void notifyError(DownloadTask task) {
        if (mListeners != null && mListeners.size() > 0) {
            for (DownloadListener listener : mListeners) {
                listener.onError(task);
            }
        }
    }

    /**
     * 通知下载任务成功且结束了
     * @param task
     */
    public synchronized void notifyCompleted(DownloadTask task) {
        if (mListeners != null && mListeners.size() > 0) {
            for (DownloadListener listener : mListeners) {
                listener.onCompleted(task);
            }
        }
    }

    /**
     * 下载某个任务
     * @param task
     */
    public void download(DownloadTask task, Context context) {
        mPool.asyncExecute(task, context);
    }

    private class LaunchTaskPool {
        private ThreadPoolExecutor mPool;

        /**
         * the queue to use for holding tasks before they are
         * executed.  This queue will hold only the {@code Runnable}
         * tasks submitted by the {@code execute} method.
         */
        private LinkedBlockingQueue<Runnable> mWorkQueue;

        public LaunchTaskPool() {
            init();
        }

        public void asyncExecute(DownloadTask task, Context context) {
            mPool.execute(new LaunchTaskRunnable(task, context));
        }

        private void init() {
            mWorkQueue = new LinkedBlockingQueue<>();
            mPool = DownloadExecutors.newDefaultThreadPool(3, mWorkQueue, "LauncherTask");
        }

        /**
         * 停止所有的任务
         */
        public void expireAll() {
            KLog.d(TAG, "launch task pool expire all task");
            mPool.shutdown();
        }
    }

    /**
     * 文件下载的具体任务
     */
    private class LaunchTaskRunnable implements Runnable {
        
        private DownloadTask mTask;
        
        private Context mContext;

        public LaunchTaskRunnable(DownloadTask mTask, Context context) {
            this.mTask = mTask;
            this.mContext = context;
        }

        @Override
        public void run() {
            //通知该任务开始了
            notifyStart(mTask);
            //开始下载
            Map<String, Object> param = mTask.getParams();
            if (SystemUtil.isEmpty(param) || param.get("attach") == null) { //参数为空
                KLog.d(TAG, "downloader launch task param is empty or attach is null");
                notifyError(mTask);
                return;
            }
            Attach attach = (Attach) param.get("attach");
            boolean result = false;
            try {
                result = NoteApi.downAttachFile(mContext, attach);
                KLog.d(TAG, "downloader launch task down load success:" + attach);
                result = true;
            } catch (IOException e) {
                KLog.d(TAG, "downloader launch task down load error:" + e.getMessage());
                e.printStackTrace();
            }
            if (result) {
                notifyCompleted(mTask);
            } else {
                notifyError(mTask);
            }
        }
    }
}
