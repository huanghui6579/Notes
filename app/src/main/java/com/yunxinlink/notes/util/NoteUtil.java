package com.yunxinlink.notes.util;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.NoteManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huanghui1
 * @update 2016/7/11 19:15
 * @version: 0.0.1
 */
public class NoteUtil {
    private NoteUtil() {}

    /**
     * 删除多条笔记
     * @param noteList 要删除的笔记的集合
     */
    public static void handleDeleteNote(Context context, final List<DetailNoteInfo> noteList, boolean hasDeleteOpt) {
        if (!hasDeleteOpt) {   //之前是否有删除操作，如果没有，则需弹窗           
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.prompt)
                    .setMessage(R.string.confirm_to_trash)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doDeleteNote(noteList);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {    //直接删除
            doDeleteNote(noteList);
        }
    }

    /**
     * 删除多条笔记
     * @param note 要删除的笔记
     */
    public static void handleDeleteNote(Context context, NoteInfo note, boolean hasDeleteOpt) {
        final List<DetailNoteInfo> list = new ArrayList<>();
        DetailNoteInfo detailNote = new DetailNoteInfo();
        detailNote.setNoteInfo(note);
        list.add(detailNote);
        handleDeleteNote(context, list, hasDeleteOpt);
    }

    /**
     * 删除多条笔记
     * @param noteList 要删除的笔记的集合
     */
    private static void doDeleteNote(final List<DetailNoteInfo> noteList) {
        final List<DetailNoteInfo> list = new ArrayList<>(noteList);
        SystemUtil.getThreadPool().execute(new NoteTask(list) {
            @Override
            public void run() {
                NoteManager.getInstance().deleteNote((List<DetailNoteInfo>) params[0]);
            }
        });
    }


    /**
     * 显示笔记详情
     * @param note
     */
    public static void showInfo(Context context, final NoteInfo note) {
        String info = note.getNoteInfo(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(note.getNoteTitle())
                .setMessage(info)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
