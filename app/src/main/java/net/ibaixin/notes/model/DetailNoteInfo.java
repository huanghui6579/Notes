package net.ibaixin.notes.model;

import android.text.TextUtils;

import net.ibaixin.notes.util.Constants;

import java.util.List;

/**
 * 清单笔记的实体
 * @author huanghui1
 * @update 2016/8/5 16:35
 * @version: 0.0.1
 */
public class DetailNoteInfo {
    
    private NoteInfo noteInfo;
    /**
     * 笔记的清单列表
     */
    private List<DetailList> detailList;

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

    /**
     * 获取笔记的标题和清单的内容
     * @return
     */
    public CharSequence getNoteText() {
        CharSequence title = "";
        if (noteInfo != null && !TextUtils.isEmpty(noteInfo.getContent())) {
            title = noteInfo.getContent();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(title).append(Constants.TAG_NEXT_LINE);
        if (detailList != null && detailList.size() > 0) {
            for (DetailList detail : detailList) {
                title = detail.getTitle();
                title = title == null ? "" : title;
                builder.append(title).append(Constants.TAG_NEXT_LINE);
            }
        }
        builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_NEXT_LINE));
        return builder;
    }

    /**
     * 是否具有清单
     * @return
     */
    public boolean hasDetailList() {
        return detailList != null && detailList.size() > 0;
    }
}
