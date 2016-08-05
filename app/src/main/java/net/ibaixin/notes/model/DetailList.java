package net.ibaixin.notes.model;

import android.text.TextUtils;

import net.ibaixin.notes.util.SystemUtil;

import java.util.Comparator;

/**
 * 清单实体
 * @author huanghui1
 * @update 2016/7/28 20:25
 * @version: 0.0.1
 */
public class DetailList implements Comparator<DetailList> {
    /**
     * 主键
     */
    private int id;

    /**
     * sid
     */
    private String sId;

    /**
     * 标题
     */
    private String title;

    /**
     * 老的标题
     */
    private String oldTitle;

    /**
     * 笔记的sid,关联笔记表
     */
    private String noteId;

    /**
     * 排序
     */
    private int sort;

    /**
     * 上一次的排序
     */
    private int oldSort;

    /**
     * 是否选中，true：选中
     */
    private boolean checked;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 最后修改时间
     */
    private long modifyTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSId() {
        return sId;
    }

    public void setSId(String sId) {
        this.sId = sId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOldTitle() {
        return oldTitle;
    }

    public void setOldTitle(String oldTitle) {
        this.oldTitle = oldTitle;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
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

    public int getOldSort() {
        return oldSort;
    }

    public void setOldSort(int oldSort) {
        this.oldSort = oldSort;
    }

    /**
     * 会自动生成sid
     */
    public DetailList() {
        this.sId = SystemUtil.generateDetailSid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DetailList that = (DetailList) o;

        return sId.equals(that.sId);

    }

    @Override
    public int hashCode() {
        return sId.hashCode();
    }

    /**
     * 清单内容是否为空
     * @return
     */
    public boolean isEmptyText() {
        return TextUtils.isEmpty(title);
    }

    @Override
    public String toString() {
        return "DetailList{" +
                "sort=" + sort +
                ", oldSort=" + oldSort +
                ", title='" + title + '\'' +
                '}';
    }

    @Override
    public int compare(DetailList lhs, DetailList rhs) {
        if (lhs.sort > rhs.sort) {
            return 1;
        } else if (lhs.sort < rhs.sort) {
            return -1;
        } else {
            return 0;
        }
    }
}
