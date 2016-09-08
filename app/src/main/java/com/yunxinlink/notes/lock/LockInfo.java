package com.yunxinlink.notes.lock;

/**
 * 密码锁的基本属性
 * @author huanghui1
 * @update 2016/9/8 17:04
 * @version: 0.0.1
 */
public class LockInfo {
    /**
     * 是否有锁
     */
    private boolean hasLock;

    /**
     * 是否被锁定
     */
    private boolean isLocking;

    /**
     * 密码锁的类型
     */
    private LockType lockType;

    public boolean hasLock() {
        return hasLock;
    }

    public void setHasLock(boolean hasLock) {
        this.hasLock = hasLock;
    }

    public boolean isLocking() {
        return isLocking;
    }

    public void setLocking(boolean locking) {
        isLocking = locking;
    }

    public LockType getLockType() {
        return lockType;
    }

    public void setLockType(LockType lockType) {
        this.lockType = lockType;
    }

    /**
     * 密码锁是否可用
     * @return
     */
    public boolean isLockAvailable() {
        return hasLock && lockType != null;
    }

    @Override
    public String toString() {
        return "LockInfo{" +
                "lockType=" + lockType +
                ", isLocking=" + isLocking +
                ", hasLock=" + hasLock +
                '}';
    }
}
