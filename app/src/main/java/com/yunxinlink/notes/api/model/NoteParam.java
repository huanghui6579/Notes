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

    @Override
    public String toString() {
        return "NoteParam{" +
                "detailNoteInfos=" + detailNoteInfos +
                ", folder=" + folder +
                ", userSid='" + userSid + '\'' +
                '}';
    }
}
