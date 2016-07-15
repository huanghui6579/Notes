package net.ibaixin.notes.persistent;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.db.observer.Observable;
import net.ibaixin.notes.db.observer.Observer;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.model.DeleteState;
import net.ibaixin.notes.model.SyncState;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.FileUtil;
import net.ibaixin.notes.util.log.Log;

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
        values.put(Provider.AttachmentColumns.SID, attach.getSId());
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
        values.put(Provider.AttachmentColumns.DECRIPTION, attach.getDecription());
        values.put(Provider.AttachmentColumns.FILE_NAME, attach.getFilename());
        values.put(Provider.AttachmentColumns.LOCAL_PATH, attach.getLocalPath());
        values.put(Provider.AttachmentColumns.NOTE_ID, attach.getNoteId());
        values.put(Provider.AttachmentColumns.SERVER_PATH, attach.getServerPath());
        values.put(Provider.AttachmentColumns.SIZE, attach.getSize());
        values.put(Provider.AttachmentColumns.TYPE, attach.getType());
        return values;
    }

    /**
     * 添加附件
     * @param attach 附件
     * @return 返回添加后的附件
     */
    public Attach addAttach(Attach attach) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = initAttachValues(attach);
        db.beginTransaction();
        long rowId = 0;
        try {
            rowId = db.insert(Provider.AttachmentColumns.TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "---addAttach--error--" + e.getMessage());
        } finally {
            db.endTransaction();
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
