package com.yunxinlink.notes.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.ui.MainActivity;
import com.yunxinlink.notes.ui.NoteEditActivity;
import com.yunxinlink.notes.ui.SearchActivity;
import com.yunxinlink.notes.util.NoteUtil;

/**
 * Implementation of App Widget functionality.
 * 快速创建笔记的widget
 */
public class ShortCreateAppWidget extends AppWidgetProvider {
    private static final String TAG = "ShortCreateAppWidget";

    private static final int REQ_MAIN = 1000;
    private static final int REQ_SETTINGS = 1001;
    
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        KLog.d(TAG, "updateAppWidget invoke appWidgetId:" + appWidgetId + " package name:" + context.getPackageName());
//        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.short_create_app_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);
        
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, appWidgetId + REQ_MAIN, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.iv_logo, mainPendingIntent);
        
        Intent settingsIntent = new Intent(context, ShortCreateAppWidgetConfigure.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle extra = new Bundle();
        extra.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        settingsIntent.putExtras(extra);
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(context, appWidgetId + REQ_SETTINGS, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.btn_settings, settingsPendingIntent);
        
        String defaultFolderSid = ((NoteApplication) context.getApplicationContext()).getDefaultFolderSid();

        KLog.d(TAG, "default folder sid:" + defaultFolderSid);

        WidgetItem[] widgetItems = ShortCreateAppWidgetConfigure.getSelectedItems();
        if (widgetItems != null && widgetItems.length > 0) {
            KLog.d(TAG, "updateAppWidget widgetItems is not null");
            Folder defaultFolder = FolderCache.getInstance().getCacheFolder(defaultFolderSid);
            if (defaultFolder != null) {
                views.setTextViewText(R.id.tv_header_name, defaultFolder.getName());
            }
            
            //Get the ID for R.layout.widget_blue
            int length = widgetItems.length;
            for (int i = 0; i < length; i++) {
                WidgetItem item = widgetItems[i];
                int resId = context.getResources().getIdentifier("item_" + (i + 1), "id", context.getPackageName());
                if (resId != 0) {
                    Intent intent = null;
                    if (item.getType() == WidgetAction.NOTE_SEARCH.ordinal()) { //搜索
                        intent = new Intent(context, SearchActivity.class);
                    } else {
                        intent = new Intent(context, NoteEditActivity.class);
                    }
                    intent.putExtra(NoteEditActivity.ARG_NOTE_ADD_TYPE, item.getType());
                    intent.putExtra(NoteEditActivity.ARG_FOLDER_ID, defaultFolderSid);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId + i + 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    views.setImageViewResource(resId, item.getResId());
                    views.setOnClickPendingIntent(resId, pendingIntent);
                }
            }
        } else {
            KLog.d(TAG, "updateAppWidget widgetItems is null");
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        KLog.d(TAG, "ShortCreateAppWidget onUpdate");
        new WidgetAsyncTask(context, appWidgetManager, appWidgetIds).execute();
    }

    @Override
    public void onEnabled(Context context) {
        KLog.d(TAG, "ShortCreateAppWidget onEnabled");
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        KLog.d(TAG, "ShortCreateAppWidget onDisabled");
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        KLog.d(TAG, "ShortCreateAppWidget onReceive");
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        KLog.d(TAG, "ShortCreateAppWidget onAppWidgetOptionsChanged");
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        KLog.d(TAG, "ShortCreateAppWidget onDeleted");
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        KLog.d(TAG, "ShortCreateAppWidget onRestored");
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }

    /**
     * 后台更新widget的任务
     */
    class WidgetAsyncTask extends AsyncTask<Void, Void, WidgetItem[]> {
        private Context context;
        private AppWidgetManager appWidgetManager;
        private int[] appWidgetIds;

        public WidgetAsyncTask(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            this.context = context;
            this.appWidgetManager = appWidgetManager;
            this.appWidgetIds = appWidgetIds;
        }

        @Override
        protected WidgetItem[] doInBackground(Void... params) {
            int[] shortWidgetIds = NoteUtil.getShortCreateAppWidgetId(context);
            if (shortWidgetIds == null || shortWidgetIds.length == 0) {
                KLog.d(TAG, "WidgetAsyncTask do in background but shortWidgetIds is null or length is 0");
                return null;
            }
            if (appWidgetIds == null || appWidgetIds.length == 0) {
                KLog.d(TAG, "WidgetAsyncTask do in background but appWidgetIds is null or length is 0");
                return null;
            }
            KLog.d(TAG, "WidgetAsyncTask do in background shortWidgetIds size is:" + shortWidgetIds.length);
            int shortWidgetId = shortWidgetIds[0];
            boolean hasId = false;
            for (int widgetId : appWidgetIds) {
                if (widgetId == shortWidgetId) {
                    hasId = true;
                    break;
                }
            }
            hasId = true;
            if (hasId) {
                KLog.d(TAG, "WidgetAsyncTask load widget items do in background");
                return ShortCreateAppWidgetConfigure.getSelectedItems();
            } else {
                KLog.d(TAG, "WidgetAsyncTask load widget items do in background but not is note short create widget id");
                return null;
            }
        }

        @Override
        protected void onPostExecute(WidgetItem[] widgetItems) {
            if (widgetItems == null || widgetItems.length == 0) {
                KLog.d(TAG, "WidgetAsyncTask result widget items is null or length is 0");
            } else {
                if (appWidgetIds == null || appWidgetIds.length == 0) {
                    KLog.d(TAG, "WidgetAsyncTask result is ok but appwidget ids is null or length is 0");
                    return;
                }
                KLog.d(TAG, "WidgetAsyncTask result is ok and will update widget views");
                for (int appWidgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                }
            }
        }
    }

}

