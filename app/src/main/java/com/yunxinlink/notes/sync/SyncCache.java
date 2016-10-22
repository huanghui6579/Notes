package com.yunxinlink.notes.sync;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步的数据链
 * @author huanghui1
 * @update 2016/10/13 17:03
 * @version: 0.0.1
 */
public class SyncCache {
    
    private static final String TAG = "SyncCache";
    
    private static SyncCache mInstance;
    
    private Map<String, SyncData> mMap;
    
    private SyncCache() {
        mMap = new HashMap<>();
    }
    
    public synchronized static SyncCache getInstance() {
        if (mInstance == null) {
            mInstance = new SyncCache();
        }
        return mInstance;
    }

    /**
     * 添加同步数据
     * @param sid 同步的sid
     * @param syncData
     */
    private void add(String sid, SyncData syncData) {
        mMap.put(sid, syncData);
    }

    /**
     * 移除
     * @param sid
     */
    public synchronized void remove(String sid) {
        mMap.remove(sid);
    }

    /**
     * 添加或更新同步数据
     * @param sid
     * @param syncData
     * @return
     */
    public synchronized String addOrUpdate(String sid, SyncData syncData) {
        //检查是否已经有存在的任务了
        SyncData oldSyncData = getSyncData(sid);
        if (oldSyncData != null) {  //存在了
            boolean hasSync = oldSyncData.isSyncing();
            if (hasSync) {  //则重新添加
                sid = SystemUtil.generateSid();
                KLog.d(TAG, "sync cache add sync data old is exists and is syncing so new sid is:" + sid);
            } else {
                KLog.d(TAG, "sync cache add sync data old is exists and is not syncing sid is:" + sid);
            }
        } else {    //不存在，则添加
            KLog.d(TAG, "sync cache add sync data old is not exists sid is:" + sid);
        }
        add(sid, syncData);
        return sid;
    }

    /**
     * 是否同步中
     * @param sid
     * @return
     */
    public boolean isSyncing(String sid) {
        SyncData syncData = mMap.get(sid);
        return syncData != null && syncData.getSyncable() != null && syncData.isSyncing();
    }

    /**
     * 获取同步的数据
     * @param sid
     * @return
     */
    public SyncData getSyncData(String sid) {
        return mMap.get(sid);
    }

    /**
     * 清空
     */
    public synchronized void clear() {
        mMap.clear();
    }

    /**
     * 队列中是否为空
     * @return
     */
    public boolean isEmpty() {
        return mMap.isEmpty();
    }
}
