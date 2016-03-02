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
     * 文本内容
     */
    private String content;

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
