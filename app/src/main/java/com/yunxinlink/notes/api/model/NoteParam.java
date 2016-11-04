package com.yunxinlink.notes.api.model;

import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.sync.Syncable;

import java.util.List;

/**
 * 笔记同步的参数，与{@link NoteDto}相似
 * @author huanghui1
 * @update 2016/10/14 10:40
 * @version: 0.0.1
 */
public class NoteParam implements Syncable {
    /**
     * 同步笔记的内容，包括笔记的状态和其他内容
     */
    public static final int SYNC_CONTENT = 0;
    /**
     * 只同步笔记的状态
     */
    public static final int SYNC_STATE = 1;

    /**
     * 同步的范围，
     */
    private int syncScope = SYNC_CONTENT;
    
    /**
     * 该笔记本下的笔记
     */
    private List<DetailNoteInfo> detailNoteInfos;

    /**
     * 笔记本
     */
    private Folder folder;

    private String userSid;

    public List<DetailNoteInfo> getDetailNoteInfos() {
        return detailNoteInfos;
    }

    public void setDetailNoteInfos(List<DetailNoteInfo> detailNoteInfos) {
        this.detailNoteInfos = detailNoteInfos;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public String getUserSid() {
        return userSid;
    }

    public void setUserSid(String userSid) {
        this.userSid = userSid;
    }

    public int getSyncScope() {
        return syncScope;
    }

    public void setSyncScope(int syncScope) {
        this.syncScope = syncScope;
    }

    @Override
    public String toString() {
        return "NoteParam{" +
                "syncScope=" + syncScope +
                ", detailNoteInfos=" + detailNoteInfos +
                ", folder=" + folder +
                ", userSid='" + userSid + '\'' +
                '}';
    }
}
