package com.yunxinlink.notes.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.StrikethroughSpan;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.DigestUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 记事本的基本信息实体
 * @author huanghui1
 * @update 2016/2/26 14:49
 * @version: 0.0.1
 */
public class NoteInfo implements Parcelable, Comparator<NoteInfo> {
    //最后修改时间排序
    public static final int SORT_MODIFY_TIME = 0;
    //创建时间排序
    public static final int SORT_CREATE_TIME = 1;
    //标题排序
    public static final int SORT_TITLE = 2;
    
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
     * 实际显示的文本，仅用于本地
     */
    private String showContent;

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
     * 笔记的类型，主要分为{@link com.yunxinlink.notes.model.NoteInfo.NoteKind#TEXT}和{@link com.yunxinlink.notes.model.NoteInfo.NoteKind#DETAILED_LIST}
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
        sid = SystemUtil.generateNoteSid();
    }
    
    public NoteInfo(String sid) {
        this.sid = sid;
    }
    
    public NoteInfo(int id) {
        this.id = id;
    }

    public NoteInfo(Parcel in) {
        id = in.readInt();
        sid = in.readString();
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
        dest.writeString(sid);
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

        return sid != null ? sid.equals(noteInfo.sid) : noteInfo.sid == null;

    }

    @Override
    public int hashCode() {
        return sid != null ? sid.hashCode() : 0;
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
                ", sid='" + sid + '\'' +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", showContent='" + showContent + '\'' +
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
        
        public static NoteKind valueOf(int value) {
            switch (value) {
                case 0:
                    return TEXT;
                case 1:
                    return DETAILED_LIST;
                default:
                    return TEXT;
            }
        }
    }

