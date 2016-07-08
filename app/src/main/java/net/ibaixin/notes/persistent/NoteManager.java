package net.ibaixin.notes.persistent;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.cache.FolderCache;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.db.observer.Observable;
import net.ibaixin.notes.db.observer.Observer;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.model.DeleteState;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.model.SyncState;
import net.ibaixin.notes.model.User;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * note表的服务层
 * @author huanghui1
 * @update 2016/3/7 17:22
 * @version: 0.0.1
 */
public class NoteManager extends Observable<Observer> {
    private static NoteManager mInstance = null;
    
    private static final String TAG = "NoteManager";
    
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
        String folder = null;
        boolean isRecycle = false;
        if (args != null) {
            folder = args.getString("folderId", null);
            isRecycle = args.getBoolean("isRecycle", false);
        }
        int deleteState = isRecycle ? 1 : 0;
        //是否加载回收站里的笔记
        int userId = 0;
        if (user != null) { //当前用户有登录
            userId = user.getId();
            if (deleteState == 0) {
                selection = Provider.NoteColumns.USER_ID + " = ? AND (" + Provider.NoteColumns.DELETE_STATE + " is null or " + Provider.NoteColumns.DELETE_STATE + " = " + deleteState + ")";
            } else {
                selection = Provider.NoteColumns.USER_ID + " = ? AND " + Provider.NoteColumns.DELETE_STATE + " = " + deleteState;
            }
            
            if (!TextUtils.isEmpty(folder)) {
                selection += " AND " + Provider.NoteColumns.FOLDER_ID + " = ?";
                selectionArgs = new String[] {String.valueOf(userId), folder};
            } else {
                selectionArgs = new String[] {String.valueOf(userId)};
            }
        } else {    //当前用户没有登录
            if (!TextUtils.isEmpty(folder)) {
                if (deleteState == 0) {
                    selection = Provider.NoteColumns.FOLDER_ID + " = ? AND (" + Provider.NoteColumns.DELETE_STATE + " is null or " + Provider.NoteColumns.DELETE_STATE + " = " + deleteState + ")";
                } else {
                    selection = Provider.NoteColumns.FOLDER_ID + " = ? AND " + Provider.NoteColumns.DELETE_STATE + " = " + deleteState;
                }
                selectionArgs = new String[] {folder};
            } else {
                if (deleteState == 0) {
                    selection = Provider.NoteColumns.DELETE_STATE + " is null or " + Provider.NoteColumns.DELETE_STATE + " = ?";
                } else {
                    selection = Provider.NoteColumns.DELETE_STATE + " = ?";
                }
                selectionArgs = new String[] {String.valueOf(deleteState)};
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
     * 返回移动笔记到垃圾桶的数据
     * @param note 笔记
     * @return 返回数据
     */
    private ContentValues initTrashValues(NoteInfo note) {

        note.setDeleteState(DeleteState.DELETE_TRASH);
        note.setSyncState(SyncState.SYNC_UP);
        note.setModifyTime(System.currentTimeMillis());
        
        ContentValues values = new ContentValues();
        values.put(Provider.NoteColumns.DELETE_STATE, note.getDeleteState().ordinal());
        values.put(Provider.NoteColumns.SYNC_STATE, note.getSyncState().ordinal());
        values.put(Provider.NoteColumns.MODIFY_TIME, note.getModifyTime());
        return values;
    }
    
    /**
     * 删除笔记
     * @author huanghui1
     * @update 2016/6/21 15:16
     * @version: 1.0.0
     */
    public boolean deleteNote(NoteInfo note) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = initTrashValues(note);
        int row = 0;
        try {
            db.beginTransaction();
            row = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns._ID + " = ?", new String[] {String.valueOf(note.getId())});
            updateFolderCount(note, false);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "--deleteNote---error---" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (row > 0) {
            notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.DELETE, note);
            return true;
        } else {
            Log.w(TAG, "-------deleteNote---failed----");
            return false;
        }
    }

