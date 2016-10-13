package com.yunxinlink.notes.sync.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.model.TaskParam;
import com.yunxinlink.notes.util.Constants;

import static com.yunxinlink.notes.lockpattern.Alp.TAG;

/**
 * 同步数据的服务
 * @author huanghui1
 * @update 2016/10/12 17:56
 * @version: 1.0.0
 */
public class SyncService extends Service {
    
    public SyncService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int syncType = intent.getIntExtra(Constants.ARG_CORE_OPT, 0);
            TaskParam param = new TaskParam();
            param.optType = syncType;
            String sid = null;
            switch (syncType) {
                case Constants.SYNC_UP_NOTE:    //向服务器上传笔记信息
                    sid = intent.getStringExtra(Constants.ARG_CORE_OBJ);
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
            return null;
        }
    }
}