    /**
     * 根据内容获取标题，只取内容的第一行
     * @param showAttach 是否包含附件信息
     * @return
     */
    public CharSequence getNoteTitle(boolean showAttach) {
        if (TextUtils.isEmpty(title) && !isDetailNote()) {
            if (content == null) {
                return null;
            }
            String tContent = null;
            //去掉开头和结尾的空格
            if (!showAttach) {
                tContent = showContent == null ? null : showContent.trim();
            }
            if (tContent == null) {
                tContent = content.trim();
            }
            
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

    /**
     * 获取笔记的全内容
     * @param detailLists 笔记的清单
     * @return
     */
    public CharSequence getStyleContent(List<DetailList> detailLists) {
        return getStyleContent(false, detailLists);
    }

    /**
     * 获取笔记的全内容
     * @param hasTitle 是否包含标题，仅对清单有效
     * @param detailLists 笔记的清单
     * @return
     */
    public CharSequence getStyleContent(boolean hasTitle, List<DetailList> detailLists) {
        return getStyleContent(hasTitle, detailLists, null, 0);
    }

    /**
     * 获取笔记的全内容
     * @param hasTitle 是否包含标题，仅对清单有效
     * @param detailLists 笔记的清单
     * @param keyword 要高亮的关键字
     * @param color 高亮的颜色               
     * @return
     */
    public CharSequence getStyleContent(boolean hasTitle, List<DetailList> detailLists, String keyword, int color) {
        if (isDetailNote()) {   //清单笔记
//            String tContent = content;
            
//            StringTokenizer tokenizer = new StringTokenizer(tContent, Constants.TAG_NEXT_LINE);
            SpannableStringBuilder builder = new SpannableStringBuilder();

            boolean deleteLast = false;
            if (hasTitle && !TextUtils.isEmpty(title)) {
                builder.append(title).append(Constants.TAG_NEXT_LINE);
                deleteLast = true;
            }

            int i = 0;
            if (detailLists != null && detailLists.size() > 0) {    //有清单
                for (DetailList detail : detailLists) {
                    deleteLast = true;
                    if (i > 10) {
                        break;
                    }
                    String line = detail.getTitle();
                    if (TextUtils.isEmpty(line)) {  //空的清单
                        line = " ";
                    }
                    SpannableString spannableString = styleText(line, detail, keyword, color);

                    builder.append(spannableString).append(Constants.TAG_NEXT_LINE);
                    i++;
                }
            }

            if (deleteLast) {
                int len = builder.length();
                builder.delete(len - 1, len - 1);
            }
            return builder;
        } else {
            String text = getShowText();
            int subLength = 200;
            if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(keyword) && color != 0) {    //需要加高亮
                String subText = null;
                if (text.length() > subLength) {  //只识高亮前200个字符
                    subText = text.substring(0, subLength);
                } else {
                    subText = text;
                }
                SpannableString spannableString = new SpannableString(subText);
                highlightText(spannableString, subText, keyword, color);
                return spannableString;
            } else {
                return text;
            }
        }
    }

    /**
     * 获取显示的文本内容
     * @return
     */
    public String getShowText() {
        if (showContent == null) {
            return content;
        } else {
            return showContent.trim();
        }
    }
    
    /**
     * 附件的文件类型是否是图片类型，包含图片文件和涂鸦
     * @return
     */
    public boolean isImage(int attachType) {
        return attachType == Attach.IMAGE || attachType == Attach.PAINT;
    }

    /**
     * 获取附件的显示标题
     * @param context
     * @param attachType 附件内容
     * @return
     */
    public CharSequence getAttachShowTitle(Context context, int attachType) {
        CharSequence title = getNoteTitle(false);
        if (TextUtils.isEmpty(title) && !isImage(attachType)) { //内容为空且附件类型不是图片
            title = context.getResources().getString(R.string.no_title);
        }
        return title;
    }

    /**
     * 给文本的前面加上"点"
     * @param text
     * @param keyword 要高亮的关键字
     * @param color 高亮的颜色
     * @return
     */
    private SpannableString styleText(String text, DetailList detail, String keyword, int color) {
        SpannableString spannableString = new SpannableString(text);

        //黑的小圆点
        BulletSpan bulletSpan = new BulletSpan(12);

        spannableString.setSpan(bulletSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        int length = text.length();
        
        if (detail.isChecked()) {   //该清单项已完成，添加删除线
            StrikethroughSpan strikethroughSpan = new StrikethroughSpan();
            spannableString.setSpan(strikethroughSpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (!TextUtils.isEmpty(keyword) && color != 0) {    //需要加高亮

            highlightText(spannableString, text, keyword, color);
        }
        
        return spannableString;
    }

    /**
     * 高亮文本
     * @param text 原始的文本
     * @param keyword 要高亮的文字
     * @param color 颜色
     * @return
     */
    public SpannableString highlightText(SpannableString spannableString, String text, String keyword, int color) {
        Pattern pattern = Pattern.compile(keyword);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            BackgroundColorSpan colorSpan = new BackgroundColorSpan(color);
            spannableString.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    /**
     * 获取笔记的显示时间
     * @param isModifyTime 是否显示修改时间
     * @return
     */
    public long getShowTime(boolean isModifyTime) {
        if (isModifyTime) {
            return modifyTime;
        } else {
            return createTime;
        }
    }
    
    /**
     * 是否是清单笔记
     * @return
     */
    public boolean isDetailNote() {
        return kind != null && kind == NoteKind.DETAILED_LIST;
    }

    public boolean hasId() {
        return id > 0;
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

    public String getShowContent() {
        return showContent;
    }

    public void setShowContent(String showContent) {
        this.showContent = showContent;
    }

    /**
     * 生成hash值
     * 该hash值由title;content;folderSid;kind;deleteState的格式组成，顺序不能错,如果为null,则用""代替
     * @return
     */
    public String generateHash() {
        String spliter = Constants.TAG_SEMICOLON;
        String title = this.title == null ? "" : this.title;
        String content = this.content == null ? "" : this.content;
        String folderSid = this.folderId == null ? "" : this.folderId;
        int kind = this.kind == null ? NoteKind.TEXT.ordinal() : this.kind.ordinal();
        int deleteState = this.deleteState == null ? 0 : this.deleteState.ordinal();
        StringBuilder builder = new StringBuilder();
        builder.append(title).append(spliter)
                .append(content).append(spliter)
                .append(folderSid).append(spliter)
                .append(kind).append(spliter)
                .append(deleteState);
        return DigestUtil.md5Hex(builder.toString());
    }

    /**
     * 是否没有被删除
     * @return
     */
    public boolean isNoneDelete() {
        return deleteState == null || deleteState == DeleteState.DELETE_NONE;
    }

    /**
     * 检验笔记的删除状态
     * @param deleteState
     * @return
     */
    public boolean checkDeleteState(DeleteState deleteState) {
        if (this.deleteState == null && (deleteState == null || deleteState == DeleteState.DELETE_NONE)) {
            return true;
        } else {
            return deleteState == this.deleteState;
        }
    }
}
