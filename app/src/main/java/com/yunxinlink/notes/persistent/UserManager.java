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

import java.util.ArrayList;
import java.util.List;

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
        user.setAvatarHash(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.AVATAR_HASH)));
        user.setNickname(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.NICKNAME)));
        user.setToken(cursor.getString(cursor.getColumnIndex(Provider.UserColumns.TOKEN)));
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
        
        String avatarHash = user.getAvatarHash();
        if (avatarHash != null) {
            values.put(Provider.UserColumns.AVATAR_HASH, avatarHash);
        }
        
        String nickname = user.getNickname();
        if (nickname != null) {
            values.put(Provider.UserColumns.NICKNAME, nickname);
        }
        
        if (isAdd) {
            long createTime = user.getCreateTime();
            
            if (createTime == 0) {
                createTime = System.currentTimeMillis();
            }

            values.put(Provider.UserColumns.CREATE_TIME, createTime);
        }

        String token = user.getToken();
        if (token != null) {
            values.put(Provider.UserColumns.TOKEN, token);
        }
        return values;
    }

    /**
     * 根据条件查询本地用户信息
     * @param param
     * @return
     */
    public User getAccountInfo(User param) {
        String selection = null;
        String[] selectionArgs = null;
        int userId = param.getId();
        if (userId > 0) {
            selection = Provider.UserColumns._ID + " = ?";
            selectionArgs = new String[] {String.valueOf(userId)};
        } else if (!TextUtils.isEmpty(param.getSid())) {
            selection = Provider.UserColumns.SID + " = ?";
            selectionArgs = new String[] {param.getSid()};
        } else if (!TextUtils.isEmpty(param.getUsername())) {
            selection = Provider.UserColumns.USERNAME + " = ?";
            selectionArgs = new String[] {param.getUsername()};
        } else if (!TextUtils.isEmpty(param.getOpenUserId())) {
            selection = Provider.UserColumns.OPEN_USER_ID + " = ?";
            selectionArgs = new String[] {param.getOpenUserId()};
        } else if (!TextUtils.isEmpty(param.getEmail())) {
            selection = Provider.UserColumns.EMAIL + " = ?";
            selectionArgs = new String[] {param.getEmail()};
        } else if (!TextUtils.isEmpty(param.getMobile())) {
            selection = Provider.UserColumns.MOBILE + " = ?";
            selectionArgs = new String[] {param.getMobile()};
        }
        if (selection == null) {
            return null;
        }
        User user = null;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, null, selection, selectionArgs, null, null, null);
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
        if (user != null) {
            notifyObservers(Provider.UserColumns.NOTIFY_FLAG, Observer.NotifyType.REFRESH, user);
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
        if (user != null) {
            notifyObservers(Provider.UserColumns.NOTIFY_FLAG, Observer.NotifyType.REFRESH, user);
        }
        return user;
    }

    /**
     * 获取用户的基本信息
     * @param sid 根据用户的sid来查询本地信息
     * @return
     */
    public User getAccountInfoBySid(String sid) {
        return getAccountInfoBySid(sid, true);
    }

    /**
     * 获取用户的基本信息
     * @param sid 根据用户的sid来查询本地信息
     * @param notify 是否需要通知           
     * @return
     */
    public User getAccountInfoBySid(String sid, boolean notify) {
        if (sid == null) {
            KLog.d(TAG, "get account info by sid failed sid is null");
            return null;
        }
        User user = null;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, null, Provider.UserColumns.SID + " = ?", new String[] {sid}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            user = cursor2User(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
        if (user != null && notify) {
            notifyObservers(Provider.UserColumns.NOTIFY_FLAG, Observer.NotifyType.REFRESH, user);
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
        return update(user, db, true);
    }

    /**
     * 更新用户信息
     * @param user
     * @param refresh 是否刷新界面
     * @return
     */
    private boolean update(User user, SQLiteDatabase db, boolean refresh) {
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
        boolean reload = false;
        if (user.checkId()) {   //id可用
            selection = Provider.UserColumns._ID + " = ?";
            args = new String[] {String.valueOf(user.getId())};
            KLog.d(TAG, "update user with user id:" + user.getId());
        } else {
            List<String> argList = new ArrayList<>();
            selection = Provider.UserColumns.SID + " = ?";
            argList.add(user.getSid());
            if (!TextUtils.isEmpty(user.getEmail())) {
                selection += " or " + Provider.UserColumns.EMAIL + " = ?";
                argList.add(user.getEmail());
            }
            if (!TextUtils.isEmpty(user.getMobile())) {
                selection += " or " + Provider.UserColumns.MOBILE + " = ?";
                argList.add(user.getMobile());
            }
            if (!TextUtils.isEmpty(user.getUsername())) {
                selection += " or " + Provider.UserColumns.USERNAME + " = ?";
                argList.add(user.getUsername());
            }
            args = new String[argList.size()];
            args = argList.toArray(args);
            KLog.d(TAG, "update user with user sid:" + user.getSid());
            //用户没有id,则需要重新查询
            reload = true;
        }
        int rowId = db.update(Provider.UserColumns.TABLE_NAME, values, selection, args);
        boolean success = rowId > 0;
        if (reload) {
            user = getAccountInfoBySid(user.getSid(), false);
            KLog.d(TAG, "update user and reload user by sid:" + user);
        }
        if (refresh) {
            NoteApplication.getInstance().setCurrentUser(user);
            if (success) {
                //通知界面刷新
                notifyObservers(Provider.UserColumns.NOTIFY_FLAG, Observer.NotifyType.UPDATE, user);
            }
        }
        return success;
    }

    /**
     * 添加用户
     * @param user
     * @return
     */
    private boolean add(User user, SQLiteDatabase db) {
        return add(user, db, true);
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
     * 添加用户
     * @param user
     * @param db
     * @param refresh
     * @return
     */
    private boolean add(User user, SQLiteDatabase db, boolean refresh) {
        if (user == null) {
            KLog.d(TAG, "add user failed user param is null");
            return false;
        }
        if (db == null) {
            db = mDBHelper.getWritableDatabase();
        }
        user.setCreateTime(System.currentTimeMillis());
        ContentValues values = initValues(user, true);
        long rowId = 0;
        try {
            rowId = db.insert(Provider.UserColumns.TABLE_NAME, null, values);
        } catch (Exception e) {
            KLog.e(TAG, "add user error:" + e.getMessage());
        }
        boolean success = rowId > 0;
        if (success) {
            user.setId((int) rowId);
            if (refresh) {
                NoteApplication.getInstance().setCurrentUser(user);
                //通知界面刷新
                notifyObservers(Provider.UserColumns.NOTIFY_FLAG, Observer.NotifyType.ADD, user);
            }
        }
        return success;
    }

    /**
     * 添加用户
     * @param user
     * @param refresh
     * @return
     */
    public boolean add(User user, boolean refresh) {
        return add(user, null, refresh);
    }

    /**
     * 添加或更新数据，若记录没有，则添加,有，则更新
     * @param user
     * @return
     */
    public boolean insertOrUpdate(User user) {
        return insertOrUpdate(user, true);
    }
    
    /**
     * 添加或更新数据，若记录没有，则添加,有，则更新
     * @param user
     * @param refresh 是否刷新界面，并且设置到缓存里
     * @return
     */
    public boolean insertOrUpdate(User user, boolean refresh) {
        if (user == null || TextUtils.isEmpty(user.getSid())) {
            KLog.d(TAG, "insertOdUpdate user failed user param is null:" + user);
            return false;
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        long id = getUserId(user, db);
        boolean success = false;
        if (id > 0) {    //本地有该用户，则更新
            user.setId((int) id);
            success = update(user, db, refresh);
        } else {    //添加
            success = add(user, db, refresh);
        }
        return success;
    }

    /**
     * 获取用户本地的id
     * @param user
     * @param db
     * @return
     */
    private long getUserId(User user, SQLiteDatabase db) {
        if (db == null) {
            db = mDBHelper.getReadableDatabase();
        }
        String openUserId = user.getOpenUserId();
        String selection = null;
        String[] args = null;
        if (TextUtils.isEmpty(openUserId)) {
            List<String> argList = new ArrayList<>();
            selection = Provider.UserColumns.SID + " = ? or " + Provider.UserColumns.EMAIL + " = ? or " + Provider.UserColumns.MOBILE + " = ? or " + Provider.UserColumns.USERNAME + " = ?";
            argList.add(user.getSid());
            if (!TextUtils.isEmpty(user.getEmail())) {
                selection += " or " + Provider.UserColumns.EMAIL + " = ?";
                argList.add(user.getEmail());
            }
            if (!TextUtils.isEmpty(user.getMobile())) {
                selection += " or " + Provider.UserColumns.MOBILE + " = ?";
                argList.add(user.getMobile());
            }
            if (!TextUtils.isEmpty(user.getUsername())) {
                selection += " or " + Provider.UserColumns.USERNAME + " = ?";
                argList.add(user.getUsername());
            }
            args = new String[argList.size()];
            args = argList.toArray(args);
            KLog.d(TAG, "insert or update check user exists with sid:" + user.getSid());
        } else {
            selection = Provider.UserColumns.OPEN_USER_ID + " = ?";
            args = new String[] {openUserId};
            KLog.d(TAG, "insert or update check user exists with open user id:" + openUserId);
        }
        Cursor cursor = db.query(Provider.UserColumns.TABLE_NAME, new String[] {Provider.UserColumns._ID}, selection, args, null, null, null);
        long id = 0;
        if (cursor != null && cursor.moveToFirst()) {
            id = cursor.getLong(0);
        }
        if (cursor != null) {
            cursor.close();
        }
        return id;
    }

    /**
     * 用户退出登录
     * @param user
     */
    public void logoutUser(User user) {
        UserManager.getInstance().notifyObservers(Provider.UserColumns.NOTIFY_FLAG, Observer.NotifyType.REMOVE, user);
    }

    /**
     * 修改当前用户最后的同步时间
     * @param user 当前用户
     * @return
     */
    public boolean updateSyncTime(User user) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        long lastSyncTime = user.getLastSyncTime();
        long time = System.currentTimeMillis();
        if (lastSyncTime == 0 || lastSyncTime < time) {
            user.setLastSyncTime(time);
        }
        values.put(Provider.UserColumns.LAST_SYNC_TIME, user.getLastSyncTime());
        long rowId = 0;
        try {
            rowId = db.update(Provider.UserColumns.TABLE_NAME, values, Provider.UserColumns._ID + " = ?", new String[] {String.valueOf(user.getId())});
        } catch (Exception e) {
            KLog.d(TAG, "update user last sync time error:" + e.getMessage());
        }
        return rowId > 0;
    }
}
