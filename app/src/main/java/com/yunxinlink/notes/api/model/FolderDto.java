package com.yunxinlink.notes.api.model;

/**
 * 对应服务器的Folder
 * @author huanghui1
 * @update 2016/10/13 9:40
 * @version: 0.0.1
 */
public class FolderDto {
    private int id;

    /**
     * 实际的主键
     */
    private String sid;

    /**
     * 用户的id
     */
    private int userId;

    /**
     * 文件夹的名称
     */
    private String name;

    /**
     * 是否被锁定
     */
    private boolean isLock;

    /**
     * 排序
     */
    private int sort;

    /**
     * 删除的状态
     */
    private int deleteState;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 修改时间
     */
    private long modifyTime;

    /**
     * 该文件夹下笔记的数量
     */
    private int count;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLock() {
        return isLock;
    }

    public void setLock(boolean lock) {
        isLock = lock;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public int getDeleteState() {
        return deleteState;
    }

    public void setDeleteState(int deleteState) {
        this.deleteState = deleteState;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(long modifyTime) {
        this.modifyTime = modifyTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
