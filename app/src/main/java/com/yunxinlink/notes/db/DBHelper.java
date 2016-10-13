package com.yunxinlink.notes.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.util.log.Log;

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
                .append(Provider.NoteColumns.TITLE).append(" TEXT, ")
                .append(Provider.NoteColumns.CONTENT).append(" TEXT, ")
                .append(Provider.NoteColumns.SHOW_CONTENT).append(" TEXT, ")
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
                .append(Provider.FolderColumns.NAME).append(" TEXT NOT NULL, ")
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
                .append(Provider.AttachmentColumns.NOTE_ID).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.USER_ID).append(" INTEGER DEFAULT 0, ")
                .append(Provider.AttachmentColumns.FILE_NAME).append(" TEXT, ")
                .append(Provider.AttachmentColumns.LOCAL_PATH).append(" TEXT, ")
                .append(Provider.AttachmentColumns.DESCRIPTION).append(" TEXT, ")
                .append(Provider.AttachmentColumns.SERVER_PATH).append(" TEXT, ")
                .append(Provider.AttachmentColumns.SYNC_STATE).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.DELETE_STATE).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.MODIFY_TIME).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.TYPE).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.SIZE).append(" INTEGER, ")
                .append(Provider.AttachmentColumns.HASH).append(" TEXT, ")
                .append(Provider.AttachmentColumns.MIME_TYPE).append(" TEXT); ");
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
                .append(Provider.DetailedListColumns.NOTE_ID).append(" TEXT NOT NULL, ")
                .append(Provider.DetailedListColumns.USER_ID).append(" INTEGER DEFAULT 0, ")
                .append(Provider.DetailedListColumns.TITLE).append(" TEXT, ")
                .append(Provider.DetailedListColumns.CHECKED).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.SORT).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.OLD_SORT).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.SYNC_STATE).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.DELETE_STATE).append(" INTEGER, ")
                .append(Provider.DetailedListColumns.OLD_TITLE).append(" TEXT, ")
                .append(Provider.DetailedListColumns.HASH).append(" TEXT, ")
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
                .append(Provider.UserColumns.USERNAME).append(" TEXT UNIQUE, ")
                .append(Provider.UserColumns.PASSWORD).append(" TEXT , ")
                .append(Provider.UserColumns.SID).append(" TEXT UNIQUE , ")
                .append(Provider.UserColumns.MOBILE).append(" TEXT UNIQUE, ")
                .append(Provider.UserColumns.EMAIL).append(" TEXT UNIQUE, ")
                .append(Provider.UserColumns.ACCESS_TOKEN).append(" TEXT, ")
                .append(Provider.UserColumns.AVATAR).append(" TEXT, ")
                .append(Provider.UserColumns.GENDER).append(" INTEGER, ")
                .append(Provider.UserColumns.STATE).append(" INTEGER, ")
                .append(Provider.UserColumns.CREATE_TIME).append(" INTEGER, ")
                .append(Provider.UserColumns.MODIFY_TIME).append(" INTEGER, ")
                .append(Provider.UserColumns.OPEN_USER_ID).append(" TEXT, ")
                .append(Provider.UserColumns.AVATAR_HASH).append(" TEXT, ")
                .append(Provider.UserColumns.NICKNAME).append(" TEXT, ")
                .append(Provider.UserColumns.SYNC_STATE).append(" INTEGER, ")
                .append(Provider.UserColumns.LAST_SYNC_TIME).append(" INTEGER); ");
        db.execSQL(builder.toString());
        
        //创建widget的表
        builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.WidgetColumns.TABLE_NAME).append(" (")
                .append(Provider.WidgetColumns._ID).append(" INTEGER PRIMARY KEY NOT NULL, ")
                .append(Provider.WidgetColumns.TITLE).append(" TEXT NOT NULL, ")
                .append(Provider.WidgetColumns.TYPE).append(" INTEGER, ")
                .append(Provider.WidgetColumns.SORT).append(" INTEGER, ")
                .append(Provider.WidgetColumns.SORT2).append(" INTEGER);");
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
        builder.append("CREATE TRIGGER IF NOT EXISTS ").append(Provider.NoteColumns.TRI_UPDATE_FOLDER)
                .append(" AFTER UPDATE ON ").append(Provider.NoteColumns.TABLE_NAME)
                .append(" BEGIN UPDATE ").append(Provider.FolderColumns.TABLE_NAME)
                .append(" SET ").append(Provider.FolderColumns.MODIFY_TIME)
                .append(" = NEW.").append(Provider.FolderColumns.MODIFY_TIME)
                .append(", ").append(Provider.FolderColumns.SYNC_STATE).append(" = ").append(SyncState.SYNC_UP.ordinal()).append(" WHERE ")
                .append(Provider.FolderColumns.SID).append(" = NEW.").append(Provider.NoteColumns.FOLDER_ID)
                .append("; END;");
        db.execSQL(builder.toString());
        
        //创建添加笔记时的触发器
        /*CREATE TRIGGER tri_insert_note AFTER INSERT ON notes  
        WHEN NEW.folder_id IS NOT NULL BEGIN  
        UPDATE folder SET modify_time = NEW.create_time, _count = _count + 1, 
        sync_state = 0 WHERE sid = NEW.folder_id;  END;*/
        builder = new StringBuilder();
        builder.append("CREATE TRIGGER ").append(Provider.NoteColumns.TRI_INSERT_NOTE)
                .append(" AFTER INSERT ON ").append(Provider.NoteColumns.TABLE_NAME)
                .append(" WHEN NEW.").append(Provider.NoteColumns.FOLDER_ID).append(" IS NOT NULL BEGIN UPDATE ").append(Provider.FolderColumns.TABLE_NAME)
                .append(" SET ").append(Provider.FolderColumns.MODIFY_TIME)
                .append(" = NEW.").append(Provider.FolderColumns.CREATE_TIME)
                .append(", ").append(Provider.FolderColumns._COUNT).append(" = ").append(Provider.FolderColumns._COUNT).append(" + 1")
                .append(", ").append(Provider.FolderColumns.SYNC_STATE).append(" = ").append(SyncState.SYNC_UP.ordinal()).append(" WHERE ")
                .append(Provider.FolderColumns.SID).append(" = NEW.").append(Provider.NoteColumns.FOLDER_ID)
                .append("; END;");
        db.execSQL(builder.toString());
        
        //创建更新文件夹中笔记数量增加的触发器

        /*CREATE TRIGGER tri_note_count_add AFTER UPDATE ON notes
        WHEN NEW.folder_id IS NOT NULL AND OLD.delete_state != 0 AND NEW.delete_state = 0 BEGIN
        UPDATE folder SET modify_time = NEW.modify_time, _count = _count + 1,
                sync_state = 0 WHERE sid = NEW.folder_id;  END;*/
        builder = new StringBuilder();
        builder.append("CREATE TRIGGER ").append(Provider.NoteColumns.TRI_NOTE_COUNT_ADD)
                .append(" AFTER UPDATE ON ").append(Provider.NoteColumns.TABLE_NAME)
                .append(" WHEN NEW.").append(Provider.NoteColumns.FOLDER_ID).append(" IS NOT NULL AND OLD.")
                .append(Provider.NoteColumns.DELETE_STATE).append(" != ").append(DeleteState.DELETE_NONE.ordinal()).append(" AND NEW.")
                .append(Provider.NoteColumns.DELETE_STATE).append(" = ").append(DeleteState.DELETE_NONE.ordinal()).append(" BEGIN UPDATE ")
                .append(Provider.FolderColumns.TABLE_NAME)
                .append(" SET ").append(Provider.FolderColumns.MODIFY_TIME)
                .append(" = NEW.").append(Provider.FolderColumns.MODIFY_TIME)
                .append(", ").append(Provider.FolderColumns._COUNT).append(" = ")
                .append(Provider.FolderColumns._COUNT).append(" + 1")
                .append(", ").append(Provider.FolderColumns.SYNC_STATE).append(" = ").append(SyncState.SYNC_UP.ordinal()).append(" WHERE ")
                .append(Provider.FolderColumns.SID).append(" = NEW.").append(Provider.NoteColumns.FOLDER_ID)
                .append("; END;");
        db.execSQL(builder.toString());
        /*CREATE TRIGGER tri_note_count_minus AFTER UPDATE ON notes
        WHEN NEW.folder_id IS NOT NULL AND OLD.delete_state = 0 AND NEW.delete_state != 0 BEGIN
        UPDATE folder SET modify_time = NEW.modify_time, _count =  _count - 1,
                sync_state = 0 WHERE sid = NEW.folder_id;  END;*/

        builder = new StringBuilder();
        builder.append("CREATE TRIGGER ").append(Provider.NoteColumns.TRI_NOTE_COUNT_MINUS)
                .append(" AFTER UPDATE ON ").append(Provider.NoteColumns.TABLE_NAME)
                .append(" WHEN NEW.").append(Provider.NoteColumns.FOLDER_ID).append(" IS NOT NULL AND OLD.")
                .append(Provider.NoteColumns.DELETE_STATE).append(" = ").append(DeleteState.DELETE_NONE.ordinal()).append(" AND NEW.")
                .append(Provider.NoteColumns.DELETE_STATE).append(" != ").append(DeleteState.DELETE_NONE.ordinal()).append(" BEGIN UPDATE ")
                .append(Provider.FolderColumns.TABLE_NAME)
                .append(" SET ").append(Provider.FolderColumns.MODIFY_TIME)
                .append(" = NEW.").append(Provider.FolderColumns.MODIFY_TIME)
                .append(", ").append(Provider.FolderColumns._COUNT).append(" = ")
                .append(Provider.FolderColumns._COUNT).append(" - 1")
                .append(", ").append(Provider.FolderColumns.SYNC_STATE).append(" = ").append(SyncState.SYNC_UP.ordinal()).append(" WHERE ")
                .append(Provider.FolderColumns.SID).append(" = NEW.").append(Provider.NoteColumns.FOLDER_ID)
                .append("; END;");
        db.execSQL(builder.toString());
        
        //创建添加文件夹后设置排序的触发器
        /*CREATE TRIGGER tri_set_folder_sort AFTER INSERT ON folder FOR EACH ROW
                BEGIN
        UPDATE folder SET sort = NEW._id WHERE _id = NEW._id;
        END;*/
        builder = new StringBuilder();
        builder.append("CREATE TRIGGER ").append(Provider.FolderColumns.TRI_SET_FOLDER_SORT)
                .append(" AFTER INSERT ON ").append(Provider.FolderColumns.TABLE_NAME)
                .append(" BEGIN UPDATE ").append(Provider.FolderColumns.TABLE_NAME).append(" SET ")
                .append(Provider.FolderColumns.SORT).append(" = NEW.").append(Provider.FolderColumns._ID)
                .append(" WHERE ").append(Provider.FolderColumns._ID).append(" = NEW.")
                .append(Provider.FolderColumns._ID).append( ";END;");
        db.execSQL(builder.toString());
        
        //创建移动文件夹到回收站的触发器
        /*CREATE TRIGGER "tri_trash_folder" AFTER UPDATE ON "folder"
        WHEN (OLD.delete_state IS NULL OR OLD.delete_state != 1) AND NEW.delete_state = 1
        BEGIN
        UPDATE notes SET delete_state = 1, sync_state = 0, modify_time = NEW.modify_time WHERE folder_id = NEW.sid;
        END;*/
        builder = new StringBuilder();
        builder.append("CREATE TRIGGER ").append(Provider.FolderColumns.TRI_TRASH_FOLDER)
                .append(" AFTER UPDATE ON ").append(Provider.FolderColumns.TABLE_NAME)
                .append(" WHEN (OLD.").append(Provider.FolderColumns.DELETE_STATE).append(" IS NULL OR OLD.").append(Provider.FolderColumns.DELETE_STATE).append(" != ")
                .append(DeleteState.DELETE_TRASH.ordinal()).append(") AND NEW.")
                .append(Provider.FolderColumns.DELETE_STATE).append(" = ").append(DeleteState.DELETE_TRASH.ordinal())
                .append(" BEGIN UPDATE ").append(Provider.NoteColumns.TABLE_NAME).append(" SET ")
                .append(Provider.FolderColumns.DELETE_STATE).append(" = ").append(DeleteState.DELETE_TRASH.ordinal()).append(", ")
                .append(Provider.FolderColumns.SYNC_STATE).append(" = ").append(SyncState.SYNC_UP.ordinal()).append(", ")
                .append(Provider.FolderColumns.MODIFY_TIME).append(" = NEW.").append(Provider.FolderColumns.MODIFY_TIME)
                .append(" WHERE ").append(Provider.NoteColumns.FOLDER_ID).append(" = NEW.")
                .append(Provider.FolderColumns.SID).append( ";END;");
        db.execSQL(builder.toString());

        //创建将文件移出回收站的触发器
        /*CREATE TRIGGER "tri_untrash_folder" AFTER UPDATE ON "folder"
        WHEN (OLD.delete_state IS NOT NULL AND OLD.delete_state != 0) AND NEW.delete_state = 0
        BEGIN
        UPDATE notes SET delete_state = 0, sync_state = 0, modify_time = NEW.modify_time WHERE folder_id = NEW.sid;
        END;*/
        builder = new StringBuilder();
        builder.append("CREATE TRIGGER ").append(Provider.FolderColumns.TRI_UNTRASH_FOLDER)
                .append(" AFTER UPDATE ON ").append(Provider.FolderColumns.TABLE_NAME)
                .append(" WHEN (OLD.").append(Provider.FolderColumns.DELETE_STATE).append(" IS NOT NULL AND OLD.")
                .append(Provider.FolderColumns.DELETE_STATE).append(" != ").append(DeleteState.DELETE_NONE.ordinal()).append(") AND NEW.")
                .append(Provider.FolderColumns.DELETE_STATE).append(" = ").append(DeleteState.DELETE_NONE.ordinal())
                .append(" BEGIN UPDATE ").append(Provider.NoteColumns.TABLE_NAME).append(" SET ")
                .append(Provider.FolderColumns.DELETE_STATE).append(" = ").append(DeleteState.DELETE_NONE.ordinal()).append(", ")
                .append(Provider.FolderColumns.SYNC_STATE).append(" = ").append(SyncState.SYNC_UP.ordinal()).append(", ")
                .append(Provider.FolderColumns.MODIFY_TIME).append(" = NEW.").append(Provider.FolderColumns.MODIFY_TIME)
                .append(" WHERE ").append(Provider.NoteColumns.FOLDER_ID).append(" = NEW.")
                .append(Provider.FolderColumns.SID).append( ";END;");
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
        db.execSQL("DROP TRIGGER IF EXISTS " + Provider.NoteColumns.TRI_UPDATE_FOLDER);
        
        //删除更新笔记后更新文件夹的触发器
        db.execSQL("DROP TRIGGER IF EXISTS " + Provider.NoteColumns.TRI_INSERT_NOTE);
        
        //删除添加文件夹后设置排序的触发器
        db.execSQL("DROP TRIGGER IF EXISTS " + Provider.FolderColumns.TRI_SET_FOLDER_SORT);
        
        //删除笔记数量添加的触发器
        db.execSQL("DROP TRIGGER IF EXISTS " + Provider.NoteColumns.TRI_NOTE_COUNT_ADD);
        
        //删除笔记数量减少的触发器
        db.execSQL("DROP TRIGGER IF EXISTS " + Provider.NoteColumns.TRI_NOTE_COUNT_MINUS);

        db.execSQL("DROP TABLE IF EXISTS " + Provider.NoteColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.FolderColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.AttachmentColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.DetailedListColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.HandWriteColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.RemindersColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.UserColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Provider.WidgetColumns.TABLE_NAME);
        
        onCreate(db);
    }
}
