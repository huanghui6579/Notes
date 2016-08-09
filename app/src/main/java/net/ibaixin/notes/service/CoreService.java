package net.ibaixin.notes.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.socks.library.KLog;

import net.ibaixin.notes.cache.NoteCache;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.model.DetailList;
import net.ibaixin.notes.model.DetailNoteInfo;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.persistent.AttachManager;
import net.ibaixin.notes.persistent.NoteManager;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.DigestUtil;
import net.ibaixin.notes.util.SystemUtil;

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
            String sid = null;
            NoteCache noteCache = null;
            DetailNoteInfo detailNote = null;
            switch (opt) {
                case Constants.OPT_ADD_NOTE:    //添加笔记
                    sid = intent.getStringExtra(Constants.ARG_CORE_OBJ);
                    noteCache = NoteCache.getInstance();
                    note = noteCache.getNote();
                    if (!checkNoteInfo(note, sid)) {
                        KLog.d(TAG, "----checkNoteInfo----false---note---" + note + "----sid:----" + sid);
                        return;
                    }

                    note.setHash(DigestUtil.md5Digest(note.getRealContent().toString()));
                    
                    detailNote = noteCache.get();
//                    note = intent.getParcelableExtra(Constants.ARG_CORE_OBJ);
                    list = intent.getStringArrayListExtra(Constants.ARG_CORE_LIST);
                    addNote(detailNote, list);
                    noteCache.clear();
                    break;
                case Constants.OPT_UPDATE_NOTE: //更新笔记
                    sid = intent.getStringExtra(Constants.ARG_CORE_OBJ);
                    noteCache = NoteCache.getInstance();
                    note = noteCache.getNote();
                    if (!checkNoteInfo(note, sid)) {
                        KLog.d(TAG, "----checkNoteInfo----false---note---" + note + "----sid:----" + sid);
                        return;
                    }
                    detailNote = noteCache.get();
                    list = intent.getStringArrayListExtra(Constants.ARG_CORE_LIST);
                    //是否更新内容
                    boolean updateContent = intent.getBooleanExtra(Constants.ARG_SUB_OBJ, true);
                    if (updateContent) {
                        note.setHash(DigestUtil.md5Digest(note.getRealContent().toString()));
                    }
                    if (!note.isDetailNote()) { //非清单，则将标题设为""
                        note.setTitle("");
                    }
                    List<DetailList> srcDetails = null;
                    Object extraObj = noteCache.getExtraData();
                    if (extraObj != null && extraObj instanceof List) {
                        srcDetails = (List<DetailList>) extraObj;
                    }
                    updateNote(detailNote, list, updateContent, srcDetails);
                    noteCache.clear();
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
                                KLog.d(TAG, "---opt_remove_note_attach---delete--parent---dir----error----" + parentDir + ":" + e.getMessage());
                                e.printStackTrace();
                            }
                            KLog.d(TAG, "---opt_remove_note_attach---delete--parent---dir--" + parentDir);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 检查笔记的一致性
     * @param note
     * @param sid 传过来的sid
     * @return
     */
    private boolean checkNoteInfo(NoteInfo note, String sid) {
        if (note == null || TextUtils.isEmpty(sid)) {
            KLog.d(TAG, "-----opt_add_note--sid--is---null---or--note----is--null--note:---" + note + "---sid:--" + sid);
            return false;
        }
        if (!sid.equals(note.getSId())) {
            KLog.d(TAG, "-----opt_add_note--sid----does---not--equals---sid---note.sid:---" + note.getSId() + "---sid:---" + sid);
            return false;
        }
        return true;
    }
    
    /**
     * 添加笔记
     * @param detailNote 笔记的包装类
     * @param attSidList
     */
    private void addNote(DetailNoteInfo detailNote, List<String> attSidList) {
        NoteInfo note = detailNote.getNoteInfo();
        List<String> attachSids = SystemUtil.getAttachSids(note.getContent());
        String sid = note.getSId();
        detailNote = mNoteManager.addDetailNote(detailNote, attSidList, attachSids);
        boolean success = detailNote != null;
        KLog.d(TAG, "---onHandleIntent---addNote----result---" + success + "---note---" + sid);
    }

    /**
     * 更新笔记
     * @param detailNote 笔记的包装类
     * @param attSidList
     * @param updateContent 是否更新内容
     * @param detailLists 原始笔记中的清单，更新笔记时，需与原始笔记对比                     
     */
    private void updateNote(DetailNoteInfo detailNote, List<String> attSidList, boolean updateContent, List<DetailList> detailLists) {
        if (!updateContent && (attSidList == null || attSidList.size() == 0)) {
            //缓存中没有附件，也不做处理
            return;
        }
        
        KLog.d(TAG, "----updateNote---updateContent-----" + updateContent);
        
        NoteInfo note = detailNote.getNoteInfo();
        List<String> attachSids = SystemUtil.getAttachSids(note.getContent());
        String sid = note.getSId();
        boolean success = false;
        if (updateContent) {
            success = mNoteManager.updateDetailNote(detailNote, attSidList, attachSids, detailLists);
        } else {
            //不需要更新内容
            mNoteManager.updateTextAttach(null, note, attSidList, attachSids, false);
            success = true;
        }
        KLog.d(TAG, "---onHandleIntent---updateNote----result---" + success + "---note---" + sid);
    }
}
