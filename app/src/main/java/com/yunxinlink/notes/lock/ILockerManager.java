package com.yunxinlink.notes.lock;

import com.yunxinlink.notes.lock.ui.LockDigitalActivity;
import com.yunxinlink.notes.lock.ui.LockPatternActivity;

/**
 * @author huanghui1
 * @update 2016/9/7 15:50
 * @version: 0.0.1
 */
public interface ILockerManager {

    enum LockerActivityAction {
        LOCKER_ACTIVITY_DIGITAL(LockDigitalActivity.LOCK_ACTION),
        LOCKER_ACTIVITY_PATTERN(LockPatternActivity.LOCK_ACTION);

        private String action;

        private LockerActivityAction(String action) {
            this.action = action;
        }

        public String getAction() {
            return action;
        }

        public String toString() {
            return action;
        }
    }
    
    /**
     * 是否已锁定了
     * @return
     */
    boolean hasLock();


    /**
     * 是否解锁中
     * @return
     */
    boolean isBeingLocked();

    /**
     * 解锁
     */
    void unlock();

    /**
     * 锁定
     */
    void lock();

    /**
     * 获取不同解锁方式的解锁界面
     * @return
     */
    String acquireLockerActivityAction();
}
