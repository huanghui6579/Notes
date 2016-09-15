package com.yunxinlink.notes.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.ui.MainActivity;
import com.yunxinlink.notes.ui.NoteEditActivity;

/**
 * Implementation of App Widget functionality.
 * 笔记列表的widget
 */
public class NoteListAppWidget extends AppWidgetProvider {

    private static final int REQ_MAIN = 1;
    private static final int REQ_ADD = 2;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.note_list_app_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, REQ_MAIN, mainIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.tv_title, mainPendingIntent);

        String defaultFolderSid = ((NoteApplication) context.getApplicationContext()).getDefaultFolderSid();
        
        Intent addIntent = new Intent(context, NoteEditActivity.class);
        addIntent.putExtra(NoteEditActivity.ARG_FOLDER_ID, defaultFolderSid);
        addIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent addPendingIntent = PendingIntent.getActivity(context, REQ_ADD, addIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.btn_add, addPendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
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
}

