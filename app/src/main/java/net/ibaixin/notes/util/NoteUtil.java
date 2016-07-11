package net.ibaixin.notes.util;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.persistent.NoteManager;

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
    public static void handleDeleteNote(Context context, List<NoteInfo> noteList, boolean hasDeleteOpt) {
        final List<NoteInfo> list = new ArrayList<>();
        list.addAll(noteList);
        if (!hasDeleteOpt) {   //之前是否有删除操作，如果没有，则需弹窗           
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.prompt)
                    .setMessage(R.string.confirm_to_trash)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doDeleteNote(list);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {    //直接删除
            doDeleteNote(list);
        }
    }

    /**
     * 删除多条笔记
     * @param note 要删除的笔记
     */
    public static void handleDeleteNote(Context context, NoteInfo note, boolean hasDeleteOpt) {
        final List<NoteInfo> list = new ArrayList<>();
        list.add(note);
        if (!hasDeleteOpt) {   //之前是否有删除操作，如果没有，则需弹窗           
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.prompt)
                    .setMessage(R.string.confirm_to_trash)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doDeleteNote(list);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {    //直接删除
            doDeleteNote(list);
        }
    }

    /**
     * 删除多条笔记
     * @param noteList 要删除的笔记的集合
     */
    private static void doDeleteNote(final List<NoteInfo> noteList) {

        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {

                NoteManager.getInstance().deleteNote(noteList);
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
        builder.setTitle(note.getTitle())
                .setMessage(info)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
