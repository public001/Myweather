package com.gweather.app;

import java.util.ArrayList;
import com.gweather.app.R;
import com.gweather.utils.Utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class AddCityActivity extends Activity {
	private static final String TAG = "Gweather.AddCityActivity";

	private EditText cityName;
	private ImageButton searchCity;
	private ListView cityList;
	private View loadProgressView;

	private ArrayList<CityInfo> mCityInfos = new ArrayList<CityInfo>();
	private ArrayAdapter<String> cityInfoAdapter;
	private InternetWorker mInternetWorker;
	private WeatherRefreshedReceiver mWeatherRefreshedReceiver;
	
	private class WeatherRefreshedReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (null == intent) {
				return;
			}
			String action = intent.getAction();
			Log.d(TAG, "WeatherRefreshedReceiver, " + action);
			if (WeatherAction.ACTION_ADD_WEATHER_FINISH.equals(action)) {
				
				AddCityActivity.this.setResult(
						CityMangerActivity.REQUEST_CODE_CITY_ADD, null);
				
				AddCityActivity.this.finish();
				showLoadingProgress(false);
			} 

		}

	}

	public void setWeatherFromInternet(CityInfo cityInfo) {
		Log.d(TAG, "setWeatherFromInternet, woeid:" + cityInfo.getWoeid());
		showLoadingProgress(true);
		if(!mInternetWorker.addWeatherByCity(cityInfo ,false)) {
			showLoadingProgress(false);
		}
	}
	
	InternetWorker.OnCityQueryFinishedListener onCityQueryFinishedListener = new InternetWorker.OnCityQueryFinishedListener() {
		
		@Override
		public void queryFinished() {
			if(!mCityInfos.isEmpty()) {
				final int count = mCityInfos.size();
				String[] cityInfosStrings = new String[count];
				for (int i = 0; i < count; i++) {
					cityInfosStrings[i] = mCityInfos.get(i).toString();
					Log.d(TAG, "[" + i + "] CityInfo="
							+ mCityInfos.get(i).getWoeid() + ": "
							+ mCityInfos.get(i).toString());
				}
				
				cityInfoAdapter = new ArrayAdapter<String>(
						AddCityActivity.this,
						R.layout.simple_list_item, cityInfosStrings);
				cityList.setAdapter(cityInfoAdapter);
				cityList.setVisibility(View.VISIBLE);

			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.city_not_found),
						Toast.LENGTH_SHORT).show();
			}

			showLoadingProgress(false);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.set_international_city);
		mInternetWorker = InternetWorker.getInstance(getApplicationContext());
		
		cityName = (EditText) findViewById(R.id.city_name);
		searchCity = (ImageButton) findViewById(R.id.search_city);
		searchCity.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (Utils.isNetworkAvailable(AddCityActivity.this)) {
					searchCity();
				} else {
					Toast.makeText(AddCityActivity.this,
							R.string.toast_net_inavailable, Toast.LENGTH_SHORT)
							.show();
					Log.d(TAG, "Search city BTN, network NOT available");
					return;
				}

			}
		});

		cityList = (ListView) findViewById(R.id.city_list);
		cityList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.d(TAG, "position = " + position + ", id = " + id);
				int length = mCityInfos.size();
				if (position < length) {
					addCity(mCityInfos.get(position));
				}
			}
		});

		loadProgressView = findViewById(R.id.loading_progress_view);
	
		mWeatherRefreshedReceiver = new WeatherRefreshedReceiver();
		IntentFilter filter = new IntentFilter(
				WeatherAction.ACTION_ADD_WEATHER_FINISH);
		registerReceiver(mWeatherRefreshedReceiver, filter);
	}

	protected void onResume() {
		super.onResume();
	};

	@Override
	protected void onDestroy() {
		cityName.addTextChangedListener(null);
		unregisterReceiver(mWeatherRefreshedReceiver);
		mWeatherRefreshedReceiver = null;
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mInternetWorker.stopQueryCity();
	}

	private void searchCity() {
		String name = cityName.getText().toString();
		if (name.isEmpty()) {
			// Any toast?
		} else {
			showLoadingProgress(true);
			mInternetWorker.setCityListener(onCityQueryFinishedListener);
			if (!mInternetWorker.queryCity(name, mCityInfos)) {
				showLoadingProgress(false);
			}
		}
	}

	private void addCity(CityInfo info) {
		mInternetWorker.stopQueryCity();
		
		String woeid = info.getWoeid();
		Log.d(TAG, "addCity, woeid=" + woeid);

		cityList.setVisibility(View.GONE);

		setWeatherFromInternet(info);
	}

	private void showLoadingProgress(boolean show) {
		Log.d(TAG, "showLoadingProgress:" + show);
		if (show) {
			loadProgressView.setVisibility(View.VISIBLE);
			searchCity.setEnabled(false);
		} else {
			loadProgressView.setVisibility(View.GONE);
			searchCity.setEnabled(true);
		}
	}

}
