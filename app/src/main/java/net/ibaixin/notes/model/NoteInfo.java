package net.ibaixin.notes.model;

import java.util.Comparator;

/**
 * 记事本的基本信息实体
 * @author huanghui1
 * @update 2016/2/26 14:49
 * @version: 0.0.1
 */
public class NoteInfo implements Comparator<NoteInfo> {
    private int id;

    /**
     * 实际唯一标识
     */
    private String sId;

    /**
     * 对应的用户id
     */
    private int userId;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 提醒的id
     */
    private int remindId;

    /**
     * 提醒的时间
     */
    private long remindTime;

    /**
     * 文件夹的id
     */
    private String folderId;

    /**
     * 笔记的类型，主要分为{@link net.ibaixin.notes.model.NoteInfo.NoteKind#TEXT}和{@link net.ibaixin.notes.model.NoteInfo.NoteKind#DETAILED_LIST}
     */
    private NoteKind kind;

    /**
     * 同步的状态
     */
    private SyncState syncState;

    /**
     * 删除的状态
     */
    private DeleteState deleteState;

    /**
     * 是否有附件
     */
    private boolean hasAttach;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 修改时间
     */
    private long modifyTime;

    /**
     * 文本内容的hash
     */
    private String hash;

    /**
     * 前一版本的文本内容
     */
    private String oldContent;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NoteInfo noteInfo = (NoteInfo) o;

        return id == noteInfo.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compare(NoteInfo lhs, NoteInfo rhs) {
        int lId = lhs.getId();
        int rId = rhs.getId();
        if (lId > rId) {
            return 1;
        } else if (lId < rId) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * 笔记的类型，主要分为文本笔记和清单笔记
     * @author tiger
     * @update 2016/3/7 21:44
     * @version 1.0.0
     */
    public enum NoteKind {
        /**
         * 文本笔记
         */
        TEXT,
        /**
         * 清单笔记
         */
        DETAILED_LIST;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getSId() {
        return sId;
    }

    public void setSId(String sid) {
        this.sId = sid;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRemindId() {
        return remindId;
    }

    public void setRemindId(int remindId) {
        this.remindId = remindId;
    }

    public long getRemindTime() {
        return remindTime;
    }

    public void setRemindTime(long remindTime) {
        this.remindTime = remindTime;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public NoteKind getKind() {
        return kind;
    }

    public void setKind(NoteKind kind) {
        this.kind = kind;
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

    public boolean hasAttach() {
        return hasAttach;
    }

    public void setHasAttach(boolean hasAttach) {
        this.hasAttach = hasAttach;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getOldContent() {
        return oldContent;
    }

    public void setOldContent(String oldContent) {
        this.oldContent = oldContent;
    }
}
