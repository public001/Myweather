package com.gweather.app;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.gweather.utils.CityNameXMLParser;
import com.gweather.utils.Utils;
import com.gweather.utils.WeatherDataUtil;
import com.gweather.utils.WeatherXMLParser;
import com.gweather.utils.WebAccessTools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

public class InternetWorker {
	private static final String TAG = "Gweather.InternetWorker";
	
	private static final String URL_QUERY_WEATHER_PART1 = "http://query.yahooapis.com/v1/public/yql?q=select+*+from+weather.forecast+where+woeid=";
	private static final String URL_QUERY_WEATHER_PART2 = "+and+u='c'";

	private static final String URL_QUERY_CITY_PART1 = "http://query.yahooapis.com/v1/public/yql?q=select+*+from+geo.places+where+text='";
	private static final String URL_QUERY_CITY_PART2 = "*'+and+lang='en-US'";

	private static final String URL_QUERY_LOCATION_PART1 = "https://query.yahooapis.com/v1/public/yql?q=select+*+from+geo.places+where+woeid+in+(select+place.woeid+from+flickr.places+where+api_key="
			+ Utils.KEY_PUBLIC + "and+lat='";
	private static final String URL_QUERY_LOCATION_PART2 = "'+and+lon='";
	private static final String URL_QUERY_LOCATION_PART3 = "')+and+lang='en-US'";;
	
	enum State {
		IDLE, WORK_WEATHER, WORK_CITY, WORK_LOCATION
	}
	
	enum QueryWeatherType {
		CURRENT, ALL, ADD_NEW
	}
	
	private OnCityQueryFinishedListener cityListener;
	
	public interface OnCityQueryFinishedListener {
		void queryFinished();
	}
	
	public void setCityListener(OnCityQueryFinishedListener listener) {
		cityListener = listener;
	}

	private static InternetWorker INSTANCE;

	private Context mContext;
	private State mState = State.IDLE;
	private QueryWeatherType mQueryWeatherType = QueryWeatherType.CURRENT;
	private List<WeatherInfo> mWeatherInfoList;
	private WeatherInfo tempWeatherInfo;

	private QueryWeatherTask mQueryWeatherTask;
	private QueryCityTask mQueryCityTask;
	private QueryLocationTask mQueryLocationTask;
	
	private int updateWeatherCount = 0;
	private int updateFinishedWeatherCount = 0;

	private InternetWorker(Context context) {
		mContext = context;
	}

	public static InternetWorker getInstance(Context context) {
		if (null == INSTANCE) {
			INSTANCE = new InternetWorker(context);
		}

		return INSTANCE;
	}
	
	private void setAutoRefreshAlarm(Context context) {

		SharedPreferences sp = context.getSharedPreferences(
				MainActivity.SETTINGS_SP, Context.MODE_PRIVATE);
		boolean isAutoRefreshEnable = sp.getBoolean(
				MainActivity.SETTINGS_AUTO_REFRESH_ENABLE,
				context.getResources().getBoolean(
						R.bool.config_auto_refresh_enable));
		Log.d(TAG, "UpdateWidgetService - setAutoRefreshAlarm, "
				+ isAutoRefreshEnable);

		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(WeatherAction.ACTION_AUTO_REFRESH);
		PendingIntent operation = PendingIntent.getBroadcast(context, 0,
				intent, 0);

		int time;
		if (isAutoRefreshEnable) {
			time = sp.getInt(MainActivity.SETTINGS_AUTO_REFRESH, context
					.getResources().getInteger(R.integer.config_auto_refresh));
			Log.d(TAG, "setAutoRefreshAlarm, " + time);
			long deltaTime = WeatherDataUtil.getRefreshDelta(
					mContext, time);
			Log.d(TAG, "setAutoRefreshAlarm, " + deltaTime);
			alarmManager.cancel(operation);
			alarmManager.set(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + deltaTime, operation);
		} else {
			alarmManager.cancel(operation);
		}
	}

