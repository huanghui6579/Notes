package com.yunxinlink.notes.sync.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.impl.NoteApi;
import com.yunxinlink.notes.api.model.NoteParam;
import com.yunxinlink.notes.listener.SimpleOnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.TaskParam;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.sync.SyncCache;
import com.yunxinlink.notes.sync.SyncData;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.List;
import java.util.Map;

import static com.yunxinlink.notes.lockpattern.Alp.TAG;

/**
 * 同步数据的服务
 * @author huanghui1
 * @update 2016/10/12 17:56
 * @version: 1.0.0
 */
public class SyncService extends Service {
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

            param.data = syncSid;
            //开始同步
            KLog.d(TAG, "sync service start sync task sid is :" + syncSid);
            executeTask(param);
        } else {
            KLog.d(TAG, "sync service not start sync task because sid is null");
        }
    }

    /**
     * 执行后台任务
     * @param param
     */
    private void executeTask(TaskParam param) {
        new SyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, param);
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
        if (syncData == null || syncData.isSyncing()
                || !syncData.hasSyncData() || !(syncData.getSyncable() instanceof NoteParam)) {
            KLog.d(TAG, "sync service sync up note info but sync data is null or is syncing or not has sync data:" + syncData);
        } else {
            NoteParam noteParam = (NoteParam) syncData.getSyncable();
            Folder folder = noteParam.getFolder();
            List<DetailNoteInfo> detailNoteInfos = noteParam.getDetailNoteInfos();
            try {
                resultParam.optType = Constants.SYNC_UP_NOTE;
                //该方法是同步的，所以回调也是在同一个线程里
                NoteApi.syncUpNote(mContext, folder, detailNoteInfos, new SimpleOnLoadCompletedListener<ActionResult<Void>>() {
                    @Override
                    public void onLoadSuccess(ActionResult<Void> result) {
                        super.onLoadSuccess(result);
                        resultParam.code = result.getResultCode();
                    }

                    @Override
                    public void onLoadFailed(int errorCode, String reason) {
                        super.onLoadFailed(errorCode, reason);
                        resultParam.code = errorCode;
                    }
                });
            } catch (Exception e) {
                KLog.e(TAG, "sync service sync up note info error:" + e.getMessage());
            }
            //移除该同步记录
            SyncCache.getInstance().remove(syncSid);
        }
        return resultParam;
    }

    /**
     * 检测该同步任务的状态
     * @param syncSid 同步任务的id
     * @return true:可以继续执行该同步任务，false：该任务已经在执行了
     */
    private boolean checkSyncState(String syncSid) {
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
            KLog.d(TAG, "sync service sync down note user is null or user is not available user:" + user);
            return null;
        }

        if (!checkSyncState(syncSid)) {
            return null;
        }

        final TaskParam resultParam = new TaskParam();
        resultParam.optType = Constants.SYNC_DOWN_NOTE;
        //同步笔记本
        syncDownFolder(user);

        SyncCache.getInstance().remove(syncSid);
        return resultParam;
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
            if (param != null && param.isAvailable()) { //返回的结果可用
                switch (param.optType) {
                    case Constants.SYNC_UP_NOTE:    //向上同步笔记
                        syncUpResult(param.code);
                        break;
                }
            } else {
                KLog.d(TAG, "sync service on post execute result param is null or is not available");
            }
        }
    }
}
