package com.yunxinlink.notes.api.model;

import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.util.FileUtil;

/**
 * 对应服务器的Attach
 * @author huanghui1
 * @update 2016/10/13 9:41
 * @version: 0.0.1
 */
public class AttachDto {
    /**
     * 主键
     */
    private int id;

    /**
     * sid
     */
    private String sid;

    /**
     * 笔记的sid
     */
    private String noteSid;

    /**
     * 用户的id
     */
    private int userId;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件类型
     */
    private int type;

    /**
     * 本地的存储路径，全路径
     */
    private String localPath;

    /**
     * 文件的描述
     */
    private String description;

    /**
     * 服务器的全路径
     */
    private String serverPath;

    /**
     * 删除状态
     */
    private int deleteState;

    /**
     * 文件的创建时间
     */
    private long createTime;

    /**
     * 文件的修改时间
     */
    private long modifyTime;

    /**
     * 文件的大小
     */
    private long size;

    /**
     * 文件的mime类型
     */
    private String mimeType;

    /**
     * 文件的MD5值
     */
    private String hash;

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

    public String getNoteSid() {
        return noteSid;
    }

    public void setNoteSid(String noteSid) {
        this.noteSid = noteSid;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServerPath() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * 转换成附件
     * @param noteInfo 附件所属的笔记
     * @return 返回转换后的附件信息
     */
    public Attach convert2Attach(NoteInfo noteInfo) {
        Attach attach = new Attach();
        attach.setSid(sid);
        attach.setModifyTime(modifyTime);
        attach.setCreateTime(createTime);
        attach.setHash(hash);
        attach.setDeleteState(DeleteState.valueOf(deleteState));
        attach.setDescription(description);
        attach.setFilename(filename);
        attach.setLocalPath(localPath);
        attach.setSize(size);
        if (FileUtil.isFileExists(localPath)) {
            attach.setSyncState(SyncState.SYNC_DONE);
        } else {//需要下载附件
            attach.setSyncState(SyncState.SYNC_DOWN);
        }
        attach.setMimeType(mimeType);
        attach.setUserId(noteInfo.getUserId());
        attach.setNoteId(noteInfo.getSid());
        attach.setType(type);
        
        return attach;
    }
}
