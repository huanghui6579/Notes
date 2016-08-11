package com.yunxinlink.notes.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;

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

    /**
     * 格式化联系人的数据，格式为"name:number"
     * @param info 联系人信息的数据[0]:姓名，[1]:号码
     * @return 格式化后的文本内容
     */
    public static CharSequence formatContactInfo(String[] info) {
        String name = info[0];
        String number = info[1];
        return name + ":" + number;
    }

    /**
     * 在编辑框中插入文本
     * @param editText
     * @param text
     */
    public static void insertText(EditText editText, CharSequence text) {
        int selectionStart = editText.getSelectionStart();
        Editable editable = editText.getEditableText();
        editable.insert(selectionStart, text);
    }

    /**
     * 当请求权限失败后，给出对应的解释和提示
     * @param activity
     * @param permission 请求的权限
     * @param rationale 用户曾经拒绝过该权限，给予用户解释需要该权限的原因
     * @param failedMsg 用户拒绝该权限后的提示语
     */
    public static void onPermissionDenied(Activity activity, String permission, String rationale, String failedMsg) {
        //如果App的权限申请曾经被用户拒绝过，就需要在这里跟用户做出解释
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                permission)) {
            if (!TextUtils.isEmpty(rationale)) {
                SystemUtil.makeLongToast(rationale);
            }
        } else {
            if (!TextUtils.isEmpty(failedMsg)) {
                SystemUtil.makeLongToast(failedMsg);
            }
            //进行权限请求
                    /*ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            EXTERNAL_STORAGE_REQ_CODE);*/
        }
    }

    /**
     * 当请求权限失败后，给出对应的解释和提示
     * @param activity
     * @param permission 请求的权限
     * @param rationaleRes 用户曾经拒绝过该权限，给予用户解释需要该权限的原因
     * @param failedRes 用户拒绝该权限后的提示语
     */
    public static void onPermissionDenied(Activity activity, String permission, int rationaleRes, int failedRes) {
        String rationale = null;
        if (rationaleRes != 0) {
            rationale = activity.getString(rationaleRes);
        }
        String failedMsg = null;
        if (failedRes != 0) {
            failedMsg = activity.getString(failedRes);
        }
        onPermissionDenied(activity, permission, rationale, failedMsg);
    }
}
