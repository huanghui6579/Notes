package com.yunxinlink.notes.sync.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.api.impl.NoteApi;
import com.yunxinlink.notes.api.model.NoteParam;
import com.yunxinlink.notes.listener.SimpleOnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.TaskParam;
import com.yunxinlink.notes.sync.SyncCache;
import com.yunxinlink.notes.sync.SyncData;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.List;

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
            String sid = null;
            switch (syncType) {
                case Constants.SYNC_UP_NOTE:    //向服务器上传笔记信息
                    sid = intent.getStringExtra(Constants.ARG_CORE_OBJ);
                    KLog.d(TAG, "sync service will start sync up note info sid : " + sid);
                    if (!TextUtils.isEmpty(sid)) {
                        param.data = sid;
                        //开始同步
                        KLog.d(TAG, "sync service start sync up sid is :" + sid);
                        executeTask(param);
                    } else {
                        KLog.d(TAG, "sync service not start sync up because sid is null");
                    }
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
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
     * 同步的后台任务
     */
    class SyncTask extends AsyncTask<TaskParam, Void, TaskParam> {

        @Override
        protected TaskParam doInBackground(TaskParam... params) {
            if (params == null || params.length == 0) {
                return null;
            }
            TaskParam param = params[0];
            String sid = (String) param.data;
            if (TextUtils.isEmpty(sid)) {
                KLog.d(TAG, "sync service sync task sid is null");
                return null;
            }
            KLog.d(TAG, "sync service sync task do in background sid :" + sid);
            final TaskParam resultParam = new TaskParam();
            int syncType = param.optType;
            switch (syncType) {
                case Constants.SYNC_UP_NOTE:    //向上同步笔记信息
                    SyncData syncData = SyncCache.getInstance().getSyncData(sid);
                    if (syncData == null || syncData.isSyncing() 
                            || !syncData.hasSyncData() || !(syncData.getSyncable() instanceof NoteParam)) {
                        KLog.d(TAG, "sync service sync up note info but sync data is null or is syncing or not has sync data:" + syncData);
                    } else {
                        NoteParam noteParam = (NoteParam) syncData.getSyncable();
                        Folder folder = noteParam.getFolder();
                        List<DetailNoteInfo> detailNoteInfos = noteParam.getDetailNoteInfos();
                        try {
                            resultParam.optType = syncType;
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
                    }
                    break;
            }
            return resultParam;
        }

        @Override
        protected void onPostExecute(TaskParam param) {
            if (param != null && param.isAvailable()) { //返回的结果可用
                switch (param.optType) {
                    case Constants.SYNC_UP_NOTE:    //向上同步笔记
                        switch (param.code) {
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
                        break;
                }
            } else {
                KLog.d(TAG, "sync service on post execute result param is null or is not available");
            }
        }
    }
}
