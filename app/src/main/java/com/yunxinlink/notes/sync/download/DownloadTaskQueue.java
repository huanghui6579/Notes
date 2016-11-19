package com.yunxinlink.notes.sync.download;

import android.content.Context;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 下载的任务队列
 * @author huanghui-iri
 * @update 2016/11/2 11:21
 * @version: 0.0.1
 */
public class DownloadTaskQueue implements DownloadListener {
    
    private static final String TAG = "DownloadTaskQueue";
    
    /**
     * 最大任务数，默认是2个
     */
    private static final int DEFAULT_MAX_COUNT = 2;

    /**
     * 同时下载的任务数量,默认是2个
     */
    private int mMaxThreadCount = DEFAULT_MAX_COUNT;
    
    /**
     * 要下载的任务列表,等待的队列
     */
    private LinkedList<DownloadTask> mWaitTasks = new LinkedList<>();

    /**
     * 正在下载的任务列表
     */
    private LinkedList<DownloadTask> mRunningTasks = new LinkedList<>();

    /**
     * 所有下载任务的总的监听器
     */
    private DownloadListener mDownloadListener;
    
    private Context mContext;
    
    public DownloadTaskQueue(Context context) {
        this.mContext = context;
        init();
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.mDownloadListener = downloadListener;
    }

    /**
     * 初始化
     */
    private void init() {
        Downloader.getInstance().attachListener(this);
    }

    /**
     * 销毁，结束所有的任务
     */
    public void destroy() {
        mWaitTasks.clear();
        mRunningTasks.clear();
        Downloader downloader = Downloader.getInstance();
//        downloader.detachListener(this);
        downloader.cancelAll();
        if (mDownloadListener != null) {
            mDownloadListener.onCompleted(null);
        }
        KLog.d(TAG, "download task queue destroy");
    }

    /**
     * 添加一个任务到下载等待的队列
     * @param task
     */
    public synchronized void add(DownloadTask task) {
        if (!mWaitTasks.contains(task)) {
            mWaitTasks.add(task);
        } else {
            KLog.d(TAG, "add a new task to the waiting queue but task is already exists:" + task);
        }
    }

    /**
     * 添加一组任务
     * @param list
     */
    public synchronized void addAll(List<DownloadTask> list) {
        //先移除已有的
        list.removeAll(mWaitTasks);
        //再添加没有移除的
        mWaitTasks.addAll(list);
    }

    /**
     * 是否是满任务在执行，即当前执行的任务数量为设置的最大数量
     * @return
     */
    private boolean isFullRunning() {
        return mRunningTasks.size() >= mMaxThreadCount;
    }

    /**
     * 是否还有等待的任务
     * @return
     */
    private boolean hasMoreWaitTask() {
        return !SystemUtil.isEmpty(mWaitTasks);
    }

    /**
     * 是否还有正在进行的任务
     * @return
     */
    private boolean hasRunningTask() {
        return !SystemUtil.isEmpty(mRunningTasks);
    }

    /**
     * 完成某个任务
     * @param task
     */
    private synchronized void completedTask(DownloadTask task) {
        mRunningTasks.remove(task);
    }

    /**
     * 进行下一个任务
     */
    private void doNext() {
        if (hasMoreWaitTask()) {
            start();
        } else {
            KLog.d(TAG, "down load queue do next task but no more wait task");
            if (hasRunningTask()) {
                KLog.d(TAG, "down load queue do next task has running task");
            } else {
                KLog.d(TAG, "down load queue do next task but no task all task is completed and will destroy");
                destroy();
            }
        }
    }
    
    /**
     * 开始下载
     */
    public synchronized void start() {
        if (isFullRunning()) {  //当前正在执行的任务经满了
            KLog.d(TAG, "download task queue running task has full");
            return;
        }
        KLog.d(TAG, "download task queue start");
        Downloader downloader = Downloader.getInstance();
        while (!isFullRunning()) {
            if (SystemUtil.isEmpty(mWaitTasks)) {
                KLog.d(TAG, "download task queue wait task list is empty");
                break;
            }
            DownloadTask task = mWaitTasks.pop();
            if (task != null) {
                KLog.d(TAG, "download task queue running task do now:" + task);
                mRunningTasks.add(task);
                downloader.download(task, mContext);
            } else {
                KLog.d(TAG, "download task queue running task is null");
            }
        }
        KLog.d(TAG, "download task queue start but running task queue is full and will waiting");
    }

    @Override
    public void onStart(DownloadTask downloadTask) {
        KLog.d(TAG, "download task queue task on start:" + downloadTask);
    }

    @Override
    public void onCompleted(DownloadTask downloadTask) {
        completedTask(downloadTask);
        KLog.d(TAG, "download task queue task on completed:" + downloadTask);
        doNext();
    }

    @Override
    public void onError(DownloadTask downloadTask) {
        completedTask(downloadTask);
        KLog.d(TAG, "download task queue task on error:" + downloadTask);
        doNext();
    }

    @Override
    public void onProgress(long bytesRead, long contentLength, boolean done) {
        
    }

    @Override
    public void onCanceled(DownloadTask downloadTask) {
        
    }
}
