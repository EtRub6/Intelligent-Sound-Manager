package com.ethanr.intelligentsoundmanager;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import android.widget.RemoteViews;


/**
 * Implementation of App Widget functionality.
 */
public class SinglePeriodManagerWidget extends AppWidgetProvider {
    private static final String TAG = "intelli_sound:" + SinglePeriodManagerWidget.class.getSimpleName();
    public final static String WIDGET_BACKGROUND_ACTION = "WIDGET_BACKGROUND_ACTION";
    public final static String WIDGET_ENABLE = "WIDGET_ENABLE";
    public final static String WIDGET_TIME = "WIDGET_TIME";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive() action " + action);

        if (action != null) {
            if (action.equals(WIDGET_BACKGROUND_ACTION)) {
                Log.d(TAG, "onReceive() WIDGET_BACKGROUND_ACTION");
                widget_background_action(context, intent.getBooleanExtra(WIDGET_ENABLE, false),
                        intent.getStringExtra(WIDGET_TIME));
            }
        }

        super.onReceive(context, intent);
    }

    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Log.d(TAG, "updateAppWidget() " + appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.single_period_manager_widget);
        views.setImageViewResource(R.id.widgetImageButton, R.drawable.light_mode_48dp);

        Intent intent = new Intent(context, PopUpActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetImageButton, pendIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        Log.d(TAG, "updateAppWidget()");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate()");
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

    public void widget_background_action(Context context, boolean enable, String time) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, SinglePeriodManagerWidget.class));
            for (int appWidgetId : appWidgetIds) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.single_period_manager_widget);
                views.setImageViewResource(R.id.widgetImageButton, enable ? R.drawable.nights_stay_48dp : R.drawable.light_mode_48dp);
                views.setTextViewText(R.id.dont_disturb, time);

                Intent intent = new Intent(context, PopUpActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.widgetImageButton, pendIntent);

                appWidgetManager.updateAppWidget(appWidgetId, views);

                Log.d(TAG, "widget_background_action() appWidgetId = " + appWidgetId + " enable = " + enable);
            }
        }
    }

}
