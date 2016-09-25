package com.yunxinlink.notes.ui;

import android.os.Handler;

import java.lang.ref.WeakReference;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/9/24 17:20
 */
public class BaseHandler<T> extends Handler {
    protected WeakReference<T> mTarget;

    public BaseHandler(T target) {
        mTarget = new WeakReference<>(target);
    }

    public T getTarget() {
        return mTarget.get();
    }
}
