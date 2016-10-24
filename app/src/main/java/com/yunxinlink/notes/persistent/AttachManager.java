package com.yunxinlink.notes.persistent;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.db.DBHelper;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.db.observer.Observer;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.log.Log;

import java.util.List;

/**
 * 附件的数据库服务层
 * @author huanghui1
 * @update 2016/7/6 15:58
 * @version: 0.0.1
 */
public class AttachManager extends Observable<Observer> {
    private static final String TAG = "AttachManager";
    
    private static AttachManager mInstance;

    private DBHelper mDBHelper;

    private AttachManager() {
        mDBHelper = new DBHelper(NoteApplication.getInstance());
    }

    public static AttachManager getInstance() {
        if (mInstance == null) {
            synchronized (AttachManager.class) {
                if (mInstance == null) {
                    mInstance = new AttachManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 设置参数
     * @param attach 附件
     * @return 返回数据
     */
    private ContentValues initAttachValues(Attach attach) {
        ContentValues values = new ContentValues();
        values.put(Provider.AttachmentColumns.SID, attach.getSid());
        DeleteState deleteState = attach.getDeleteState();
        if (deleteState != null) {
            values.put(Provider.AttachmentColumns.DELETE_STATE, deleteState.ordinal());
        }
        SyncState syncState = attach.getSyncState();
        if (syncState != null) {
            values.put(Provider.AttachmentColumns.SYNC_STATE, syncState.ordinal());
        }
        values.put(Provider.AttachmentColumns.CREATE_TIME, attach.getCreateTime());
        values.put(Provider.AttachmentColumns.MODIFY_TIME, attach.getModifyTime());
        values.put(Provider.AttachmentColumns.DESCRIPTION, attach.getDescription());
        values.put(Provider.AttachmentColumns.FILE_NAME, attach.getFilename());
        values.put(Provider.AttachmentColumns.LOCAL_PATH, attach.getLocalPath());
        values.put(Provider.AttachmentColumns.NOTE_ID, attach.getNoteId());
        values.put(Provider.AttachmentColumns.SERVER_PATH, attach.getServerPath());
        values.put(Provider.AttachmentColumns.SIZE, attach.getSize());
        values.put(Provider.AttachmentColumns.TYPE, attach.getType());
        values.put(Provider.AttachmentColumns.USER_ID, attach.getUserId());
        values.put(Provider.AttachmentColumns.MIME_TYPE, attach.getMimeType());
        values.put(Provider.AttachmentColumns.HASH, attach.getHash());
        return values;
    }

    /**
     * 设置更新附件的参数
     * @param attach
     * @return
     */
    private ContentValues initUpdateAttachValues(Attach attach) {
        ContentValues values = new ContentValues();
        SyncState syncState = attach.getSyncState();
        if (syncState != null) {
            values.put(Provider.AttachmentColumns.SYNC_STATE, syncState.ordinal());
        }
        long modifyTime = attach.getModifyTime();
        if (modifyTime == 0) {
            modifyTime = System.currentTimeMillis();
        }
        values.put(Provider.AttachmentColumns.MODIFY_TIME, modifyTime);
        values.put(Provider.AttachmentColumns.SIZE, attach.getSize());
        values.put(Provider.AttachmentColumns.USER_ID, attach.getUserId());
        String hash = attach.getHash();
        if (hash != null) {
            values.put(Provider.AttachmentColumns.HASH, attach.getHash());
        }
    }

    /**
     * 添加附件
     * @param attach 附件
     * @return 返回添加后的附件
     */
    public Attach addAttach(Attach attach) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = initAttachValues(attach);
        long rowId = 0;
        try {
            rowId = db.insert(Provider.AttachmentColumns.TABLE_NAME, null, values);
        } catch (Exception e) {
            Log.e(TAG, "---addAttach--error--" + e.getMessage());
        }
        if (rowId > 0) {
            if (rowId > 0) {
                attach.setId((int) rowId);
            }
            notifyObservers(Provider.AttachmentColumns.NOTIFY_FLAG, Observer.NotifyType.ADD, attach);
            return attach;
        } else {
            return null;
        }
    }

    /**
     * 更新附件
     * @param attach
     * @return
     */
    public Attach updateAttach(Attach attach) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = initUpdateAttachValues(attach);
        db.beginTransaction();
        long rowId = 0;
        try {
            String selection = Provider.AttachmentColumns.SID + " = ?";
            String[] args = {attach.getSid()};
            rowId = db.update(Provider.AttachmentColumns.TABLE_NAME, values, selection, args);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "---updateAttach--error--" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (rowId > 0) {
            notifyObservers(Provider.AttachmentColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, attach);
            return attach;
        } else {
            return null;
        }
    }

    /**
     * 添加或者更新附件
     * @param attach 附件信息
     * @return 是否更新成功
     */
    public boolean addOrUpdateAttach(Attach attach, SQLiteDatabase db) {
        
    }

    /**
     * 更新笔记附件的同步状态
     * @param attach
     * @return
     */
    public boolean updateAttachSyncState(Attach attach) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Provider.AttachmentColumns.SYNC_STATE, attach.getSyncState().ordinal());
        long rowId = 0;
        try {
            String selection = Provider.AttachmentColumns.SID + " = ?";
            String[] args = {attach.getSid()};
            rowId = db.update(Provider.AttachmentColumns.TABLE_NAME, values, selection, args);
        } catch (Exception e) {
            Log.e(TAG, "---updateAttach-sync-error--" + e.getMessage());
        }
        boolean success = rowId > 0;
        if (success) {
            notifyObservers(Provider.AttachmentColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, attach);
        }
        return success;
    }

    /**
     * 删除附件，仅仅是设为删除状态
     * @author huanghui1
     * @update 2016/6/25 11:46
     * @version: 1.0.0
     */
    public boolean deleteAttach(Attach attach) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Provider.AttachmentColumns.DELETE_STATE, attach.getDeleteState().ordinal());
        db.beginTransaction();
        int row = 0;
        try {
            row = db.update(Provider.AttachmentColumns.TABLE_NAME, values, Provider.AttachmentColumns._ID + " = ?", new String[] {String.valueOf(attach.getId())});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "-----deleteAttach----error-----" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (row > 0) {
            notifyObservers(Provider.AttachmentColumns.NOTIFY_FLAG, Observer.NotifyType.DELETE, attach);
            return true;
        }
        return false;
    }

