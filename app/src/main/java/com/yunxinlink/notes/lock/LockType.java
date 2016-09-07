package com.yunxinlink.notes.lock;

/**
 * 密码锁的类型
 * @author huanghui1
 * @update 2016/9/7 16:58
 * @version: 0.0.1
 */
public enum LockType {
    PATTERN(1),
    DIGITAL(2);
    
    private int type;
    
    LockType(int type) {
        this.type = type;
    }
    
    public int getType() {
        return type;
    }
    
    public static LockType valueOf(int type) {
        switch (type) {
            case 1:
                return PATTERN;
            case 2:
                return DIGITAL;
        }
        return null;
    }
}
