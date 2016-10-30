package com.yunxinlink.notes.api.model;

import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.DetailList;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.SyncState;

/**
 * 对应服务器的DetailList
 * @author huanghui1
 * @update 2016/10/13 9:44
 * @version: 0.0.1
 */
public class DetailListDto {
    /**
     * 主键
     */
    private int id;

    /**
     * sid
     */
    private String sid;

    /**
     * 标题
     */
    private String title;

    /**
     * 笔记的sid,关联笔记表
     */
    private String noteSid;

    /**
     * 排序
     */
    private int sort;

    /**
     * 是否选中，true：选中
     */
    private Boolean checked;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 最后修改时间
     */
    private long modifyTime;

    /**
     * 删除的状态
     */
    private int deleteState;

    /**
     * 该清单的hash，主要用来检测更新
     */
    private String hash;

    public int getDeleteState() {
        return deleteState;
    }

    public void setDeleteState(int deleteState) {
        this.deleteState = deleteState;
    }

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNoteSid() {
        return noteSid;
    }

    public void setNoteSid(String noteSid) {
        this.noteSid = noteSid;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
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

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * 转换成清单
     * @param noteInfo 笔记信息
     * @return 清单
     */
    public DetailList convert2DetailList(NoteInfo noteInfo) {
        return convert2DetailList(noteInfo.getSid());
    }

    /**
     * 转换成清单
     * @param noteSid 笔记的sid
     * @return 清单
     */
    public DetailList convert2DetailList(String noteSid) {
        DetailList detailList = new DetailList();
        detailList.setSid(sid);
        detailList.setTitle(title);
        detailList.setNoteId(noteSid);
        detailList.setDeleteState(DeleteState.valueOf(deleteState));
        detailList.setChecked(checked);
        detailList.setCreateTime(createTime);
        detailList.setModifyTime(modifyTime);
        detailList.setSort(sort);
        detailList.setOldSort(sort);
        detailList.setSyncState(SyncState.SYNC_DONE);
        return detailList;
    }
}
