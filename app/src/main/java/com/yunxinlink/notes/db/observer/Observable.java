package com.yunxinlink.notes.db.observer;

import java.util.LinkedList;
import java.util.List;

/**
 * 数据库的观察者模式
 * @author huanghui1
 * @update 2016/3/9 16:26
 * @version: 0.0.1
 */
public abstract class Observable<T> {
    protected final List<T> mObservers = new LinkedList<>();
    
    /**
     * 添加观察者
     * @author huanghui1
     * @update 2016/3/9 16:35
     * @version: 1.0.0
     */
    public void addObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized (mObservers) {
            if (mObservers.contains(observer)) {
                throw new IllegalStateException("Observer " + observer + " is already registered.");
            }
            mObservers.add(observer);
        }
    }
    
    /**
     * 移除观察者
     * @author huanghui1
     * @update 2016/3/9 16:36
     * @version: 1.0.0
     */
    public void removeObserver(T observer) {
        if (observer == null) {
            throw new IllegalArgumentException("The observer is null.");
        }
        synchronized (mObservers) {
            int index = mObservers.indexOf(observer);
            if (index == -1) {
                throw new IllegalStateException("Observer " + observer + " was not registered.");
            }
            mObservers.remove(index);
        }
    }
    
    /**
     * 清空观察者
     * @author huanghui1
     * @update 2016/3/9 16:37
     * @version: 1.0.0
     */
    public void removeObservers() {
        synchronized (mObservers) {
            mObservers.clear();
        }
    }

    /**
     * 通知所有的观察者
     * @author huanghui1
     * @update 2016/3/9 16:49
     * @version: 1.0.0
     */
    public void notifyObservers(int notifyFlag, Observer.NotifyType notifyType) {
        notifyObservers(notifyFlag, notifyType, null);
    }

    /**
     * 通知所有的观察者
     * @author huanghui1
     * @update 2016/3/9 16:48
     * @version: 1.0.0
     */
    public void notifyObservers(int notifyFlag, Observer.NotifyType notifyType, Object data) {
        int size = 0;
        Observer[] arrays = null;
        synchronized (this) {
            size = mObservers.size();
            arrays = new Observer[size];
            mObservers.toArray(arrays);
        }
        for (Observer observer : arrays) {
            observer.dispatchUpdate(this, notifyFlag, notifyType, data);
        }
    }
}
