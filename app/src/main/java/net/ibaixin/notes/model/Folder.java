package net.ibaixin.notes.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 笔记的分类文件夹
 * @author huanghui1
 * @update 2016/2/24 18:23
 * @version: 0.0.1
 */
public class Folder implements Parcelable {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Folder folder = (Folder) o;

        return id == folder.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
    
    public Folder() {}

    public Folder(Parcel in) {
        id = in.readInt();
        sId = in.readString();
        userId = in.readInt();
        name = in.readString();
        isLock = in.readByte() != 0;
        sort = in.readInt();
        createTime = in.readLong();
        modifyTime = in.readLong();
        count = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(sId);
        dest.writeInt(userId);
        dest.writeString(name);
        dest.writeByte((byte) (isLock ? 1 : 0));
        dest.writeInt(sort);
        dest.writeLong(createTime);
        dest.writeLong(modifyTime);
        dest.writeInt(count);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Folder> CREATOR = new Creator<Folder>() {
        @Override
        public Folder createFromParcel(Parcel in) {
            return new Folder(in);
        }

        @Override
        public Folder[] newArray(int size) {
            return new Folder[size];
        }
    };

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
