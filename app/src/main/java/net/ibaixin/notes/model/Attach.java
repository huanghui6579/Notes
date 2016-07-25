package net.ibaixin.notes.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.nostra13.universalimageloader.core.download.ImageDownloader;

/**
 * 附件
 * @author huanghui1
 * @update 2016/7/6 16:01
 * @version: 0.0.1
 */
public class Attach implements Parcelable {
    /**
     * 附件类型为图片
     */
    public static final int IMAGE = 1;
    /**
     * 附件类型为语音
     */
    public static final int VOICE = 2;

    /**
     * 涂鸦文件
     */
    public static final int PAINT = 3;
    
    /**
     * 主键
     */
    private int id;

    /**
     * sid
     */
    private String sId;

    /**
     * 笔记的id
     */
    private String noteId;

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
     * 文件的uri,不存入数据库
     */
    private String uri;
    
    /**
     * 本地的存储路径，全路径
     */
    private String localPath;

    /**
     * 文件的描述
     */
    private String decription;

    /**
     * 服务器的全路径
     */
    private String serverPath;

    /**
     * 同步状态
     */
    private SyncState syncState;

    /**
     * 删除状态
     */
    private DeleteState deleteState;

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
    
    public Attach() {}

    protected Attach(Parcel in) {
        id = in.readInt();
        sId = in.readString();
        noteId = in.readString();
        userId = in.readInt();
        filename = in.readString();
        type = in.readInt();
        uri = in.readString();
        localPath = in.readString();
        decription = in.readString();
        serverPath = in.readString();
        createTime = in.readLong();
        modifyTime = in.readLong();
        size = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(sId);
        dest.writeString(noteId);
        dest.writeInt(userId);
        dest.writeString(filename);
        dest.writeInt(type);
        dest.writeString(uri);
        dest.writeString(localPath);
        dest.writeString(decription);
        dest.writeString(serverPath);
        dest.writeLong(createTime);
        dest.writeLong(modifyTime);
        dest.writeLong(size);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Attach> CREATOR = new Creator<Attach>() {
        @Override
        public Attach createFromParcel(Parcel in) {
            return new Attach(in);
        }

        @Override
        public Attach[] newArray(int size) {
            return new Attach[size];
        }
    };

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

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
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

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getDecription() {
        return decription;
    }

    public void setDecription(String decription) {
        this.decription = decription;
    }

    public String getServerPath() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * 获取可用的uri,优先使用uri
     * @return uri
     */
    public String getAvailableUri() {
        if (uri != null) {
            return uri;
        } else if (localPath != null) {
            return ImageDownloader.Scheme.FILE.wrap(localPath);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "Attach{" +
                "id=" + id +
                ", sId='" + sId + '\'' +
                ", noteId='" + noteId + '\'' +
                ", userId=" + userId +
                ", filename='" + filename + '\'' +
                ", type=" + type +
                ", uri='" + uri + '\'' +
                ", localPath='" + localPath + '\'' +
                ", decription='" + decription + '\'' +
                ", serverPath='" + serverPath + '\'' +
                ", syncState=" + syncState +
                ", deleteState=" + deleteState +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                ", size=" + size +
                '}';
    }

}