    /**
     * 删除多条笔记，移动到回收站，并不是真正的删除
     * @param noteList 要删除的笔记的集合
     * @return 返回是否删除成功
     */
    public boolean deleteNote(List<NoteInfo> noteList) {
        if (noteList == null || noteList.size() == 0) {
            Log.d(TAG, "----deleteNote---list--size--0--success---");
            return true;
        }
        if (noteList.size() == 1) { //只有一条
            return deleteNote(noteList.get(0));
        } else {
            int row = 0;
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            try {
                ContentValues values = initTrashValues(noteList.get(0));
                //拼凑sql语句
                StringBuilder builder = new StringBuilder(Provider.NoteColumns._ID);
                builder.append(" in (");
                int size = noteList.size();
                String[] selectionArgs = new String[size];
                for (int i = 0; i < size; i++) {
                    NoteInfo note = noteList.get(i);
                    builder.append("?").append(Constants.TAG_COMMA);
                    selectionArgs[i] = String.valueOf(note.getId());
                }
                builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_COMMA));
                builder.append(")");
                
                db.beginTransaction();
                row = db.update(Provider.NoteColumns.TABLE_NAME, values, builder.toString(), selectionArgs);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e("TAG", "--deleteNote--list--error--" + e.getMessage());
            } finally {
                db.endTransaction();
            }
            if (row > 0) {
                notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.DELETE, noteList);
                return true;
            } else {
                Log.w(TAG, "-------deleteNote--list--failed----");
                return false;
            }
        }
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
        DeleteState deleteState = note.getDeleteState();
        if (deleteState != null) {
            values.put(Provider.NoteColumns.DELETE_STATE, deleteState.ordinal());
        }
        SyncState syncState = note.getSyncState();
        if (syncState != null) {
            values.put(Provider.NoteColumns.SYNC_STATE, syncState.ordinal());
        }
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
    
    private Attach cursor2Attach(Cursor cursor) {
        Attach attach = new Attach();
        attach.setId(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns._ID)));
        attach.setSId(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.SID)));
        attach.setUserId(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns.USER_ID)));
        attach.setNoteId(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.NOTE_ID)));
        attach.setType(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns.TYPE)));
        attach.setLocalPath(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.LOCAL_PATH)));
        attach.setCreateTime(cursor.getLong(cursor.getColumnIndex(Provider.AttachmentColumns.CREATE_TIME)));
        attach.setDecription(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.DECRIPTION)));
        attach.setSyncState(SyncState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns.SYNC_STATE))));
        attach.setDeleteState(DeleteState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns.DELETE_STATE))));
        attach.setFilename(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.FILE_NAME)));
        attach.setModifyTime(cursor.getLong(cursor.getColumnIndex(Provider.AttachmentColumns.MODIFY_TIME)));
        attach.setServerPath(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.SERVER_PATH)));
        attach.setSize(cursor.getLong(cursor.getColumnIndex(Provider.AttachmentColumns.SIZE)));
        return attach;
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
        db.beginTransaction();
        long rowId = 0;
        try {
            rowId = db.insert(Provider.NoteColumns.TABLE_NAME, null, values);
            updateFolderCount(note, true);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "--addNote--error--" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (rowId > 0) {
            note.setId((int) rowId);
            notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.ADD, note);
            return note;
        } else {
            return null;
        }
    }

    /**
     * 从缓存里获取文件夹
     * @param sid
     * @return
     */
    private Folder getCacheFolder(String sid) {
        return FolderCache.getInstance().getFolderMap().get(sid);
    }
    
    /**
     * 更新文件夹里笔记的数量
     * @author huanghui1
     * @update 2016/6/29 19:20
     * @version: 1.0.0
     */
    private void updateFolderCount(NoteInfo note, boolean isAdd) {
        Folder folder = getCacheFolder(note.getFolderId());
        if (folder != null) {
            if (isAdd) {
                folder.setCount(folder.getCount() + 1);
            } else {
                folder.setCount(folder.getCount() - 1);
            }
            folder.setModifyTime(note.getModifyTime());
            folder.setSyncState(SyncState.SYNC_UP);
        }
    }
    
    /**
     * 更新文件夹的状态
     * @author huanghui1
     * @update 2016/6/29 19:21
     * @version: 1.0.0
     */
    private void updateFolder(NoteInfo note) {
        Folder folder = getCacheFolder(note.getFolderId());
        if (folder != null) {
            folder.setModifyTime(note.getModifyTime());
            folder.setSyncState(SyncState.SYNC_UP);
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
            
            //获取笔记中的附件
            Map<String, Attach> map = getAttachs(info, db);

            info.setAttaches(map);
        }
        if (cursor != null) {
            cursor.close();
        }
        return info;
    }

    /**
     * 获取笔记中的附件列表
     * @param note 笔记
     * @return 返回笔记中的附件列表
     */
    public Map<String, Attach> getAttachs(NoteInfo note, SQLiteDatabase db) {
        String selection = Provider.AttachmentColumns.NOTE_ID + " = ? AND (" + Provider.AttachmentColumns.DELETE_STATE + " IS NULL OR " + Provider.AttachmentColumns.DELETE_STATE + " = ?)";
        String[] selectionArgs = {note.getSId(), String.valueOf(DeleteState.DELETE_NONE.ordinal())};
        Cursor cursor = db.query(Provider.AttachmentColumns.TABLE_NAME, null, selection, selectionArgs, null, null, null, null);
        Map<String, Attach> map = null;
        if (cursor != null) {
            map = new HashMap<>();
            while (cursor.moveToNext()) {
                Attach attach = cursor2Attach(cursor);
                if (attach != null) {
                    map.put(attach.getSId(), attach);
                }
            }
        }
        return map;
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
            values.put(Provider.NoteColumns.KIND, noteKind.name());
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
                updateFolder(note);
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
     * 初始化移动笔记的数据
     * @param note
     * @param time
     * @param folderId
     * @return
     */
    private ContentValues initNoteMoveValues(NoteInfo note, long time, String folderId) {
        note.setFolderId(folderId);
        note.setSyncState(SyncState.SYNC_UP);
        note.setModifyTime(time);

        ContentValues values = new ContentValues();
        values.put(Provider.NoteColumns.FOLDER_ID, note.getFolderId());
        values.put(Provider.NoteColumns.SYNC_STATE, note.getSyncState().ordinal());
        values.put(Provider.NoteColumns.MODIFY_TIME, note.getModifyTime());
        return values;
    }

    /**
     * 保存移动笔记后的文件夹
     * @param db
     * @param oldFolder
     * @param newFolder
     * @param time
     */
    private void updateNoteMoteFolder(SQLiteDatabase db, Folder oldFolder, Folder newFolder, long time) {
        if (oldFolder != null && !oldFolder.isEmpty()) { //非“所有文件夹”

            SyncState syncState = SyncState.SYNC_UP;

            oldFolder.setCount(oldFolder.getCount() - 1);
            oldFolder.setModifyTime(time);
            oldFolder.setSyncState(syncState);

            newFolder.setCount(newFolder.getCount() + 1);
            newFolder.setModifyTime(time);
            newFolder.setSyncState(syncState);

            //更新文件夹的数量，文件夹的其他字段的更新有note表中的触发器来更新
                    /*UPDATE folder SET _count = (
                        CASE
                    WHEN _id = ? THEN
                    3
                    WHEN _id = ? THEN
                    1
                    ELSE
                            _count
                    END
                    ), modify_time = ?, sync_state = ? where _id in (?, ?)*/
            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE ").append(Provider.FolderColumns.TABLE_NAME).append(" set ").append(Provider.FolderColumns._COUNT)
                    .append(" = (CASE WHEN ").append(Provider.FolderColumns._ID).append(" = ? THEN ? WHEN ")
                    .append(Provider.FolderColumns._ID).append(" = ? THEN ? ELSE ").append(Provider.FolderColumns._COUNT)
                    .append(" END), ").append(Provider.FolderColumns.MODIFY_TIME).append(" = ?, ").append(Provider.FolderColumns.SYNC_STATE)
                    .append(" = ? WHERE ").append(Provider.FolderColumns._ID).append(" IN (?, ?)");
            Object[] seletionArgs = {oldFolder.getId(), oldFolder.getCount(), newFolder.getId(), newFolder.getCount(),
                    time, syncState.ordinal(), oldFolder.getId(), newFolder.getId()};
            db.execSQL(sb.toString(), seletionArgs);
        } else {
            //原始文件夹是所有文件夹，则只更新目的文件夹
                    /*UPDATE folder SET _count = ? where _id = ?*/
            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE ").append(Provider.FolderColumns.TABLE_NAME).append(" set ").append(Provider.FolderColumns._COUNT)
                    .append(" = ? WHERE ").append(Provider.FolderColumns._ID).append(" = ?");
            Object[] seletionArgs = {newFolder.getCount(), newFolder.getId()};
            db.execSQL(sb.toString(), seletionArgs);
        }
    }

    /**
     * 更新笔记的文件夹，移动到指定的文件夹
     * @param notes 笔记
     * @param oldFolder 原始的文件夹
     * @param newFolder 新的文件夹                 
     * @author huanghui1
     * @update 2016/6/30 11:52
     * @version: 1.0.0
     */
    public boolean move2Folder(List<NoteInfo> notes, Folder oldFolder, Folder newFolder) {
        long time = System.currentTimeMillis();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            List<NoteInfo> noteList = new ArrayList<>();
            for (NoteInfo note : notes) {
                if (newFolder.getSId().equals(note.getFolderId())) {
                    continue;
                }
                ContentValues values = initNoteMoveValues(note, time, newFolder.getSId());
    
                int row = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns._ID + " = ?", new String[] {String.valueOf(note.getId())});
                
                if (row > 0) {
                    updateNoteMoteFolder(db, oldFolder, newFolder, time);
                    noteList.add(note);
                }
                
            }
            if (noteList.size() > 0) {
                if (noteList.size() == 1) {
                    notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.MOVE, noteList.get(0));
                } else {
                    notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.MOVE, noteList);
                }
            } else {
                Log.d(TAG, "--move2Folder----result----list---0----not---notifyObservers---");
            }
            db.setTransactionSuccessful();
            return true;
        } catch (SQLException e) {
            Log.e(TAG, "---move2Folder---list---error----" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return false;
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
