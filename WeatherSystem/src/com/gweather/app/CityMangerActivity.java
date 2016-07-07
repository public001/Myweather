package com.gweather.app;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
/*
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
*/
import com.gweather.utils.Utils;
import com.gweather.utils.WeatherDataUtil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class CityMangerActivity extends Activity /*implements
		AMapLocationListener*/ {
	private static final String TAG = "Gweather.CityManger";
	public static final int REQUEST_CODE_CITY_ADD = 1001;
	public static final int REQUEST_CODE_PERMISSION = 101;
	public static final int CITY_COUNT_MAX = 10;

	private static final boolean GPS_DEBUG = false;

	private ImageView addCity;
	private ImageView location;
	private ImageView allRefresh;
	private TextView allRefreshTimeText;
	private ListView cityList;
	private View loadProgressView;
	
	private InternetWorker mInternetWorker;

	private CityListAdapter mCityListAdapter;
	private CityListItem gpsItem;
	private List<CityListItem> items = new ArrayList<CityListItem>();
	private ArrayList<CityInfo> mCityInfos = new ArrayList<CityInfo>();
	private CityInfo gpsCityInfo = new CityInfo();
	private List<WeatherInfo> mWeatherInfoList;
	private List<CityInfo> mGpsCityInfoList;

	private int deletePosition;

	//private AMapLocationClient locationClient = null;
	//private AMapLocationClientOption locationOption = null;

	private static LocationManager mLocationManager;
	private static Thread mGetLocationThread = null;
	private static boolean isStoped = false;
	private boolean isAutoGps = false;

	private CityManagerReceiver mCityManagerReceiver;

	private class CityManagerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (null == intent) {
				return;
			}

			String action = intent.getAction();
			Log.d(TAG, "WeatherRefreshedReceiver, " + action);
			if (WeatherAction.ACTION_WEATHER_REFRESHED_ALL.equals(action)) {
				refreshCityList();
				startUpdateService(CityMangerActivity.this,
						WeatherWidget.ACTION_UPDATE,
						AppWidgetManager.INVALID_APPWIDGET_ID);
				showLoadingProgress(false);
			} else if (WeatherAction.ACTION_QUERT_LOCATION_FINISH
					.equals(action)) {
				if (!mCityInfos.isEmpty()) {
					CityInfo city = mCityInfos.get(0);
					
					Log.d(TAG, "CityInfo, "
							+ city.getLocationInfo().getSouthWestLat() + ", "
							+ city.getLocationInfo().getSouthWestLon() + ", "
							+ city.getLocationInfo().getNorthEastLat() + ", "
							+ city.getLocationInfo().getNorthEastLon());
					if (null == gpsCityInfo) { 
						gpsCityInfo = city;
						WeatherApp.mModel.saveGpsCityInfoToDB(gpsCityInfo);
					}
				}

				isAutoGps = false;

				if (isStoped) {
					Log.w(TAG, "City Activity has Stoped");
				} else {
					getLocationFindDialog(false).show();
				}

				clearLocationThing();
				showLoadingProgress(false);
			} else if (WeatherAction.ACTION_QUERT_GPS_WEATHER_FINISH
					.equals(action)) {
				refreshCityList();
				String defaultWoeid = WeatherDataUtil.getInstance()
						.getDefaultCityWoeid(CityMangerActivity.this);
				if (defaultWoeid.isEmpty() && !mWeatherInfoList.isEmpty()) {
					WeatherDataUtil
							.getInstance()
							.updateDefaultCityWoeid(
									CityMangerActivity.this,
									mWeatherInfoList.get(0).isGps() ? WeatherDataUtil.DEFAULT_WOEID_GPS
											: mWeatherInfoList.get(0)
													.getWoeid());

					startUpdateService(CityMangerActivity.this,
							WeatherWidget.ACTION_UPDATE,
							AppWidgetManager.INVALID_APPWIDGET_ID);
				}
				WeatherDataUtil.getInstance().setNeedUpdateMainUI(
						CityMangerActivity.this, true);
				showLoadingProgress(false);
			}

		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.city_manager);
		WeatherApp.mModel = WeatherModel.getInstance(getApplicationContext());
		mInternetWorker = InternetWorker.getInstance(getApplicationContext());
		//AMapInit(CityMangerActivity.this);

		initUI();
		refreshCityList();

		checkFirstRun();
		mCityManagerReceiver = new CityManagerReceiver();
		IntentFilter filter = new IntentFilter(
				WeatherAction.ACTION_WEATHER_REFRESHED_ALL);
		filter.addAction(WeatherAction.ACTION_QUERT_LOCATION_FINISH);
		filter.addAction(WeatherAction.ACTION_QUERT_GPS_WEATHER_FINISH);
		registerReceiver(mCityManagerReceiver, filter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		isStoped = false;
	}

	private void checkFirstRun() {
		Intent intent = getIntent();
		boolean isFirstRun = intent.getBooleanExtra("isFirstRun", false);
		Log.d(TAG, "isFirstRun=" + isFirstRun);
		if (isFirstRun) {
			boolean hasPermission = false;
			if (Build.VERSION.SDK_INT >= 23) {
				hasPermission = PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
				Log.d(TAG, "hasPermission=" + hasPermission);
				if (!hasPermission) {
					return;
				}
			}

			boolean agpsOpen = Utils.isAPGSOpen(CityMangerActivity.this);

			Log.d(TAG, "agpsOpen=" + agpsOpen);
			if (agpsOpen) {
				if (mGetLocationThread == null) {
					showLoadingProgress(true);
					isAutoGps = true;
					mGetLocationThread = new Thread(new GetLocationRunnable());
					mGetLocationThread.start();
					//AMapInit(CityMangerActivity.this);
					//AMapStart();
				}
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult, requestCode: " + requestCode
				+ ", resultCode: " + resultCode);
		switch (requestCode) {
		case REQUEST_CODE_CITY_ADD:
			refreshCityList();
			String defaultWoeid = WeatherDataUtil.getInstance()
					.getDefaultCityWoeid(CityMangerActivity.this);
			if (defaultWoeid.isEmpty() && !mWeatherInfoList.isEmpty()) {
				WeatherDataUtil
						.getInstance()
						.updateDefaultCityWoeid(
								CityMangerActivity.this,
								mWeatherInfoList.get(0).isGps() ? WeatherDataUtil.DEFAULT_WOEID_GPS
										: mWeatherInfoList.get(0).getWoeid());
				startUpdateService(CityMangerActivity.this,
						WeatherWidget.ACTION_UPDATE,
						AppWidgetManager.INVALID_APPWIDGET_ID);
			}
			WeatherDataUtil.getInstance().setNeedUpdateMainUI(
					CityMangerActivity.this, true);
			break;

		default:
			break;
		}

	}

	private void initUI() {
		addCity = (ImageView) findViewById(R.id.add_city);
		addCity.setOnClickListener(onClickListener);

		location = (ImageView) findViewById(R.id.location);
		location.setOnClickListener(onClickListener);

		allRefresh = (ImageView) findViewById(R.id.all_refresh);
		allRefresh.setOnClickListener(onClickListener);
		allRefreshTimeText = (TextView) findViewById(R.id.latest_all_refresh_time);
		cityList = (ListView) findViewById(R.id.city_list);

		loadProgressView = findViewById(R.id.loading_progress_view);
	}

	View.OnClickListener onClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.all_refresh:
				SharedPreferences sp = getSharedPreferences(
						MainActivity.SETTINGS_SP, Context.MODE_PRIVATE);
				if (sp.getBoolean(
						MainActivity.SETTINGS_WIFI_ONLY,
						getResources().getBoolean(
								R.bool.config_wifi_only_enable))) {
					if (Utils.isNetworkTypeWifi(CityMangerActivity.this)) {
						showLoadingProgress(true);
						if (!mInternetWorker.updateWeather()) {
							showLoadingProgress(false);
						}
					} else {
						Toast.makeText(CityMangerActivity.this,
								R.string.toast_wifi_only_mode,
								Toast.LENGTH_SHORT).show();
						Log.d(TAG,
								"CityManager-SETTINGS_WIFI_ONLY, network type NOT WIFI.");
					}
				} else {
					if (Utils.isNetworkAvailable(CityMangerActivity.this)) {
						showLoadingProgress(true);
						mInternetWorker.updateWeather();
					} else {
						Toast.makeText(CityMangerActivity.this,
								R.string.toast_net_inavailable,
								Toast.LENGTH_SHORT).show();
						Log.d(TAG,
								"CityManager-Refresh BTN, network NOT available");
					}
				}

				break;
			case R.id.add_city:
				if (items.size() < CITY_COUNT_MAX) {
					Intent intent = new Intent(CityMangerActivity.this,
							AddCityActivity.class);
					intent.putExtra("from_manager", true);
					startActivityForResult(intent, REQUEST_CODE_CITY_ADD);
				} else {
					Toast.makeText(
							CityMangerActivity.this,
							getResources().getString(R.string.city_max_toast,
									CITY_COUNT_MAX), Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.location:
				if (!Utils.isNetworkAvailable(CityMangerActivity.this)) {
					Toast.makeText(CityMangerActivity.this,
							R.string.toast_net_inavailable, Toast.LENGTH_SHORT)
							.show();
					Log.d(TAG, "Location BTN, network NOT available");
					return;
				}

				if (Build.VERSION.SDK_INT >= 23) {
					boolean hasPermission = PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
					Log.d(TAG, "ACCESS_COARSE_LOCATION, hasPermission="
							+ hasPermission);
					if (!hasPermission) {
						requestPermissions(
								new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
								REQUEST_CODE_PERMISSION);
						return;
					}

				}

				if (Utils.isAPGSOpen(CityMangerActivity.this)) {
					Log.d(TAG, "CityManager - AGPS Opened");
					if (mGetLocationThread == null) {
						showLoadingProgress(true);
						mGetLocationThread = new Thread(
								new GetLocationRunnable());
						mGetLocationThread.start();

						//AMapInit(CityMangerActivity.this);
						//AMapStart();
					}
				} else {
					Log.d(TAG, "CityManager - AGPS Not Open");
					Intent intent = new Intent(
							Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(intent);
				}
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onStop() {
		super.onStop();
		isStoped = true;
		mInternetWorker.stopQueryCity();

		clearLocationThing();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mCityManagerReceiver);
		mCityManagerReceiver = null;
		//AMapDestroy();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && isAutoGps) {
			isAutoGps = false;
			mInternetWorker.stopQueryCity();
			clearLocationThing();
			showLoadingProgress(false);
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	private void refreshCityList() {
		Log.d(TAG, "refreshCityList");
		gpsItem = null;
		items.clear();
		CityListItem item;
		mWeatherInfoList = WeatherApp.mModel.getWeatherInfos();

		for (WeatherInfo weatherInfo : mWeatherInfoList) {
			item = new CityListItem(weatherInfo.getWoeid(),
					weatherInfo.getName());
			item.setText(weatherInfo.getCondition().getText());
			item.setWeather(weatherInfo.getCondition().getTemp());
			item.setGPs(weatherInfo.isGps());
			if (weatherInfo.isGps()) {
				gpsItem = item;
			} else {
				items.add(item);
			}
		}
		if (null != gpsItem) {
			items.add(0, gpsItem);
		}

		if (items.size() < CITY_COUNT_MAX) {
			addCity.setImageResource(R.drawable.add_city);
		} else {
			addCity.setImageResource(R.drawable.add_city_disabled);
		}

		mCityListAdapter = new CityListAdapter(CityMangerActivity.this, items);
		cityList.setAdapter(mCityListAdapter);
		cityList.setOnItemLongClickListener(longClickListener);

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date mDate = new Date(WeatherDataUtil.getInstance().getRefreshTime(
				CityMangerActivity.this));
		String refreshTime = getResources().getString(R.string.refresh_time,
				format.format(mDate));
		allRefreshTimeText.setText(refreshTime);
	}

	AdapterView.OnItemLongClickListener longClickListener = new AdapterView.OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			Log.d(TAG, "List-onItemLongClick:" + position);
			deletePosition = position;

			String title = getResources().getString(R.string.delete_city,
					items.get(position).name);
			final AlertDialog dialog = new AlertDialog.Builder(
					CityMangerActivity.this,
					AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
					.setTitle(title)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									boolean isGps = items.get(deletePosition).isGps;
									WeatherApp.mModel.deleteWeatherFromDB(
											items.get(deletePosition).woeid,
											isGps);

									String defaultWoeid = WeatherDataUtil
											.getInstance().getDefaultCityWoeid(
													CityMangerActivity.this);

									if ((!isGps
											&& defaultWoeid.equals(items
													.get(deletePosition).woeid) && !defaultWoeid
												.equals(WeatherDataUtil.DEFAULT_WOEID_GPS))
											|| (isGps && defaultWoeid
													.equals(WeatherDataUtil.DEFAULT_WOEID_GPS))) {
										WeatherDataUtil
												.getInstance()
												.updateDefaultCityWoeid(
														CityMangerActivity.this,
														"");

										String firstWoeid = WeatherApp.mModel
												.getFirstWeatherFromDB();
										if (null != firstWoeid) {
											WeatherDataUtil
													.getInstance()
													.updateDefaultCityWoeid(
															CityMangerActivity.this,
															firstWoeid);
										}

										startUpdateService(
												CityMangerActivity.this,
												WeatherWidget.ACTION_UPDATE,
												AppWidgetManager.INVALID_APPWIDGET_ID);
									}

									refreshCityList();
									WeatherDataUtil.getInstance()
											.setNeedUpdateMainUI(
													CityMangerActivity.this,
													true);
								}
							}).setNeutralButton(android.R.string.cancel, null)
					.create();
			dialog.show();
			return true;
		}
	};

	private void startUpdateService(Context context, String action, int widgetId) {
		Log.d(TAG, "CityManager - startUpdateService");
		Intent intent = new Intent(context, UpdateWidgetService.class);
		intent.setAction(action);
		intent.setData(Uri.parse(String.valueOf(widgetId)));
		context.startService(intent);
	}

	class GetLocationRunnable implements Runnable {
		@Override
		public void run() {
			String contextService = Context.LOCATION_SERVICE;
			mLocationManager = (LocationManager) getSystemService(contextService);
			if (mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER) == null) {
				Log.d(TAG, "NETWORK_PROVIDER is NULL");
			} else {
				Log.d(TAG, "NETWORK_PROVIDER is OK");
				Looper.prepare();
				String provider = LocationManager.NETWORK_PROVIDER;
				mLocationManager.requestLocationUpdates(provider, 5000, 1,
						netLocationListener, Looper.myLooper());

				provider = LocationManager.GPS_PROVIDER;
				mLocationManager.requestLocationUpdates(provider, 5000, 1,
						gpsLocationListener, Looper.myLooper());
				mLocationManager.addGpsStatusListener(gpsStatusListener);
				Looper.loop();
			}
		}

	}

	private final LocationListener netLocationListener = new LocationListener() {
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.d(TAG, "netLocationListener - onStatusChanged");
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.d(TAG, "netLocationListener - onProviderEnabled");
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.d(TAG, "netLocationListener - onProviderDisabled");
		}

		@Override
		public void onLocationChanged(Location location) {
			Log.d(TAG, "netLocationListener - onLocationChanged");
			updateLocation(location);
		}
	};

	private final LocationListener gpsLocationListener = new LocationListener() {
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.d(TAG, "gpsLocationListener - onStatusChanged");
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.d(TAG, "gpsLocationListener - onProviderEnabled");
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.d(TAG, "gpsLocationListener - onProviderDisabled");
		}

		@Override
		public void onLocationChanged(Location location) {
			Log.d(TAG, "gpsLocationListener - onLocationChanged");
			updateLocation(location);
		}
	};

	private GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {

		@Override
		public void onGpsStatusChanged(int event) {
			switch (event) {
			case GpsStatus.GPS_EVENT_STARTED:
				Log.d(TAG, "onGpsStatusChanged - GPS_EVENT_STARTED");
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				Log.d(TAG, "onGpsStatusChanged - GPS_EVENT_STOPPED");
				break;
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				Log.d(TAG, "onGpsStatusChanged - GPS_EVENT_FIRST_FIX");
				break;
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				Log.d(TAG, "onGpsStatusChanged - GPS_EVENT_SATELLITE_STATUS");
				break;

			default:
				break;
			}

		}
	};

	private void updateLocation(double latitude, double longitude) {
		Location location = new Location(LocationManager.NETWORK_PROVIDER);
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		updateLocation(location);
	}

	private boolean testGPS = false;

	private void updateLocation(Location location) {
		if (location != null) {
			double lat = location.getLatitude();
			double lng = location.getLongitude();
			Log.d(TAG, "updateLocation, lat=" + lat + ", lng=" + lng);
			if (testGPS) {
				lat = 31.079437;
				lng = 121.735133;
				Log.d(TAG, "Test, lat=" + lat + ", lng=" + lng);
			}
			gpsCityInfo = hasLocated(lat, lng);
			mCityInfos.clear();
			if (GPS_DEBUG || null != gpsCityInfo) {
				Log.d(TAG, "CityManager, Laction not changed");
				mCityInfos.add(gpsCityInfo);
				Intent intent = new Intent(
						WeatherAction.ACTION_QUERT_LOCATION_FINISH);
				sendBroadcast(intent);
			} else {
				if (!mInternetWorker.queryLocation(location, mCityInfos)) {
					Intent intent = new Intent(
							WeatherAction.ACTION_QUERT_LOCATION_FINISH);
					sendBroadcast(intent);
				}
			}

			if (mLocationManager != null) {
				mLocationManager.removeUpdates(netLocationListener);
				mLocationManager.removeUpdates(gpsLocationListener);
				mLocationManager.removeGpsStatusListener(gpsStatusListener);
				mLocationManager = null;
			}

			//AMapStop();
			//AMapDestroy();
		}
	}

	private AlertDialog getLocationFindDialog(final boolean isNew) {
		Log.d(TAG, "getLocationFindDialog:" + isNew);
		String title = getResources().getString(R.string.update_location_city,
				gpsCityInfo.getName());
		final AlertDialog dialog = new AlertDialog.Builder(
				CityMangerActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
				.setTitle(title)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								showLoadingProgress(true);
								if (!mInternetWorker.addWeatherByCity(
										gpsCityInfo, true)) {
									showLoadingProgress(false);
								}
							}
						}).setNeutralButton(android.R.string.cancel, null)
				.create();
		return dialog;
	}

	private void clearLocationThing() {
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(netLocationListener);
			mLocationManager.removeUpdates(gpsLocationListener);
			mLocationManager.removeGpsStatusListener(gpsStatusListener);
			mLocationManager = null;
		}

		if (mGetLocationThread != null) {
			mGetLocationThread.interrupt();
			mGetLocationThread = null;
		}

		//AMapStop();
		//AMapDestroy();
	}
	
	private CityInfo hasLocated(double lat, double lng) {
		Log.d(TAG, "hasLocated, lat=" + lat + ", lng=" + lng);
		mGpsCityInfoList = WeatherApp.mModel.getGpsCityInfos();
		Log.d(TAG, "cityInfo count:" + mGpsCityInfoList.size());
		if (mGpsCityInfoList.isEmpty()) {
			return null;
		} else {
			for (CityInfo cityInfo:mGpsCityInfoList) {
				if (cityInfo.getWoeid() != null
						&& !cityInfo.getWoeid().isEmpty()
						&& ((cityInfo.getLocationInfo().getLat() == lat 
						&& cityInfo.getLocationInfo().getLon() == lng) 
						|| (cityInfo.getLocationInfo().getNorthEastLat() <= lat
						&& cityInfo.getLocationInfo().getSouthWestLat() >= lat
						&& cityInfo.getLocationInfo().getNorthEastLon() <= lng 
						&& cityInfo.getLocationInfo().getSouthWestLon() >= lng))) {
					return cityInfo;
				}
			}
			return null;
		}
	}

	private void showLoadingProgress(boolean show) {
		Log.d(TAG, "showLoadingProgress:" + show);
		if (show) {
			loadProgressView.setVisibility(View.VISIBLE);
			location.setEnabled(false);
			addCity.setEnabled(false);
			allRefresh.setEnabled(false);
		} else {
			loadProgressView.setVisibility(View.GONE);
			location.setEnabled(true);
			addCity.setEnabled(true);
			allRefresh.setEnabled(true);
		}
	}

	class CityListAdapter extends BaseAdapter {
		private List<CityListItem> mList;
		private LayoutInflater mInflater;

		public CityListAdapter(Context context, List<CityListItem> mList) {
			super();
			this.mList = mList;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder mHolder;

			if (convertView == null || convertView.getTag() == null) {
				// Time consuming 1 -- inflate
				convertView = mInflater.inflate(R.layout.city_list_item, null);
				mHolder = new ViewHolder();
				// Time consuming 2 -- findViewById
				mHolder.name = (TextView) convertView.findViewById(R.id.name);
				mHolder.image = (ImageView) convertView
						.findViewById(R.id.image);
				mHolder.text = (TextView) convertView.findViewById(R.id.text);
				mHolder.weather = (TextView) convertView
						.findViewById(R.id.weather);
				mHolder.gpsIcon = (ImageView) convertView
						.findViewById(R.id.ic_gps);
				convertView.setTag(mHolder);
			} else {
				mHolder = (ViewHolder) convertView.getTag();
			}
			CityListItem bean = mList.get(position);
			mHolder.name.setText(bean.name);
			mHolder.image.setImageResource(bean.imgRes);
			mHolder.text.setText(bean.text);
			mHolder.weather.setText(bean.weather + "℃");
			mHolder.gpsIcon
					.setVisibility(bean.isGps ? View.VISIBLE : View.GONE);
			return convertView;
		}

		// Google I/O
		class ViewHolder {
			public TextView name;
			public ImageView image;
			public TextView text;
			public TextView weather;
			public ImageView gpsIcon;
		}
	}

	class CityListItem {
		public String woeid;
		public String name;
		public int imgRes;
		public String text;
		public String weather;
		public boolean isGps;

		public CityListItem(String woeid, String name) {
			super();
			this.woeid = woeid;
			this.name = name;
		}

		public void setImgRes(int imgRes) {
			this.imgRes = imgRes;
		}

		public void setWeather(String weather) {
			this.weather = weather;
		}

		public void setText(String text) {
			this.text = text;
		}

		public void setGPs(boolean isGps) {
			this.isGps = isGps;
		}
	}

	// AMap Start
    /*
	boolean supportAmap = false;
	
	private void AMapInit(Context context) {
		supportAmap = context.getResources().getBoolean(R.bool.config_use_amap);
		if (supportAmap) {
			AMapDestroy();
	
			locationClient = new AMapLocationClient(this.getApplicationContext());
			locationOption = new AMapLocationClientOption();
			locationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			locationClient.setLocationListener(this);
		}
	}

	private void AMapStart() {
		if (supportAmap) {
			locationClient.setLocationOption(locationOption);
			locationClient.startLocation();
		}
	}

	private void AMapStop() {
		if (supportAmap) {
			if (null != locationClient) {
				locationClient.stopLocation();
			}
		}
	}

	private void AMapDestroy() {
		if (supportAmap) {
			if (null != locationClient) {
				locationClient.onDestroy();
				locationClient = null;
				locationOption = null;
			}
		}
	}

	@Override
	public void onLocationChanged(AMapLocation location) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		Log.d(TAG, "AMap - lat=" + lat + ", lon=" + lon);
		if (lat == 0.0 && lon == 0.0) {
			Log.d(TAG, "AMap - invalid value");
		} else {
			updateLocation(lat, lon);
		}
	}
    */
	// AMap End
}
