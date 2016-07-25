package net.ibaixin.notes.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.persistent.AttachManager;
import net.ibaixin.notes.persistent.NoteManager;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author huanghui1
 * @update 2016/6/18 16:50
 * @version: 0.0.1
 */
public class CoreService extends IntentService {
    private static final String TAG = "CoreService";
    
    private NoteManager mNoteManager;
    
    public CoreService() {
        super("CoreService");
        mNoteManager = NoteManager.getInstance();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (mNoteManager == null) {
                mNoteManager = NoteManager.getInstance();
            }
            int opt = intent.getIntExtra(Constants.ARG_CORE_OPT, 0);
            NoteInfo note = null;
            List<String> list = null;
            switch (opt) {
                case Constants.OPT_ADD_NOTE:    //添加笔记
                    note = intent.getParcelableExtra(Constants.ARG_CORE_OBJ);
                    if (note != null) {

                        list = intent.getStringArrayListExtra(Constants.ARG_CORE_LIST);
                        addNote(note, list);
                    }
                    break;
                case Constants.OPT_UPDATE_NOTE: //更新笔记
                    note = intent.getParcelableExtra(Constants.ARG_CORE_OBJ);
                    if (note != null) {
                        list = intent.getStringArrayListExtra(Constants.ARG_CORE_LIST);
                        //是否更新内容
                        boolean updateContent = intent.getBooleanExtra(Constants.ARG_SUB_OBJ, true);
                        updateNote(note, list, updateContent);
                    }
                    break;
                case Constants.OPT_REMOVE_NOTE_ATTACH:  //移除笔记中的附件数据库记录，彻底删除
                    List<Attach> attachList = intent.getParcelableArrayListExtra(Constants.ARG_CORE_LIST);
                    //是否删除父类的目录
                    String parentDir = intent.getStringExtra(Constants.ARG_SUB_OBJ);
                    if (attachList != null && attachList.size() > 0) {
                        list = new ArrayList<>();
                        List<String> fileList = new ArrayList<>();
                        for (Attach attach : attachList) {
                            list.add(attach.getSId());
                            fileList.add(attach.getLocalPath());
                        }
                        AttachManager.getInstance().removeAttachs(list, fileList);
                        if (!TextUtils.isEmpty(parentDir)) {
                            try {
                                File dir = new File(parentDir);
                                if (dir.exists()) {
                                    dir.delete();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "---opt_remove_note_attach---delete--parent---dir----error----" + parentDir + ":" + e.getMessage());
                                e.printStackTrace();
                            }
                            Log.d(TAG, "---opt_remove_note_attach---delete--parent---dir--" + parentDir);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 添加笔记
     * @param note
     * @param attSidList
     */
    private void addNote(NoteInfo note, List<String> attSidList) {
        List<String> attachSids = SystemUtil.getAttachSids(note.getContent());
        String sid = note.getSId();
        note = mNoteManager.addNote(note, attSidList, attachSids);
        boolean success = note != null;
        Log.d(TAG, "---onHandleIntent---addNote----result---" + success + "---note---" + sid);
    }

    /**
     * 更新笔记
     * @param note
     * @param attSidList
     * @param updateContent 是否更新内容
     */
    private void updateNote(NoteInfo note, List<String> attSidList, boolean updateContent) {
        if (!updateContent && (attSidList == null || attSidList.size() == 0)) {
            //缓存中没有附件，也不做处理
            return;
        }
        List<String> attachSids = SystemUtil.getAttachSids(note.getContent());
        String sid = note.getSId();
        boolean success = false;
        if (updateContent) {
            success = mNoteManager.updateNote(note, attSidList, attachSids);
        } else {
            //不需要更新内容
            mNoteManager.updateTextAttach(null, note, attSidList, attachSids, false);
            success = true;
        }
        Log.d(TAG, "---onHandleIntent---updateNote----result---" + success + "---note---" + sid);
    }
}
