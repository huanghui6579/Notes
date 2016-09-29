package com.yunxinlink.notes.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.cache.NoteCache;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailList;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.AttachManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.richtext.AttachText;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.DigestUtil;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author huanghui1
 * @update 2016/6/18 16:50
 * @version: 0.0.1
 */
public class CoreService extends IntentService {
    private static final String TAG = "CoreService";
    
    private NoteManager mNoteManager;
    
    private NoteObserver mNoteObserver;
    
    public CoreService() {
        super("CoreService");
        mNoteManager = NoteManager.getInstance();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //注册观察者
        registerObserver();
    }

    /**
     * 注册观察者
     */
    private void registerObserver() {
        if (mNoteObserver == null) {
            mNoteObserver = new NoteObserver(new Handler());
        }
        UserManager.getInstance().addObserver(mNoteObserver);
    }

    /**
     * 注销观察者
     */
    private void unregisterObserver() {
        if (mNoteObserver != null) {
            UserManager.getInstance().removeObserver(mNoteObserver);
        }
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

                    note.setHash(DigestUtil.md5Hex(note.getRealContent().toString()));
                    
                    detailNote = noteCache.get();
//                    note = intent.getParcelableExtra(Constants.ARG_CORE_OBJ);
//                    list = intent.getStringArrayListExtra(Constants.ARG_CORE_LIST);
                    addNote(detailNote);
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
//                    list = intent.getStringArrayListExtra(Constants.ARG_CORE_LIST);
                    //是否更新内容
                    boolean updateContent = intent.getBooleanExtra(Constants.ARG_SUB_OBJ, true);
                    if (updateContent) {
                        note.setHash(DigestUtil.md5Hex(note.getRealContent().toString()));
                    }
                    if (!note.isDetailNote()) { //非清单，则将标题设为""
                        note.setTitle("");
                    }
                    List<DetailList> srcDetails = null;
                    Object extraObj = noteCache.getExtraData();
                    if (extraObj != null && extraObj instanceof List) {
                        srcDetails = (List<DetailList>) extraObj;
                    }
                    updateNote(detailNote, /*list, */updateContent, srcDetails);
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

    @Override
    public void onDestroy() {
        //注销观察者
        unregisterObserver();
        super.onDestroy();
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
     * 获取笔记的最后一个附件
     * @param detailNote 笔记
     * @param attachSids 文本中的笔记sid集合
     * @return
     */
    private List<String> getCacheAttachList(DetailNoteInfo detailNote, List<String> attachSids) {
        Object obj = detailNote.getExtraObj();
        List<String> cacheList = null;
        Map<String, Attach> attachMap = null;
        if (obj != null && obj instanceof Map) {
            attachMap = (Map<String, Attach>) obj;
            cacheList = new ArrayList<>(attachMap.keySet());
        }
        NoteInfo note = detailNote.getNoteInfo();
        note.setHasAttach(true);
        if (attachMap != null) {
            Attach lastAttach = detailNote.getLastAttach();
            if (lastAttach == null || !attachSids.contains(lastAttach.getSId())) {
                String lastSid = attachSids.get(attachSids.size() - 1);
                lastAttach = attachMap.get(lastSid);
                detailNote.setLastAttach(lastAttach);
            }
            KLog.d(TAG, "-----lastAttach-----" + lastAttach);
            
            //设置笔记所拥有的附件
            Map<String, Attach> map = new HashMap<>();
            for (String sid : attachSids) {
                Attach attach = attachMap.get(sid);
                if (attach == null) {
                    continue;
                }
                map.put(sid, attach);
            }
            if (map.size() > 0) {
                note.setAttaches(map);
            }
        }
        return cacheList;
    }
    
    /**
     * 添加笔记
     * @param detailNote 笔记的包装类
     */
    private void addNote(DetailNoteInfo detailNote) {
        NoteInfo note = detailNote.getNoteInfo();
        AttachText attachText = SystemUtil.getAttachSids(note.getContent());
        List<String> attachSids = attachText.getAttachSids();
        String sid = note.getSId();
        List<String> attSidList = null;
        if (attachSids != null && attachSids.size() > 0) {
            attSidList = getCacheAttachList(detailNote, attachSids);
        }
        note.setShowContent(attachText.getText());
        detailNote = mNoteManager.addDetailNote(detailNote, attSidList, attachSids);
        boolean success = detailNote != null;
        if (success) {
            NoteUtil.notifyAppWidgetList(this);
        }
        KLog.d(TAG, "---onHandleIntent---addNote----result---" + success + "---note---" + sid);
    }

    /**
     * 更新笔记
     * @param detailNote 笔记的包装类
     * @param updateContent 是否更新内容
     * @param detailLists 原始笔记中的清单，更新笔记时，需与原始笔记对比                     
     */
    private void updateNote(DetailNoteInfo detailNote, boolean updateContent, List<DetailList> detailLists) {
        Object obj = detailNote.getExtraObj();
        List<String> cacheList = null;
        if (obj != null && obj instanceof Map) {
            Map<String, Attach> attachMap = (Map<String, Attach>) obj;
            cacheList = new ArrayList<>(attachMap.keySet());
        }
        if (!updateContent && (cacheList == null || cacheList.size() == 0)) {
            //缓存中没有附件，也不做处理
            return;
        }
        
        KLog.d(TAG, "----updateNote---updateContent-----" + updateContent);
        
        NoteInfo note = detailNote.getNoteInfo();
        AttachText attachText = SystemUtil.getAttachSids(note.getContent());
        List<String> attachSids = attachText.getAttachSids();
        String sid = note.getSId();

        List<String> attSidList = null;
        if (attachSids != null && attachSids.size() > 0) {
            attSidList = getCacheAttachList(detailNote, attachSids);
        }
        note.setShowContent(attachText.getText());
        boolean success = false;
        if (updateContent) {
            success = mNoteManager.updateDetailNote(detailNote, attSidList, attachSids, detailLists);
        } else {
            //不需要更新内容
            mNoteManager.updateTextAttach(null, note, attSidList, attachSids, false);
            success = true;
        }
        if (success) {
            NoteUtil.notifyAppWidgetList(this);
        }
        KLog.d(TAG, "---onHandleIntent---updateNote----result---" + success + "---note---" + sid);
    }

    /**
     * 笔记的观察者
     */
    private class NoteObserver extends ContentObserver {

        public NoteObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            switch (notifyFlag) {
                case Provider.UserColumns.NOTIFY_FLAG:
                    switch (notifyType) {
                        case REMOVE:    //用户退出登录了，则移除当前跟用户有关的任务
                            KLog.d(TAG, "core service user is logout and this service will stop self user:" + data);
                            stopSelf();
//                            if (data != null && data instanceof User) {
//                                User user = (User) data;
//                                
//                            }
                            break;
                    }
                    break;
            }
        }
    }
}
