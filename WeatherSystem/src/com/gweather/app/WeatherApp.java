package com.gweather.app;

import android.app.Application;
import android.util.Log;

public class WeatherApp extends Application {
	private static final String TAG = "Gweather.WeatherApp";
	
	public static WeatherModel mModel = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		
		mModel = WeatherModel.getInstance(getApplicationContext());
		mModel.init();
	}
}
