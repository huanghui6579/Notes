package com.yunxinlink.notes.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.ui.MainActivity;
import com.yunxinlink.notes.ui.NoteEditActivity;
import com.yunxinlink.notes.util.NoteUtil;

/**
 * Implementation of App Widget functionality.
 * 笔记列表的widget
 */
public class NoteListAppWidget extends AppWidgetProvider {

    public static final String ACTION_NOTIFY_CHANGE = "com.yunxinlink.notes.appwidget.action.APPWIDGET_NOTIFY_CHANGE";
    public static final String ACTION_ITEM_CLICK = "com.yunxinlink.notes.appwidget.action.APPWIDGET_ITEM_CLICK";

    private static final int REQ_MAIN = 1;
    private static final int REQ_ADD = 2;
    private static final int REQ_CLICK = 3;

    private static final String TAG = "NoteListAppWidget";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.note_list_app_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);

        Intent mainIntent = new Intent(MainActivity.ACTION_MAIN);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, REQ_MAIN, mainIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.iv_logo, mainPendingIntent);

        String defaultFolderSid = ((NoteApplication) context.getApplicationContext()).getDefaultFolderSid();
        
        Intent addIntent = new Intent(NoteEditActivity.ACTION_EDIT);
        addIntent.putExtra(NoteEditActivity.ARG_FOLDER_ID, defaultFolderSid);
        addIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent addPendingIntent = PendingIntent.getActivity(context, REQ_ADD, addIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.btn_add, addPendingIntent);

        Intent intent = new Intent(context, NoteListRemoteViewsService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        views.setRemoteAdapter(R.id.lv_data, intent);

        Intent clickIntent = new Intent(ACTION_ITEM_CLICK);
        PendingIntent pendingIntentTemplate = PendingIntent.getBroadcast(context, REQ_CLICK, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.lv_data, pendingIntentTemplate);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
//        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_data);

        NoteUtil.saveListAppWidgetId(context, appWidgetId);

        KLog.d(TAG, "updateAppWidget appWidgetId:" + appWidgetId);
    }

    /**
     * 更新列表
     * @param context
     * @param appWidgetId
     */
    void notifyDataSetChanged(Context context, int appWidgetId) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            KLog.d("note list widget notify data set changed appWidgetId:" + appWidgetId);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_data);
        }
    }

    /**
     * 显示笔记
     * @param context
     */
    void showNote(Context context, Intent intent) {
        Intent detailIntent = new Intent(NoteEditActivity.ACTION_EDIT);
        detailIntent.putExtra(NoteEditActivity.ARG_NOTE_ID, intent.getIntExtra(NoteEditActivity.ARG_NOTE_ID, -1));
        detailIntent.putExtra(NoteEditActivity.ARG_NOTE_SID, intent.getStringExtra(NoteEditActivity.ARG_NOTE_SID));
        detailIntent.putExtra(NoteEditActivity.ARG_FOLDER_ID, intent.getStringExtra(NoteEditActivity.ARG_FOLDER_ID));
        detailIntent.putExtra(NoteEditActivity.ARG_IS_NOTE_TEXT, intent.getBooleanExtra(NoteEditActivity.ARG_IS_NOTE_TEXT, true));
        detailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(detailIntent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        KLog.d(TAG, "onUpdate");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        NoteUtil.removeListAppWidgetId(context);
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (ACTION_NOTIFY_CHANGE.equals(action)) {
            notifyDataSetChanged(context, appWidgetId);
        } else if (ACTION_ITEM_CLICK.equals(action)) {
            showNote(context, intent);
        } else {
            super.onReceive(context, intent);
        }
    }
}

