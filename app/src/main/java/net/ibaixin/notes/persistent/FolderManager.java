package net.ibaixin.notes.persistent;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.cache.FolderCache;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.model.DeleteState;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.model.SyncState;
import net.ibaixin.notes.model.User;

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
public class FolderManager {
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
        folder.setIsDefault(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.DEFAULT_FOLDER)) == 1);
        folder.setIsHidden(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.IS_HIDDEN)) == 1);
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
            selection = Provider.FolderColumns.USER_ID + " = ? AND " + Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
            selectionArgs = new String[] {String.valueOf(userId)};
        } else {
            selection = Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
        }
        List<Folder> list = null;
        Cursor cursor = db.query(Provider.FolderColumns.TABLE_NAME, null, selection, selectionArgs, null, null, Provider.FolderColumns.DEFAULT_SORT);
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
}
