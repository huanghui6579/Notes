package net.ibaixin.notes.persistent;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.db.Provider;
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
public class NoteManager {
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
    
}
