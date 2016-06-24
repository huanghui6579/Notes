package net.ibaixin.notes.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.ibaixin.notes.util.log.Log;

/**
 * 创建数据库
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/5 16:11
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "notes.db";
    
    private static final String TAG = "DBHelper";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "---DBHelper---onCreate---begin--");
        //创建笔记表
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.NoteColumns.TABLE_NAME).append(" (")
                .append(Provider.NoteColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(Provider.NoteColumns.SID).append(" TEXT UNIQUE NOT NULL, ")
                .append(Provider.NoteColumns.USER_ID).append(" INTEGER DEFAULT 0, ")
                .append(Provider.NoteColumns.CONTENT).append(" TEXT, ")
                .append(Provider.NoteColumns.REMIND_ID).append(" INTEGER, ")
                .append(Provider.NoteColumns.REMIND_TIME).append(" INTEGER, ")
                .append(Provider.NoteColumns.FOLDER_ID).append(" TEXT, ")
                .append(Provider.NoteColumns.KIND).append(" TEXT, ")
                .append(Provider.NoteColumns.SYNC_STATE).append(" INTEGER, ")
                .append(Provider.NoteColumns.DELETE_STATE).append(" INTEGER, ")
                .append(Provider.NoteColumns.HAS_ATTACH).append(" INTEGER, ")
                .append(Provider.NoteColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.NoteColumns.MODIFY_TIME).append(" INTEGER, ")
                .append(Provider.NoteColumns.HASH).append(" TEXT, ")
                .append(Provider.NoteColumns.OLD_CONTENT).append(" TEXT);");
        db.execSQL(builder.toString());

        //创建文件夹表
        builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.FolderColumns.TABLE_NAME).append(" (")
                .append(Provider.FolderColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(Provider.FolderColumns.SID).append(" TEXT UNIQUE NOT NULL, ")
                .append(Provider.FolderColumns.USER_ID).append(" INTEGER DEFAULT 0, ")
                .append(Provider.FolderColumns.IS_LOCK).append(" INTEGER, ")
                .append(Provider.FolderColumns.NAME).append(" TEXT UNIQUE NOT NULL, ")
                .append(Provider.FolderColumns.SORT).append(" INTEGER, ")
                .append(Provider.FolderColumns.SYNC_STATE).append(" INTEGER, ")
                .append(Provider.FolderColumns.DELETE_STATE).append(" INTEGER, ")
                .append(Provider.FolderColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.FolderColumns.MODIFY_TIME).append(" INTEGER, ")
                .append(Provider.FolderColumns._COUNT).append(" INTEGER);");
        db.execSQL(builder.toString());

        //创建附件表
        builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.AttachmentColumns.TABLE_NAME).append(" (")
                .append(Provider.AttachmentColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(Provider.AttachmentColumns.SID).append(" TEXT UNIQUE NOT NULL, ")
                .append(Provider.AttachmentColumns.NOTE_ID).append(" INTEGER NOT NULL, ")
                .append(Provider.AttachmentColumns.USER_ID).append(" INTEGER DEFAULT 0, ")
                .append(Provider.AttachmentColumns.FILE_NAME).append(" TEXT, ")
                .append(Provider.AttachmentColumns.LOCAL_PATH).append(" TEXT, ")
                .append(Provider.AttachmentColumns.DECRIPTION).append(" TEXT, ")
                .append(Provider.AttachmentColumns.SERVER_PATH).append(" TEXT, ")
                .append(Provider.AttachmentColumns.SYNC_STATE).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.DELETE_STATE).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.MODIFY_TIME).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.SIZE).append(" INTEGER); ");
        db.execSQL(builder.toString());

        //创建手写、涂鸦表
        builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.HandWriteColumns.TABLE_NAME).append(" (")
                .append(Provider.HandWriteColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(Provider.HandWriteColumns.ATTACH_ID).append(" INTEGER UNIQUE, ")
                .append(Provider.HandWriteColumns.LOCAL_PATH).append(" TEXT, ")
                .append(Provider.HandWriteColumns.SIZE).append(" INTEGER, ")
                .append(Provider.HandWriteColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.HandWriteColumns.MODIFY_TIME).append(" INTEGER); ");
        db.execSQL(builder.toString());

        //创建清单表
        builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.DetailedListColumns.TABLE_NAME).append(" (")
                .append(Provider.DetailedListColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(Provider.DetailedListColumns.SID).append(" TEXT UNIQUE NOT NULL, ")
                .append(Provider.DetailedListColumns.NOTE_ID).append(" TEXT UNIQUE NOT NULL, ")
                .append(Provider.DetailedListColumns.USER_ID).append(" INTEGER DEFAULT 0, ")
                .append(Provider.DetailedListColumns.TITLE).append(" TEXT, ")
                .append(Provider.DetailedListColumns.CHECKED).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.SORT).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.SYNC_STATE).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.DELETE_STATE).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.TITLE_OLD).append(" TEXT, ")
                .append(Provider.DetailedListColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.MODIFY_TIME).append(" INTEGER); ");
        db.execSQL(builder.toString());

        //创建提醒表
        builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.RemindersColumns.TABLE_NAME).append(" (")
                .append(Provider.RemindersColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(Provider.RemindersColumns.REMIND_TIME).append(" INTEGER, ")
                .append(Provider.RemindersColumns.IS_REMINDED).append(" INTEGER, ")
                .append(Provider.RemindersColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.RemindersColumns.MODIFY_TIME).append(" INTEGER); ");
        db.execSQL(builder.toString());

        //创建用户表
        builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.UserColumns.TABLE_NAME).append(" (")
                .append(Provider.UserColumns._ID).append(" INTEGER PRIMARY KEY NOT NULL, ")
                .append(Provider.UserColumns.USERNAME).append(" TEXT UNIQUE NOT NULL, ")
                .append(Provider.UserColumns.PASSWORD).append(" TEXT NOT NULL, ")
                .append(Provider.UserColumns.ACCESS_TOKEN).append(" TEXT, ")
                .append(Provider.UserColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.UserColumns.MODIFY_TIME).append(" INTEGER, ")
                .append(Provider.UserColumns.LAST_SYNC_TIME).append(" INTEGER); ");
        db.execSQL(builder.toString());

        //创建笔记的sid索引
        builder = new StringBuilder();
        builder.append("CREATE UNIQUE INDEX IF NOT EXISTS ").append(Provider.NoteColumns.NOTE_ID_IDX)
                .append(" on ").append(Provider.NoteColumns.TABLE_NAME)
                .append("(").append(Provider.NoteColumns.SID).append(");");
        db.execSQL(builder.toString());

        //创建文件夹的sid索引
        builder = new StringBuilder();
        builder.append("CREATE UNIQUE INDEX IF NOT EXISTS ").append(Provider.FolderColumns.FOLDER_ID_IDX)
                .append(" on ").append(Provider.FolderColumns.TABLE_NAME)
                .append("(").append(Provider.FolderColumns.SID).append(");");
        db.execSQL(builder.toString());

        //创建附件的sid索引
        builder = new StringBuilder();
        builder.append("CREATE UNIQUE INDEX IF NOT EXISTS ").append(Provider.AttachmentColumns.ATTACH_ID_IDX)
                .append(" on ").append(Provider.AttachmentColumns.TABLE_NAME)
                .append("(").append(Provider.AttachmentColumns.SID).append(");");
        db.execSQL(builder.toString());

        //创建清单的sid索引
        builder = new StringBuilder();
        builder.append("CREATE UNIQUE INDEX IF NOT EXISTS ").append(Provider.DetailedListColumns.DETAILEDLIST_ID_IDX)
                .append(" on ").append(Provider.DetailedListColumns.TABLE_NAME)
                .append("(").append(Provider.DetailedListColumns.SID).append(");");
        db.execSQL(builder.toString());

        //创建用户名的索引
        builder = new StringBuilder();
        builder.append("CREATE UNIQUE INDEX IF NOT EXISTS ").append(Provider.UserColumns.USERNAME_IDX)
                .append(" on ").append(Provider.UserColumns.TABLE_NAME)
                .append("(").append(Provider.UserColumns.USERNAME).append(");");
        db.execSQL(builder.toString());
        
        //创建更新文件夹的触发器,当笔记更新时
        builder = new StringBuilder();
        builder.append("CREATE TRIGGER IF NOT EXISTS ").append(Provider.NoteColumns.EVENT_UPDATE_FOLDER)
                .append(" AFTER UPDATE ON ").append(Provider.NoteColumns.TABLE_NAME)
                .append(" BEGIN UPDATE ").append(Provider.FolderColumns.TABLE_NAME)
                .append(" SET ").append(Provider.FolderColumns.MODIFY_TIME)
                .append(" = NEW.").append(Provider.FolderColumns.MODIFY_TIME)
                .append(", ").append(Provider.FolderColumns.SYNC_STATE).append(" = 1 WHERE ")
                .append(Provider.FolderColumns._ID).append(" = NEW.").append(Provider.NoteColumns.FOLDER_ID)
                .append("; END;");
        db.execSQL(builder.toString());
        
        //创建添加笔记时的触发器
        /*CREATE TRIGGER event_insert_note AFTER INSERT ON notes  
        WHEN NEW.folder_id IS NOT NULL AND NEW.folder_id > 0 BEGIN  
        UPDATE folder SET modify_time = NEW.create_time, _count = _count + 1, 
        sync_state = 1 WHERE _id = NEW.folder_id;  END;*/
        builder = new StringBuilder();
        builder.append("CREATE TRIGGER ").append(Provider.NoteColumns.EVENT_INSERT_NOTE)
                .append(" AFTER INSERT ON ").append(Provider.NoteColumns.TABLE_NAME)
                .append(" WHEN NEW.folder_id IS NOT NULL AND NEW.folder_id > 0 BEGIN UPDATE ").append(Provider.FolderColumns.TABLE_NAME)
                .append(" SET ").append(Provider.FolderColumns.MODIFY_TIME)
                .append(" = NEW.").append(Provider.FolderColumns.CREATE_TIME)
                .append(", ").append(Provider.FolderColumns._COUNT).append(" = ").append(Provider.FolderColumns._COUNT).append(" + 1")
                .append(", ").append(Provider.FolderColumns.SYNC_STATE).append(" = 1 WHERE ")
                .append(Provider.FolderColumns._ID).append(" = NEW.").append(Provider.NoteColumns.FOLDER_ID)
                .append("; END;");
        db.execSQL(builder.toString());

        long endTime = System.currentTimeMillis();
        Log.d(TAG, "---DBHelper---onCreate---end--" + (endTime - startTime));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //删除note的sid索引
        db.execSQL("DROP INDEX IF EXISTS " + Provider.NoteColumns.NOTE_ID_IDX);
        //删除文件夹的sid索引
        db.execSQL("DROP INDEX IF EXISTS " + Provider.FolderColumns.FOLDER_ID_IDX);
        //删除附件的sid索引
        db.execSQL("DROP INDEX IF EXISTS " + Provider.AttachmentColumns.ATTACH_ID_IDX);
        //删除清单的sid索引
        db.execSQL("DROP INDEX IF EXISTS " + Provider.DetailedListColumns.DETAILEDLIST_ID_IDX);
        //删除用户名的索引
        db.execSQL("DROP INDEX IF EXISTS " + Provider.UserColumns.USERNAME_IDX);

        //删除修改笔记后更新文件夹的触发器
        db.execSQL("DROP TRIGGER IF EXISTS " + Provider.NoteColumns.EVENT_UPDATE_FOLDER);
        
        //删除更新笔记后更新文件夹的触发器
        db.execSQL("DROP TRIGGER IF EXISTS " + Provider.NoteColumns.EVENT_INSERT_NOTE);

        db.execSQL("DROP TABLE IF EXISTS " + Provider.NoteColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.FolderColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.AttachmentColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.DetailedListColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.HandWriteColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.RemindersColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.UserColumns.TABLE_NAME);
        
        onCreate(db);
    }
}
