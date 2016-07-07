package com.gweather.utils;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.gweather.app.CityInfo;

public class CityNameXMLParser extends DefaultHandler {
	private final static String TAG_CITY = "place";

	private final static String TAG_WOEID = "woeid";

	private final static String TAG_COUNTRY = "country";

	private final static String TAG_ADMIN1 = "admin1";

	private final static String TAG_ADMIN2 = "admin2";

	private final static String TAG_ADMIN3 = "admin3";

	private final static String TAG_NAME = "name";

	private final static String QNAME_TYPE = "type";
	
	private final static String TAG_SOUTHWEST = "southWest";
	private final static String TAG_NORTHEAST = "northEast";
	private final static String TAG_LAT = "latitude";
	private final static String TAG_LON = "longitude";
	
	private ArrayList<CityInfo> mCityInfoLit;
	private CityInfo cityInfo;
	private String content;

	private boolean skip = false;
	
	private boolean isSouthWest = false;
	private boolean isNorthEast = false;

	public CityNameXMLParser(ArrayList<CityInfo> cityInfoLit) {
		mCityInfoLit = cityInfoLit;
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);

		content = new String(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		skip = false;
		if (TAG_CITY.equals(localName)) {
			cityInfo = new CityInfo();
		} else if (TAG_ADMIN1.equals(localName) || TAG_ADMIN2.equals(localName)
				|| TAG_ADMIN3.equals(localName)) {
			if (attributes == null || attributes.getLength() == 0) {
				skip = true;
			}
		} else if(TAG_SOUTHWEST.equals(localName)) {
			isSouthWest = true;
		} else if (TAG_NORTHEAST.equals(localName)) {
			isNorthEast = true;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		super.endElement(uri, localName, qName);
		if (TAG_WOEID.equals(localName)) {
			cityInfo.setWoeid(content);
		} else if (TAG_COUNTRY.equals(localName)) {
			cityInfo.setCountry(content);
		} else if (TAG_ADMIN1.equals(localName)) {
			if(skip) {
				cityInfo.setAdmin1("");
			} else {
				cityInfo.setAdmin1(content);
			}
		} else if (TAG_ADMIN2.equals(localName)) {
			if(skip) {
				cityInfo.setAdmin2("");
			} else {
				cityInfo.setAdmin2(content);
			}
		} else if (TAG_ADMIN3.equals(localName)) {
			if(skip) {
				cityInfo.setAdmin3("");
			} else {
				cityInfo.setAdmin3(content);
			}
		} else if (TAG_NAME.equals(localName)) {
			cityInfo.setName(content);
		} else if(TAG_SOUTHWEST.equals(localName)) {
			isSouthWest = false;
		} else if (TAG_NORTHEAST.equals(localName)) {
			isNorthEast = false;
		} else if (TAG_LAT.equals(localName)) {
			if(isSouthWest) {
				cityInfo.getLocationInfo().setSouthWestLat(content);
			} else if(isNorthEast) {
				cityInfo.getLocationInfo().setNorthEastLat(content);
			}
		} else if (TAG_LON.equals(localName)) {
			if(isSouthWest) {
				cityInfo.getLocationInfo().setSouthWestLon(content);
			} else if(isNorthEast) {
				cityInfo.getLocationInfo().setNorthEastLon(content);
			}
		} else if (TAG_CITY.equals(localName)) {
			mCityInfoLit.add(cityInfo);
		}
	}
}
