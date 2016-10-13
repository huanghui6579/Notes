package com.yunxinlink.notes.api.model;

import java.util.List;

/**
 * 不同于{@link NoteInfoDto},这个是对应服务器的NoteDto
 * @author huanghui1
 * @update 2016/10/13 9:45
 * @version: 0.0.1
 */
public class NoteDto {
    /**
     * 该笔记本下的笔记
     */
    private List<NoteInfoDto> noteInfos;
    
    /**
     * 笔记本
     */
    private FolderDto folder;

    private String userSid;

    public List<NoteInfoDto> getNoteInfos() {
        return noteInfos;
    }

    public void setNoteInfos(List<NoteInfoDto> noteInfos) {
        this.noteInfos = noteInfos;
    }

    public FolderDto getFolder() {
        return folder;
    }

    public void setFolder(FolderDto folder) {
        this.folder = folder;
    }

    public String getUserSid() {
        return userSid;
    }

    public void setUserSid(String userSid) {
        this.userSid = userSid;
    }
}
