package com.yunxinlink.notes.appwidget;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.db.DBHelper;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.db.observer.Observer;
import com.yunxinlink.notes.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * widget 数据库的持久层
 * @author huanghui1
 * @update 2016/9/15 14:40
 * @version: 0.0.1
 */
public class WidgetManager extends Observable<Observer> {
    private static final String TAG = "WidgetManager";
    
    private static WidgetManager mInstance;
    
    private DBHelper mDBHelper;
    
    private WidgetManager() {
        mDBHelper = new DBHelper(NoteApplication.getInstance());
    }
    
    public static WidgetManager getInstance() {
        if (mInstance == null) {
            synchronized (WidgetManager.class) {
                if (mInstance == null) {
                    mInstance = new WidgetManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 初始化小部件的数据
     * @param list
     */
    public void initWidgets(List<WidgetItem> list) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (WidgetItem item : list) {
                ContentValues values = new ContentValues();
                values.put(Provider.WidgetColumns.TITLE, item.getName());
                values.put(Provider.WidgetColumns.TYPE, item.getType());
                values.put(Provider.WidgetColumns.SORT, item.getSort());
                values.put(Provider.WidgetColumns.SORT2, item.getSort2());
                long rowId = db.insert(Provider.WidgetColumns.TABLE_NAME, null, values);
                if (rowId > 0) {
                    item.setId((int) rowId);
                }
            }
            KLog.d(TAG, "initWidgets success");
            db.setTransactionSuccessful();
        } catch (Exception e) {
            KLog.e(TAG, "initWidgets error:" + e.getMessage());
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 更新小部件各项的排序
     * @param list
     * @param sortIndex 要排序的字段，0：表示sort, 1:表示sort2
     */
    public void updateSort(List<WidgetItem> list, int sortIndex) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (WidgetItem item : list) {
                ContentValues values = new ContentValues();
                if (sortIndex == 0) {
                    values.put(Provider.WidgetColumns.SORT, item.getSort());
                } else {
                    values.put(Provider.WidgetColumns.SORT2, item.getSort2());
                }
                db.update(Provider.WidgetColumns.TABLE_NAME, values, Provider.WidgetColumns._ID + " = ?", new String[] {String.valueOf(item.getResId())});
            }
            KLog.d(TAG, "updateSort success");
            db.setTransactionSuccessful();
        } catch (Exception e) {
            KLog.e(TAG, "initWidgets error:" + e.getMessage());
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 获取所有的widget 项
     * @return
     */
    public List<WidgetItem> getAllWidgetItems() {
        
        List<WidgetItem> list = null;
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.query(Provider.WidgetColumns.TABLE_NAME, null, null, null, null, null, Provider.WidgetColumns.DEFAULT_SORT);
        if (cursor != null) {
            list = new ArrayList<>();
            int i = 0;
            while (cursor.moveToNext()) {
                i++;
                WidgetItem item = new WidgetItem();
                item.setId(cursor.getInt(cursor.getColumnIndex(Provider.WidgetColumns._ID)));
                item.setName(cursor.getString(cursor.getColumnIndex(Provider.WidgetColumns.TITLE)));
                item.setType(cursor.getInt(cursor.getColumnIndex(Provider.WidgetColumns.TYPE)));
                item.setSort(cursor.getInt(cursor.getColumnIndex(Provider.WidgetColumns.SORT)));
                item.setSort2(cursor.getInt(cursor.getColumnIndex(Provider.WidgetColumns.SORT2)));
                if (i <= Constants.MAX_WIDGET_ITEM_SIZE) {
                    item.setChecked(true);
                }
                list.add(item);
            }
            KLog.d(TAG, "getAllWidgetItems success");
            WidgetItemCache.getInstance().setWidgetItems(list);
            cursor.close();
        }
        return list;
    }
}
