package net.ibaixin.notes.model;

/**
 * 记事本的基本信息实体
 * @author huanghui1
 * @update 2016/2/26 14:49
 * @version: 0.0.1
 */
public class NoteInfo {
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
    private NoteKind mKind;


    /**
     * 摘要
     */
    private String summary;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 修改时间
     */
    private long modifyTime;
    
    /**
     * 笔记的类型，主要分为文本笔记和清单笔记
     * @author tiger
     * @update 2016/3/7 21:44
     * @version 1.0.0
     */
    enum NoteKind {
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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
}
