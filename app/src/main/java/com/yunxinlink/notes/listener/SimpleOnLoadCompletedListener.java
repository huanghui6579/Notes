package com.yunxinlink.notes.listener;

import com.socks.library.KLog;

/**
 * @author huanghui1
 * @update 2016/9/20 16:02
 * @version: 0.0.1
 */
public class SimpleOnLoadCompletedListener<T> implements OnLoadCompletedListener<T> {
    private static final String TAG = "SimpleOnLoadCompletedListener";
    
    @Override
    public void onLoadSuccess(T result) {
        KLog.w(TAG, "SimpleOnLoadCompletedListener onLoadSuccess:" + result);
    }

    @Override
    public void onLoadFailed(String reason) {
        KLog.w(TAG, "SimpleOnLoadCompletedListener onLoadFailed:" + reason);
    }
}