    /**
     * 移除指定的附件记录，只删除数据库记录，彻底删除
     * @param sidList 附件的sid集合
     * @param fileList 本地文件的集合，需要删除本地文件               
     * @return
     */
    public boolean removeAttachs(List<String> sidList, List<String> fileList) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        int size = sidList.size();
        String selection = null;
        String[] selectionArgs = new String[size];
        if (size == 1) { //只有一个附件
            selection = Provider.AttachmentColumns.NOTE_ID + " IS NULL AND " + Provider.AttachmentColumns.SID + " = ?";
            selectionArgs[0] = sidList.get(0);
        } else {    //多个附件
            StringBuilder sb = new StringBuilder(Provider.AttachmentColumns.NOTE_ID + " IS NULL AND " + Provider.AttachmentColumns.SID).append(" in (");
            for (int i = 0; i < size; i++) {
                String sid = sidList.get(i);
                selectionArgs[i] = sid;
                sb.append("?").append(Constants.TAG_COMMA);
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(")");
            selection = sb.toString();
        }
        db.beginTransaction();
        int row = 0;
        try {
            row = db.delete(Provider.AttachmentColumns.TABLE_NAME, selection, selectionArgs);
            Log.d(TAG, "--removeAttachs---success--list---" + sidList);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "--removeAttachs--error--" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        boolean success = row > 0;
        if (success) {
            //删除本地文件
            FileUtil.deleteFiles(fileList);
        }
        return success;

    }
}
