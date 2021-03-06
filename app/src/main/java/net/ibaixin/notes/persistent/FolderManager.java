package net.ibaixin.notes.persistent;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.cache.FolderCache;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.db.observer.Observable;
import net.ibaixin.notes.db.observer.Observer;
import net.ibaixin.notes.model.DeleteState;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.model.SyncState;
import net.ibaixin.notes.model.User;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件夹的管理器
 * @author huanghui1
 * @update 2016/6/22 20:47
 * @version: 0.0.1
 */
public class FolderManager extends Observable<Observer> {
    private static FolderManager mInstance;

    private static final String TAG = "NoteManager";

    private DBHelper mDBHelper;

    private FolderManager() {
        mDBHelper = new DBHelper(NoteApplication.getInstance());
    }

    public static FolderManager getInstance() {
        if (mInstance == null) {
            synchronized (FolderManager.class) {
                if (mInstance == null) {
                    mInstance = new FolderManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 将cursor转换成folder
     * @param cursor
     * @return
     */
    private Folder cursor2Folder(Cursor cursor) {
        Folder folder = new Folder();
        folder.setId(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns._ID)));
        folder.setSId(cursor.getString(cursor.getColumnIndex(Provider.FolderColumns.SID)));
        folder.setCount(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns._COUNT)));
        folder.setCreateTime(cursor.getLong(cursor.getColumnIndex(Provider.FolderColumns.CREATE_TIME)));
        folder.setModifyTime(cursor.getLong(cursor.getColumnIndex(Provider.FolderColumns.MODIFY_TIME)));
        folder.setDeleteState(DeleteState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.DELETE_STATE))));
        folder.setIsLock(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.IS_LOCK)) == 1);
        folder.setName(cursor.getString(cursor.getColumnIndex(Provider.FolderColumns.NAME)));
        folder.setSort(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.SORT)));
        folder.setSyncState(SyncState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.SYNC_STATE))));
        folder.setUserId(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.USER_ID)));
        return folder;
    }
    
    /**
     * 获取所有的文件夹
     * @param user 用户
     * @param args 参数
     * @author huanghui1
     * @update 2016/6/22 20:50
     * @version: 1.0.0
     */
    public List<Folder> getAllFolders(User user, Bundle args) {
        List<Folder> list = null;
        Cursor cursor = getAllFolderCursor(user, args);
        if (cursor != null) {
            list = new ArrayList<>();
            Map<String, Folder> map = new HashMap<>();
            while (cursor.moveToNext()) {
                Folder folder = cursor2Folder(cursor);
                list.add(folder);
                map.put(folder.getSId(), folder);
            }
            FolderCache.getInstance().setFolderMap(map);
            cursor.close();
        }
        return list;
    }
    
    /**
     * 获取所有文件夹的cursor
     * @author huanghui1
     * @update 2016/6/30 10:29
     * @version: 1.0.0
     */
    public Cursor getAllFolderCursor(User user, Bundle args) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;
        int userId = 0;
        boolean isRecycle = false;
        if (args != null) {
            isRecycle = args.getBoolean("isRecycle", false);
        }
        int deleteState = isRecycle ? 1 : 0;
        if (user != null) { //当前用户有登录
            userId = user.getId();
            if (deleteState == 0) {
                selection = Provider.FolderColumns.USER_ID + " = ? AND " + Provider.FolderColumns.DELETE_STATE + " is null or " + Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
            } else {
                selection = Provider.FolderColumns.USER_ID + " = ? AND " + Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
            }
            selectionArgs = new String[] {String.valueOf(userId)};
        } else {
            if (deleteState == 0) {
                selection = Provider.FolderColumns.DELETE_STATE + " is null or " + Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
            } else {
                selection = Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
            }
        }
        return db.query(Provider.FolderColumns.TABLE_NAME, null, selection, selectionArgs, null, null, Provider.FolderColumns.DEFAULT_SORT);
    }
    
    /**
     * 获取对应文件夹的笔记数量
     * @author huanghui1
     * @update 2016/6/23 20:04
     * @version: 1.0.0
     */
    public int getNoteCount(Folder folder) {
        int count = 0;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;
        if (folder != null) {
            int userId = folder.getUserId();
            if (userId == 0) {  //没有登录账号
                String folderId = folder.getSId();
                if (folderId == null) { //没有id，则查询所有
                    selection = Provider.NoteColumns.USER_ID + " = 0 AND " + Provider.NoteColumns.DELETE_STATE + " = ?";
                    selectionArgs = new String[] {String.valueOf(DeleteState.DELETE_NONE.ordinal())};
                } else {
                    selection = Provider.NoteColumns.USER_ID + " = 0 AND " + Provider.NoteColumns.FOLDER_ID + " = ? AND " + Provider.NoteColumns.DELETE_STATE + " = ?";
                    selectionArgs = new String[] {folderId, String.valueOf(DeleteState.DELETE_NONE.ordinal())};
                }
            } else {    //有登录账号
                String folderId = folder.getSId();
                if (folderId == null) { //没有id，则查询所有
                    selection = Provider.NoteColumns.USER_ID + " = ?" + Provider.NoteColumns.DELETE_STATE + " = ?";
                    selectionArgs = new String[] {String.valueOf(userId), String.valueOf(DeleteState.DELETE_NONE.ordinal())};
                } else {
                    selection = Provider.NoteColumns.USER_ID + " = ? AND " + Provider.NoteColumns.FOLDER_ID + " = ? AND " + Provider.NoteColumns.DELETE_STATE + " = ?";
                    selectionArgs = new String[] {String.valueOf(userId), folderId, String.valueOf(DeleteState.DELETE_NONE.ordinal())};
                }
            }
        } else {    //查询所有没有用户id的且没有被删除的
            selection = Provider.NoteColumns.USER_ID + " = 0 AND " + Provider.NoteColumns.DELETE_STATE + " = ?";
            selectionArgs = new String[] {String.valueOf(DeleteState.DELETE_NONE.ordinal())};
        }
        Cursor cursor = db.query(Provider.NoteColumns.TABLE_NAME, new String[] {"count(*)"}, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToNext()) {
            count = (int) cursor.getLong(0);
        }
        if (cursor != null) {
            cursor.close();
        }
        return count;
    }

    /**
     * 设置参数
     * @param folder
     * @return
     */
    private ContentValues initFolderValues(Folder folder) {
        ContentValues values = new ContentValues();
        values.put(Provider.FolderColumns.SID, folder.getSId());
        DeleteState deleteState = folder.getDeleteState();
        if (deleteState != null) {
            values.put(Provider.FolderColumns.DELETE_STATE, deleteState.ordinal());
        }
        SyncState syncState = folder.getSyncState();
        if (syncState != null) {
            values.put(Provider.FolderColumns.SYNC_STATE, syncState.ordinal());
        }
        values.put(Provider.FolderColumns.CREATE_TIME, folder.getCreateTime());
        values.put(Provider.FolderColumns.MODIFY_TIME, folder.getModifyTime());
        values.put(Provider.FolderColumns.IS_LOCK, folder.isLock());
        values.put(Provider.FolderColumns.NAME, folder.getName());
        values.put(Provider.FolderColumns.SORT, folder.getSort());
        values.put(Provider.FolderColumns.USER_ID, folder.getUserId());
        values.put(Provider.FolderColumns._COUNT, folder.getCount());
        return values;
    }

    /**
     * 更新文件夹的缓存
     * @param folder
     */
    private void updateFolderCache(Folder folder) {
        FolderCache.getInstance().getFolderMap().put(folder.getSId(), folder);
    }

    /**
     * 从缓存中移除
     * @param folder
     */
    private void removeFolderCache(Folder folder) {
        FolderCache.getInstance().getFolderMap().remove(folder.getSId());
    }
    
    /**
     * 添加笔记本文件夹
     * @param folder 文件夹
     * @author huanghui1
     * @update 2016/6/23 21:11
     * @version: 1.0.0
     */
    public Folder addFolder(Folder folder) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = initFolderValues(folder);
        db.beginTransaction();
        long rowId = 0;
        try {
            rowId = db.insert(Provider.FolderColumns.TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "----addFolder---error----" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (rowId > 0) {
            int id = (int) rowId;
            folder.setId(id);
            //添加文件夹时由触发器将sort字段更新为id的值
            folder.setSort(id);
            if (folder.isDefault()) {
                saveDefaultFolder(folder.getSId());
            }
            updateFolderCache(folder);
            notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.ADD, folder);
            return folder;
        } else {
            return null;
        }
    }
    
    /**
     * 更新笔记
     * @author huanghui1
     * @update 2016/6/24 14:32
     * @version: 1.0.0
     */
    public boolean updateFolder(Folder folder) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (folder.getUserId() > 0) {
            values.put(Provider.FolderColumns.USER_ID, folder.getUserId());
        }
        DeleteState deleteState = folder.getDeleteState();
        if (deleteState != null) {
            values.put(Provider.FolderColumns.DELETE_STATE, deleteState.ordinal());
        }
        SyncState syncState = folder.getSyncState();
        if (syncState != null) {
            values.put(Provider.FolderColumns.SYNC_STATE, syncState.ordinal());
        }
        values.put(Provider.FolderColumns._COUNT, folder.getCount());
        values.put(Provider.FolderColumns.SORT, folder.getSort());
        if (!TextUtils.isEmpty(folder.getName())) {
            values.put(Provider.FolderColumns.NAME, folder.getName());
        }
        values.put(Provider.FolderColumns.IS_LOCK, folder.isLock() ? 1 : 0);
        values.put(Provider.FolderColumns.MODIFY_TIME, folder.getModifyTime());
        db.beginTransaction();
        long rowId = 0;
        try {
            rowId = db.update(Provider.FolderColumns.TABLE_NAME, values, Provider.FolderColumns._ID + " = ?", new String[] {String.valueOf(folder.getId())});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "----updateFolder---error----" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (rowId > 0) {
            if (folder.isDefault()) {
                saveDefaultFolder(folder.getSId());
            }
            updateFolderCache(folder);
            notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, folder);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * 删除文件夹
     * @author huanghui1
     * @update 2016/6/25 11:46
     * @version: 1.0.0
     */
    public boolean deleteFolder(Folder folder) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Provider.FolderColumns.DELETE_STATE, DeleteState.DELETE_TRASH.ordinal());
        db.beginTransaction();
        int row = 0;
        try {
            row = db.update(Provider.FolderColumns.TABLE_NAME, values, Provider.FolderColumns._ID + " = ?", new String[] {String.valueOf(folder.getId())});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "-----deleteFolder----error-----" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (row > 0) {
            removeDefaultFolderSid(folder.getSId());
            removeFolderCache(folder);
            notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.DELETE, folder);
            return true;
        }
        return false;
    }
    
    /**
     * 将两个文件夹的顺序调换
     * @param fromFolder 主动调换的文件夹
     * @param toFolder 被调换的文件                  
     * @author huanghui1
     * @update 2016/6/25 16:09
     * @version: 1.0.0
     */
    public void sortFolder(Folder fromFolder, Folder toFolder) {
        /*UPDATE folder SET sort = (
                CASE
        WHEN _id = 1 THEN
        3
        WHEN _id = 3 THEN
        1
        ELSE
                sort
        END
        )*/
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(Provider.FolderColumns.TABLE_NAME).append(" set ").append(Provider.FolderColumns.SORT)
            .append(" = (CASE WHEN ").append(Provider.FolderColumns._ID).append(" = ? THEN ? WHEN ")
        .append(Provider.FolderColumns._ID).append(" = ? THEN ? ELSE ").append(Provider.FolderColumns.SORT)
        .append(" END)");
        Object[] seletionArgs = {fromFolder.getId(), fromFolder.getSort(), toFolder.getId(), toFolder.getSort()};
        db.execSQL(sb.toString(), seletionArgs);
        updateFolderCache(fromFolder);
        updateFolderCache(toFolder);
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.ARG_CORE_OBJ, fromFolder);
        map.put(Constants.ARG_SUB_OBJ, toFolder);
        notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, map);
    }
    
    /**
     * 修改文件夹的显示状态
     * @author huanghui1
     * @update 2016/6/25 16:42
     * @version: 1.0.0
     */
    public void updateShowState(Context context, boolean isShow) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.PREF_SHOW_FOLDER_ALL, isShow);
        editor.apply();
        
        NoteApplication noteApp = NoteApplication.getInstance();
        noteApp.setShowFolderAll(isShow);
        
        Folder folder = new Folder();
        folder.setShow(isShow);

        notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, folder);
    }
    
    /**
     * 保存默认的文件夹
     * @author huanghui1
     * @update 2016/6/25 10:57
     * @version: 1.0.0
     */
    public void saveDefaultFolder(String sid) {
        NoteApplication noteApp = NoteApplication.getInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(noteApp);
        String oldSid = sharedPreferences.getString(Constants.PREF_DEFAULT_FOLDER, "");
        if (!oldSid.equals(sid)) {  //不同，就保存
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREF_DEFAULT_FOLDER, sid);
            editor.apply();
            
            noteApp.setDefaultFolderSid(sid);
        }
    }
    
    /**
     * 移除默认的sid
     * @author huanghui1
     * @update 2016/6/25 11:50
     * @version: 1.0.0
     */
    public void removeDefaultFolderSid(String sid) {
        NoteApplication noteApp = NoteApplication.getInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(noteApp);
        String oldSid = sharedPreferences.getString(Constants.PREF_DEFAULT_FOLDER, "");
        if (oldSid.equals(sid)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(sid);
            editor.apply();

            noteApp.setDefaultFolderSid(null);
        }
    }
    
}
