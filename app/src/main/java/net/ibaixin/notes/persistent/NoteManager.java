package net.ibaixin.notes.persistent;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.db.observer.Observable;
import net.ibaixin.notes.db.observer.Observer;
import net.ibaixin.notes.model.DeleteState;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.model.SyncState;
import net.ibaixin.notes.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * note表的服务层
 * @author huanghui1
 * @update 2016/3/7 17:22
 * @version: 0.0.1
 */
public class NoteManager extends Observable<Observer> {
    private static NoteManager mInstance = null;
    
    private DBHelper mDBHelper;
    
    private NoteManager() {
        mDBHelper = new DBHelper(NoteApplication.getInstance());
    }
    
    /**
     * 获取实例
     * @author huanghui1
     * @update 2016/3/7 17:38
     * @version: 1.0.0
     */
    public static NoteManager getInstance() {
        if (mInstance == null) {
            synchronized (NoteManager.class) {
                if (mInstance == null) {
                    mInstance = new NoteManager();
                }
            }
        }
        return mInstance;
    }
    
    /**
     * 获取当前用户下所有的笔记,默认按时间降序排列
     * @param user 对应的用户
     * @param args 额外的参数            
     * @author huanghui1
     * @update 2016/3/7 17:41
     * @version: 1.0.0
     */
    public List<NoteInfo> getAllNotes(User user, Bundle args) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;
        int folder = 0;
        if (args != null) {
            folder = args.getInt("folderId", 0);
        }
        int userId = 0;
        if (user != null) { //当前用户有登录
            userId = user.getId();
            selection = Provider.NoteColumns.USER_ID + " = ?";
            
            if (folder != 0) {
                selection += " AND " + Provider.NoteColumns.FOLDER_ID + " = ?";
                selectionArgs = new String[] {String.valueOf(userId), String.valueOf(folder)};
            } else {
                selectionArgs = new String[] {String.valueOf(userId)};
            }
        } else {    //当前用户没有登录
            if (folder != 0) {
                selection = Provider.NoteColumns.FOLDER_ID + " = ?";
                selectionArgs = new String[] {String.valueOf(folder)};
            }
        }
        List<NoteInfo> list = null;
        Cursor cursor = db.query(Provider.NoteColumns.TABLE_NAME, null, selection, selectionArgs, null, null, Provider.NoteColumns.DEFAULT_SORT);
        if (cursor != null) {
            list = new ArrayList<>();
            while (cursor.moveToNext()) {
                NoteInfo note = cursor2Note(cursor);

                list.add(note);
            }
            cursor.close();
        }
        return list;
    }
    
    /**
     * 加载当前用户所有的文件夹
     * @author huanghui1
     * @update 2016/3/8 15:14
     * @version: 1.0.0
     */
    public List<Folder> getAllFolders(User user, Bundle args) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;
        int userId = 0;
        if (user != null) { //当前用户有登录
            userId = user.getId();
            selection = Provider.FolderColumns.USER_ID + " = ?";
            selectionArgs = new String[] {String.valueOf(userId)};
        }
        List<Folder> list = null;
        Cursor cursor = db.query(Provider.FolderColumns.TABLE_NAME, null, selection, selectionArgs, null, null, Provider.FolderColumns.DEFAULT_SORT);
        if (cursor != null) {
            list = new ArrayList<>();
            while (cursor.moveToNext()) {
                Folder folder = new Folder();
                folder.setId(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns._ID)));
                folder.setSId(cursor.getString(cursor.getColumnIndex(Provider.FolderColumns.SID)));
                folder.setIsDefault(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.DEFAULT_FOLDER)) == 1);
                folder.setIsHidden(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.IS_HIDDEN)) == 1);
                folder.setIsLock(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.IS_LOCK)) == 1);
                folder.setName(cursor.getString(cursor.getColumnIndex(Provider.FolderColumns.NAME)));
                folder.setSyncState(SyncState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.SYNC_STATE))));
                folder.setDeleteState(DeleteState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.DELETE_STATE))));
                folder.setCreateTime(cursor.getLong(cursor.getColumnIndex(Provider.FolderColumns.CREATE_TIME)));
                folder.setModifyTime(cursor.getLong(cursor.getColumnIndex(Provider.FolderColumns.MODIFY_TIME)));
                folder.setCount(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns._COUNT)));

                list.add(folder);
            }
            cursor.close();
        }
        return list;
    }
    
    /**
     * 初始化note的添加数据
     * @author huanghui1
     * @update 2016/6/18 11:41
     * @version: 1.0.0
     */
    private ContentValues initNoteValues(NoteInfo note) {
        ContentValues values = new ContentValues();
        values.put(Provider.NoteColumns.CONTENT, note.getContent());
        values.put(Provider.NoteColumns.CREATE_TIME, note.getCreateTime());
        values.put(Provider.NoteColumns.FOLDER_ID, note.getFolderId());
        values.put(Provider.NoteColumns.HAS_ATTACH, (note.hasAttach() ? 1 : 0));
        values.put(Provider.NoteColumns.HASH, note.getHash());
        values.put(Provider.NoteColumns.KIND, note.getKind().name());
        values.put(Provider.NoteColumns.MODIFY_TIME, note.getModifyTime());
        values.put(Provider.NoteColumns.OLD_CONTENT, note.getOldContent());
        values.put(Provider.NoteColumns.REMIND_ID, note.getRemindId());
        values.put(Provider.NoteColumns.REMIND_TIME, note.getRemindTime());
        values.put(Provider.NoteColumns.DELETE_STATE, note.getDeleteState().ordinal());
        values.put(Provider.NoteColumns.SYNC_STATE, note.getSyncState().ordinal());
        values.put(Provider.NoteColumns.SID, note.getSId());
        return values;
    }
    
    private NoteInfo cursor2Note(Cursor cursor) {
        NoteInfo note = new NoteInfo();
        note.setId(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns._ID)));
        note.setSId(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.SID)));
        note.setUserId(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns.USER_ID)));
        note.setContent(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.CONTENT)));
        note.setRemindId(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns.REMIND_ID)));
        note.setRemindTime(cursor.getLong(cursor.getColumnIndex(Provider.NoteColumns.REMIND_TIME)));
        note.setFolderId(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.FOLDER_ID)));
        note.setKind(NoteInfo.NoteKind.valueOf(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.KIND))));
        note.setSyncState(SyncState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns.SYNC_STATE))));
        note.setDeleteState(DeleteState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns.DELETE_STATE))));
        note.setHasAttach(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns.HAS_ATTACH)) == 1);
        note.setCreateTime(cursor.getLong(cursor.getColumnIndex(Provider.NoteColumns.CREATE_TIME)));
        note.setModifyTime(cursor.getLong(cursor.getColumnIndex(Provider.NoteColumns.MODIFY_TIME)));
        note.setHash(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.HASH)));
        note.setOldContent(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.OLD_CONTENT)));
        return note;
    }
    
    /**
     * 添加一个记事本
     * @author huanghui1
     * @update 2016/6/18 11:14
     * @version: 1.0.0
     */
    public NoteInfo addNote(NoteInfo note) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = initNoteValues(note);
        long rowId = db.insert(Provider.NoteColumns.TABLE_NAME, null, values);
        if (rowId > 0) {
            note.setId((int) rowId);
            notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.ADD, note);
            return note;
        } else {
            return null;
        }
    }
    
    /**
     * 获取笔记的信息
     * @author huanghui1
     * @update 2016/6/18 14:35
     * @version: 1.0.0
     */
    public NoteInfo getNote(NoteInfo note) {
        NoteInfo info = null;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.query(Provider.NoteColumns.TABLE_NAME, null, Provider.NoteColumns._ID + " = ?", new String[] {String.valueOf(note.getId())}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            info = cursor2Note(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        return info;
    }
    
    /**
     * 更新笔记
     * @author huanghui1
     * @update 2016/6/18 16:46
     * @version: 1.0.0
     */
    public boolean updateNote(NoteInfo note) {
        ContentValues values = new ContentValues();
        String content = note.getContent();
        if (!TextUtils.isEmpty(content)) {
            values.put(Provider.NoteColumns.CONTENT, content);
        }
        DeleteState deleteState = note.getDeleteState();
        if (deleteState != null) {
            values.put(Provider.NoteColumns.DELETE_STATE, deleteState.ordinal());
        }
        String folderId = note.getFolderId();
        if (!TextUtils.isEmpty(folderId)) {
            values.put(Provider.NoteColumns.FOLDER_ID, folderId);
        }
        String hash = note.getHash();
        if (!TextUtils.isEmpty(hash)) {
            values.put(Provider.NoteColumns.HASH, hash);
        }
        NoteInfo.NoteKind noteKind = note.getKind();
        if (noteKind != null) {
            values.put(Provider.NoteColumns.KIND, noteKind.ordinal());
        }
        long time = note.getModifyTime();
        if (time == 0) {
            time = System.currentTimeMillis();
        }
        values.put(Provider.NoteColumns.MODIFY_TIME, time);
        String oldContent = note.getOldContent();
        if (!TextUtils.isEmpty(oldContent)) {
            values.put(Provider.NoteColumns.OLD_CONTENT, oldContent);
        }
        int remindId = note.getRemindId();
        if (remindId > 0) {
            values.put(Provider.NoteColumns.REMIND_ID, remindId);
        }
        long remindTime = note.getRemindTime();
        if (remindTime > 0) {
            values.put(Provider.NoteColumns.REMIND_TIME, remindTime);
        }
        String sid = note.getSId();
        if (!TextUtils.isEmpty(sid)) {
            values.put(Provider.NoteColumns.SID, sid);
        }
        SyncState syncState = note.getSyncState();
        if (syncState != null) {
            values.put(Provider.NoteColumns.SYNC_STATE, syncState.ordinal());
        }
        if (values.size() > 0) {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            int row = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns._ID + " = ?", new String[] {String.valueOf(note.getId())});
            if (row > 0) {
                notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, note);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
    
    /**
     * 获取笔记的信息
     * @author huanghui1
     * @update 2016/6/18 14:35
     * @version: 1.0.0
     */
    public NoteInfo getNote(int noteId) {
        NoteInfo info = new NoteInfo();
        info.setId(noteId);
        return getNote(info);
    }
    
}
