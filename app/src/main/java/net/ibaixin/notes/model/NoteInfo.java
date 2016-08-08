package net.ibaixin.notes.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;

import net.ibaixin.notes.R;
import net.ibaixin.notes.cache.FolderCache;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.TimeUtil;

import java.util.Comparator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 记事本的基本信息实体
 * @author huanghui1
 * @update 2016/2/26 14:49
 * @version: 0.0.1
 */
public class NoteInfo implements Parcelable, Comparator<NoteInfo> {
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
     * 文件夹的id
     */
    private String folderId;

    /**
     * 笔记的类型，主要分为{@link net.ibaixin.notes.model.NoteInfo.NoteKind#TEXT}和{@link net.ibaixin.notes.model.NoteInfo.NoteKind#DETAILED_LIST}
     */
    private NoteKind kind = NoteKind.TEXT;

    /**
     * 同步的状态
     */
    private SyncState syncState;

    /**
     * 删除的状态
     */
    private DeleteState deleteState = DeleteState.DELETE_NONE;

    /**
     * 是否有附件
     */
    private boolean hasAttach;

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
     * 前一版本的文本内容
     */
    private String oldContent;

    /**
     * 笔记中的附件
     */
    private Map<String, Attach> attaches;
    
    public NoteInfo() {
        sId = SystemUtil.generateNoteSid();
    }
    
    public NoteInfo(String sid) {
        this.sId = sid;
    }
    
    public NoteInfo(int id) {
        this.id = id;
    }

    public NoteInfo(Parcel in) {
        id = in.readInt();
        sId = in.readString();
        userId = in.readInt();
        title = in.readString();
        content = in.readString();
        remindId = in.readInt();
        remindTime = in.readLong();
        folderId = in.readString();
        hasAttach = in.readByte() != 0;
        createTime = in.readLong();
        modifyTime = in.readLong();
        hash = in.readString();
        oldContent = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(sId);
        dest.writeInt(userId);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeInt(remindId);
        dest.writeLong(remindTime);
        dest.writeString(folderId);
        dest.writeByte((byte) (hasAttach ? 1 : 0));
        dest.writeLong(createTime);
        dest.writeLong(modifyTime);
        dest.writeString(hash);
        dest.writeString(oldContent);
    }

    public Map<String, Attach> getAttaches() {
        return attaches;
    }

