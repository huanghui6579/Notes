package com.yunxinlink.notes.persistent;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.db.DBHelper;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.db.observer.Observer;
import com.yunxinlink.notes.model.User;

/**
 * 用户账号的服务层
 * @author huanghui1
 * @update 2016/3/7 17:47
 * @version: 0.0.1
 */
public class UserManager extends Observable<Observer> {
    private static final String TAG = "UserManager";
    private static UserManager mInstance = null;

    private DBHelper mDBHelper;
    
    private UserManager() {
        mDBHelper = new DBHelper(NoteApplication.getInstance());
    }

    /**
     * 获取实例
     * @author huanghui1
     * @update 2016/3/7 17:38
     * @version: 1.0.0
     */
    public static UserManager getInstance() {
        if (mInstance == null) {
            synchronized (UserManager.class) {
                if (mInstance == null) {
                    mInstance = new UserManager();
                }
            }
        }
        return mInstance;
    }
    
    /**
     * 判断本地是否有账号
     * @author huanghui1
     * @update 2016/3/7 17:50
     * @version: 1.0.0
     */
    public boolean hasAccount() {
        return hasAccount(null);
    }

    /**
     * 判断本地是否有账号
     * @author huanghui1
     * @update 2016/3/7 18:18
     * @version: 1.0.0
     */
    public boolean hasAccount(SQLiteDatabase db) {
        boolean hasAccount = false;
        if (db == null) {
            db = mDBHelper.getReadableDatabase();
        }
        Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, new String[]{"count(*) as count"}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            hasAccount = count > 0;
        }
        if (cursor != null) {
            cursor.close();
        }
        return hasAccount;
    }

    /**
     * 获取cursor中的值
     * @param cursor
     * @return
     */
    private User cursor2User(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns._ID)));
        user.setUsername(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.USERNAME)));
        user.setPassword(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.PASSWORD)));
        user.setAvatar(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.AVATAR)));
        user.setCreateTime(cursor.getLong(cursor.getColumnIndex(Provider.UserColumns.CREATE_TIME)));
        user.setGender(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns.GENDER)));
        user.setLastSyncTime(cursor.getLong(cursor.getColumnIndex(Provider.UserColumns.LAST_SYNC_TIME)));
        user.setMobile(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.MOBILE)));
        user.setEmail(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.EMAIL)));
        user.setSid(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.SID)));
        user.setState(cursor.getInt(cursor.getColumnIndex(Provider.UserColumns.STATE)));
        user.setOpenUserId(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.OPEN_USER_ID)));
        return user;
    }

    /**
     * 初始化更新的values
     * @param user
     * @return
     */
    private ContentValues initValues(User user, boolean isAdd) {
        ContentValues values = new ContentValues();
        String username = user.getUsername();
        if (username != null) {
            values.put(Provider.UserColumns.USERNAME, username);
        }
        String password = user.getPassword();
        if (password != null) {
//            String encodePwd = SystemUtil.md5Hex(password);
            values.put(Provider.UserColumns.PASSWORD, password);
        }
        Integer state = user.getState();
        if (state != null) {
            values.put(Provider.UserColumns.STATE, state);
        }
        String avatar = user.getAvatar();
        if (avatar != null) {
            values.put(Provider.UserColumns.AVATAR, avatar);
        }
        Integer gender = user.getGender();
        if (gender != null) {
            values.put(Provider.UserColumns.GENDER, gender);
        }
        String mobile = user.getMobile();
        if (mobile != null) {
            values.put(Provider.UserColumns.MOBILE, mobile);
        }
        String email = user.getEmail();
        if (email != null) {
            values.put(Provider.UserColumns.EMAIL, email);
        }
        String sid = user.getSid();
        if (sid != null) {
            values.put(Provider.UserColumns.SID, sid);
        }
        values.put(Provider.UserColumns.MODIFY_TIME, System.currentTimeMillis());
        
        Long lastSyncTime = user.getLastSyncTime();
        if (lastSyncTime != null) {
            values.put(Provider.UserColumns.LAST_SYNC_TIME, lastSyncTime);
        }
        
        String openUserId = user.getOpenUserId();
        if (openUserId != null) {
            values.put(Provider.UserColumns.OPEN_USER_ID, openUserId);
        }
        
        if (isAdd) {
            long createTime = user.getCreateTime();

            values.put(Provider.UserColumns.CREATE_TIME, createTime);
        }
        return values;
    }

    /**
     * 获取用户的基本信息
     * @param userId 本地用户的id
     * @return
     */
    public User getAccountInfo(int userId) {
        if (userId <= 0) {
            KLog.d(TAG, "get account info failed user id <= 0");
            return null;
        }
        User user = null;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, null, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(userId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            user = cursor2User(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        return user;
    }

    /**
     * 获取用户的基本信息
     * @param openUserId 第三方账号的用户id
     * @return
     */
    public User getAccountInfo(String openUserId) {
        if (openUserId == null) {
            KLog.d(TAG, "get account info failed openUserId is null");
            return null;
        }
        User user = null;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, null, Provider.UserColumns.OPEN_USER_ID + " = ?", new String[] {openUserId}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            user = cursor2User(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        return user;
    }

    /**
     * 修改本地用户的状态
     * @param user
     * @return
     */
    public boolean changeUserState(User user) {
        if (user == null) {
            KLog.d(TAG, "change user state failed user param is null");
            return false;
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Provider.UserColumns.STATE, user.getState());
        values.put(Provider.UserColumns.MODIFY_TIME, System.currentTimeMillis());
        
        int rowId = db.update(Provider.UserColumns.TABLE_NAME, values, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(user.getId())});
        return rowId > 0;
    }

    /**
     * 更新用户信息
     * @param user
     * @return
     */
    public boolean update(User user) {
        return update(user, null);
    }
    
    /**
     * 更新用户信息
     * @param user
     * @return
     */
    private boolean update(User user, SQLiteDatabase db) {
        if (user == null) {
            KLog.d(TAG, "update user failed user param is null");
            return false;
        }
        if (db == null) {
            db = mDBHelper.getWritableDatabase();
        }
        ContentValues values = initValues(user, false);
        String selection = null;
        String[] args = null;
        if (user.checkId()) {   //id可用
            selection = Provider.UserColumns._ID + " = ?";
            args = new String[] {String.valueOf(user.getId())};
            KLog.d(TAG, "update user with user id:" + user.getId());
        } else {
            selection = Provider.UserColumns.SID + " = ?";
            args = new String[] {user.getSid()};
            KLog.d(TAG, "update user with user sid:" + user.getSid());
        }
        int rowId = db.update(Provider.UserColumns.TABLE_NAME, values, selection, args);
        boolean success = rowId > 0;
        NoteApplication.getInstance().setCurrentUser(user);
        if (success) {
            //通知界面刷新
            notifyObservers(Provider.UserColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, user);
        }
        return success;
    }

    /**
     * 添加用户
     * @param user
     * @return
     */
    private boolean add(User user, SQLiteDatabase db) {
        if (user == null) {
            KLog.d(TAG, "add user failed user param is null");
            return false;
        }
        if (db == null) {
            db = mDBHelper.getWritableDatabase();
        }
        user.setCreateTime(System.currentTimeMillis());
        ContentValues values = initValues(user, true);
        long rowId = db.insert(Provider.UserColumns.TABLE_NAME, null, values);
        boolean success = rowId > 0;
        if (success) {
            user.setId((int) rowId);
            NoteApplication.getInstance().setCurrentUser(user);
            //通知界面刷新
            notifyObservers(Provider.UserColumns.NOTIFY_FLAG, Observer.NotifyType.ADD, user);
        }
        return success;
    }

    /**
     * 添加用户
     * @param user
     * @return
     */
    public boolean add(User user) {
        return add(user, null);
    }

    /**
     * 添加或更新数据，若记录没有，则添加,有，则更新
     * @param user
     * @return
     */
    public boolean insertOrUpdate(User user) {
        if (user == null || TextUtils.isEmpty(user.getSid())) {
            KLog.d(TAG, "insertOdUpdate user failed user param is null:" + user);
            return false;
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        String openUserId = user.getOpenUserId();
        String selection = null;
        String[] args = null;
        if (TextUtils.isEmpty(openUserId)) {
            selection = Provider.UserColumns.SID + " = ?";
            args = new String[] {user.getSid()};
            KLog.d(TAG, "insert or update check user exists with sid:" + user.getSid());
        } else {
            selection = Provider.UserColumns.OPEN_USER_ID + " = ?";
            args = new String[] {openUserId};
            KLog.d(TAG, "insert or update check user exists with open user id:" + openUserId);
        }
        Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, new String[] {"count(*) as count"}, selection, args, null, null, null);
        long count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getLong(0);
        }
        if (cursor != null) {
            cursor.close();
        }
        boolean success = false;
        if (count > 0) {    //本地有该用户，则更新
            success = update(user, db);
        } else {    //添加
            success = add(user, db);
        }
        return success;
    }
    
}
