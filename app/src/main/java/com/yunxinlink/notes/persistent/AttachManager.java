package com.yunxinlink.notes.persistent;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.socks.library.KLog;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return values;
    }

    /**
     * 添加附件
     * @param attach 附件
     * @return 返回添加后的附件
     */
    public Attach addAttach(Attach attach) {
        return addAttach(attach, null, true);
    }

    /**
     * 添加附件
     * @param attach 附件
     * @param db 数据库，如果为null，则创建
     * @param notify 是否通知刷新界面
     * @return
     */
    private Attach addAttach(Attach attach, SQLiteDatabase db, boolean notify) {
        if (db == null) {
            db = mDBHelper.getWritableDatabase();
        }
        ContentValues values = initAttachValues(attach);
        long rowId = 0;
        try {
            rowId = db.insert(Provider.AttachmentColumns.TABLE_NAME, null, values);
        } catch (Exception e) {
            Log.e(TAG, "---addAttach--error--" + e.getMessage());
        }
        if (rowId > 0) {
            attach.setId((int) rowId);
            if (notify) {
                notifyObservers(Provider.AttachmentColumns.NOTIFY_FLAG, Observer.NotifyType.ADD, attach);
            }
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
        return updateAttach(attach, null, true);
    }

    /**
     * 更新附件信息
     * @param attach 附件
     * @param db 数据库，如果为null，则创建
     * @param notify 是否通知刷新界面
     * @return
     */
    private Attach updateAttach(Attach attach, SQLiteDatabase db, boolean notify) {
        if (db == null) {
            db = mDBHelper.getWritableDatabase();
        }
        ContentValues values = initUpdateAttachValues(attach);
        long rowId = 0;
        try {
            String selection = Provider.AttachmentColumns.SID + " = ?";
            String[] args = {attach.getSid()};
            rowId = db.update(Provider.AttachmentColumns.TABLE_NAME, values, selection, args);
        } catch (Exception e) {
            Log.e(TAG, "---updateAttach--error--" + e.getMessage());
        }
        if (rowId > 0) {
            if (notify) {
                notifyObservers(Provider.AttachmentColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, attach);
            }
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
        //先更新附件，如果更新行数为0，则添加
        boolean result = updateAttach(attach, db, false) != null;
        if (!result) {  //更新失败，则添加
            KLog.d(TAG, "add or update attach update failed and will add:" + attach);
            result = addAttach(attach, db, false) != null;
        }
        KLog.d(TAG, "add or update attach result:" + result);
        return result;
    }

    /**
     * 保存或者更新附件
     * @param attachList 附件的列表
     * @return 是否更新成功
     */
    public boolean addOrUpdateAttachs(List<Attach> attachList) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        boolean success = false;
        db.beginTransaction();
        try {
            for (Attach attach : attachList) {
                addOrUpdateAttach(attach, db);
            }
            db.setTransactionSuccessful();
            success = true;
        } catch (Exception e) {
            KLog.e(TAG, "add or update attach list error:" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return success;
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
        String filePath = attach.getLocalPath();
        if (filePath != null) {
            values.put(Provider.AttachmentColumns.LOCAL_PATH, filePath);
        }
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

    /**
     * 获取基本的附件信息
     * @param sidList 附件的sid集合
     * @return
     */
    public Map<String, Attach> getBasicAttachList(List<String> sidList) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String[] projection = {Provider.AttachmentColumns.HASH, Provider.AttachmentColumns.SYNC_STATE, Provider.AttachmentColumns.SID};
        Cursor cursor = null;
        Map<String, Attach> map = new HashMap<>();
        try {
            if (sidList.size() == 1) {  //只有一条记录
                String sid = sidList.get(0);
                cursor = db.query(Provider.AttachmentColumns.TABLE_NAME, projection, Provider.AttachmentColumns.SID + " = ? and " +
                                Provider.AttachmentColumns.SYNC_STATE + " is not null and " + Provider.AttachmentColumns.SYNC_STATE + " != ?",
                        new String[] {sid, String.valueOf(SyncState.SYNC_UP.ordinal())}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    Attach attach = new Attach();
                    attach.setSid(sid);
                    attach.setHash(cursor.getString(0));
                    attach.setSyncState(SyncState.valueOf(cursor.getInt(1)));
                    map.put(sid, attach);
                }
            } else {    //多条记录
                StringBuilder selection = new StringBuilder(Provider.AttachmentColumns.SYNC_STATE + " is not null and " + Provider.AttachmentColumns.SYNC_STATE + " != ? and " + Provider.AttachmentColumns.SID + " in (");
                List<String> argList = new ArrayList<>();
                argList.add(String.valueOf(SyncState.SYNC_UP.ordinal()));
                for (String sid : sidList) {
                    map.put(sid, null);
                    selection.append("?").append(Constants.TAG_COMMA);
                    argList.add(sid);
                }
                selection.deleteCharAt(selection.lastIndexOf(Constants.TAG_COMMA));
                selection.append(")");
                String[] args = new String[argList.size()];
                args = argList.toArray(args);
                cursor = db.query(Provider.AttachmentColumns.TABLE_NAME, projection, selection.toString(),
                        args, null, null, null);
    
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        Attach attach = new Attach();
                        attach.setHash(cursor.getString(0));
                        attach.setSyncState(SyncState.valueOf(cursor.getInt(1)));
                        attach.setSid(cursor.getString(2));
                        map.put(attach.getSid(), attach);
                    }
                }
            }
        } catch (Exception e) {
            KLog.e(TAG, "get basic attach error:" + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return map;
    }
}
