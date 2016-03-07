package net.ibaixin.notes.persistent;

import android.database.sqlite.SQLiteDatabase;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.db.DBHelper;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.model.User;
import net.ibaixin.notes.util.log.Log;

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
        if (user == null) { //当前用户没有登录，或者还没有创建账号
            UserManager userManager = UserManager.getInstance();
            if (!userManager.hasAccount(db)) {   //本地没有账号，则加载所有没有指明账号的笔记
                
            } else {    //有账号，但是没有登录
                Log.d("---getAllNotes---user-----not-----login----");
            }
        }
        return null;
    }

}
