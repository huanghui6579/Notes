package com.yunxinlink.notes.model;

import android.text.TextUtils;

import com.yunxinlink.notes.util.Constants;

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
        /*StringBuilder builder = new StringBuilder();
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
        builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_NEXT_LINE));*/
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
}
