package com.gweather.utils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;

import com.gweather.app.WeatherInfo;
import com.gweather.app.WeatherInfo.Forecast;



public class WeatherXMLParser extends DefaultHandler {
	private static final String TAG = "Gweather.WeatherXMLParser";
	
	private final static String TAG_CONDITION = "condition";
	private final static String TAG_FORECAST = "forecast";
	private final static String QNAME_CODE = "code";
	private final static String QNAME_DATE = "date";
	private final static String QNAME_DAY = "day";
	private final static String QNAME_TMP = "temp";
	private final static String QNAME_HIGH = "high";
	private final static String QNAME_LOW = "low";
	private final static String QNAME_TEXT = "text";
	
	private Context mContext;
	private WeatherInfo mWeatherInfo;
	private Forecast forecast;
	
	private String woeid;
	
	public WeatherXMLParser(Context context, WeatherInfo info, String woeid) {
		mContext = context;
		mWeatherInfo = info;
		this.woeid = woeid;
	}
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
	}
	
	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		int weatherCode;
		int textRes;
		if (null == mWeatherInfo.getCondition().getCode()) {
			Log.d(TAG, "endDocument-code NULL");
		} else {
			weatherCode = Integer.valueOf(mWeatherInfo.getCondition().getCode());
			textRes = WeatherDataUtil.getInstance().getWeatherTextResByCode(weatherCode);
			if (WeatherDataUtil.INVALID_WEAHTER_RESOURCE != textRes) {
				mWeatherInfo.getCondition().setText(mContext.getResources().getString(textRes));
			}
			
			for (WeatherInfo.Forecast forecast : mWeatherInfo.getForecasts()) {
				weatherCode = Integer.valueOf(forecast.getCode());
				textRes = WeatherDataUtil.getInstance().getWeatherTextResByCode(weatherCode);
				if (WeatherDataUtil.INVALID_WEAHTER_RESOURCE != textRes) {
					forecast.setText(mContext.getResources().getString(textRes));
				}
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);

		if (TAG_CONDITION.equals(localName)) {
			mWeatherInfo.setWoeid(woeid);
			for (int i = 0; i < attributes.getLength(); i++) {
				String qn = attributes.getQName(i);
				if(QNAME_CODE.equals(qn)) {
					mWeatherInfo.getCondition().setCode(attributes.getValue(i));
				} else if (QNAME_DATE.equals(qn)) {
					mWeatherInfo.getCondition().setDate(attributes.getValue(i));
				} else if (QNAME_TMP.equals(qn)) {
					mWeatherInfo.getCondition().setTemp(attributes.getValue(i));
				} else if (QNAME_TEXT.equals(qn)) {
					mWeatherInfo.getCondition().setText(attributes.getValue(i));
					mWeatherInfo.setUpdateTime(System.currentTimeMillis());
				}
			};
		} else if (TAG_FORECAST.equals(localName)) {
			forecast = mWeatherInfo.new Forecast();
			for (int i = 0; i < attributes.getLength(); i++) {
				String qn = attributes.getQName(i);
				if(QNAME_CODE.equals(qn)) {
					forecast.setCode(attributes.getValue(i));
				} else if (QNAME_DATE.equals(qn)) {
					forecast.setDate(attributes.getValue(i));
				} else if (QNAME_DAY.equals(qn)) {
					forecast.setDay(attributes.getValue(i));
				} else if (QNAME_HIGH.equals(qn)) {
					forecast.setHigh(attributes.getValue(i));
				} else if (QNAME_LOW.equals(qn)) {
					forecast.setLow(attributes.getValue(i));
				} else if (QNAME_TEXT.equals(qn)) {
					forecast.setText(attributes.getValue(i));
				}
			}
			mWeatherInfo.getForecasts().add(forecast);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		super.endElement(uri, localName, qName);
		
	}
}
