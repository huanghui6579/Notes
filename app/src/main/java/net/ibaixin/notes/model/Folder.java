package net.ibaixin.notes.model;

/**
 * 笔记的分类文件夹
 * @author huanghui1
 * @update 2016/2/24 18:23
 * @version: 0.0.1
 */
public class Folder {
    private int id;

    /**
     * 实际的主键
     */
    private String sId;

    /**
     * 用户的id
     */
    private int userId;

    /**
     * 是否是默认的文件夹
     */
    private boolean isDefault;

    /**
     * 文件夹的名称
     */
    private String name;

    /**
     * 是否被隐藏
     */
    private boolean isHidden;

    /**
     * 是否被锁定
     */
    private boolean isLock;

    /**
     * 排序
     */
    private int sort;

    /**
     * 同步的状态
     */
    private SyncState syncState;

    /**
     * 删除的状态
     */
    private DeleteState deleteState;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSId() {
        return sId;
    }

    public void setSId(String sId) {
        this.sId = sId;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setIsHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public boolean isLock() {
        return isLock;
    }

    public void setIsLock(boolean isLock) {
        this.isLock = isLock;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public SyncState getSyncState() {
        return syncState;
    }

    public void setSyncState(SyncState syncState) {
        this.syncState = syncState;
    }

    public DeleteState getDeleteState() {
        return deleteState;
    }

    public void setDeleteState(DeleteState deleteState) {
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
