package com.yunxinlink.notes.persistent;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.db.DBHelper;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.db.observer.Observer;
import com.yunxinlink.notes.listener.OnLoadCallback;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.QueryType;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.log.Log;

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
     * 根据Folder的查询条件来获取笔记本的信息
     * @param param
     * @return
     */
    private Folder getFolderInfo(Folder param, SQLiteDatabase db) {
        if (db == null) {
            db = mDBHelper.getReadableDatabase();
        }
        Folder folder = null;
        String selection = null;
        String[] args = null;
        if (param.checkId()) {  //有id
            selection = Provider.FolderColumns._ID + " = ?";
            args = new String[] {String.valueOf(param.getId())};
        } else {
            selection = Provider.FolderColumns.SID + " = ?";
            args = new String[] {param.getSid()};
        }
        Cursor cursor = db.query(Provider.FolderColumns.TABLE_NAME, null, selection, args, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            folder = cursor2Folder(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        return folder;
    }

    /**
     * 根据Folder的查询条件来获取笔记本的信息
     * @param param
     * @return
     */
    public Folder getFolderInfo(Folder param) {
        return getFolderInfo(param, null);
    }

    /**
     * 将cursor转换成folder
     * @param cursor
     * @return
     */
    private Folder cursor2Folder(Cursor cursor) {
        Folder folder = new Folder();
        folder.setId(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns._ID)));
        folder.setSid(cursor.getString(cursor.getColumnIndex(Provider.FolderColumns.SID)));
        folder.setCount(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns._COUNT)));
        folder.setCreateTime(cursor.getLong(cursor.getColumnIndex(Provider.FolderColumns.CREATE_TIME)));
        folder.setModifyTime(cursor.getLong(cursor.getColumnIndex(Provider.FolderColumns.MODIFY_TIME)));
        folder.setDeleteState(DeleteState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.DELETE_STATE))));
        folder.setLock(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.IS_LOCK)) == 1);
        folder.setName(cursor.getString(cursor.getColumnIndex(Provider.FolderColumns.NAME)));
        folder.setSort(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.SORT)));
        folder.setSyncState(SyncState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.FolderColumns.SYNC_STATE))));
        folder.setHash(cursor.getString(cursor.getColumnIndex(Provider.FolderColumns.HASH)));
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
        //加载的笔记本的类型，true：只加载回收站的笔记本，false，只加载非回收站的笔记本
        int queryValue = -1;
        if (args != null) {
            queryValue = args.getInt(Constants.ARG_QUERY_TYPE, -1);
        }
        QueryType queryType = QueryType.valueOf(queryValue);
        Cursor cursor = getAllFolderCursor(user, args);
        if (cursor != null) {
            list = new ArrayList<>();
            Map<String, Folder> map = new HashMap<>();
            while (cursor.moveToNext()) {
                Folder folder = cursor2Folder(cursor);
                map.put(folder.getSid(), folder);
                if (QueryType.TRASH == queryType) {//只加载回收站的笔记本
                    if (folder.isTrashed()) {
                        list.add(folder);
                    }
                } else {    //只加载没有被删除的笔记本
                    if (folder.isNormal()) {
                        list.add(folder);
                    }
                }
                
            }
            FolderCache.getInstance().setFolderMap(map);
            cursor.close();
        }
        return list;
    }

    /**
     * 加载所有的笔记本
     * @param user
     * @param args
     * @param callback
     */
    public void loadAllFolders(User user, Bundle args, OnLoadCallback<Map<String, Folder>> callback) {
        Map<String, Folder> map = FolderCache.getInstance().getFolderMap();
        if (map == null || map.size() == 0) {    //没有笔记本
            SystemUtil.getThreadPool().execute(new NoteTask(user, args, callback) {
                @Override
                public void run() {
                    User u = (User) params[0];
                    Bundle extra = (Bundle) params[1];
                    OnLoadCallback<Map<String, Folder>> tCallback = (OnLoadCallback<Map<String, Folder>>) params[2];

                    getAllFolders(u, extra);
                    KLog.d(TAG, "load all folders completed");
                    if (tCallback != null) {
                        KLog.d(TAG, "load all folders completed onLoadCompleted");
                        tCallback.onLoadCompleted(FolderCache.getInstance().getFolderMap(), extra);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onLoadCompleted(map, args);
            }
        }
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
        if (user != null) { //当前用户有登录
            if (!user.checkOnLine()) {  //用户离线、退出登录或者不可用
                KLog.d(TAG, "get all folder user is offline or disable :" + user);
                return null;
            }
            userId = user.getId();
            selection = Provider.FolderColumns.USER_ID + " = ?";
            /*if (filterRecycle) {    //需要过滤掉回收站中的数据
                if (deleteState == 0) {
                    selection = Provider.FolderColumns.USER_ID + " = ? AND " + Provider.FolderColumns.DELETE_STATE + " is null or " + Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
                } else {
                    selection = Provider.FolderColumns.USER_ID + " = ? AND " + Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
                }
            } else {
                
            }*/
            selectionArgs = new String[] {String.valueOf(userId)};
        }/* else {
            if (deleteState == 0) {
                selection = Provider.FolderColumns.DELETE_STATE + " is null or " + Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
            } else {
                selection = Provider.FolderColumns.DELETE_STATE + " = " + deleteState;
            }
        }*/
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
        if (folder != null && !folder.isEmpty()) {
            int userId = folder.getUserId();
            if (userId == 0) {  //没有登录账号
                String folderId = folder.getSid();
                if (folderId == null) { //没有id，则查询所有
                    selection = Provider.NoteColumns.USER_ID + " = 0 AND " + Provider.NoteColumns.DELETE_STATE + " = ?";
                    selectionArgs = new String[] {String.valueOf(DeleteState.DELETE_NONE.ordinal())};
                } else {
                    selection = Provider.NoteColumns.USER_ID + " = 0 AND " + Provider.NoteColumns.FOLDER_ID + " = ? AND " + Provider.NoteColumns.DELETE_STATE + " = ?";
                    selectionArgs = new String[] {folderId, String.valueOf(DeleteState.DELETE_NONE.ordinal())};
                }
            } else {    //有登录账号
                String folderId = folder.getSid();
                if (folderId == null) { //没有id，则查询所有
                    selection = Provider.NoteColumns.USER_ID + " = ?" + Provider.NoteColumns.DELETE_STATE + " = ?";
                    selectionArgs = new String[] {String.valueOf(userId), String.valueOf(DeleteState.DELETE_NONE.ordinal())};
                } else {
                    selection = Provider.NoteColumns.USER_ID + " = ? AND " + Provider.NoteColumns.FOLDER_ID + " = ? AND " + Provider.NoteColumns.DELETE_STATE + " = ?";
                    selectionArgs = new String[] {String.valueOf(userId), folderId, String.valueOf(DeleteState.DELETE_NONE.ordinal())};
                }
            }
        } else {    //查询所有没有用户id的且没有被删除的
            int userId = folder == null ? 0 : folder.getUserId();
            selection = Provider.NoteColumns.USER_ID + " = ? AND " + Provider.NoteColumns.DELETE_STATE + " = ?";
            selectionArgs = new String[] {String.valueOf(userId), String.valueOf(DeleteState.DELETE_NONE.ordinal())};
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
        String sid = folder.getSid();
        if (sid != null) {
            values.put(Provider.FolderColumns.SID, folder.getSid());
        }
        DeleteState deleteState = folder.getDeleteState();
        if (deleteState != null) {
            values.put(Provider.FolderColumns.DELETE_STATE, deleteState.ordinal());
        }
        SyncState syncState = folder.getSyncState();
        if (syncState != null) {
            values.put(Provider.FolderColumns.SYNC_STATE, syncState.ordinal());
        }
        long createTime = folder.getCreateTime();
        if (createTime != 0) {
            values.put(Provider.FolderColumns.CREATE_TIME, createTime);
        }
        long modifyTime = folder.getModifyTime();
        if (modifyTime == 0) {
            modifyTime = System.currentTimeMillis();
        }
        values.put(Provider.FolderColumns.MODIFY_TIME, modifyTime);
        values.put(Provider.FolderColumns.IS_LOCK, folder.isLock() ? 1 : 0);
        String name = folder.getName();
        if (name != null) {
            values.put(Provider.FolderColumns.NAME, name);
        }
        int sort = folder.getSort();
        if (sort != 0) {
            values.put(Provider.FolderColumns.SORT, sort);
        }
        String hash = folder.getHash();
        if (hash != null) {
            values.put(Provider.FolderColumns.HASH, hash);
        }
        int userId = folder.getUserId();
        if (userId != 0) {
            values.put(Provider.FolderColumns.USER_ID, userId);
        }
        values.put(Provider.FolderColumns._COUNT, folder.getCount());
        return values;
    }

    /**
     * 更新笔记本的同步状态
     * @param folder
     * @return
     */
    private ContentValues initSyncFolderValues(Folder folder) {
        ContentValues values = new ContentValues();
        values.put(Provider.FolderColumns.MODIFY_TIME, folder.getModifyTime());
        SyncState syncState = folder.getSyncState();
        if (syncState != null) {
            values.put(Provider.FolderColumns.SYNC_STATE, syncState.ordinal());
        }
        return values;
    }

    /**
     * 更新文件夹的缓存
     * @param folder
     */
    private void updateFolderCache(Folder folder) {
        FolderCache.getInstance().getFolderMap().put(folder.getSid(), folder);
    }

    /**
     * 从缓存中移除
     * @param folder
     */
    private void removeFolderCache(Folder folder) {
        FolderCache.getInstance().getFolderMap().remove(folder.getSid());
    }

    /**
     * 为Folder设置sort
     * @param folder
     * @return
     */
    private int setSort(Folder folder) {
        int sort = folder.getSort();
        if (sort == 0) {    //没有设置排序
            List<Folder> sortList = FolderCache.getInstance().getSortFolders();
            if (SystemUtil.isEmpty(sortList)) {
                sort = 1;
            } else {
                sort = sortList.get(sortList.size() - 1).getSort() + 1;
            }
            folder.setSort(sort);
        }
        return sort;
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
        setSort(folder);
        String hash = folder.generateHash();
        folder.setHash(hash);
        ContentValues values = initFolderValues(folder);
        long rowId = 0;
        try {
            rowId = db.insert(Provider.FolderColumns.TABLE_NAME, null, values);
        } catch (Exception e) {
            Log.e(TAG, "----addFolder---error----" + e.getMessage());
        }
        if (rowId > 0) {
            int id = (int) rowId;
            folder.setId(id);
            //添加文件夹时由触发器将sort字段更新为id的值
            folder.setSort(id);
            if (folder.isDefault()) {
                saveDefaultFolder(folder.getSid());
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
        setSort(folder);
        String hash = folder.generateHash();
        folder.setHash(hash);
        ContentValues values = initFolderValues(folder);

        long rowId = 0;
        try {
            rowId = db.update(Provider.FolderColumns.TABLE_NAME, values, Provider.FolderColumns._ID + " = ?", new String[] {String.valueOf(folder.getId())});
        } catch (Exception e) {
            Log.e(TAG, "----updateFolder---error----" + e.getMessage());
        }
        if (rowId > 0) {
            if (folder.isDefault()) {
                saveDefaultFolder(folder.getSid());
            }
            updateFolderCache(folder);
            notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, folder);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 添加或更新笔记本，若数据不存在，则更新
     * @param folderList
     */
    public void addOrUpdate(List<Folder> folderList) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        List<Folder> changeList = new ArrayList<>();
        db.beginTransaction();
        try {
            for (Folder folder : folderList) {
                if (!folder.hasId()) {    //该问加你是“所有”，不存到数据库里
                    KLog.d(TAG, "add or update folder but folder not has id or sid:" + folder);
                    continue;
                }

                String selection = null;
                String[] args = null;
                if (folder.checkId()) { //id可用
                    selection = Provider.FolderColumns._ID + " = ?";
                    args = new String[] {String.valueOf(folder.getId())};
                } else {
                    selection = Provider.FolderColumns.SID + " = ?";
                    args = new String[] {folder.getSid()};
                }
                ContentValues values = initFolderValues(folder);
                long rowId = 0;
                try {
                    rowId = db.update(Provider.FolderColumns.TABLE_NAME, values, selection, args);
                } catch (Exception e) {
                    KLog.e(TAG, "add or update folder update error error:" + e.getMessage());
                }
                if (rowId <= 0) {   //没有成功，则添加
                    KLog.d(TAG, "add or update folder update failed and will add folder");
                    try {
                        rowId = db.insert(Provider.FolderColumns.TABLE_NAME, null, values);
                    } catch (Exception e) {
                        KLog.e(TAG, "add or update folder add failed error:" + e.getMessage());
                    }
                    if (rowId > 0) {    //添加成功，则添加到缓存
                        folder.setId((int) rowId);
                        FolderCache folderCache = FolderCache.getInstance();
                        folderCache.addFolder(folder.getSid(), folder);
                        changeList.add(folder);
                    }
                } else {    //更新成功，则更新到缓存
                    KLog.d(TAG, "add or update folder update success:" + folder);
                    FolderCache folderCache = FolderCache.getInstance();
                    Folder tFolder = folderCache.getCacheFolder(folder.getSid());
                    if (tFolder != null) {  //更新缓存数据
                        String name = folder.getName();
                        if (name != null) {
                            tFolder.setName(name);
                        }
                        int sort = folder.getSort();
                        if (sort != 0) {
                            tFolder.setSort(sort);
                        }
                        String hash = folder.getHash();
                        if (hash != null) {
                            tFolder.setHash(hash);
                        }
                        DeleteState deleteState = folder.getDeleteState();
                        if (deleteState != null) {
                            tFolder.setDeleteState(deleteState);
                        }
                        tFolder.setSyncState(SyncState.SYNC_DONE);
                        long modifyTime = folder.getModifyTime();
                        if (modifyTime == 0) {
                            modifyTime = System.currentTimeMillis();
                        }
                        tFolder.setModifyTime(modifyTime);
                        tFolder.setLock(folder.isLock());
                        int userId = folder.getUserId();
                        if (userId != 0) {
                            tFolder.setUserId(userId);
                        }

                    } else {    //缓存中不存在，则查询该记录，然后添加到缓存中
                        tFolder = getFolderInfo(folder, db);
                        folderCache.addFolder(tFolder.getSid(), tFolder);
                    }
                    changeList.add(tFolder);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            KLog.e(TAG, "add or update folder error:" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (changeList.size() == 1) {   //只有一条记录
            notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, changeList.get(0));
        } else {    //多条
            notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.BATCH_UPDATE, changeList);
        }
    }

    /**
     * 更新笔记本的同步状态
     * @param folder
     * @return
     */
    public boolean updateSyncFolder(Folder folder) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        long rowId = 0;
        try {
            //1、先查询该笔记本下的所有笔记是否都已同步完
            /*boolean hasSynced = isAllNoteSynced(folder, db);
            if (hasSynced) {    //该笔记本下所有的笔记都同步完毕
                folder.setSyncState(SyncState.SYNC_DONE);
            } else {
                KLog.d(TAG, "update sync folder but has notes not sync done");
            }*/
//            folder.setModifyTime(System.currentTimeMillis());
            ContentValues values = initSyncFolderValues(folder);
            rowId = db.update(Provider.FolderColumns.TABLE_NAME, values, Provider.FolderColumns._ID + " = ?", new String[] {String.valueOf(folder.getId())});
        } catch (Exception e) {
            Log.e(TAG, "----update sync Folder---error----" + e.getMessage());
        }
        if (rowId > 0) {
            Folder cacheFolder = FolderCache.getInstance().getCacheFolder(folder.getSid());
            if (cacheFolder != null) {
                cacheFolder.setModifyTime(folder.getModifyTime());
                cacheFolder.setSyncState(folder.getSyncState());
            }
            notifyObservers(Provider.FolderColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, folder);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 查询该笔记本下的所有笔记是否都已同步完，包括回收站的笔记
     * @param folder
     * @return
     */
    public boolean isAllNoteSynced(Folder folder, SQLiteDatabase db) {
        if (db == null) {
            db = mDBHelper.getReadableDatabase();
        }
        boolean hasSynced = false;
        Cursor cursor = null;
        try {
            cursor = db.query(Provider.NoteColumns.TABLE_NAME, new String[] {"count(*) as count"}, Provider.NoteColumns.FOLDER_ID  + " = ? and (" + Provider.NoteColumns.SYNC_STATE + " IS NULL OR " + Provider.NoteColumns.SYNC_STATE + " != ?)", new String[] {folder.getSid(), String.valueOf(SyncState.SYNC_DONE.ordinal())}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                long count = cursor.getLong(0);
                hasSynced = count == 0;
            }
            
        } catch (Exception e) {
            KLog.e(TAG, "is all note synced invoke error:" + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        KLog.d(TAG, "is all note synced result:" + hasSynced + ", folder:" + folder);
        return hasSynced;
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
        String hash = folder.generateHash();
        folder.setHash(hash);
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
            removeDefaultFolderSid(folder.getSid());
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

    /**
     * 获取笔记本的集合，优先从缓存中获取，若缓存中不存在，则从数据库中获取，key：folder的sid，value：folder
     * @param user 用户
     * @param args
     * @return
     */
    public Map<String, Folder> getFolders(User user, Bundle args) {
        Map<String, Folder> map = FolderCache.getInstance().getFolderMap();
        if (SystemUtil.isEmpty(map)) {  //缓存中为空，则从数据库中获取
            getAllFolders(user, args);
            map = FolderCache.getInstance().getFolderMap();
        }
        return map;
    }

    /**
     * 获取笔记本的列表，有排序
     * @param user
     * @param args
     * @return
     */
    public List<Folder> getSortFolders(User user, Bundle args) {
        getFolders(user, args);
        return FolderCache.getInstance().getSortFolders(); 
    }
}
