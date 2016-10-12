package com.yunxinlink.notes.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.yunxinlink.notes.util.Constants;

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
            switch (syncType) {
                case Constants.SYNC_UP_NOTE:    //向服务器上传笔记信息
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
