package com.gweather.app;

import java.util.List;

import com.gweather.utils.WeatherDataUtil;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class UpdateWidgetService extends Service {
	private static final String TAG = "Gweather.UpdateWidgetService";

	public static final String WOEID_GPS = "woeid_gps";
	public static final String WOEID_ALL = "woeid_all";

	private WeatherInfo mWidgetWeatherInfo;
	private List<WeatherInfo> mWeatherInfoList;
	private InternetWorker mInternetWorker;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand, intent = " + intent);

		if (intent == null) {
			return START_REDELIVER_INTENT;
		}

		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(getApplicationContext());
		RemoteViews views = new RemoteViews(this.getPackageName(),
				R.layout.widget_layout);
		WeatherApp.mModel = WeatherModel.getInstance(getApplicationContext());
		mInternetWorker = InternetWorker.getInstance(getApplicationContext());

		int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		if (intent.getDataString() != null) {
			widgetId = Integer.parseInt(intent.getDataString());
		}

		String intentAction = intent.getAction();
		Log.v(TAG, "widgetId = " + widgetId + ", intentAction = "
				+ intentAction);
		initWeather();

		if (AppWidgetManager.INVALID_APPWIDGET_ID == widgetId) {
			if (WeatherWidget.ACTION_INIT.equals(intentAction)) {
				Log.v(TAG, "ACTION_INIT ?");
				if (null != mWidgetWeatherInfo && mWidgetWeatherInfo.getForecasts().size() >= MainActivity.FORECAST_DAY) {
					updateWidgetUI();
				}
			} else if (WeatherWidget.ACTION_UPDATE.equals(intentAction)) {
				Log.v(TAG, "ACTION_UPDATE ?");
				if (null != mWidgetWeatherInfo && mWidgetWeatherInfo.getForecasts().size() >= MainActivity.FORECAST_DAY) {
					updateWidgetUI();
				} else if ("".equals(WeatherDataUtil.getInstance()
						.getDefaultCityWoeid(UpdateWidgetService.this))) {
					setDefaultInfo();
				}
			} else if (WeatherAction.ACTION_AUTO_REFRESH.equals(intentAction)) {
				Log.v(TAG, "ACTION_AUTO_REFRESH ?");
				mInternetWorker.updateWeather();
			} else if(WeatherAction.ACTION_WEATHER_REFRESH_CURRENT.equals(intentAction)) {
				
			} else if(WeatherAction.ACTION_WEATHER_REFRESH_ALL.equals(intentAction)) {
				
			} else if (WeatherAction.ACTION_QUERT_LOCATION.equals(intentAction)) {
				
			} else {
				Log.w(TAG, "Action NOT match, " + intentAction);
			}
		} else {
			Log.v(TAG, "VALID_APPWIDGET_ID");
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void initWeather() {
		Log.v(TAG, "initWeather");
		String defWoeid = WeatherDataUtil.getInstance().getDefaultCityWoeid(
				UpdateWidgetService.this);
		mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
		if (mWeatherInfoList.isEmpty()) {
			mWidgetWeatherInfo = null;
		} else {
			if(WeatherDataUtil.DEFAULT_WOEID_GPS.equals(defWoeid)) {
				for (WeatherInfo info:mWeatherInfoList) {
					if (info.isGps()) {
						Log.v(TAG, "gps");
						mWidgetWeatherInfo = info;
					}
				}
			} else {
				for (WeatherInfo info:mWeatherInfoList) {
					if (defWoeid.equals(info.getWoeid()) && !info.isGps()) {
						Log.v(TAG, "normal");
						mWidgetWeatherInfo = info;
					}
				}
			}
		}
	}

	private void updateWidgetUI() {
		Log.d(TAG, "updateWidgetUI");
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(getApplicationContext());
		RemoteViews views = new RemoteViews(this.getPackageName(),
				R.layout.widget_layout);

		String name = mWidgetWeatherInfo.getName();
		views.setTextViewText(R.id.widget_weathercity, name);
		views.setTextViewText(R.id.widget_weathertemperature,
				mWidgetWeatherInfo.getForecasts().get(0).getLow() + "/"
						+ mWidgetWeatherInfo.getForecasts().get(0).getHigh());
		views.setTextViewText(R.id.widget_weathercondition, mWidgetWeatherInfo
				.getCondition().getText());

		int code = Integer
				.parseInt(mWidgetWeatherInfo.getCondition().getCode());
		int resId;
		boolean isnight = WeatherDataUtil.getInstance().isNight();
		resId = WeatherDataUtil.getInstance().getWeatherImageResourceByCode(
				code, isnight, true);
		if (WeatherDataUtil.INVALID_WEAHTER_RESOURCE == resId) {
			resId = WeatherDataUtil.getInstance()
					.getWeatherImageResourceByText(
							mWidgetWeatherInfo.getCondition().getText(),
							isnight, true);
		}
		views.setImageViewResource(R.id.widget_img, resId);

		int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(getApplicationContext(),
						WeatherWidget.class));
		for (int i = 0; i < appWidgetIds.length; i++) {
			appWidgetManager.partiallyUpdateAppWidget(appWidgetIds[i], views);
		}
	}

	private void setDefaultInfo() {
		Log.d(TAG, "setUnknowInfo");
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(getApplicationContext());
		RemoteViews views = new RemoteViews(this.getPackageName(),
				R.layout.widget_layout);

		String defaultData = getResources().getString(
				R.string.weather_data_default);
		views.setTextViewText(R.id.widget_weathercity, defaultData);
		views.setTextViewText(R.id.widget_weathertemperature, defaultData);
		views.setTextViewText(R.id.widget_weathercondition, defaultData);

		int resId;
		boolean isnight = WeatherDataUtil.getInstance().isNight();
		resId = WeatherDataUtil.getInstance().getWeatherImageResourceByText(
				defaultData, isnight, true);
		views.setImageViewResource(R.id.widget_img, resId);

		int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(getApplicationContext(),
						WeatherWidget.class));
		for (int i = 0; i < appWidgetIds.length; i++) {
			appWidgetManager.partiallyUpdateAppWidget(appWidgetIds[i], views);
		}
	}
}