    public void setAttaches(Map<String, Attach> attaches) {
        this.attaches = attaches;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<NoteInfo> CREATOR = new Creator<NoteInfo>() {
        @Override
        public NoteInfo createFromParcel(Parcel in) {
            return new NoteInfo(in);
        }

        @Override
        public NoteInfo[] newArray(int size) {
            return new NoteInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NoteInfo noteInfo = (NoteInfo) o;

        return id == noteInfo.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compare(NoteInfo lhs, NoteInfo rhs) {
        long lId = lhs.getId();
        long rId = rhs.getId();
        if (lId > rId) {
            return 1;
        } else if (lId < rId) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "NoteInfo{" +
                "id=" + id +
                ", sId='" + sId + '\'' +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", remindId=" + remindId +
                ", remindTime=" + remindTime +
                ", folderId='" + folderId + '\'' +
                ", kind=" + kind +
                ", syncState=" + syncState +
                ", deleteState=" + deleteState +
                ", hasAttach=" + hasAttach +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                ", hash='" + hash + '\'' +
                ", oldContent='" + oldContent + '\'' +
                ", attaches=" + attaches +
                '}';
    }

    /**
     * 笔记的类型，主要分为文本笔记和清单笔记
     * @author tiger
     * @update 2016/3/7 21:44
     * @version 1.0.0
     */
    public enum NoteKind {
        /**
         * 文本笔记
         */
        TEXT,
        /**
         * 清单笔记
         */
        DETAILED_LIST;
    }

    /**
     * 获取笔记的详细信息
     * @return
     */
    public String getNoteInfo(Context context) {
        StringBuilder builder = new StringBuilder();
        String colon = context.getString(R.string.colon);
        String typeStr = "";
        switch (kind) {
            case TEXT:
                typeStr = context.getString(R.string.note_type_text);
                break;
            case DETAILED_LIST:
                typeStr = context.getString(R.string.note_type_list);
                break;
        }
        String syncStateStr = context.getString(R.string.sync_type_none);
        if (syncState != null && syncState.ordinal() == SyncState.SYNC_DONE.ordinal()) {
            syncStateStr = context.getString(R.string.sync_type_done);
        }
        String foldername = null;
        if (!TextUtils.isEmpty(folderId)) {
            Folder folder = FolderCache.getInstance().getFolderMap().get(folderId);
            if (folder != null) {
                foldername = folder.getName();
            }
        }
        String nextLine = "\r\n";
        builder.append(context.getString(R.string.note_type)).append(colon).append(typeStr).append(nextLine);
        if (!TextUtils.isEmpty(foldername)) {
            builder.append(context.getString(R.string.action_folder)).append(colon).append(foldername).append(nextLine);
        }
        builder.append(context.getString(R.string.note_words)).append(colon).append(content.length()).append(nextLine)
                .append(context.getString(R.string.create_time)).append(colon).append(TimeUtil.formatNoteTime(createTime)).append(nextLine)
                .append(context.getString(R.string.modify_time)).append(colon).append(TimeUtil.formatNoteTime(modifyTime)).append(nextLine)
                .append(context.getString(R.string.sync_state)).append(colon).append(syncStateStr);
        return builder.toString();
    }

    /**
     * 根据内容获取标题，只取内容的第一行
     * @return
     */
    public CharSequence getNoteTitle() {
        if (title == null && !isDetailNote()) {
            if (content == null) {
                return null;
            }
            //去掉开头和结尾的空格
            String tContent = content.trim();
            int index = tContent.indexOf(Constants.TAG_NEXT_LINE);
            if (index != -1) {  //有换行
                return tContent.substring(0, index);
            } else {
                return tContent;
            }
        } else {
            return title;
        }
    }

    /**
     * 根据笔记类型来获取笔记真实的内容，如果是清单笔记，则内容为：title+各清单的内容，用"\n"拼接，如果是文本笔记，则直接返回content
     * @return
     */
    public CharSequence getRealContent() {
        if (isDetailNote() && !TextUtils.isEmpty(title)) {   //清单笔记
            return title + Constants.TAG_NEXT_LINE + content;
        } else {
            return content;
        }
    }
    
    public CharSequence getStyleContent() {
        if (isDetailNote()) {   //清单笔记
            String tContent = content.trim();
            StringTokenizer tokenizer = new StringTokenizer(tContent, Constants.TAG_NEXT_LINE);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            int i = 0;
            
            while (tokenizer.hasMoreTokens()) {
                if (i > 10) {
                    break; 
                }
                String line = tokenizer.nextToken();
                
                SpannableString spannableString = new SpannableString(line);

                BulletSpan bulletSpan = new BulletSpan(12);

                spannableString.setSpan(bulletSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                builder.append(spannableString).append(Constants.TAG_NEXT_LINE);
                i++;
            }
            return builder;
        } else {
            return getContent();
        }
    }

    /**
     * 是否是清单笔记
     * @return
     */
    public boolean isDetailNote() {
        return kind != null && kind == NoteKind.DETAILED_LIST;
    }

    /**
     * 判断该笔记是否为空
     * @return
     */
    public boolean isEmpty() {
        return TextUtils.isEmpty(hash);
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

    public String getSId() {
        return sId;
    }

    public void setSId(String sid) {
        this.sId = sid;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public NoteKind getKind() {
        return kind;
    }

    public void setKind(NoteKind kind) {
        this.kind = kind;
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

    public boolean hasAttach() {
        return hasAttach;
    }

    public void setHasAttach(boolean hasAttach) {
        this.hasAttach = hasAttach;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getOldContent() {
        return oldContent;
    }

    public void setOldContent(String oldContent) {
        this.oldContent = oldContent;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
