package net.ibaixin.notes.persistent;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.model.NoteInfo;
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
     * 获取当前用户下所有的笔记
     * @author huanghui1
     * @update 2016/3/7 17:41
     * @version: 1.0.0
     */
    public List<NoteInfo> getAllNotes(User user) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        int userId = 0;
        if (user != null) { //当前用户有登录
            userId = user.getId();
        }
        List<NoteInfo> list = null;
        Cursor cursor = db.query(Provider.NoteColumns.TABLE_NAME, null, Provider.NoteColumns.USER_ID + " = ?", new String[]{String.valueOf(userId)}, null, null, Provider.NoteColumns.DEFAULT_SORT);
        if (cursor != null) {
            list = new ArrayList<>();
            while (cursor.moveToNext()) {
                NoteInfo note = new NoteInfo();
                note.setId(cursor.getInt(cursor.getColumnIndex(Provider.NoteColumns._ID)));
            }
        }
        return null;
    }

}
