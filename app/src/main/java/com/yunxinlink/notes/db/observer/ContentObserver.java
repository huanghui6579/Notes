package com.yunxinlink.notes.db.observer;

import android.os.Handler;

/**
 * @author huanghui1
 * @update 2016/3/9 16:59
 * @version: 0.0.1
 */
public abstract class ContentObserver implements Observer {
    private Handler mHandler;

    public ContentObserver(Handler handler) {
        this.mHandler = handler;
    }

    class NotificationRunnable implements Runnable {
        private Observable<?> observable;
        private int notifyFlag;
        private NotifyType notifyType;
        private Object data;

        public NotificationRunnable(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            super();
            this.observable = observable;
            this.notifyFlag = notifyFlag;
            this.notifyType = notifyType;
            this.data = data;
        }

        @Override
        public void run() {
            update(observable, notifyFlag, notifyType, data);
        }

    }

    @Override
    public void dispatchUpdate(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
        if (mHandler != null) {
            mHandler.post(new NotificationRunnable(observable, notifyFlag, notifyType, data));
        } else {
            update(observable, notifyFlag, notifyType, data);
        }
    }
}
