package com.gweather.view;

import com.gweather.app.R;
import com.gweather.app.WeatherInfo;
import com.gweather.utils.WeatherDataUtil;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WeatherInfoMainView extends LinearLayout {
	private Context mContext;

	private TextView date;
	private TextView cityName;
	private ImageView gpsImage;
	private ImageView weatherIcon;
	private TextView weatherText;
	private TextView currentTemp;
	private TextView temp;

	private LinearLayout forecastItem1;
	private LinearLayout forecastItem2;
	private LinearLayout forecastItem3;
	private LinearLayout forecastItem4;

	private ForeCastItemViews foreCastItemViews1 = new ForeCastItemViews();
	private ForeCastItemViews foreCastItemViews2 = new ForeCastItemViews();
	private ForeCastItemViews foreCastItemViews3 = new ForeCastItemViews();
	private ForeCastItemViews foreCastItemViews4 = new ForeCastItemViews();
	
	public WeatherInfoMainView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		mContext = context;

		LayoutInflater.from(context).inflate(R.layout.weather_info_main, this);

		date = (TextView) findViewById(R.id.date_y);
		cityName = (TextView) findViewById(R.id.city_name);
		gpsImage = (ImageView) findViewById(R.id.main_gps);
		weatherIcon = (ImageView) findViewById(R.id.weather_icon);
		weatherText = (TextView) findViewById(R.id.weather_text);
		currentTemp = (TextView) findViewById(R.id.currentTemp);
		temp = (TextView) findViewById(R.id.temp);
		
		forecastItem1 = (LinearLayout) findViewById(R.id.forecast_item_1);
		forecastItem2 = (LinearLayout) findViewById(R.id.forecast_item_2);
		forecastItem3 = (LinearLayout) findViewById(R.id.forecast_item_3);
		forecastItem4 = (LinearLayout) findViewById(R.id.forecast_item_4);
		
		
		initForecastItemView(forecastItem1, foreCastItemViews1);
		initForecastItemView(forecastItem2, foreCastItemViews2);
		initForecastItemView(forecastItem3, foreCastItemViews3);
		initForecastItemView(forecastItem4, foreCastItemViews4);
	}

	public WeatherInfoMainView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WeatherInfoMainView(Context context) {
		this(context, null);
	}
	
	private void initForecastItemView(LinearLayout parent,
			ForeCastItemViews views) {
		views.dayText = (TextView) parent.findViewById(R.id.forecast_day);
		views.dateText = (TextView) parent.findViewById(R.id.forecast_date);
		views.iconImage = (ImageView) parent.findViewById(R.id.forecast_icon);
		views.textText = (TextView) parent.findViewById(R.id.forecast_text);
		views.weatherText = (TextView) parent
				.findViewById(R.id.forecast_weather);
	}

	public void bindView(String dateS, String nameS, int imgRes, String textS,
			String currentTempS, String tempS, boolean isGps) {
		date.setText(dateS);
		cityName.setText(nameS);
		weatherIcon.setImageResource(imgRes);
		weatherText.setText(textS);
		currentTemp.setText(currentTempS);
		temp.setText(tempS);
		gpsImage.setVisibility(isGps?View.VISIBLE:View.GONE);
	}

	public void updateForeCastItem(int index,
			WeatherInfo.Forecast forecast) {
		ForeCastItemViews views = null;
		switch (index) {
		case 1:
			views = foreCastItemViews1;
			break;
		case 2:
			views = foreCastItemViews2;
			break;
		case 3:
			views = foreCastItemViews3;
			break;
		case 4:
			views = foreCastItemViews4;
			break;
		default:
			break;
		}
		if (views == null) {
			return;
		}
		views.dayText.setText(forecast.getDay());
		views.dateText.setText(forecast.getDateShort() + "("+forecast.getDay()+")");
		views.textText.setText(forecast.getText());
		views.weatherText.setText(forecast.getLow() + "℃/" + forecast.getHigh()
				+ "℃");
		int code = Integer.parseInt(forecast.getCode());
		int resId;
		resId = WeatherDataUtil.getInstance().getWeatherImageResourceByCode(
				code, false, false);
		if (WeatherDataUtil.INVALID_WEAHTER_RESOURCE == resId) {
			resId = WeatherDataUtil.getInstance()
					.getWeatherImageResourceByText(forecast.getText(), false,
							false);
		}
		views.iconImage.setImageResource(resId);
	}
	
	class ForeCastItemViews {
		public TextView dayText;
		public TextView dateText;
		public ImageView iconImage;
		public TextView textText;
		public TextView weatherText;
	}
}
