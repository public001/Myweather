package com.gweather.app;

import com.gweather.utils.Utils;
import com.gweather.utils.WeatherDataUtil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
	private static final String TAG = "Gweather.BootCompletedReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (null == intent) {
			return;
		}
		String action = intent.getAction();
		Log.d(TAG, "BootCompletedReceiver," + action);
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			SharedPreferences sp = context.getSharedPreferences(
					MainActivity.SETTINGS_SP, Context.MODE_PRIVATE);
			boolean isAutoRefreshEnable = sp.getBoolean(
					MainActivity.SETTINGS_AUTO_REFRESH_ENABLE,
					context.getResources().getBoolean(
							R.bool.config_auto_refresh_enable));
			if (isAutoRefreshEnable) {
				int time = sp.getInt(
						MainActivity.SETTINGS_AUTO_REFRESH,
						context.getResources().getInteger(
								R.integer.config_auto_refresh));

				setAutoRefreshAlarm(context, time);
			}
		} else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)){
			SharedPreferences sp = context.getSharedPreferences(MainActivity.SETTINGS_SP,
					Context.MODE_PRIVATE);
			boolean isAutoRefreshEnable = sp.getBoolean(
					MainActivity.SETTINGS_AUTO_REFRESH_ENABLE,
					context.getResources().getBoolean(
							R.bool.config_auto_refresh_enable));
			if (isAutoRefreshEnable) {
				int time = sp.getInt(
						MainActivity.SETTINGS_AUTO_REFRESH,
						context.getResources().getInteger(
								R.integer.config_auto_refresh));
				long refreshtimeOld = WeatherDataUtil.getInstance().getRefreshTime(
						context);
				
				boolean needRefreshNow = false;
				switch (time) {
				case MainActivity.SETTINGS_AUTO_REFRESH_6H:
					if (refreshtimeOld + MainActivity.TIME_6H
							- System.currentTimeMillis() <= 0) {
						needRefreshNow = true;
					}
					
					break;
				case MainActivity.SETTINGS_AUTO_REFRESH_12H:
					if (refreshtimeOld + MainActivity.TIME_12H
							- System.currentTimeMillis() <= 0) {
						needRefreshNow = true;
					}
					break;
				case MainActivity.SETTINGS_AUTO_REFRESH_24H:
					if (refreshtimeOld + MainActivity.TIME_24H
							- System.currentTimeMillis() <= 0) {
						needRefreshNow = true;
					}
					break;

				default:
					Log.d(TAG, "setAutoRefreshAlarm, " + time);
				}
				if (needRefreshNow) {
					if(sp.getBoolean(MainActivity.SETTINGS_WIFI_ONLY, context.getResources().getBoolean(R.bool.config_wifi_only_enable))) {
						if (Utils.isNetworkTypeWifi(context)) {
							setAutoRefreshAlarm(context, time);
						}
					} else {
						if (Utils.isNetworkAvailable(context)) {
							setAutoRefreshAlarm(context, time);
						}
					}
				}
			}
			
			
		}
	}

	private void setAutoRefreshAlarm(Context context, int time) {
		Log.d(TAG, "setAutoRefreshAlarm, time:" + time);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(WeatherAction.ACTION_AUTO_REFRESH);
		PendingIntent operation = PendingIntent.getBroadcast(context, 0,
				intent, 0);

		long deltaTime = WeatherDataUtil.getRefreshDelta(context, time);
		Log.d(TAG, "setAutoRefreshAlarm, deltaTime:" + deltaTime);
		alarmManager.cancel(operation);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + deltaTime, operation);
		
	}
}
