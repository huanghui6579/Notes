package net.ibaixin.notes.persistent;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.db.Provider;

/**
 * 用户账号的服务层
 * @author huanghui1
 * @update 2016/3/7 17:47
 * @version: 0.0.1
 */
public class UserManager {
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
    
}
