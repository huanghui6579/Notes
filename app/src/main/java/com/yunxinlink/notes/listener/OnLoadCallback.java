package com.yunxinlink.notes.listener;

import android.os.Bundle;

/**
 * 数据加载完成的回调
 * @author huanghui1
 * @update 2016/9/15 9:45
 * @version: 0.0.1
 */
public interface OnLoadCallback<T> {
    /**
     * 数据加载完成的回调方法
     * @param data 加载的数据
     * @param args 参数
     */
    void onLoadCompleted(T data, Bundle args);
}
