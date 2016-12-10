package com.yunxinlink.notes.sync.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.os.AsyncTaskCompat;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.impl.DeviceApi;
import com.yunxinlink.notes.api.impl.NoteApi;
import com.yunxinlink.notes.api.impl.UserApi;
import com.yunxinlink.notes.api.model.NoteParam;
import com.yunxinlink.notes.api.model.VersionInfo;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.Observer;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.QueryType;
import com.yunxinlink.notes.model.TaskParam;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.sync.SyncCache;
import com.yunxinlink.notes.sync.SyncData;
import com.yunxinlink.notes.sync.SyncSettingState;
import com.yunxinlink.notes.sync.Syncable;
import com.yunxinlink.notes.sync.download.DownloadListener;
import com.yunxinlink.notes.sync.download.DownloadTask;
import com.yunxinlink.notes.sync.download.DownloadTaskQueue;
import com.yunxinlink.notes.sync.download.SimpleDownloadListener;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.NoteUtil;
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
                case Constants.SYNC_NOTE:   //开始同步笔记，先向下同步，后向上同步
                    KLog.d(TAG, "sync service will start sync note info");
                    handleSyncCommand(intent, param);
                    break;
                case Constants.SYNC_USER:   //开始同步用户信息
                    KLog.d(TAG, "sync service will start sync user info");
                    handleSyncUser(intent, param);
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 同步用户信息
     * @param intent
     * @param param
     */
    private void handleSyncUser(Intent intent, final TaskParam param) {
        //同步任务的编号
        final String syncSid = intent.getStringExtra(Constants.ARG_CORE_OBJ);
        KLog.d(TAG, "sync service will start sync user task sid : " + syncSid);
        if (!TextUtils.isEmpty(syncSid)) {
            param.data = syncSid;
            //开始同步
            executeTask(param);
        }
    }

    /**
     * 处理同步的命令
     * @param intent
     * @param param
     */
    private void handleSyncCommand(Intent intent, final TaskParam param) {
        //同步任务的编号
        final String syncSid = intent.getStringExtra(Constants.ARG_CORE_OBJ);
        KLog.d(TAG, "sync service will start sync task sid : " + syncSid);
        if (!TextUtils.isEmpty(syncSid)) {
            if (!param.isManual) {   //非手动的，才需要再做检查了
                SyncSettingState settingState = NoteUtil.checkSyncSetting(mContext);

                if (settingState != SyncSettingState.ENABLE) {
                    removeSyncTask(syncSid);
                    KLog.d(TAG, "sync note check settings the result is can not sync");
                    return;
                }
            } else {
                KLog.d(TAG, "sync service is manual task ");
            }

            doHandleSyncCommand(syncSid, param);
        } else {
            KLog.d(TAG, "sync service not start sync task because sid is null");
        }
    }

    /**
     * 执行同步任务的准备工作
     * @param syncSid
     * @param param
     */
    private void doHandleSyncCommand(String syncSid, TaskParam param) {
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
     * 同步用户信息
     * @param syncSid
     * @return
     */
    private TaskParam syncUser(String syncSid) {
        final TaskParam resultParam = new TaskParam();
        SyncData syncData = SyncCache.getInstance().getSyncData(syncSid);
        if (!checkSyncUpState(syncData, true)) {
            return null;
        } else {
            User user = (User) syncData.getSyncable();
            UserApi.syncUserInfo(user);
            resultParam.optType = Constants.SYNC_USER;
        }
        return resultParam;
    }

    /**
     * 上传笔记
     * @param syncSid 同步任务的编号
     * @param checkSyncState 是否检查同步的状态               
     */
    private TaskParam syncUpNotes(String syncSid, boolean checkSyncState) {
        final TaskParam resultParam = new TaskParam();
        resultParam.optType = Constants.SYNC_UP_NOTE;
        SyncData syncData = SyncCache.getInstance().getSyncData(syncSid);
        if (checkSyncState && !checkSyncUpState(syncData, false)) {
            return null;
        }

        User user = getCurrentUser();
        if (user == null || !user.isAvailable()) {
            resultParam.code = ActionResult.RESULT_STATE_DISABLE;
            KLog.d(TAG, "sync service sync up notes user is null or not available");
            return resultParam;
        }
        
        Syncable syncable = syncData.getSyncable();
        if (syncable == null) { //向上同步所有的笔记
            KLog.d("sync service sync up all notes");
            //TODO 完善笔记的同步功能
           
            List<Folder> folderList = FolderManager.getInstance().getSortFolders(user, null);
            if (SystemUtil.isEmpty(folderList)) {
                folderList = new ArrayList<>();
                folderList.add(new Folder());
                KLog.d(TAG, "sync service sync up notes folder list is empty and create new list");
            }
            for (Folder folder : folderList) {
                Folder paramFolder = null;
                if (!TextUtils.isEmpty(folder.getSid())) {   //非默认的空笔记
                    paramFolder = FolderCache.getInstance().getCacheFolder(folder.getSid());
                }
                if (paramFolder == null) {
                    paramFolder = new Folder();
                    KLog.d(TAG, "sync service sync up notes param folder is null and new one:" + paramFolder);
                }
                KLog.d(TAG, "sync service sync up notes folder is:" + paramFolder);
                //查询该folder下的笔记
                Bundle args = new Bundle();
                args.putString(Constants.ARG_FOLDER_ID, paramFolder.getSid());
                args.putInt(Constants.ARG_QUERY_TYPE, QueryType.ALL.ordinal());
                args.putBoolean(Constants.ARG_IS_SYNC_UP, true);
                List<DetailNoteInfo> detailNoteInfos = NoteManager.getInstance().getAllDetailNotes(user, args);
                if (!SystemUtil.isEmpty(detailNoteInfos)) {  //没有要同步的笔记
                    doSyncUpNotes(resultParam, folder, false, detailNoteInfos);
                } else {
                    resultParam.code = ActionResult.RESULT_SUCCESS;
                    KLog.d(TAG, "sync service sync up notes list is empty");
                }
            }
        } else {    //只同步指定的笔记
            NoteParam noteParam = (NoteParam) syncData.getSyncable();
            Folder folder = noteParam.getFolder();
            List<DetailNoteInfo> detailNoteInfos = noteParam.getDetailNoteInfos();
            doSyncUpNotes(resultParam, folder, noteParam.onlySyncState(), detailNoteInfos);
        }
        return resultParam;
    }

    /**
     * 具体的执行同步笔记的操作
     * @param resultParam
     * @param folder
     * @param onlySyncState 是否只同步笔记的状态
     * @param detailNoteInfos
     * @return
     */
    private TaskParam doSyncUpNotes(TaskParam resultParam, Folder folder, boolean onlySyncState, List<DetailNoteInfo> detailNoteInfos) {
        if (detailNoteInfos == null) {
            detailNoteInfos = new ArrayList<>();
        }
        try {
            //空的笔记，则可以只同步笔记本
            int noteSize = detailNoteInfos.size() == 0 ? 1 : detailNoteInfos.size();
            //循环提交，每次提交10条数据
            int pageSize = Constants.PAGE_SIZE_DEFAULT / 2;
            int pageNumber = noteSize % pageSize == 0 ? noteSize / pageSize : noteSize / pageSize + 1;
            for (int i = 0; i < pageNumber; i++) {
                if (!checkUserState()) {
                    KLog.d(TAG, "sync up note user is null or not available");
                    break;
                }
                KLog.d(TAG, "sync up note page number:" + i);
                //该方法是同步的，所以回调也是在同一个线程里
                if (onlySyncState) {    //只同步笔记的状态
                    resultParam.code = NoteApi.updateNoteDeleteState(mContext, detailNoteInfos);
                } else {    //同步笔记的内容
                    resultParam.code = NoteApi.syncUpNote(mContext, folder, detailNoteInfos);
                }
            }
        } catch (Exception e) {
            KLog.e(TAG, "sync service sync up note info error:" + e.getMessage());
        }
        return resultParam;
    }

    /**
     * 检测向上同步的状态
     * @param syncData 同步数据
     * @param checkData 是否需要校验是否有同步数据                
     * @return
     */
    private synchronized boolean checkSyncUpState(SyncData syncData, boolean checkData) {
        if (syncData == null) {
            KLog.d(TAG, "sync service sync up check state sync data is null");
            return false;
        }
        if (checkData && !syncData.hasSyncData()) {
            KLog.d(TAG, "sync service sync up check state need check but syncable is null");
            return false;
        }
        if (syncData.isSyncing()) {
            KLog.d(TAG, "sync service sync up but sync data is null or is syncing or not has sync data:" + syncData);
            return false;
        } else {
            syncData.setState(SyncData.SYNC_ING);
            return true;
        }
    }

    /**
     * 检查用户当前的状态是否可用
     * @return
     */
    private boolean checkUserState() {
        User user = getCurrentUser();
        return user != null && user.isAvailable();
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
     * @param taskParam 任务的参数
     * @param syncSid 向下同步任务的编号
     * @return
     */
    private TaskParam syncDownNote(String syncSid, TaskParam taskParam) {
        User user = getCurrentUser();
        if (user == null || !user.isAvailable()) {
            KLog.d(TAG, "sync service sync down note user is null or user is not available user:" + user);
            onEndSync();
            removeSyncTask(syncSid);
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
        downAttachFile(user, !taskParam.isManual);

//        removeSyncTask(syncSid);
        return resultParam;
    }

    /**
     * 同步笔记，先向下同步，再向上同步
     * @param syncSid 同步任务的编号
     * @param taskParam 同步任务的参数
     * @return
     */
    private TaskParam syncNote(String syncSid, TaskParam taskParam) {
        KLog.d(TAG, "sync note sid:" + syncSid);
        TaskParam param = syncDownNote(syncSid, taskParam);
        if (param == null) {
            KLog.d(TAG, "sync note but down note task param is null");
            return null;
        }
        //TODO 同步笔记有问题
        param = syncUpNotes(syncSid, false);
        if (param == null) {
            KLog.d(TAG, "sync note but up note task param is null");
            return null;
        }
        param.optType = taskParam.optType;
        return param;
    }

    /**
     * 移除同步的sid
     * @param syncSid
     */
    private void removeSyncTask(String syncSid) {
        SyncCache.getInstance().remove(syncSid);
    }

    /**
     * 下载附件
     * @param autoNotifyDone 是否自动通知下载完毕
     * @return 是否有附件需要下载，true：有附件需要下载
     */
    private boolean downAttachFile(User user, boolean autoNotifyDone) {
        DownloadListener downloadListener = new MyDownloadListener(autoNotifyDone);
        //查询需要下载的附件
        List<Attach> attachList = NoteManager.getInstance().getUnDownloadAttach(user);
        if (SystemUtil.isEmpty(attachList)) {
            KLog.d(TAG, "sync service download attach file but list is empty");
            downloadListener.onCompleted(null);
            return false;
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
        downloadTaskQueue.setDownloadListener(downloadListener);
        downloadTaskQueue.addAll(taskList);
        downloadTaskQueue.start();
        return true;
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
        //是否自动通知同步完成
        private boolean mAutoNotifyDone;

        public MyDownloadListener(boolean autoNotifyDone) {
            this.mAutoNotifyDone = autoNotifyDone;
        }

        @Override
        public void onCompleted(DownloadTask downloadTask) {
            super.onCompleted(downloadTask);
            if (mAutoNotifyDone) {
                //同步结束
                onEndSync();
                //如果本地已经有了新版本的信息，则下载新版本
                NoteApplication app = (NoteApplication) getApplication();
                boolean hasNewVersion = app.hasNewVersion();
                if (hasNewVersion) {
                    VersionInfo tmp = app.getVersionInfo();
                    VersionInfo versionInfo = tmp.clone();
                    app.setVersionInfo(null);
                    boolean isWifi = SystemUtil.isWifiConnected(app);
                    if (isWifi) {
                        KLog.d(TAG, "sync service has new version and wifi connected and will download new app");
                        
                        if (versionInfo == null) {
                            KLog.d(TAG, "sync service has new version clone version info error");
                            return;
                        }
                        DeviceApi.downloadApp(versionInfo, new AppDownloadListener());
                    } else {
                        KLog.d(TAG, "sync service has new version but is not wifi");
                    }
                }
                
            }
            KLog.d(TAG, "sync service download file listener completed");
        }
    }

    /**
     * APP下载的监听器
     */
    class AppDownloadListener extends SimpleDownloadListener {

        @Override
        public void onStart(DownloadTask downloadTask) {
            super.onStart(downloadTask);
            KLog.d(TAG, "download app sync service onStart task:" + downloadTask);
        }

        @Override
        public void onCompleted(DownloadTask downloadTask) {
            super.onCompleted(downloadTask);
            KLog.d(TAG, "download app sync service onStart task:" + downloadTask);
        }

        @Override
        public void onError(DownloadTask downloadTask) {
            super.onError(downloadTask);
            KLog.d(TAG, "download app sync service onError task:" + downloadTask);
        }

        @Override
        public void onProgress(long bytesRead, long contentLength, boolean done) {
            super.onProgress(bytesRead, contentLength, done);
            KLog.d(TAG, "download app sync service onProgress bytesRead:" + bytesRead + ", contentLength:" + contentLength + ", done:" + done);
        }

        @Override
        public void onCanceled(DownloadTask downloadTask) {
            super.onCanceled(downloadTask);
            KLog.d(TAG, "download app sync service onCanceled task:" + downloadTask);
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
            //是否是同步笔记
            boolean syncNote = true;
            TaskParam resultParam = null;
            KLog.d(TAG, "sync service sync task do in background syncSid :" + syncSid);
            int syncType = param.optType;
            switch (syncType) {
                case Constants.SYNC_UP_NOTE:    //向上同步笔记信息，即上传笔记
                    resultParam = syncUpNotes(syncSid, true);
                    break;
                case Constants.SYNC_DOWN_NOTE:  //向下同步笔记，即下载笔记
                    resultParam = syncDownNote(syncSid, param);
                    break;
                case Constants.SYNC_NOTE:   //先向下同步笔记，后向上同步笔记
                    param.isManual = true;
                    resultParam = syncNote(syncSid, param);
                    break;
                case Constants.SYNC_USER:   //同步用户的信息
                    syncNote = false;
                    resultParam = syncUser(syncSid);
                    break;
            }
            //移除同步的sid
            removeSyncTask(syncSid);
            if (syncNote) {
                //修改最后的同步时间
                User user = getCurrentUser();
                if (user != null && user.isAvailable()) {
                    user.setLastSyncTime(System.currentTimeMillis());
                    UserManager.getInstance().updateSyncTime(user);
                }
            }
            return resultParam;
        }

        @Override
        protected void onPostExecute(TaskParam param) {
            if (param != null/* && param.isAvailable()*/) { //返回的结果可用
                switch (param.optType) {
                    case Constants.SYNC_NOTE:   //同步笔记，这里不需要break
                        syncUpResult(param.code);
                    case Constants.SYNC_UP_NOTE:    //向上同步笔记
                        onEndSync();
                        break;
                    case Constants.SYNC_DOWN_NOTE:
                        break;
                }
            } else {
                syncUpResult(ActionResult.RESULT_FAILED);
                onEndSync();
                KLog.d(TAG, "sync service on post execute result param is null or is not available");
            }
        }
    }
}
