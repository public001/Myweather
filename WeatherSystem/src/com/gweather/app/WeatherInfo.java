package com.gweather.app;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class WeatherInfo implements Parcelable {
	private String woeid;
	private String name;
	private long updateTime;
	private boolean isGps;
	
	private Condition condition = new Condition();
	private List<Forecast> forecasts = new ArrayList<Forecast>();

	public String getWoeid() {
		return woeid;
	}
	
	public void setWoeid(String woeid) {
		this.woeid = woeid;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isGps() {
		return isGps;
	}

	public void setGps(boolean isGps) {
		this.isGps = isGps;
	}

	public long getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
	
	public Condition getCondition() {
		return condition;
	}
	
	public List<Forecast> getForecasts() {
		return forecasts;
	}

	public class Condition {
		private int index;
		private String code;
		private String date;
		private String temp;
		private String text;
		
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public String getDate() {
			return date;
		}
		public void setDate(String date) {
			this.date = date;
		}
		public String getTemp() {
			return temp;
		}
		public void setTemp(String temp) {
			this.temp = temp;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		
		
	}
	
	public class Forecast {
		private int index;
		private String code;
		private String date;
		private String dateShort = "";
		private String day;
		private String high;
		private String low;
		private String text;
		
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
		public String getDate() {
			return date;
		}
		public void setDate(String date) {
			this.date = date;
			String[] sa = date.split(" ");
			this.dateShort = sa[0] + " " + sa[1];
		}
		public String getDateShort() {
			return dateShort;
		}
		public String getDay() {
			return day;
		}
		public void setDay(String day) {
			this.day = day;
		}
		public String getHigh() {
			return high;
		}
		public void setHigh(String high) {
			this.high = high;
		}
		public String getLow() {
			return low;
		}
		public void setLow(String low) {
			this.low = low;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
	}
	
	public void copyInfo(WeatherInfo info) {
		setUpdateTime(info.getUpdateTime());
		condition.setCode(info.getCondition().getCode());
		condition.setDate(info.getCondition().getDate());
		condition.setIndex(info.getCondition().getIndex());
		condition.setTemp(info.getCondition().getTemp());
		condition.setText(info.getCondition().getText());
		
		int forecastCount = MainActivity.FORECAST_DAY;
		forecasts.clear();
		Forecast forecast;
		for (int i=0; i<forecastCount; i++) {
			forecast = new Forecast();
			forecast.setCode(info.getForecasts().get(i).getCode());
			forecast.setDate(info.getForecasts().get(i).getDate());
			forecast.setDay(info.getForecasts().get(i).getDay());
			forecast.setHigh(info.getForecasts().get(i).getHigh());
			forecast.setIndex(info.getForecasts().get(i).getIndex());
			forecast.setLow(info.getForecasts().get(i).getLow());
			forecast.setText(info.getForecasts().get(i).getText());
			forecasts.add(forecast);
		}
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}
}
