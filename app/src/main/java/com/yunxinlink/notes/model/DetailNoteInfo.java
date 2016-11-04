package com.yunxinlink.notes.model;

import android.content.Context;
import android.text.TextUtils;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.TimeUtil;

import java.util.Comparator;
import java.util.List;

/**
 * 清单笔记的实体
 * @author huanghui1
 * @update 2016/8/5 16:35
 * @version: 0.0.1
 */
public class DetailNoteInfo implements Comparator<DetailNoteInfo> {
    
    private NoteInfo noteInfo;
    /**
     * 笔记的清单列表
     */
    private List<DetailList> detailList;

    /**
     * 最后一个图片的附件，主要用来显示背景
     */
    private Attach mLastAttach;

    /**
     * 额外的参数
     */
    private Object mExtraObj;

    public List<DetailList> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<DetailList> detailList) {
        this.detailList = detailList;
    }

    public NoteInfo getNoteInfo() {
        return noteInfo;
    }

    public void setNoteInfo(NoteInfo noteInfo) {
        this.noteInfo = noteInfo;
    }

    public Attach getLastAttach() {
        return mLastAttach;
    }

    public void setLastAttach(Attach lastAttach) {
        this.mLastAttach = lastAttach;
    }

    public Object getExtraObj() {
        return mExtraObj;
    }

    public void setExtraObj(Object extraObj) {
        this.mExtraObj = extraObj;
    }

    /**
     * 获取清单的条数
     * @return
     */
    public int getDetailCount() {
        return SystemUtil.isEmpty(detailList) ? 0 : detailList.size();
    }

    /**
     * 获取笔记的标题和清单的内容
     * @return
     */
    public CharSequence getNoteText() {
        CharSequence title = null;
        if (noteInfo != null && !TextUtils.isEmpty(noteInfo.getTitle())) {
            title = noteInfo.getTitle();
        }
        CharSequence text = null;
        if (!TextUtils.isEmpty(title)) {
            text = title + Constants.TAG_NEXT_LINE + noteInfo.getContent();
        } else {
            text = noteInfo.getContent();
        }
        
        if (TextUtils.isEmpty(text)) {  //笔记本身没有保存清单的数据
            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(title)) {
                builder.append(title).append(Constants.TAG_NEXT_LINE);
            }
            if (detailList != null && detailList.size() > 0) {
                for (DetailList detail : detailList) {
                    title = detail.getTitle();
                    title = title == null ? "" : title;
                    builder.append(title).append(Constants.TAG_NEXT_LINE);
                }
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_NEXT_LINE));
            }
            text = builder.toString();
        }
        
        return text;
    }

    /**
     * 是否具有清单
     * @return
     */
    public boolean hasDetailList() {
        return detailList != null && detailList.size() > 0;
    }

    @Override
    public String toString() {
        return "DetailNoteInfo{" +
                "noteInfo=" + noteInfo +
                ", detailList=" + detailList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DetailNoteInfo that = (DetailNoteInfo) o;

        return noteInfo != null ? noteInfo.equals(that.noteInfo) : that.noteInfo == null;

    }

    @Override
    public int hashCode() {
        return noteInfo != null ? noteInfo.hashCode() : 0;
    }

    @Override
    public int compare(DetailNoteInfo lhs, DetailNoteInfo rhs) {
        if (rhs == null) {
            return 1;
        } else {
            return lhs.getNoteInfo().compare(lhs.getNoteInfo(), rhs.getNoteInfo());
        }
    }

    /**
     * 获取笔记的详细信息
     * @return
     */
    public String getNoteInfo(Context context) {
        StringBuilder builder = new StringBuilder();
        String colon = context.getString(R.string.colon);
        String typeStr = "";
        //是否是文本笔记
        boolean isText = true;
        switch (noteInfo.getKind()) {
            case TEXT:
                typeStr = context.getString(R.string.note_type_text);
                break;
            case DETAILED_LIST:
                isText = false;
                typeStr = context.getString(R.string.note_type_list);
                break;
        }
        String syncStateStr = context.getString(R.string.sync_type_none);
        SyncState syncState = noteInfo.getSyncState();
        if (syncState != null && syncState.ordinal() == SyncState.SYNC_DONE.ordinal()) {
            syncStateStr = context.getString(R.string.sync_type_done);
        }
        String foldername = null;
        String folderId = noteInfo.getFolderId();
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
        long createTime = noteInfo.getCreateTime();
        long modifyTime = noteInfo.getModifyTime();
        String wordTip = null;
        String wordCount = null;
        if (isText) {
            String text = getNoteText().toString();
            wordTip = context.getString(R.string.note_words);
            wordCount = context.getString(R.string.unit_word_count, text.length());
        } else {
            wordTip = context.getString(R.string.note_detail_count);
            wordCount = context.getString(R.string.unit_detail_count, getDetailCount());
        }
        builder.append(wordTip).append(colon).append(wordCount).append(nextLine)
                .append(context.getString(R.string.create_time)).append(colon).append(TimeUtil.formatNoteTime(createTime)).append(nextLine)
                .append(context.getString(R.string.modify_time)).append(colon).append(TimeUtil.formatNoteTime(modifyTime)).append(nextLine)
                .append(context.getString(R.string.sync_state)).append(colon).append(syncStateStr);
        return builder.toString();
    }

    /**
     * 判断笔记是否没有被删除
     * @return
     */
    public boolean isAvailableNote() {
        return noteInfo != null && noteInfo.isNoneDelete();
    }

    /**
     * 是否是垃圾桶的笔记
     * @return
     */
    public boolean isTrashNote() {
        return noteInfo != null && noteInfo.checkDeleteState(DeleteState.DELETE_TRASH);
    }

    /**
     * 判断该笔记是否已经同步了
     * @return
     */
    public boolean isSynced() {
        return noteInfo != null && noteInfo.isSynced();
    }
}
