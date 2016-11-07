package com.yunxinlink.notes.sync.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.os.AsyncTaskCompat;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.impl.NoteApi;
import com.yunxinlink.notes.api.model.NoteParam;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.Observer;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.TaskParam;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.sync.SyncCache;
import com.yunxinlink.notes.sync.SyncData;
import com.yunxinlink.notes.sync.download.DownloadTask;
import com.yunxinlink.notes.sync.download.DownloadTaskQueue;
import com.yunxinlink.notes.sync.download.SimpleDownloadListener;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步数据的服务
 * @author huanghui1
 * @update 2016/10/12 17:56
 * @version: 1.0.0
 */
public class SyncService extends Service {
    private static final String TAG = "SyncService";
    
    private Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mContext = this;
    }

    public SyncService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        KLog.d(TAG, "sync service on start invoke : " + intent);
        if (intent != null) {
            int syncType = intent.getIntExtra(Constants.ARG_CORE_OPT, 0);
            TaskParam param = new TaskParam();
            param.optType = syncType;
            switch (syncType) {
                case Constants.SYNC_UP_NOTE:    //向服务器上传笔记信息
                    KLog.d(TAG, "sync service will start sync up note info");
                    handleSyncCommand(intent, param);
                    break;
                case Constants.SYNC_DOWN_NOTE:  //从服务器下载笔记信息
                    //从服务器上下载笔记本的hash值，并本地的比较
                    KLog.d(TAG, "sync service will start sync down note info");
                    handleSyncCommand(intent, param);
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 处理同步的命令
     * @param intent
     * @param param
     */
    private void handleSyncCommand(Intent intent, TaskParam param) {
        //同步任务的编号
        String syncSid = intent.getStringExtra(Constants.ARG_CORE_OBJ);
        KLog.d(TAG, "sync service will start sync task sid : " + syncSid);
        if (!TextUtils.isEmpty(syncSid)) {

            if (isSyncIng(syncSid)) {   //该任务已在同步中
                KLog.d(TAG, "sync service start sync task but this task is syncing sync sid is :" + syncSid);
                return;
            }
            
            //通知界面显示进度条
            onStartSync();
            
            param.data = syncSid;
            //开始同步
            KLog.d(TAG, "sync service start sync task sid is :" + syncSid);
            executeTask(param);
        } else {
            KLog.d(TAG, "sync service not start sync task because sid is null");
        }
    }

    /**
     * 开始同步数据
     */
    private void onStartSync() {
        KLog.d(TAG, "sync service start sync");
        NoteManager.getInstance().notifyObservers(Provider.NOTIFY_FLAG, Observer.NotifyType.LOADING);
    }

    /**
     * 同步结束
     */
    private void onEndSync() {
        KLog.d(TAG, "sync service end sync");
        NoteManager.getInstance().notifyObservers(Provider.NOTIFY_FLAG, Observer.NotifyType.DONE);
    }

    /**
     * 执行后台任务
     * @param param
     */
    private void executeTask(TaskParam param) {
        AsyncTaskCompat.executeParallel(new SyncTask(), param);
//        new SyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, param);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 获取当前的用户
     * @return
     */
    private User getCurrentUser() {
        NoteApplication app = (NoteApplication) getApplicationContext();
        return app.getCurrentUser();
    }

    /**
     * 上传笔记
     * @param syncSid 同步任务的编号
     */
    private TaskParam syncUpNotes(String syncSid) {
        final TaskParam resultParam = new TaskParam();
        SyncData syncData = SyncCache.getInstance().getSyncData(syncSid);
        if (!checkSyncUpState(syncData)) {
            return null;
        } else {
            NoteParam noteParam = (NoteParam) syncData.getSyncable();
            Folder folder = noteParam.getFolder();
            List<DetailNoteInfo> detailNoteInfos = noteParam.getDetailNoteInfos();
            try {
                resultParam.optType = Constants.SYNC_UP_NOTE;
                //该方法是同步的，所以回调也是在同一个线程里
                if (noteParam.onlySyncState()) {    //只同步笔记的状态
                    resultParam.code = NoteApi.updateNoteDeleteState(mContext, detailNoteInfos);
                } else {    //同步笔记的内容
                    resultParam.code = NoteApi.syncUpNote(mContext, folder, detailNoteInfos);
                }
            } catch (Exception e) {
                KLog.e(TAG, "sync service sync up note info error:" + e.getMessage());
            }
            //移除该同步记录
            SyncCache.getInstance().remove(syncSid);
        }
        return resultParam;
    }

    /**
     * 检测向上同步的状态
     * @param syncData 同步数据
     * @return
     */
    private synchronized boolean checkSyncUpState(SyncData syncData) {
        if (syncData == null || syncData.isSyncing()
                || !syncData.hasSyncData() || !(syncData.getSyncable() instanceof NoteParam)) {
            KLog.d(TAG, "sync service sync up note info but sync data is null or is syncing or not has sync data:" + syncData);
            return false;
        } else {
            syncData.setState(SyncData.SYNC_ING);
            return true;
        }
    }

    /**
     * 检测该同步任务的状态
     * @param syncSid 同步任务的id
     * @return true:可以继续执行该同步任务，false：该任务已经在执行了
     */
    private synchronized boolean checkSyncState(String syncSid) {
        SyncData syncData = SyncCache.getInstance().getSyncData(syncSid);
        KLog.d(TAG, "sync service sync down note task in cache:" + syncData);
        if (syncData == null) { //没有同步数据
            KLog.d(TAG, "sync service sync down note task is null and will add to task cache syncSid:" + syncSid);
            syncData = new SyncData();
            syncData.setState(SyncData.SYNC_NONE);
            SyncCache.getInstance().addOrUpdate(syncSid, syncData);
        }
        if (syncData.isSyncing()) { //已经有任务在同步了，则不必处理了
            KLog.d(TAG, "sync service sync down note already hash sync task so do nothing syncSid:" + syncSid);
            return false;
        }
        syncData.setState(SyncData.SYNC_ING);
        return true;
    }

    /**
     * 向下同步笔记
     * @param syncSid 向下同步任务的编号
     * @return
     */
    private TaskParam syncDownNote(String syncSid) {
        User user = getCurrentUser();
        if (user == null || !user.isAvailable()) {
            SyncCache.getInstance().remove(syncSid);
            KLog.d(TAG, "sync service sync down note user is null or user is not available user:" + user);
            return null;
        }

        //根据同步的sid来检查该同步任务是否正在进行
        if (!checkSyncState(syncSid)) {
            return null;
        }

        final TaskParam resultParam = new TaskParam();
        resultParam.optType = Constants.SYNC_DOWN_NOTE;
        //同步笔记本
        syncDownFolder(user);

        //下载笔记
        downNotes(user);
        
        //下载附件
        downAttachFile(user);

        SyncCache.getInstance().remove(syncSid);
        return resultParam;
    }

    /**
     * 下载附件
     */
    private void downAttachFile(User user) {
        //查询需要下载的附件
        List<Attach> attachList = NoteManager.getInstance().getUnDownloadAttach(user);
        if (SystemUtil.isEmpty(attachList)) {
            KLog.d(TAG, "sync service download attach file but list is empty");
            return;
        }
        
        List<DownloadTask> taskList = new ArrayList<>();
        for (Attach attach : attachList) {
            DownloadTask task = new DownloadTask();
            task.setId(attach.getSid());
            task.setFilename(attach.getFilename());
            String savePath = null;
            try {
                savePath = SystemUtil.getAttachFilePath(attach.getNoteId(), attach.getType(), attach.getFilename());
            } catch (IOException e) {
                e.printStackTrace();
            }
            task.setSavePath(savePath);
            Map<String, Object> param = new HashMap<>();
            param.put("attach", attach);
            task.setParams(param);

            taskList.add(task);
        }
        KLog.d(TAG, "sync service download attach file start");
        DownloadTaskQueue downloadTaskQueue = new DownloadTaskQueue(mContext);
        downloadTaskQueue.setDownloadListener(new MyDownloadListener());
        downloadTaskQueue.addAll(taskList);
        downloadTaskQueue.start();
    }

    /**
     * 同步笔记本
     * @param user 当前登录的用户
     */
    private void syncDownFolder(User user) {
        //同步笔记本
        Map<String, Folder> folderMap = FolderManager.getInstance().getFolders(user, null);
        if (SystemUtil.isEmpty(folderMap)) {  //本地没有笔记本，则直接下载服务器的笔记本
            KLog.d(TAG, "down folders all local folder is empty");
            NoteApi.downFolders(mContext);
        } else {
            KLog.d(TAG, "down folders any local has folder");
            List<Integer> folderIdList = NoteApi.downFolderIds(mContext);
            KLog.d(TAG, "sync down note folder id list:" + folderIdList);
            //获取该组id对应的笔记本的数据，也是分页请求数据，每页20条
            NoteApi.downFolders(folderIdList, mContext);
        }
    }

    /**
     * 下载笔记
     * @param user
     */
    private void downNotes(User user) {
        //先检查本地是否有
        long count = NoteManager.getInstance().getNoteCount(user, null);
        if (count == 0) {   //本地没有笔记，则下载服务器的全部笔记
            KLog.d(TAG, "down notes all local note is empty");
            NoteApi.downNotes(mContext);
        } else {    //本地有部分笔记则按sid来匹配下载
            KLog.d(TAG, "down notes any local has note will down with ids");
            NoteApi.downNotesWithIds(mContext);
        }
    }

    /**
     * 检测同步的任务是否在进行中，如果进行中，则不做处理
     * @param syncSid
     * @return
     */
    private boolean isSyncIng(String syncSid) {
        SyncData syncData = SyncCache.getInstance().getSyncData(syncSid);
        return syncData != null && syncData.isSyncing();
    }

    /**
     * 处理向上同步的结果
     * @param code 结果码
     */
    private void syncUpResult(int code) {
        switch (code) {
            case ActionResult.RESULT_STATE_DISABLE://用户不可用
            case ActionResult.RESULT_DATA_NOT_EXISTS://用户不可用
                SystemUtil.makeShortToast("用户没有登录或者不可用");
                break;
            case ActionResult.RESULT_PARAM_ERROR:   //参数错误
                SystemUtil.makeShortToast("参数错误");
                break;
            case ActionResult.RESULT_ERROR:   //请求错误或者服务器错误
                SystemUtil.makeShortToast("同步失败，请稍后再试");
                break;
            case ActionResult.RESULT_SUCCESS:   //同步成功
                SystemUtil.makeShortToast("同步成功");
                break;
            default:
                SystemUtil.makeShortToast("同步失败，请稍后再试");
                break;
        }
    }

    /**
     * 附件下载的监听器
     */
    class MyDownloadListener extends SimpleDownloadListener {
        @Override
        public void onCompleted(DownloadTask downloadTask) {
            super.onCompleted(downloadTask);
            onEndSync();
            KLog.d(TAG, "sync service download file listener completed");
        }
    }

    /**
     * 同步的后台任务
     */
    class SyncTask extends AsyncTask<TaskParam, Void, TaskParam> {

        @Override
        protected TaskParam doInBackground(TaskParam... params) {
            if (params == null || params.length == 0) {
                return null;
            }
            TaskParam param = params[0];
            String syncSid = (String) param.data;
            if (TextUtils.isEmpty(syncSid)) {
                KLog.d(TAG, "sync service sync task sid is null");
                return null;
            }
            TaskParam resultParam = null;
            KLog.d(TAG, "sync service sync task do in background syncSid :" + syncSid);
            int syncType = param.optType;
            switch (syncType) {
                case Constants.SYNC_UP_NOTE:    //向上同步笔记信息，即上传笔记
                    resultParam = syncUpNotes(syncSid);
                    break;
                case Constants.SYNC_DOWN_NOTE:  //向下同步笔记，即下载笔记
                    resultParam = syncDownNote(syncSid);
                    break;
            }
            return resultParam;
        }

        @Override
        protected void onPostExecute(TaskParam param) {
            if (param != null/* && param.isAvailable()*/) { //返回的结果可用
                switch (param.optType) {
                    case Constants.SYNC_UP_NOTE:    //向上同步笔记
                        onEndSync();
                        syncUpResult(param.code);
                        break;
                    case Constants.SYNC_DOWN_NOTE:
                        break;
                }
            } else {
                KLog.d(TAG, "sync service on post execute result param is null or is not available");
            }
        }
    }
}
