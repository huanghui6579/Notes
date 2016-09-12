package com.yunxinlink.notes.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;

/**
 * Implementation of App Widget functionality.
 * 快速创建笔记的widget
 */
public class ShortCreateAppWidget extends AppWidgetProvider {
    private static final String TAG = "ShortCreateAppWidget";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.short_create_app_widget);
        views.setTextViewText(R.id.appwidget_text, widgetText);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        KLog.d(TAG, "ShortCreateAppWidget onUpdate");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
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
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
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
}

