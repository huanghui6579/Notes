package com.yunxinlink.notes.model;

/**
 * 后台任务的参数
 * @author huanghui1
 * @update 2016/9/29 10:38
 * @version: 0.0.1
 */
public class TaskParam {
    /**
     * 操作类型
     */
    public int optType;

    /**
     * 结果编码
     */
    public int code;

    /**
     * 数据
     */
    public Object data;

    /**
     * 是否有效
     * @return
     */
    public boolean isAvailable() {
        return code != 0 && data != null;
    }
}
