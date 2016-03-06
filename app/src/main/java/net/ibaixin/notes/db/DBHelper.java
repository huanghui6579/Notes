package net.ibaixin.notes.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 创建数据库
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/5 16:11
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "notes.db";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建笔记表
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(Provider.NoteColumns.TABLE_NAME).append(" (")
                .append(Provider.NoteColumns._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(Provider.NoteColumns.SID).append(" TEXT UNIQUE NOT NULL, ")
                .append(Provider.NoteColumns.USER_ID).append(" INTEGER, ")
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
                .append(Provider.NoteColumns.OLD_CONTENT).append(" TEXT);");
        db.execSQL(builder.toString());

        builder = new StringBuilder();

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