	public boolean updateWeather(WeatherInfo weatherInfo) {
		Log.v(TAG, "updateWeather, city: "+weatherInfo.getName());
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.CURRENT;
			mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask && 
					mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			
			mQueryWeatherTask = new QueryWeatherTask(weatherInfo);
			mQueryWeatherTask.execute();
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	public boolean updateWeather() {
		Log.v(TAG, "updateWeather");
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.ALL;
			mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
			updateWeatherCount = mWeatherInfoList.size();
			updateFinishedWeatherCount = 0;
			
			for(int i=0; i<updateWeatherCount; i++) {
				new QueryWeatherTask(mWeatherInfoList.get(i)).execute();
			}
			
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	public boolean addWeatherByCity(CityInfo cityInfo, boolean isGps) {
		Log.v(TAG, "addWeatherByCity");
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.ADD_NEW;
			mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask && 
					mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			
			WeatherInfo info = new WeatherInfo();
			info.setWoeid(cityInfo.getWoeid());
			info.setName(cityInfo.getName());
			info.setGps(isGps);
			
			mQueryWeatherTask = new QueryWeatherTask(info);
			mQueryWeatherTask.execute();
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}

	class QueryWeatherTask extends AsyncTask<Void, Void, Void> {
		private WeatherInfo mWeatherInfo;

		public QueryWeatherTask(WeatherInfo weatherInfo) {
			mWeatherInfo = weatherInfo;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "QueryWeather - doing");
			String url = URL_QUERY_WEATHER_PART1 + mWeatherInfo.getWoeid()
					+ URL_QUERY_WEATHER_PART2;
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseWeather(content, mWeatherInfo);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			Log.d(TAG, "QueryWeather - DONE");
			if (tempWeatherInfo.getForecasts().size() < MainActivity.FORECAST_DAY) {
				
				Log.w(TAG, "QueryWeather Failed: " + tempWeatherInfo.getForecasts().size());
			} else {
				
				tempWeatherInfo.setName(mWeatherInfo.getName());
				mWeatherInfo.copyInfo(tempWeatherInfo);
				WeatherApp.mModel.saveWeatherInfoToDB(mWeatherInfo);
			}
			
			updateFinishedWeatherCount++;
			
			Log.d(TAG, "updateFinishedWeatherCount:" + updateFinishedWeatherCount
					+ ", updateWeatherCount:" + updateWeatherCount);
			
			if (updateFinishedWeatherCount == updateWeatherCount) {
				Intent intent;
				if (QueryWeatherType.ALL == mQueryWeatherType) {
					Log.d(TAG, "ALL");
					WeatherDataUtil.getInstance().setRefreshTime(mContext, System.currentTimeMillis());
					intent = new Intent(WeatherAction.ACTION_WEATHER_REFRESHED_ALL);
					mContext.sendBroadcast(intent);
				} else if (QueryWeatherType.CURRENT == mQueryWeatherType) {
					Log.d(TAG, "CURRENT");
					intent = new Intent(WeatherAction.ACTION_WEATHER_REFRESHED);
					mContext.sendBroadcast(intent);
				} else if (QueryWeatherType.ADD_NEW == mQueryWeatherType){
					Log.d(TAG, "ADD_NEW");
					if (mWeatherInfo.isGps()) {
						Log.d(TAG, "GPS");
						WeatherApp.mModel.updateGpsWeatherInfo(mWeatherInfo);
						intent = new Intent(WeatherAction.ACTION_QUERT_GPS_WEATHER_FINISH);
					} else {
						Log.d(TAG, "Normal");
						intent = new Intent(WeatherAction.ACTION_ADD_WEATHER_FINISH);
					}
					mContext.sendBroadcast(intent);
				}
				
				setAutoRefreshAlarm(mContext);
				
				mState = State.IDLE;
			}
		}

	}
	
	private void parseWeather(String content, WeatherInfo mWeatherInfo) {
		if (content == null || content.isEmpty()) {
			Log.w(TAG, "parseWeather content is Empty");
			return;
		}
		
		tempWeatherInfo = new WeatherInfo();
		
		SAXParserFactory mSAXParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser mSAXParser = mSAXParserFactory.newSAXParser();
			XMLReader mXmlReader = mSAXParser.getXMLReader();
			WeatherXMLParser handler = new WeatherXMLParser(mContext, 
					tempWeatherInfo, mWeatherInfo.getWoeid());
			mXmlReader.setContentHandler(handler);
			StringReader stringReader = new StringReader(content);
			InputSource inputSource = new InputSource(stringReader);
			mXmlReader.parse(inputSource);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean queryCity(String name, ArrayList<CityInfo> cityInfos) {
		Log.v(TAG, "queryCity");
		if (mState == State.IDLE) {
			mState = State.WORK_CITY;
			if (null != mQueryCityTask && 
					mQueryCityTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryCityTask.cancel(true);
			}
			
			mQueryCityTask = new QueryCityTask(cityInfos);
			mQueryCityTask.execute(name);
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	public void stopQueryCity() {
		if (null != mQueryCityTask && 
				mQueryCityTask.getStatus() == AsyncTask.Status.RUNNING) {
			mQueryCityTask.cancel(true);
		}
		if (mState == State.WORK_CITY) {
			setCityListener(null);
			mState = State.IDLE;
		}
	}
	
	class QueryCityTask extends AsyncTask<String, Void, Void> {
		private ArrayList<CityInfo> mCityInfos;
		
		public QueryCityTask(ArrayList<CityInfo> cityInfos) {
			mCityInfos = cityInfos;
		}
		
		@Override
		protected Void doInBackground(String... params) {
			String url = URL_QUERY_CITY_PART1
					+ params[0] + URL_QUERY_CITY_PART2;
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseCity(content, mCityInfos);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if(null != cityListener) {
				cityListener.queryFinished();
			}
			setCityListener(null);
			mState = State.IDLE;
		}
	}
	
	private void parseCity(String content, ArrayList<CityInfo> mCityInfos) {
		if (null == content || content.isEmpty()) {
			Log.w(TAG, "parseCity content is Empty");
			return;
		}

		mCityInfos.clear();

		SAXParserFactory mSAXParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser mSAXParser = mSAXParserFactory.newSAXParser();
			XMLReader mXmlReader = mSAXParser.getXMLReader();
			CityNameXMLParser handler = new CityNameXMLParser(mCityInfos);
			mXmlReader.setContentHandler(handler);
			StringReader stringReader = new StringReader(content);
			InputSource inputSource = new InputSource(stringReader);
			mXmlReader.parse(inputSource);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public boolean queryLocation(Location location, ArrayList<CityInfo> mCityInfos) {
		Log.v(TAG, "queryLocation");
		if(State.IDLE == mState) {
			mState = State.WORK_LOCATION;
			if (null != mQueryLocationTask && 
					mQueryLocationTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryLocationTask.cancel(true);
			}
			
			mQueryLocationTask = new QueryLocationTask(location, mCityInfos);
			mQueryLocationTask.execute();
			
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	class QueryLocationTask extends AsyncTask<Void, Void, Void> {
		private Location location;
		private ArrayList<CityInfo> mCityInfos;
		
		public QueryLocationTask(Location location, ArrayList<CityInfo> mCityInfos) {
			this.location = location;
			this.mCityInfos = mCityInfos;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			String url= URL_QUERY_LOCATION_PART1
					+ location.getLatitude()
					+ URL_QUERY_LOCATION_PART2
					+ location.getLongitude()
					+ URL_QUERY_LOCATION_PART3;
			
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseCity(content, mCityInfos);
			if (mCityInfos.size() > 0) {
				mCityInfos.get(0).getLocationInfo()
						.setLat(location.getLatitude());
				mCityInfos.get(0).getLocationInfo()
						.setLon(location.getLongitude());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mState = State.IDLE;
			Intent intent = new Intent(WeatherAction.ACTION_QUERT_LOCATION_FINISH);
			mContext.sendBroadcast(intent);
		}
	}
	
	public void stopQueryLocation() {
		if (null != mQueryLocationTask && 
				mQueryLocationTask.getStatus() == AsyncTask.Status.RUNNING) {
			mQueryLocationTask.cancel(true);
		}
		
		if (State.WORK_LOCATION == mState) {
			mState = State.IDLE;
		}
	}
}
