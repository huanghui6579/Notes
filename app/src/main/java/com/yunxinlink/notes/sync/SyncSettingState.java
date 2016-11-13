package com.yunxinlink.notes.sync;

/**
 * 同步设置的状态，主要是可以同步、不能同步、给出同步的提示并按提示操作
 * @author huanghui-iri
 * @update 2016/11/11 9:43
 * @version: 0.0.1
 */
public enum SyncSettingState {
    /**
     * 网络不可用
     */
    NET_DISABLE,
    /**
     * 可以同步
     */
    ENABLE,

    /**
     * 不能同步
     */
    DISABLE,

    /**
     * 给出同步的提示并按提示操作
     */
    PROMPT
}
