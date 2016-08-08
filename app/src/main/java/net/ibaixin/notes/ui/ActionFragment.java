package net.ibaixin.notes.ui;

import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.model.DetailNoteInfo;
import net.ibaixin.notes.model.NoteInfo;

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
     * 获取笔记的类型，主要分为{@link net.ibaixin.notes.model.NoteInfo.NoteKind#TEXT}和{@link net.ibaixin.notes.model.NoteInfo.NoteKind#DETAILED_LIST}
     * @return
     */
    NoteInfo.NoteKind getNoteType();
}
