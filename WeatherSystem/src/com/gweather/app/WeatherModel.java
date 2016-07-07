package com.gweather.app;

import java.util.ArrayList;
import java.util.List;

import com.gweather.utils.WeatherDataUtil;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class WeatherModel {
	private static final String TAG = "Gweather.Model";
	private static final String URI_GWEATHER = "content://com.gweather.app.weather/gweather";
	private static final String URI_GCITY = "content://com.gweather.app.weather/gcity";
	
	private static final int GPS_CITY_COUNT_MAX = 32;
	
	private static WeatherModel INSTANCE;
	
	private Context mApp;
	private boolean inited = false;
	private List<WeatherInfo> mWeatherInfoList = new ArrayList<WeatherInfo>();
	private List<CityInfo> mGpsCityInfoList = new ArrayList<CityInfo>();
	
	
	public static WeatherModel  getInstance(Context app) {
		if(null == INSTANCE) {
			INSTANCE = new WeatherModel(app);
		}
		
		return INSTANCE;
	}
	
	private WeatherModel(Context app) {
		mApp = app;
	}
	
	public boolean isInited() {
		return inited;
	}
	
	public void init() {
		Log.d(TAG, "init");
		loadWeatherInfos();
		loadGpsCityInfos();
		inited = true;
	}
	
	public List<WeatherInfo> getWeatherInfos() {
		if(!isInited() || mWeatherInfoList.isEmpty()) {
			Log.w(TAG, "getWeatherInfos, load again");
			loadWeatherInfos();
		}
		return mWeatherInfoList;
	}
	
	private void loadWeatherInfos() {
		Log.d(TAG, "loadWeatherInfos");
		WeatherInfo.Forecast forecast = null;
		mWeatherInfoList.clear();
		
		ContentResolver mContentResolver = mApp.getContentResolver();
		Uri uri = Uri.parse(URI_GWEATHER);
		Cursor cursor = mContentResolver.query(uri, null, "gIndex=?",
				new String[] { Integer
				.toString(WeatherProvider.CONDITION_INDEX) }, null);
		
		
		WeatherInfo info;
		String woeid = "";
		if (cursor != null) {
			while (cursor.moveToNext()) {
				int cursorIndex = cursor.getColumnIndex(WeatherProvider.WOEID);
				woeid = cursor.getString(cursorIndex);
				info = new WeatherInfo();
				info.setWoeid(woeid);
				cursorIndex = cursor.getColumnIndex(WeatherProvider.GPS);
				info.setGps(WeatherProvider.FLAG_GPS == cursor
						.getInt(cursorIndex));
				mWeatherInfoList.add(info);
			}
			cursor.close();
		}
		
		if (mWeatherInfoList.size() == 0) {
			// No infos
			Log.w(TAG, "loadWeatherInfos, no Infos");
			return;
		}
		
		
		for (WeatherInfo mInfo : mWeatherInfoList) {
			if (mInfo.isGps()) {
				cursor = mContentResolver
						.query(uri, null, WeatherProvider.GPS + "=?",
								new String[] { String
										.valueOf(WeatherProvider.FLAG_GPS) },
								WeatherProvider.INDEX);
			} else {
				cursor = mContentResolver.query(
						uri,
						null,
						"woeid=? AND " + WeatherProvider.GPS + "!=?",
						new String[] { mInfo.getWoeid(),
								String.valueOf(WeatherProvider.FLAG_GPS) },
						WeatherProvider.INDEX);
			}
			if (cursor != null) {
				while (cursor.moveToNext()) {
					int cursorIndex = cursor
							.getColumnIndex(WeatherProvider.INDEX);
					int index = cursor.getInt(cursorIndex);
					Log.d(TAG, "loadWeatherInfos, index=" + index);

					if (WeatherProvider.CONDITION_INDEX == index) {// CONDITION
						mInfo.getCondition().setIndex(index);

						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.NAME);
						mInfo.setName(cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.CODE);
						mInfo.getCondition().setCode(
								cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.DATE);
						mInfo.getCondition().setDate(
								cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.TEMP);
						mInfo.getCondition().setTemp(
								cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.TEXT);
						mInfo.getCondition().setText(
								cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.GPS);
						mInfo.setGps(WeatherProvider.FLAG_GPS == cursor
								.getInt(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.UPDATE_TIME);
						mInfo.setUpdateTime(cursor.getLong(cursorIndex));
					} else {// forecast
						forecast = mInfo.new Forecast();

						forecast.setIndex(index);
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.CODE);
						forecast.setCode(cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.DATE);
						forecast.setDate(cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.TEXT);
						forecast.setText(cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.DAY);
						forecast.setDay(cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.HIGH);
						forecast.setHigh(cursor.getString(cursorIndex));
						cursorIndex = cursor
								.getColumnIndex(WeatherProvider.LOW);
						forecast.setLow(cursor.getString(cursorIndex));

						mInfo.getForecasts().add(forecast);
					}
				}
				cursor.close();
			}
		}
	}
	
	private void loadGpsCityInfos() {
		Log.d(TAG, "loadGpsCityInfos");
		mGpsCityInfoList.clear();
		
		ContentResolver mContentResolver = mApp.getContentResolver();
		Uri uri = Uri.parse(URI_GCITY);
		
		CityInfo info;
		Cursor cursor = mContentResolver.query(uri, null, null, null, null);
		if (cursor != null) {
			Log.d(TAG, "count:" + cursor.getCount());
			while(cursor.moveToNext()) {
				info = new CityInfo();
				int cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_NAME);
				info.setName(cursor.getString(cursorIndex));
				cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_WOEID);
				info.setWoeid(cursor.getString(cursorIndex));
				cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_LAT);
				info.getLocationInfo().setLat(cursor.getString(cursorIndex));
				cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_LON);
				info.getLocationInfo().setLon(cursor.getString(cursorIndex));
				cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_SWLAT);
				info.getLocationInfo().setSouthWestLat(cursor.getString(cursorIndex));
				cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_SWLON);
				info.getLocationInfo().setSouthWestLon(cursor.getString(cursorIndex));
				cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_NELAT);
				info.getLocationInfo().setNorthEastLat(cursor.getString(cursorIndex));
				cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_NELON);
				info.getLocationInfo().setNorthEastLon(cursor.getString(cursorIndex));
				Log.d(TAG, "add | - - |");
				mGpsCityInfoList.add(info);
			}
			cursor.close();
		}
	}
	
	
	public void saveWeatherInfoToDB(WeatherInfo mWeatherInfo) {
		Log.d(TAG, "saveWeatherInfoToDB");
		boolean hasCity = false;
		ContentResolver mContentResolver = mApp.getContentResolver();
		Uri uri = Uri.parse(URI_GWEATHER);
		
		Cursor cursor;
		
		if (mWeatherInfo.isGps()) {
			Log.d(TAG, "GPS");
			cursor = mContentResolver.query(uri, null, WeatherProvider.GPS
					+ "=?",
					new String[] { String.valueOf(WeatherProvider.FLAG_GPS) },
					null);
			if (cursor != null) {
				int count = cursor.getCount();
				if (count > 0) {
					hasCity = true;
				}

				cursor.close();
			}
			Log.d(TAG, "hasCity = " + hasCity);
			ContentValues values;
			
			values = new ContentValues();
			values.put(WeatherProvider.INDEX,
					WeatherProvider.CONDITION_INDEX);
			values.put(WeatherProvider.WOEID, mWeatherInfo.getWoeid());
			values.put(WeatherProvider.NAME, mWeatherInfo.getName());
			values.put(WeatherProvider.CODE, mWeatherInfo.getCondition().getCode());
			values.put(WeatherProvider.DATE, mWeatherInfo.getCondition().getDate());
			values.put(WeatherProvider.TEMP, mWeatherInfo.getCondition().getTemp());
			values.put(WeatherProvider.TEXT, mWeatherInfo.getCondition().getText());
			values.put(WeatherProvider.UPDATE_TIME, mWeatherInfo.getUpdateTime());
			values.put(WeatherProvider.GPS, WeatherProvider.FLAG_GPS);
			if (hasCity) {
				mContentResolver
						.update(uri,
								values,
								WeatherProvider.INDEX + " = ? AND "
										+ WeatherProvider.GPS + " = ?",
								new String[] {
										Integer.toString(WeatherProvider.CONDITION_INDEX),
										String.valueOf(WeatherProvider.FLAG_GPS) });
			} else {
				mContentResolver.insert(uri, values);
			}
			
			for (int i = 0; i < MainActivity.FORECAST_DAY; i++) {
				values = new ContentValues();
				values.put(WeatherProvider.INDEX, i);
				values.put(WeatherProvider.WOEID, mWeatherInfo.getWoeid());
				values.put(WeatherProvider.CODE, mWeatherInfo.getForecasts()
						.get(i).getCode());
				values.put(WeatherProvider.DATE, mWeatherInfo.getForecasts()
						.get(i).getDate());
				values.put(WeatherProvider.DAY, mWeatherInfo.getForecasts().get(i)
						.getDay());
				values.put(WeatherProvider.HIGH, mWeatherInfo.getForecasts()
						.get(i).getHigh());
				values.put(WeatherProvider.LOW, mWeatherInfo.getForecasts().get(i)
						.getLow());
				values.put(WeatherProvider.TEXT, mWeatherInfo.getForecasts()
						.get(i).getText());
				values.put(WeatherProvider.GPS, WeatherProvider.FLAG_GPS);
				if (hasCity) {
					mContentResolver.update(
							uri,
							values,
							WeatherProvider.INDEX + " = ? AND "
									+ WeatherProvider.GPS + " = ?",
							new String[] { Integer.toString(i),
									String.valueOf(WeatherProvider.FLAG_GPS) });
				} else {
					mContentResolver.insert(uri, values);
				}
				
			}
		} else {
			Log.d(TAG, "NOT GPS");
			cursor = mContentResolver.query(
					uri,
					null,
					"woeid=? AND " + WeatherProvider.GPS + "!=?",
					new String[] { mWeatherInfo.getWoeid(),
							String.valueOf(WeatherProvider.FLAG_GPS) }, null);
			if (cursor != null) {
				int count = cursor.getCount();
				if (count > 0) {
					hasCity = true;
				}
				cursor.close();
			}

			ContentValues values;
			Log.d(TAG, "hasCity = " + hasCity);
			
			values = new ContentValues();
			values.put(WeatherProvider.INDEX,
					WeatherProvider.CONDITION_INDEX);
			values.put(WeatherProvider.WOEID, mWeatherInfo.getWoeid());
			values.put(WeatherProvider.NAME, mWeatherInfo.getName());
			values.put(WeatherProvider.CODE, mWeatherInfo.getCondition().getCode());
			values.put(WeatherProvider.DATE, mWeatherInfo.getCondition().getDate());
			values.put(WeatherProvider.TEMP, mWeatherInfo.getCondition().getTemp());
			values.put(WeatherProvider.TEXT, mWeatherInfo.getCondition().getText());
			values.put(WeatherProvider.UPDATE_TIME, mWeatherInfo.getUpdateTime());
			values.put(WeatherProvider.GPS, 0);
			if (hasCity) {
				mContentResolver
						.update(uri,
								values,
								WeatherProvider.INDEX + " = ? AND "
										+ WeatherProvider.WOEID + " = ?",
								new String[] {
										Integer.toString(WeatherProvider.CONDITION_INDEX),
										mWeatherInfo.getWoeid() });
			} else {
				mContentResolver.insert(uri, values);
			}

			for (int i = 0; i < MainActivity.FORECAST_DAY; i++) {
				values = new ContentValues();
				values.put(WeatherProvider.INDEX, i);
				values.put(WeatherProvider.WOEID, mWeatherInfo.getWoeid());
				values.put(WeatherProvider.CODE, mWeatherInfo.getForecasts()
						.get(i).getCode());
				values.put(WeatherProvider.DATE, mWeatherInfo.getForecasts()
						.get(i).getDate());
				values.put(WeatherProvider.DAY, mWeatherInfo.getForecasts().get(i)
						.getDay());
				values.put(WeatherProvider.HIGH, mWeatherInfo.getForecasts()
						.get(i).getHigh());
				values.put(WeatherProvider.LOW, mWeatherInfo.getForecasts().get(i)
						.getLow());
				values.put(WeatherProvider.TEXT, mWeatherInfo.getForecasts()
						.get(i).getText());
				values.put(WeatherProvider.GPS, 0);
				if (hasCity) {
					mContentResolver.update(
							uri,
							values,
							WeatherProvider.INDEX + " = ? AND "
									+ WeatherProvider.WOEID + " = ?",
							new String[] { Integer.toString(i),
									mWeatherInfo.getWoeid() });
				} else {
					mContentResolver.insert(uri, values);
				}
			}
		}
	}
	
	public void updateGpsWeatherInfo(WeatherInfo mWeatherInfo) {
		Log.d(TAG, "updateGpsWeatherInfo");
		boolean findGps = false;
		if (mWeatherInfo.isGps()) {
			Log.d(TAG, "GPS");
			for (WeatherInfo info : mWeatherInfoList) {
				if (info.isGps()) {
					Log.d(TAG, "GPS-copyInfo");
					findGps = true;
					info.setWoeid(mWeatherInfo.getWoeid());
					info.setName(mWeatherInfo.getName());
					info.copyInfo(mWeatherInfo);
				}
			}
			
			if (!findGps) {
				Log.d(TAG, "GPS-addInfo");
				mWeatherInfoList.add(mWeatherInfo);
			}
		}
	}
	
	public void deleteWeatherFromDB(String woeid, boolean isGps) {
		Log.d(TAG, "deleteWeatherFromDB, woeid:"+woeid+", isGps:"+isGps);
		if (!woeid.isEmpty()) {
			ContentResolver mContentResolver = mApp.getContentResolver();

			Uri weatherUri = Uri.parse(URI_GWEATHER);
			if (isGps) {
				mContentResolver
						.delete(weatherUri, WeatherProvider.GPS + "=?",
								new String[] { String
										.valueOf(WeatherProvider.FLAG_GPS) });
			} else {
				mContentResolver.delete(
						weatherUri,
						WeatherProvider.WOEID + "=? AND " + WeatherProvider.GPS
								+ "!=?",
						new String[] { woeid,
								String.valueOf(WeatherProvider.FLAG_GPS) });
			}
			
			for (WeatherInfo weatherInfo:mWeatherInfoList) {
				if (isGps) {
					if (weatherInfo.isGps()) {
						mWeatherInfoList.remove(weatherInfo);
						break;
					}
				} else {
					if (woeid.equals(weatherInfo.getWoeid())) {
						mWeatherInfoList.remove(weatherInfo);
						break;
					}
				}
				
			}
		}
	}
	
	public String getFirstWeatherFromDB() {
		Log.d(TAG, "getFirstWeatherFromDB");
		String woeid=null;
		ContentResolver mContentResolver = mApp.getContentResolver();

		Uri weatherUri = Uri.parse(URI_GWEATHER);
		Cursor cursor = mContentResolver.query(weatherUri, null,
				WeatherProvider.INDEX+"=?", new String[] { Integer
						.toString(WeatherProvider.CONDITION_INDEX) },
				null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				int cursorIndex = cursor
						.getColumnIndex(WeatherProvider.GPS);

				boolean isGps = (cursor.getInt(cursorIndex) == WeatherProvider.FLAG_GPS);
				if (isGps) {
					woeid = WeatherDataUtil.DEFAULT_WOEID_GPS;
				} else {
					cursorIndex = cursor
							.getColumnIndex(WeatherProvider.WOEID);
					woeid =	cursor.getString(cursorIndex);
				}
			}
			cursor.close();
		}
		
		return woeid;
	}
	
	public List<CityInfo> getGpsCityInfos() {
		if(!isInited() || mGpsCityInfoList.isEmpty()) {
			Log.d(TAG, "getGpsCityInfos, load again");
			loadGpsCityInfos();
		}
		return mGpsCityInfoList;
	}
	
	public void saveGpsCityInfoToDB(CityInfo mCityInfo) {
		ContentResolver mContentResolver = mApp.getContentResolver();
		Uri uri = Uri.parse(URI_GCITY);
		Cursor cursor = mContentResolver.query(uri, null, 
				WeatherProvider.CITY_WOEID+"=?", new String[] {mCityInfo.getWoeid()}, null);
		String woeid = "";
		String firstWoeid = "";
		boolean hasInfo = false;
		
		if (cursor != null) {
			if (cursor.moveToNext()) {
				int cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_WOEID);
				woeid = cursor.getString(cursorIndex);
				hasInfo = true;
			}
			if (!hasInfo) {
				if (cursor.moveToFirst()) {
					int cursorIndex = cursor.getColumnIndex(WeatherProvider.CITY_WOEID);
					firstWoeid = cursor.getString(cursorIndex);
				}
				
			}
			cursor.close();
		}
		Log.d(TAG, "saveGpsCityInfoToDB, hasInfo=" + hasInfo);
		
		if (!hasInfo && mGpsCityInfoList.size() >= GPS_CITY_COUNT_MAX) {
			Log.d(TAG, "GpsCity List size:" + mGpsCityInfoList.size());
			mContentResolver.delete(uri, WeatherProvider.CITY_WOEID+"=?", new String[] {firstWoeid});
		}
		
		ContentValues values;
		values = new ContentValues();
		values.put(WeatherProvider.CITY_WOEID, mCityInfo.getWoeid());
		values.put(WeatherProvider.CITY_NAME, mCityInfo.getName());
		values.put(WeatherProvider.CITY_LAT, mCityInfo.getLocationInfo().getLat());
		values.put(WeatherProvider.CITY_LON, mCityInfo.getLocationInfo().getLon());
		values.put(WeatherProvider.CITY_SWLAT, mCityInfo.getLocationInfo().getSouthWestLat());
		values.put(WeatherProvider.CITY_SWLON, mCityInfo.getLocationInfo().getSouthWestLon());
		values.put(WeatherProvider.CITY_NELAT, mCityInfo.getLocationInfo().getNorthEastLat());
		values.put(WeatherProvider.CITY_NELON, mCityInfo.getLocationInfo().getNorthEastLon());
		if (hasInfo) {
			mContentResolver.update(
					uri,
					values,
					WeatherProvider.WOEID+"=?",
					new String[] {woeid});
		} else {
			mContentResolver.insert(uri, values);
		}
	}
}
