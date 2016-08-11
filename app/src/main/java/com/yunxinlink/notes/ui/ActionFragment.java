package com.yunxinlink.notes.ui;

import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;

import java.util.Map;

/**
 * fragment的抽象类，提供一些公共的方法
 * @author huanghui1
 * @update 2016/8/5 11:08
 * @version: 0.0.1
 */
public interface ActionFragment {
    /**
     * 显示笔记
     * @param detailNote 笔记信息
     * @param map 笔记附件的缓存
     */
    void showNote(DetailNoteInfo detailNote, Map<String, Attach> map);

    /**
     * 获取笔记的内容
     */
    CharSequence getText();

    /**
     * 获取笔记的标题
     * @return
     */
    CharSequence getTitle();
    
    /**
     * 获取笔记的类型，主要分为{@link com.yunxinlink.notes.model.NoteInfo.NoteKind#TEXT}和{@link com.yunxinlink.notes.model.NoteInfo.NoteKind#DETAILED_LIST}
     * @return
     */
    NoteInfo.NoteKind getNoteType();

    /**
     * 插入当前时间
     * @return 返回当前时间
     */
    String insertTime();

    /**
     * 插入联系人信息，仅包含姓名和号码
     * @param info 联系人信息的数据[0]:姓名，[1]:号码
     * @return 返回插入的联系人信息
     */
    CharSequence insertContact(String[] info);
}
