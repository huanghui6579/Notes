package com.yunxinlink.notes.persistent;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.db.DBHelper;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.db.observer.Observer;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.DetailList;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.DigestUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.R.attr.id;

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
     * 获取当前用户下所有的笔记,默认按时间降序排列,<i>已废弃</i>，使用{@link #getAllDetailNotes(User, Bundle)}方法
     * @param user 对应的用户
     * @param args 额外的参数            
     * @author huanghui1
     * @update 2016/3/7 17:41
     * @version: 1.0.0
     */
    @Deprecated
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
     * 获取所有的清单护着笔记的列表
     * @author huanghui1
     * @update 2016/8/8 9:50
     * @version: 1.0.0
     */
    public List<DetailNoteInfo> getAllDetailNotes(User user, Bundle args) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;
        String folder = null;
        boolean isRecycle = false;
        int sort = 0;
        if (args != null) {
            folder = args.getString("folderId", null);
            isRecycle = args.getBoolean("isRecycle", false);
            sort = args.getInt("sort", 0);
        }
        int deleteState = isRecycle ? 1 : 0;
        //是否加载回收站里的笔记
        int userId = 0;
        if (user != null) { //当前用户有登录
            if (!user.checkOnLine()) {  //用户离线或者不可用，退出登录了
                KLog.d(TAG, "get all detail notes user is offline or disable:" + user);
                return null;
            }
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
            selection = "(" + Provider.NoteColumns.USER_ID + " = 0 OR " + Provider.NoteColumns.USER_ID + " IS NULL) AND ";
            if (!TextUtils.isEmpty(folder)) {
                if (deleteState == 0) {
                    selection += Provider.NoteColumns.FOLDER_ID + " = ? AND (" + Provider.NoteColumns.DELETE_STATE + " is null or " + Provider.NoteColumns.DELETE_STATE + " = " + deleteState + ")";
                } else {
                    selection += Provider.NoteColumns.FOLDER_ID + " = ? AND " + Provider.NoteColumns.DELETE_STATE + " = " + deleteState;
                }
                selectionArgs = new String[] {folder};
            } else {
                if (deleteState == 0) {
                    selection += "(" + Provider.NoteColumns.DELETE_STATE + " IS NULL OR " + Provider.NoteColumns.DELETE_STATE + " = ?)";
                } else {
                    selection += Provider.NoteColumns.DELETE_STATE + " = ?";
                }
                selectionArgs = new String[] {String.valueOf(deleteState)};
            }
        }
        List<DetailNoteInfo> list = null;
        String orderBy = getNoteSort(sort);
        Cursor cursor = db.query(Provider.NoteColumns.TABLE_NAME, null, selection, selectionArgs, null, null, orderBy);
        if (cursor != null) {
            list = new ArrayList<>();
            while (cursor.moveToNext()) {
                NoteInfo note = cursor2Note(cursor);
                DetailNoteInfo detailNote = new DetailNoteInfo();
                detailNote.setNoteInfo(note);
                
                String noteSid = note.getSid();
                
                if (note.isDetailNote()) {
                    List<DetailList> details = getDetailList(db, noteSid);
                    detailNote.setDetailList(details);
                }
                if (note.hasAttach()) { //有附件

                    Map<String, Attach> attachMap = getAttaches(note, db);
                    if (attachMap != null && attachMap.size() > 0) {
                        Attach attach = attachMap.values().iterator().next();
                        detailNote.setLastAttach(attach);
                    }
                    note.setAttaches(attachMap);
                }
                
                list.add(detailNote);
            }
            cursor.close();
        }
        return list;
    }

    /**
     * 获取笔记的排序方式
     * @param sort
     * @return
     */
    private String getNoteSort(int sort) {
        String orderBy = null;
        switch (sort) {
            case NoteInfo.SORT_CREATE_TIME:
                orderBy = Provider.NoteColumns.CREATE_TIME + " desc";
                break;
            case NoteInfo.SORT_TITLE:
                orderBy = Provider.NoteColumns.CONTENT + " asc";
                break;
            default:
                orderBy = Provider.NoteColumns.DEFAULT_SORT;
                break;
        }
        return orderBy;
    }

    /**
     * 获取笔记的最新的附件
     * @param noteSid
     * @return
     */
    public Attach getLastAttach(SQLiteDatabase db, String noteSid) {
        if (db == null) {
            db = mDBHelper.getReadableDatabase();
        }
        Attach attach = null;
        String selection = Provider.AttachmentColumns.NOTE_ID + " = ?";
        String[] selectionArgs = {noteSid};
        String order = Provider.AttachmentColumns.MODIFY_TIME + " DESC ";
        Cursor cursor = db.query(Provider.AttachmentColumns.TABLE_NAME, null, selection, selectionArgs, null, null, order, "1");
        if (cursor != null && cursor.moveToNext()) {
            attach = cursor2Attach(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        return attach;
    }
    
    /**
     * 返回移动笔记到垃圾桶的数据
     * @param note 笔记
     * @return 返回数据
     */
    private ContentValues initDeleteValues(NoteInfo note, DeleteState deleteState) {

        ContentValues values = new ContentValues();
        
        long time = System.currentTimeMillis();
        
        if (note != null) {
            note.setDeleteState(deleteState);
            note.setSyncState(SyncState.SYNC_UP);
            note.setModifyTime(time);
        }

        values.put(Provider.NoteColumns.DELETE_STATE, deleteState.ordinal());
        values.put(Provider.NoteColumns.SYNC_STATE, SyncState.SYNC_UP.ordinal());
        values.put(Provider.NoteColumns.MODIFY_TIME, time);
        return values;
    }
    
    /**
     * 返回移动笔记到垃圾桶的数据
     * @param note 笔记
     * @return 返回数据
     */
    private ContentValues initDeleteValues(NoteInfo note, SyncState syncState) {

        ContentValues values = new ContentValues();
        
        long time = System.currentTimeMillis();
        
        if (note != null) {
            note.setSyncState(syncState);
            note.setModifyTime(time);
        }

        values.put(Provider.NoteColumns.SYNC_STATE, syncState.ordinal());
        values.put(Provider.NoteColumns.MODIFY_TIME, time);
        return values;
    }
    
    /**
     * 删除笔记
     * @author huanghui1
     * @update 2016/6/21 15:16
     * @version: 1.0.0
     */
    @Deprecated
    public boolean deleteNote(DetailNoteInfo detailNote) {
        return deleteNote(detailNote, DeleteState.DELETE_TRASH);
    }
    
    /**
     * 删除笔记
     * @param detailNote 笔记
     * @param deleteState 删除的状态，有还原、移到回收站、彻底删除几种
     * @author huanghui1
     * @update 2016/6/21 15:16
     * @version: 1.0.0
     */
    public boolean deleteNote(DetailNoteInfo detailNote, DeleteState deleteState) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        NoteInfo note = detailNote.getNoteInfo();
        ContentValues values = initDeleteValues(note, deleteState);
        int row = 0;
        try {
            db.beginTransaction();
            row = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns._ID + " = ?", new String[] {String.valueOf(note.getId())});
            updateFolderCount(note, false);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            KLog.e(TAG, "--deleteNote---error---" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (row > 0) {
            Observer.NotifyType notifyType = null;
            if (DeleteState.DELETE_NONE == deleteState) {
                notifyType = Observer.NotifyType.ADD;
            } else if (DeleteState.DELETE_DONE == deleteState) {    //彻底删除
                notifyType = Observer.NotifyType.REMOVE;
            } else {
                notifyType = Observer.NotifyType.DELETE;
            }
            notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, notifyType, detailNote);
            return true;
        } else {
            notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.DELETE, null);
            KLog.w(TAG, "-------deleteNote---failed----");
            return false;
        }
    }

    /**
     * 删除多条笔记，移动到回收站，并不是真正的删除
     * @param noteList 要删除的笔记的集合
     * @return 返回是否删除成功
     */
    @Deprecated
    public boolean deleteNote(List<DetailNoteInfo> noteList) {
        return deleteNote(noteList, DeleteState.DELETE_TRASH);
    }

    /**
     * 删除多条笔记，移动到回收站，并不是真正的删除
     * @param noteList 要删除的笔记的集合
     * @param deleteState 删除的状态，是删除到垃圾桶还是彻底删除            
     * @return 返回是否删除成功
     */
    public boolean deleteNote(List<DetailNoteInfo> noteList, DeleteState deleteState) {
        if (noteList == null || noteList.size() == 0) {
            KLog.d(TAG, "----deleteNote---list--size--0--success--deleteState----" + deleteState);
            return true;
        }
        if (noteList.size() == 1) { //只有一条
            return deleteNote(noteList.get(0), deleteState);
        } else {
            int row = 0;
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            try {
                NoteInfo note = noteList.get(0).getNoteInfo();
                ContentValues values = initDeleteValues(note, deleteState);
                //拼凑sql语句
                StringBuilder builder = new StringBuilder(Provider.NoteColumns._ID);
                builder.append(" in (");
                int size = noteList.size();
                String[] selectionArgs = new String[size];
                for (int i = 0; i < size; i++) {
                    DetailNoteInfo detailNote = noteList.get(i);
                    note = detailNote.getNoteInfo();
                    builder.append("?").append(Constants.TAG_COMMA);
                    selectionArgs[i] = String.valueOf(note.getId());
                }
                builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_COMMA));
                builder.append(")");
                
                db.beginTransaction();
                row = db.update(Provider.NoteColumns.TABLE_NAME, values, builder.toString(), selectionArgs);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                KLog.e("TAG", "--deleteNote--list---deleteState--" + deleteState + "--error--" + e.getMessage());
            } finally {
                db.endTransaction();
            }
            if (row > 0) {
                Observer.NotifyType notifyType = null;
                if (DeleteState.DELETE_NONE == deleteState) {   //撤销删除
                    notifyType = Observer.NotifyType.ADD;
                } else if (DeleteState.DELETE_DONE == deleteState) {    //彻底删除
                    notifyType = Observer.NotifyType.REMOVE;
                } else {
                    notifyType = Observer.NotifyType.DELETE;
                }
                notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, notifyType, noteList);
                return true;
            } else {
                KLog.w(TAG, "-------deleteNote--list--failed--deleteState--" + deleteState);
                return false;
            }
        }
    }

    /**
     * 清空回收站
     * @return
     */
    public boolean clearTrash() {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = initDeleteValues(null, DeleteState.DELETE_DONE);
        String selection = Provider.NoteColumns.DELETE_STATE  + " = ?";
        String[] args = {String.valueOf(DeleteState.DELETE_TRASH.ordinal())};
        long row = 0;
        db.beginTransaction();
        try {
            row = db.update(Provider.NoteColumns.TABLE_NAME, values, selection, args);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            KLog.e("TAG", "--clearTrash--error----" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (row > 0) {
            notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.REMOVE, null);
            return true;
        } else {
            KLog.e("TAG", "--clearTrash--failed----");
            return false;
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
        values.put(Provider.NoteColumns.TITLE, note.getTitle());
        values.put(Provider.NoteColumns.CONTENT, note.getContent());
        values.put(Provider.NoteColumns.SHOW_CONTENT, note.getShowContent());
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
        values.put(Provider.NoteColumns.SID, note.getSid());
        return values;
    }
    
    private NoteInfo cursor2Note(Cursor cursor) {
        NoteInfo note = new NoteInfo();
        note.setId(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns._ID)));
        note.setSid(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.SID)));
        note.setUserId(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns.USER_ID)));
        note.setTitle(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.TITLE)));
        note.setContent(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.CONTENT)));
        note.setShowContent(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.SHOW_CONTENT)));
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
    
    /*private NoteInfo cursor2DetailNote(Cursor cursor) {
        DetailNoteInfo detailNote = new DetailNoteInfo();
        NoteInfo note = new NoteInfo();
        note.setId(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns._ID)));
        note.setSid(cursor.getString(cursor.getColumnIndex(Provider.NoteColumns.SID)));
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
        detailNote.setNoteInfo(note);
        if (note.isDetailNote()) {  //清单笔记
            DetailList detail = new DetailList();
            detail.setNoteId(note.getSid());
            detail.setId(cursor.getInt(cursor.getColumnIndex("did")));
            detail.setSid(cursor.getString(cursor.getColumnIndex("dsid")));
            detail.setTitle(cursor.getString(cursor.getColumnIndex(Provider.DetailedListColumns.TITLE)));
            detail.setOldTitle(cursor.getString(cursor.getColumnIndex(Provider.DetailedListColumns.OLD_TITLE)));
            detail.setChecked(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns.CHECKED)) == 1);
            detail.setSort(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns.SORT)));
            detail.setOldSort(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns.OLD_SORT)));
            detail.setSyncState(SyncState.valueOf(cursor.getInt(cursor.getColumnIndex("dsync"))));
            detail.setDeleteState(DeleteState.valueOf(cursor.getInt(cursor.getColumnIndex("ddelete"))));
            detail.setCreateTime(cursor.getLong(cursor.getColumnIndex("dctime")));
            detail.setModifyTime(cursor.getLong(cursor.getColumnIndex("dmtime")));
            detailNote.setDetailList();
        }
        return note;
    }*/
    
    private Attach cursor2Attach(Cursor cursor) {
        Attach attach = new Attach();
        attach.setId(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns._ID)));
        attach.setSid(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.SID)));
        attach.setUserId(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns.USER_ID)));
        attach.setNoteId(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.NOTE_ID)));
        attach.setType(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns.TYPE)));
        attach.setLocalPath(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.LOCAL_PATH)));
        attach.setCreateTime(cursor.getLong(cursor.getColumnIndex(Provider.AttachmentColumns.CREATE_TIME)));
        attach.setDescription(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.DESCRIPTION)));
        attach.setSyncState(SyncState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns.SYNC_STATE))));
        attach.setDeleteState(DeleteState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.AttachmentColumns.DELETE_STATE))));
        attach.setFilename(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.FILE_NAME)));
        attach.setModifyTime(cursor.getLong(cursor.getColumnIndex(Provider.AttachmentColumns.MODIFY_TIME)));
        attach.setServerPath(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.SERVER_PATH)));
        attach.setSize(cursor.getLong(cursor.getColumnIndex(Provider.AttachmentColumns.SIZE)));
        attach.setMimeType(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.MIME_TYPE)));
        attach.setHash(cursor.getString(cursor.getColumnIndex(Provider.AttachmentColumns.HASH)));
        return attach;
    }
    
    /**
     * 添加一个记事本
     * @param note 笔记
     * @param cacheList 缓存中的附件集合
     * @param attachList 笔记内容中的附件sid                 
     * @author huanghui1
     * @update 2016/6/18 11:14
     * @version: 1.0.0
     */
    @Deprecated
    public NoteInfo addNote(NoteInfo note, List<String> cacheList, List<String> attachList) {
        
        DetailNoteInfo detailNote = new DetailNoteInfo();
        detailNote.setNoteInfo(note);

        detailNote = addDetailNote(detailNote, cacheList, attachList);
        if (detailNote != null) {
            return detailNote.getNoteInfo();
        }
        return null;
        
    }

    /**
     * 更新附件的笔记id
     * @param db
     * @param list 附件的sid
     * @param noteId 笔记的id            
     */
    private void updateAttachNote(SQLiteDatabase db, List<String> list, String noteId) {
        ContentValues values = new ContentValues();
        values.put(Provider.AttachmentColumns.NOTE_ID, noteId);
        int size = list.size();
        String selection = null;
        String[] selectionArgs = new String[size];
        if (size == 1) { //只有一个附件
            selection = Provider.AttachmentColumns.NOTE_ID + " IS NULL AND " + Provider.AttachmentColumns.SID + " = ?";
            selectionArgs[0] = list.get(0);
        } else {    //多个附件
            StringBuilder sb = new StringBuilder(Provider.AttachmentColumns.NOTE_ID + " IS NULL AND " + Provider.AttachmentColumns.SID).append(" in (");
            for (int i = 0; i < size; i++) {
                String sid = list.get(i);
                selectionArgs[i] = sid;
                sb.append("?").append(Constants.TAG_COMMA);
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(")");
            selection = sb.toString();
        }
        db.update(Provider.AttachmentColumns.TABLE_NAME, values, selection, selectionArgs);
    }

    /**
     * 彻底删除附件
     * @param db
     * @param list 附件的sid集合
     * @param noteSid 笔记的sid，如果不为空，则删除所有属于该笔记的附件            
     */
    private void deleteAttaches(SQLiteDatabase db, List<String> list, String noteSid) {
        String selection = null;
        String[] selectionArgs = null;
        if (!TextUtils.isEmpty(noteSid)) {
            selection = Provider.AttachmentColumns.NOTE_ID + " IS NULL OR " + Provider.AttachmentColumns.NOTE_ID + " = ?";
            selectionArgs = new String[1];
            selectionArgs[0] = noteSid;
        } else {
            int size = list.size();
            selectionArgs = new String[size];
            if (size == 1) {    //只有一个附件
                selection = Provider.AttachmentColumns.NOTE_ID + " IS NULL OR " + Provider.AttachmentColumns.SID + " = ?";
                selectionArgs[0] = list.get(0);
            } else {    //多个附件
                StringBuilder sb = new StringBuilder(Provider.AttachmentColumns.NOTE_ID + " IS NULL OR " + Provider.AttachmentColumns.SID).append(" in (");
                for (int i = 0; i < size; i++) {
                    String sid = list.get(i);
                    selectionArgs[i] = sid;
                    sb.append("?").append(Constants.TAG_COMMA);
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append(")");
                selection = sb.toString();
            }
        }
        int row = db.delete(Provider.AttachmentColumns.TABLE_NAME, selection, selectionArgs);
        KLog.d(TAG, "-----deleteAttaches--row---" + row);
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
            Map<String, Attach> map = getAttaches(info, db);

            info.setAttaches(map);
        }
        if (cursor != null) {
            cursor.close();
        }
        return info;
    }


    /**
     * 初始化note的添加数据
     * @author huanghui1
     * @update 2016/6/18 11:41
     * @version: 1.0.0
     */
    private ContentValues initDetailValues(DetailList detail) {
        ContentValues values = new ContentValues();
        values.put(Provider.DetailedListColumns.SID, detail.getSid());
        values.put(Provider.DetailedListColumns.TITLE, detail.getTitle());
        values.put(Provider.DetailedListColumns.OLD_TITLE, detail.getOldTitle());
        values.put(Provider.DetailedListColumns.CHECKED, detail.isChecked() ? 1 : 0);
        values.put(Provider.DetailedListColumns.CREATE_TIME, detail.getCreateTime());
        values.put(Provider.DetailedListColumns.MODIFY_TIME, detail.getModifyTime());
        values.put(Provider.DetailedListColumns.NOTE_ID, detail.getNoteId());
        values.put(Provider.DetailedListColumns.HASH, detail.getHash());
        values.put(Provider.DetailedListColumns.SORT, detail.getSort());
        values.put(Provider.DetailedListColumns.OLD_SORT, detail.getOldSort());
        DeleteState deleteState = detail.getDeleteState();
        if (deleteState != null) {
            values.put(Provider.DetailedListColumns.DELETE_STATE, deleteState.ordinal());
        }
        SyncState syncState = detail.getSyncState();
        if (syncState != null) {
            values.put(Provider.DetailedListColumns.SYNC_STATE, syncState.ordinal());
        }
        return values;
    }

    /**
     * 添加有清单的笔记
     * @param detailNote
     * @return
     */
    public DetailNoteInfo addDetailNote(DetailNoteInfo detailNote, List<String> cacheList, List<String> attachList) {
        NoteInfo note = detailNote.getNoteInfo();
        if (attachList != null && attachList.size() > 0) {
            note.setHasAttach(true);
        } else {
            note.setHasAttach(false);
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = initNoteValues(note);
        db.beginTransaction();
        long rowId = 0;
        try {
            rowId = db.insert(Provider.NoteColumns.TABLE_NAME, null, values);
            note.setId((int) rowId);
            updateFolderCount(note, true);

            updateTextAttach(db, note, cacheList, attachList);
           
            if (detailNote.hasDetailList()) {   //有清单项
                KLog.d(TAG, "-----addDetailNote--hasDetailList--");
                //批量添加清单项
                for (DetailList detail : detailNote.getDetailList()) {
                    //设置hash
                    detail.setHash(DigestUtil.md5Hex(detail.getTitle()));
                    values = initDetailValues(detail);
                    rowId = db.insert(Provider.DetailedListColumns.TABLE_NAME, null, values);
                    detail.setId((int) rowId);
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            KLog.e(TAG, "--addNote--error--" + e.getMessage());
        } finally {
            db.endTransaction();
        }
        if (rowId > 0) {
            notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.ADD, detailNote);
            return detailNote;
        } else {
            return null;
        }
    }

    /**
     * 更新清单笔记
     * @param detailNote
     * @param cacheList
     * @param attachList
     * @param detailLists 
     * @return
     */
    public boolean updateDetailNote(DetailNoteInfo detailNote, List<String> cacheList, List<String> attachList, List<DetailList> detailLists) {
        
        NoteInfo note = detailNote.getNoteInfo();
        if (attachList != null && attachList.size() > 0) {
            note.setHasAttach(true);
        } else {
            note.setHasAttach(false);
        }
        ContentValues values = initUpdateNoteValues(note);
        if (values.size() > 0) {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            long row = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns._ID + " = ?", new String[] {String.valueOf(note.getId())});
            if (row > 0) {
                updateFolder(note);

                updateTextAttach(db, note, cacheList, attachList);
                
                //如果有清单，则更新清单列表
                if (note.isDetailNote() && detailNote.hasDetailList()) {  //清单笔记且有清单
                    
                    List<DetailList> detailListList = detailNote.getDetailList();
                    
                    if (detailLists != null && detailLists.size() > 0) {
                        //取差集，如有剩余的，则删除
                        detailLists.removeAll(detailListList);
                    }
                    
                    KLog.d(TAG, "-----updateDetailList--hasDetailList--");
                    for (DetailList detail : detailListList) {
                        //设置hash
                        detail.setHash(DigestUtil.md5Hex(detail.getTitle()));
                        int id = detail.getId();

                        if (id > 0) {   //已有，则更新
                            values = initUpdateDetailValues(detail);
                            row = db.update(Provider.DetailedListColumns.TABLE_NAME, values, Provider.DetailedListColumns._ID + " = ?", new String[] {String.valueOf(id)});
                        } else {    //添加清单
                            KLog.d(TAG, "--insert---detail---" + detail);
                            values = initDetailValues(detail);
                            row = db.insert(Provider.DetailedListColumns.TABLE_NAME, null, values);
                            KLog.d(TAG, "-----insert--detail---row---:" + row);
                        }

                    }
                    
                }

                //如果有多余的清单，则删除
                deleteDetailList(detailLists, db);

                notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, detailNote);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * 更新笔记的状态，一般用于数据同步后的本地更新
     * @param detailNoteList
     * @param syncState 同步的状态
     * @return
     */
    public boolean updateDetailNotes(List<DetailNoteInfo> detailNoteList, SyncState syncState) {
        if (SystemUtil.isEmpty(detailNoteList)) {
            return false;
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        long row = 0;
        List<DetailNoteInfo> successList = new ArrayList<>();
        for (DetailNoteInfo detailNoteInfo : detailNoteList) {
            NoteInfo note = detailNoteInfo.getNoteInfo();
            if (note == null) {
                continue;
            }
            long currentTime = System.currentTimeMillis();
            note.setModifyTime(currentTime);
            note.setSyncState(syncState);
            ContentValues values = initSyncNoteValues(note);
            db.beginTransaction();
            try {
                row = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns._ID + " = ?", new String[]{String.valueOf(note.getId())});
                if (row > 0) {  //笔记本地更新成功，如果有清单，则更新清单，附件的更新得等附件上传完了后才能更新
                    if (note.isDetailNote() && detailNoteInfo.hasDetailList()) {    //有清单
                        List<DetailList> detailListList = detailNoteInfo.getDetailList();
                        KLog.d(TAG, "update detail notes and will update detail list");
                        for (DetailList detail : detailListList) {
                            detail.setModifyTime(currentTime);
                            detail.setSyncState(syncState);
                            values = initSyncDetailValues(detail);
                            row = db.update(Provider.DetailedListColumns.TABLE_NAME, values, Provider.DetailedListColumns._ID + " = ?", new String[] {String.valueOf(id)});
                        }
                    }
                }
                db.setTransactionSuccessful();
                successList.add(detailNoteInfo);
            } catch (Exception e) {
                KLog.d(TAG, "update detail notes error:" + e.getMessage());
            } finally {
                db.endTransaction();
            }
        }
        boolean success = row > 0;
        if (success) {
            if (successList.size() == 1) {  //单个
                notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, successList.get(0));
            } else {    //多个
                notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.BATCH_UPDATE, successList);
            }
        }
        KLog.d(TAG, "update detail notes result:" + success);
        return success;
    }

    /**
     * 填充更新笔记的values
     * @param note
     * @return
     */
    private ContentValues initUpdateNoteValues(NoteInfo note) {
        ContentValues values = new ContentValues();
        String content = note.getContent();
        
        String title = note.getTitle();
        if (title != null) {
            values.put(Provider.NoteColumns.TITLE, title);
        }
        values.put(Provider.NoteColumns.HAS_ATTACH, note.hasAttach() ? 1 : 0);
        if (content != null) {
            values.put(Provider.NoteColumns.CONTENT, content);
        }
        
        values.put(Provider.NoteColumns.SHOW_CONTENT, note.getShowContent());
        
        DeleteState deleteState = note.getDeleteState();
        if (deleteState != null) {
            values.put(Provider.NoteColumns.DELETE_STATE, deleteState.ordinal());
        }
        String folderId = note.getFolderId();
        if (!TextUtils.isEmpty(folderId)) {
            values.put(Provider.NoteColumns.FOLDER_ID, folderId);
        }
        String hash = note.getHash();
        if (hash != null) {
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
        String sid = note.getSid();
        if (!TextUtils.isEmpty(sid)) {
            values.put(Provider.NoteColumns.SID, sid);
        }
        SyncState syncState = note.getSyncState();
        if (syncState != null) {
            values.put(Provider.NoteColumns.SYNC_STATE, syncState.ordinal());
        }
        return values;
    }

    /**
     * 更新笔记的同步状态
     * @param note
     * @return
     */
    private ContentValues initSyncNoteValues(NoteInfo note) {
        ContentValues values = new ContentValues();
        
        long time = note.getModifyTime();
        if (time == 0) {
            time = System.currentTimeMillis();
        }
        values.put(Provider.NoteColumns.MODIFY_TIME, time);
        
        SyncState syncState = note.getSyncState();
        if (syncState != null) {
            values.put(Provider.NoteColumns.SYNC_STATE, syncState.ordinal());
        }
        return values;
    }

    /**
     * 填充更新清单的values
     * @param detail
     * @return
     */
    private ContentValues initUpdateDetailValues(DetailList detail) {
        ContentValues values = new ContentValues();
        if (detail.getSort() >= 0) {
            values.put(Provider.DetailedListColumns.SORT, detail.getSort());
        }
        if (detail.getOldSort() >= 0) {
            values.put(Provider.DetailedListColumns.OLD_SORT, detail.getOldSort());
        }
        long time = detail.getModifyTime();
        if (time == 0) {
            time = System.currentTimeMillis();
        }
        values.put(Provider.DetailedListColumns.MODIFY_TIME, time);

        String title = detail.getTitle();
        if (title != null) {
            values.put(Provider.DetailedListColumns.TITLE, title);
        }

        String oldTitle = detail.getOldTitle();
        if (oldTitle != null) {
            values.put(Provider.DetailedListColumns.OLD_TITLE, oldTitle);
        }
        values.put(Provider.DetailedListColumns.CHECKED, detail.isChecked() ? 1 : 0);
        String hash = detail.getHash();
        if (hash != null) {
            values.put(Provider.DetailedListColumns.HASH, hash);
        }
        
        return values;
    }

    /**
     * 填充更新清单的values
     * @param detail
     * @return
     */
    private ContentValues initSyncDetailValues(DetailList detail) {
        ContentValues values = new ContentValues();

        long time = detail.getModifyTime();
        if (time == 0) {
            time = System.currentTimeMillis();
        }
        values.put(Provider.DetailedListColumns.MODIFY_TIME, time);

        SyncState syncState = detail.getSyncState();
        if (syncState != null) {
            values.put(Provider.DetailedListColumns.SYNC_STATE, syncState.ordinal());
        }
        return values;
    }

    /**
     * 删除清单，直接在本地数据库删除
     * @param list
     * @return
     */
    private boolean deleteDetailList(List<DetailList> list, SQLiteDatabase db) {
        if (list != null && list.size() > 0) {
            if (db == null) {
                db = mDBHelper.getWritableDatabase();
            }
            int size = list.size();
            String[] args = new String[size];
            StringBuilder builder = new StringBuilder();
            builder.append(Provider.DetailedListColumns._ID);
            if (size == 1) {    //只有一个清单
                builder.append(" = ?");
                args[0] = String.valueOf(list.get(0).getId());
            } else {
                builder.append(" in (");
                for (int i = 0; i < size; i++) {
                    builder.append("?").append(Constants.TAG_COMMA);
                    args[i] = String.valueOf(list.get(i).getId());
                }
                builder.deleteCharAt(builder.length() - 1);
                builder.append(")");
            }
            int row = 0;
            try {
                row = db.delete(Provider.DetailedListColumns.TABLE_NAME, builder.toString(), args);
                KLog.d(TAG, "--deleteDetailList--list---" + list);
            } catch (Exception e) {
                KLog.e(TAG, "--deleteDetailList--error--" + e.getMessage());
            }
            return row > 0;
        } else {
            return true;
        }
    }

    /**
     * 根据笔记的sid查询对应的清单
     * @param noteSid 笔记的sid
     * @return
     */
    public List<DetailList> getDetailList(SQLiteDatabase db, String noteSid) {
        if (db == null) {
            db = mDBHelper.getReadableDatabase();
        }
        List<DetailList> list = null;
        Cursor cursor = db.query(Provider.DetailedListColumns.TABLE_NAME, null, Provider.DetailedListColumns.NOTE_ID + " = ?", new String[] {noteSid}, null, null, Provider.DetailedListColumns.DEFAULT_SORT);
        if (cursor != null) {
            list = new ArrayList<>();
            while (cursor.moveToNext()) {
                DetailList detail = cursor2Detail(cursor);
                list.add(detail);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return list;
    }

    /**
     * 将cursor转化为清单对象
     * @param cursor
     * @return
     */
    private DetailList cursor2Detail(Cursor cursor) {
        DetailList detail = new DetailList();
        detail.setId(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns._ID)));
        detail.setSid(cursor.getString(cursor.getColumnIndex(Provider.DetailedListColumns.SID)));
        detail.setTitle(cursor.getString(cursor.getColumnIndex(Provider.DetailedListColumns.TITLE)));
        detail.setSort(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns.SORT)));
        detail.setOldSort(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns.OLD_SORT)));
        detail.setChecked(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns.CHECKED)) == 1);
        detail.setCreateTime(cursor.getLong(cursor.getColumnIndex(Provider.DetailedListColumns.CREATE_TIME)));
        detail.setModifyTime(cursor.getLong(cursor.getColumnIndex(Provider.DetailedListColumns.MODIFY_TIME)));
        detail.setNoteId(cursor.getString(cursor.getColumnIndex(Provider.DetailedListColumns.NOTE_ID)));
        detail.setOldTitle(cursor.getString(cursor.getColumnIndex(Provider.DetailedListColumns.OLD_TITLE)));
        detail.setHash(cursor.getString(cursor.getColumnIndex(Provider.DetailedListColumns.HASH)));
        detail.setSyncState(SyncState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns.SYNC_STATE))));
        detail.setDeleteState(DeleteState.valueOf(cursor.getInt(cursor.getColumnIndex(Provider.DetailedListColumns.DELETE_STATE))));
        return detail;
    }

    /**
     * 获取清单笔记的信息
     * @param noteId 笔记的id
     * @return
     */
    public DetailNoteInfo getDetailNote(int noteId) {
        NoteInfo note = getNote(noteId);
        if (note == null) {
            return null;
        }
        List<DetailList> list = getDetailList(null, note.getSid());
        DetailNoteInfo detailNote = new DetailNoteInfo();
        detailNote.setDetailList(list);
        detailNote.setNoteInfo(note);
        return detailNote;
    }

    /**
     * 获取笔记中的附件列表，附件按时间倒序，也就是最新的时间排在最前面
     * @param note 笔记
     * @return 返回笔记中的附件列表
     */
    public Map<String, Attach> getAttaches(NoteInfo note, SQLiteDatabase db) {
        String selection = Provider.AttachmentColumns.NOTE_ID + " = ? AND (" + Provider.AttachmentColumns.DELETE_STATE + " IS NULL OR " + Provider.AttachmentColumns.DELETE_STATE + " = ?)";
        String[] selectionArgs = {note.getSid(), String.valueOf(DeleteState.DELETE_NONE.ordinal())};
        String order = Provider.AttachmentColumns.MODIFY_TIME + " DESC ";
        Cursor cursor = db.query(Provider.AttachmentColumns.TABLE_NAME, null, selection, selectionArgs, null, null, order, null);
        Map<String, Attach> map = null;
        if (cursor != null) {
            map = new LinkedHashMap<>();
            while (cursor.moveToNext()) {
                Attach attach = cursor2Attach(cursor);
                if (attach != null) {
                    map.put(attach.getSid(), attach);
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
    @Deprecated
    public boolean updateNote(NoteInfo note, List<String> cacheList, List<String> attachList, List<DetailList> detailLists) {
        DetailNoteInfo detailNote = new DetailNoteInfo();
        detailNote.setNoteInfo(note);
        
        return updateDetailNote(detailNote, cacheList, attachList, detailLists);
    }

    /**
     * 更新文本中的附件信息
     * @param db
     * @param note
     * @param cacheList
     * @param attachList
     */
    private void updateTextAttach(SQLiteDatabase db, NoteInfo note, List<String> cacheList, List<String> attachList) {
        updateTextAttach(db, note, cacheList, attachList, true);
    }

    /**
     * 更新文本中的附件信息
     * @param db
     * @param note
     * @param cacheList 缓存中的附件集合
     * @param attachList 实际文本中识别出来的附件集合
     * @param updateUpdate 是否需要更新笔记的信息，false:仅仅删除附件数据库记录
     */
    public void updateTextAttach(SQLiteDatabase db, NoteInfo note, List<String> cacheList, List<String> attachList, boolean updateUpdate) {
        if (db == null) {
            db = mDBHelper.getWritableDatabase();
        }
        String noteSid = note.hasAttach() ? null : note.getSid();
        if (attachList != null && attachList.size() > 0) {  //有附件，则与缓存中比较
            if (updateUpdate) {
                //更新附件的noteid
                KLog.d(TAG, "--updateAttachNote--list---" + attachList);
                updateAttachNote(db, attachList, note.getSid());
            } else {
                KLog.d(TAG, "--updateTextAttach---not--updateUpdate---attach---note---");
            }
            if (cacheList != null && cacheList.size() > 0) {  //缓存中有附件sid
                //删除多余的附件
                cacheList.removeAll(attachList);
                if (cacheList.size() > 0) { //删除了还有多余的附件
                    KLog.d(TAG, "--deleteAttaches--list---" + cacheList);
                    deleteAttaches(db, cacheList, noteSid);
                }
            }
        } else {    //笔记中实际没有附件
            //删除多余的附件
            KLog.d(TAG, "--deleteAttaches--list---" + cacheList);
            deleteAttaches(db, cacheList, noteSid);
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
    private void updateNoteMoveFolder(SQLiteDatabase db, Folder oldFolder, Folder newFolder, long time) {
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
            Object[] selectionArgs = {oldFolder.getId(), oldFolder.getCount(), newFolder.getId(), newFolder.getCount(),
                    time, syncState.ordinal(), oldFolder.getId(), newFolder.getId()};
            db.execSQL(sb.toString(), selectionArgs);
        } else {
            //原始文件夹是所有文件夹，则只更新目的文件夹
                    /*UPDATE folder SET _count = ? where _id = ?*/
            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE ").append(Provider.FolderColumns.TABLE_NAME).append(" set ").append(Provider.FolderColumns._COUNT)
                    .append(" = ? WHERE ").append(Provider.FolderColumns._ID).append(" = ?");
            Object[] selectionArgs = {newFolder.getCount(), newFolder.getId()};
            db.execSQL(sb.toString(), selectionArgs);
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
    public boolean move2Folder(List<DetailNoteInfo> notes, Folder oldFolder, Folder newFolder) {
        long time = System.currentTimeMillis();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            List<DetailNoteInfo> noteList = new ArrayList<>();
            for (DetailNoteInfo detailNote : notes) {
                NoteInfo note = detailNote.getNoteInfo();
                if (newFolder.getSid().equals(note.getFolderId())) {
                    continue;
                }
                ContentValues values = initNoteMoveValues(note, time, newFolder.getSid());
    
                int row = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns._ID + " = ?", new String[] {String.valueOf(note.getId())});
                
                if (row > 0) {
                    updateNoteMoveFolder(db, oldFolder, newFolder, time);
                    noteList.add(detailNote);
                }
                
            }
            if (noteList.size() > 0) {
                if (noteList.size() == 1) {
                    notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.MOVE, noteList.get(0));
                } else {
                    notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.MOVE, noteList);
                }
            } else {
                KLog.d(TAG, "--move2Folder----result----list---0----not---notifyObservers---");
            }
            db.setTransactionSuccessful();
            return true;
        } catch (SQLException e) {
            KLog.e(TAG, "---move2Folder---list---error----" + e.getMessage());
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
        NoteInfo info = new NoteInfo(noteId);
        return getNote(info);
    }

    /**
     * 搜索笔记
     * @param keyword 关键字
     * @return
     */
    public List<DetailNoteInfo> findNotes(User user, String keyword) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String selection = null;
        String[] selectionArgs = null;
        int sort = 0;
        int deleteState = DeleteState.DELETE_NONE.ordinal();
        //是否加载回收站里的笔记
        int userId = 0;
        int state = 0;
        if (user != null) { //当前用户有登录
            userId = user.getId();
            state = user.getState();
        }
        
        if (state != 0) {   //用户不可用
            KLog.d(TAG, "find note but user is disable or offline:" + user);
            return null;
        }
        
        if (userId == 0) {  //没有用户登录或者用户不可用或者用户退出登录了
            selection = "(" + Provider.NoteColumns.USER_ID + " = ? OR " + Provider.NoteColumns.USER_ID + " IS NULL) ";
        } else {
            selection = Provider.NoteColumns.USER_ID + " = ? ";
        }
        selection += " AND (" + Provider.NoteColumns.DELETE_STATE + " IS NULL OR " + Provider.NoteColumns.DELETE_STATE + " = ?) AND " +
            "CASE WHEN " + Provider.NoteColumns.SHOW_CONTENT + " IS NOT NULL THEN " + Provider.NoteColumns.SHOW_CONTENT + " ELSE " + Provider.NoteColumns.CONTENT + " END LIKE ?";
        selectionArgs = new String[] {String.valueOf(userId), String.valueOf(deleteState), "%" + keyword + "%"};
        List<DetailNoteInfo> list = null;
        String orderBy = getNoteSort(sort);
        Cursor cursor = db.query(Provider.NoteColumns.TABLE_NAME, null, selection, selectionArgs, null, null, orderBy);
        if (cursor != null) {
            list = new ArrayList<>();
            while (cursor.moveToNext()) {
                NoteInfo note = cursor2Note(cursor);
                DetailNoteInfo detailNote = new DetailNoteInfo();
                detailNote.setNoteInfo(note);

                String noteSid = note.getSid();

                if (note.isDetailNote()) {
                    List<DetailList> details = getDetailList(db, noteSid);
                    detailNote.setDetailList(details);
                }
                if (note.hasAttach()) { //有附件
                    
                    Attach attach = getLastAttach(db, note.getSid());

                    detailNote.setLastAttach(attach);
                }

                list.add(detailNote);
            }
            cursor.close();
        }
        return list;
    }

    /**
     * 合并本地的笔记到对应的用户
     * @param user
     * @param args
     * @return
     */
    public void mergeLocalNotes(User user, Bundle args) {
        if (user == null || !user.checkId()) {  //用户不可用
            KLog.d(TAG, "merge local note user is null or id is 0");
            return;
        }
        int userId = user.getId();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        boolean success = false;
        db.beginTransaction();

        try {
            long time = System.currentTimeMillis();
            //更新文件夹所属的用户
            ContentValues values = new ContentValues();
            values.put(Provider.FolderColumns.USER_ID, userId);
            values.put(Provider.FolderColumns.MODIFY_TIME, time);
            long rowId = db.update(Provider.FolderColumns.TABLE_NAME, values, Provider.FolderColumns.USER_ID + " = 0 OR " + Provider.FolderColumns.USER_ID + " IS NULL", null);
            
            KLog.d(TAG, "merge local folder size:" + rowId);
            
            //更新笔记的所属用户
            values = new ContentValues();
            values.put(Provider.NoteColumns.USER_ID, userId);
            values.put(Provider.NoteColumns.MODIFY_TIME, time);
            rowId = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns.USER_ID + " = 0 OR " + Provider.NoteColumns.USER_ID + " IS NULL", null);

            KLog.d(TAG, "merge local note size:" + rowId);
            
            //更新附件的所属用户
            values = new ContentValues();
            values.put(Provider.NoteColumns.USER_ID, userId);
            values.put(Provider.NoteColumns.MODIFY_TIME, time);
            rowId = db.update(Provider.NoteColumns.TABLE_NAME, values, Provider.NoteColumns.USER_ID + " = 0 OR " + Provider.NoteColumns.USER_ID + " IS NULL", null);

            KLog.d(TAG, "merge local attach size:" + rowId);
            db.setTransactionSuccessful();
            success = true;
        } catch (Exception e) {
            KLog.e(TAG, "merge local notes error:" + e.getMessage());
        } finally {
            db.endTransaction();
        }

        if (success) {  //通知并刷新界面,一般是重新加载数据
            KLog.d(TAG, "merge local notes success");
            notifyObservers(Provider.NOTIFY_FLAG, Observer.NotifyType.BATCH_UPDATE);
        }
    }
    
}
