package com.yunxinlink.notes.listener;

/**
 * 数据加载的回调
 * @author huanghui1
 * @update 2016/9/20 15:13
 * @version: 0.0.1
 */
public interface OnLoadCompletedListener<T> {
    /**
     * 加载成功
     * @param result
     */
    void onLoadSuccess(T result);

    /**
     * 加载失败
     * @param errorCode
     * @param reason
     */
    void onLoadFailed(int errorCode, String reason);
}
