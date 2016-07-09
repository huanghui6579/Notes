package net.ibaixin.notes.service;

import android.app.IntentService;
import android.content.Intent;

import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.persistent.NoteManager;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

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
                        List<String> attachSids = SystemUtil.getAttachSids(note.getContent());
                        String sid = note.getSId();
                        note = mNoteManager.addNote(note, list, attachSids);
                        boolean success = note != null;
                        Log.d(TAG, "---onHandleIntent---addNote----result---" + success + "---note---" + sid);
                    }
                    break;
                case Constants.OPT_UPDATE_NOTE: //更新笔记
                    note = intent.getParcelableExtra(Constants.ARG_CORE_OBJ);
                    if (note != null) {
                        list = intent.getStringArrayListExtra(Constants.ARG_CORE_LIST);
                        boolean success = mNoteManager.updateNote(note);
                        Log.d(TAG, "---onHandleIntent---updateNote----result---" + success + "---note---" + note.getId());
                    }
                    break;
            }
        }
    }
}
