package com.yunxinlink.notes.cache;

import com.yunxinlink.notes.model.DetailList;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;

import java.util.List;

/**
 * 笔记的缓存对象
 * @author huanghui1
 * @update 2016/8/6 14:15
 * @version: 0.0.1
 */
public class NoteCache {
    private static NoteCache mInstance = null;

    /**
     * 包含清单的笔记
     */
    private ThreadLocal<DetailNoteInfo> mThreadLocal;

    /**
     * 额外数据缓存
     */
    private Object mExtraData;
    
    private NoteCache() {
        mThreadLocal = new InheritableThreadLocal<>();
    }
    
    public static NoteCache getInstance() {
        if (mInstance == null) {
            synchronized (NoteCache.class) {
                if (mInstance == null) {
                    mInstance = new NoteCache();
                }
            }
        }
        return mInstance;
    }
    
    public DetailNoteInfo get() {
        return mThreadLocal.get();
    }

    public void set(DetailNoteInfo detailNote) {
        mThreadLocal.set(detailNote);
    }

    public Object getExtraData() {
        return mExtraData;
    }

    public void setExtraData(Object extraData) {
        this.mExtraData = extraData;
    }

    /**
     * 获取笔记
     * @return
     */
    public NoteInfo getNote() {
        DetailNoteInfo detailNote = get();
        if (detailNote != null) {
            return detailNote.getNoteInfo();
        }
        return null;
    }

    /**
     * 获取清单的列表项
     * @return
     */
    public List<DetailList> getDetailList() {
        DetailNoteInfo detailNote = get();
        if (detailNote != null) {
            return detailNote.getDetailList();
        }
        return null;
    }

    /**
     * 清除
     */
    public void clear() {
        mThreadLocal.remove();
        mExtraData = null;
    }
}
