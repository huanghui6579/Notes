package com.yunxinlink.notes.sync;

/**
 * 同步的数据
 * @author huanghui1
 * @update 2016/10/13 18:03
 * @version: 0.0.1
 */
public class SyncData {
    public static final int SYNC_NONE = 0;
    public static final int SYNC_ING = 1;
    public static final int SYNC_DONE = 2;
    /**
     * 同步的状态，主要是未同步0，1：同步中，2：同步结束
     */
    private int state;
    
    private Syncable syncable;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Syncable getSyncable() {
        return syncable;
    }

    public void setSyncable(Syncable syncable) {
        this.syncable = syncable;
    }

    /**
     * 是否在同步中
     * @return
     */
    public boolean isSyncing() {
        return state == SYNC_ING;
    }
}
