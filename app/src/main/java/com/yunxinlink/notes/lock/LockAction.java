package com.yunxinlink.notes.lock;

/**
 * @author huanghui1
 * @update 2016/9/8 9:58
 * @version: 0.0.1
 */
public class LockAction {
    public String action;
    
    public Class<?> clazz;

    @Override
    public String toString() {
        return "LockAction{" +
                "action='" + action + '\'' +
                ", clazz=" + clazz +
                '}';
    }
}
