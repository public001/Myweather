package com.gweather.app;

import java.util.Calendar;

import com.gweather.utils.Utils;
import com.gweather.app.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.RemoteViews;

public class WeatherWidget extends AppWidgetProvider {
	private static final String TAG = "Gweather.WeatherWidget";
	
	public static final String ACTION_INIT = "action_init";
	public static final String ACTION_UPDATE = "action_update";

	private ComponentName mComponentName;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		int[] newAppWidgetIds = appWidgetIds;
		if (appWidgetIds == null) {
			Log.v(TAG, "appWidgetIds = null");
			newAppWidgetIds = appWidgetManager
					.getAppWidgetIds(new ComponentName(context,
							WeatherWidget.class));
		}

		Log.v(TAG, "appWidgetIds_length = " + newAppWidgetIds.length);

		for (int appWidgetId : appWidgetIds) {
			Log.v(TAG, "appWidgetId_sub = " + appWidgetId);
			updateClock(context, appWidgetManager, appWidgetId, 1);
		}
		
		startUpdateService(context, ACTION_INIT,
				AppWidgetManager.INVALID_APPWIDGET_ID);

	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		context.stopService(new Intent(context, UpdateWidgetService.class));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);

		if (null == intent) {
			return;
		}
		String action = intent.getAction();
		Log.d(TAG, "onReceive action = " + action);

		if (WeatherAction.ACTION_AUTO_REFRESH.equals(action)) {
			startUpdateService(context, action, AppWidgetManager.INVALID_APPWIDGET_ID);
		} else {
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);

			if (appWidgetManager != null) {
				int[] appWidgetIds = appWidgetManager
						.getAppWidgetIds(getComponentName(context));
				for (int appWidgetId : appWidgetIds) {
					updateClock(context, appWidgetManager, appWidgetId, 1);
				}
			}
		}
	}

	private void updateClock(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId, float ratio) {
		Log.v(TAG, "updateClock-----");

		RemoteViews widget = new RemoteViews(context.getPackageName(),
				R.layout.widget_layout);

		// Launch clock when clicking on the time in the widget only if not a
		// lock screen widget
		Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
		if (newOptions != null
				&& newOptions.getInt(
						AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
			Log.v("guocl", "updateClock-----setOnClick ");
			Intent localIntent4 = new Intent();
			localIntent4
					.setComponent(new ComponentName("com.android.deskclock",
							"com.android.deskclock.DeskClock"));
			widget.setOnClickPendingIntent(R.id.widget_timeblock,
					PendingIntent.getActivity(context, 0, localIntent4, 0));
			Intent localIntent5 = new Intent();
			localIntent5.setComponent(new ComponentName("com.android.calendar",
					"com.android.calendar.AllInOneActivity"));
			widget.setOnClickPendingIntent(R.id.weather_dateinfo,
					PendingIntent.getActivity(context, 0, localIntent5, 0));

			PendingIntent localIntent6 = getSettingPendingIntent(context);
			// widget.setOnClickPendingIntent(R.id.widget_weatherdata,
			// localIntent6);

			// Click on widget_img Change City Weather
			widget.setOnClickPendingIntent(R.id.widget_img,
					getWeatherActivityIntent(context));
		}

		refreshDate(context, widget);

		// /M: change Android default design and show the AM/PM string to
		// widget. @{
		int amPmFontSize = (int) context.getResources().getDimension(
				R.dimen.main_ampm_font_size);
		// /@}
		Utils.setTimeFormat(widget, amPmFontSize/* 0 no am/pm */,
				R.id.the_clock);
		Utils.setClockSize(context, widget, ratio);

		appWidgetManager.updateAppWidget(appWidgetId, widget);
	}

	private PendingIntent getScrollPendingIntent(Context context,
			final int appWidgetId) {
		Intent intent = new Intent(context, UpdateWidgetService.class)
				.setAction("com.weather.action.SCROLL").setData(
						Uri.parse(String.valueOf(appWidgetId)));
		intent.putExtra("direction", "direction_next");
		return (PendingIntent.getService(context, 0, intent, 0));
	}

	private PendingIntent getWeatherActivityIntent(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		return PendingIntent.getActivity(context, 0, intent, 0);
	}

	private PendingIntent getSettingPendingIntent(Context context) {
		Intent intent = new Intent("com.weather.action.SETTING");
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		return (PendingIntent.getActivity(context, 0, intent, 0));
	}

	private void refreshDate(Context context, RemoteViews widget) {
		Log.v(TAG, "refreshDate--- ");
		java.text.DateFormat shortDateFormat = DateFormat
				.getDateFormat(context);
		final Calendar now = Calendar.getInstance();
		widget.setTextViewText(R.id.date, shortDateFormat.format(now.getTime()));
	}

	private void startUpdateService(Context context, String action, int widgetId) {
		Intent intent = new Intent(context, UpdateWidgetService.class);
		intent.setAction(action);
		intent.setData(Uri.parse(String.valueOf(widgetId)));
		context.startService(intent);
	}

	/**
	 * Create the component name for this class
	 * 
	 * @param context
	 *            The Context in which the widgets for this component are
	 *            created
	 * @return the ComponentName unique to DigitalAppWidgetProvider
	 */
	private ComponentName getComponentName(Context context) {
		if (mComponentName == null) {
			mComponentName = new ComponentName(context, getClass());
			System.out.println(getClass().toString());
		}
		return mComponentName;
	}
}
