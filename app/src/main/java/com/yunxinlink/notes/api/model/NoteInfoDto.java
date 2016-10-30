package com.yunxinlink.notes.api.model;

import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.DetailList;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.richtext.AttachText;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对应服务器的NoteInfo
 * @author huanghui1
 * @update 2016/10/13 9:37
 * @version: 0.0.1
 */
public class NoteInfoDto {
    private int id;

    /**
     * 实际唯一标识
     */
    private String sid;

    /**
     * 对应的用户id
     */
    private int userId;

    /**
     * 笔记的标题
     */
    private String title;

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
     * 文件夹的sid
     */
    private String folderSid;

    /**
     * 笔记的类型，主要分为{@link com.yunxinlink.notes.model.NoteInfo.NoteKind#TEXT}和{@link com.yunxinlink.notes.model.NoteInfo.NoteKind#DETAILED_LIST}
     */
    private int kind;

    /**
     * 删除的状态
     * @see com.yunxinlink.notes.model.DeleteState
     */
    private int deleteState;

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
     * 附件列表
     */
    private List<AttachDto> attachs;

    /**
     * 清单的列表
     */
    private List<DetailListDto> details;

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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getFolderSid() {
        return folderSid;
    }

    public void setFolderSid(String folderSid) {
        this.folderSid = folderSid;
    }

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public int getDeleteState() {
        return deleteState;
    }

    public void setDeleteState(int deleteState) {
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

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<AttachDto> getAttachs() {
        return attachs;
    }

    public void setAttachs(List<AttachDto> attachs) {
        this.attachs = attachs;
    }

    public List<DetailListDto> getDetails() {
        return details;
    }

    public void setDetails(List<DetailListDto> details) {
        this.details = details;
    }

    /**
     * 是否是清单类笔记
     * @return
     */
    public boolean isDetailListNote() {
        return kind == NoteInfo.NoteKind.DETAILED_LIST.ordinal();
    }
    
    /**
     * 转换成笔记
     * @return
     */
    public DetailNoteInfo convert2NoteInfo(User user) {
        DetailNoteInfo detailNoteInfo = new DetailNoteInfo();

        int userId = user.getId();
        
        boolean hasAttach = attachs != null && attachs.size() > 0;
        
        NoteInfo noteInfo = new NoteInfo();
        noteInfo.setSid(sid);
        noteInfo.setUserId(userId);
        noteInfo.setHash(hash);
        noteInfo.setModifyTime(modifyTime);
        noteInfo.setSyncState(SyncState.SYNC_DONE);
        noteInfo.setContent(content);
        noteInfo.setCreateTime(createTime);
        noteInfo.setDeleteState(DeleteState.valueOf(deleteState));
        noteInfo.setFolderId(folderSid);
        noteInfo.setHasAttach(hasAttach);
        noteInfo.setKind(NoteInfo.NoteKind.valueOf(kind));
        noteInfo.setRemindId(remindId);
        noteInfo.setRemindTime(remindTime);
        noteInfo.setTitle(title);

        AttachText attachText = SystemUtil.getAttachSids(content);
        if (attachText != null) {
            noteInfo.setShowContent(attachText.getText());
        }

        if (hasAttach) {
            Map<String, Attach> attachesMap = new HashMap<>();
            Attach lastAttach = null;
            for (AttachDto attachDto : attachs) {
                Attach attach = attachDto.convert2Attach(noteInfo);
                lastAttach = attach;
                attachesMap.put(attach.getSid(), attach);
            }
            detailNoteInfo.setLastAttach(lastAttach);
            noteInfo.setAttaches(attachesMap);
        }

        detailNoteInfo.setNoteInfo(noteInfo);
        
        if (noteInfo.isDetailNote() && !SystemUtil.isEmpty(details)) {  //清单笔记,且有清单
            List<DetailList> detailLists = new ArrayList<>();
            for (DetailListDto detailListDto : details) {
                DetailList detailList = detailListDto.convert2DetailList(noteInfo);
                detailLists.add(detailList);
            }
            
            detailNoteInfo.setDetailList(detailLists);
        }
        
        return detailNoteInfo;
    }
}
