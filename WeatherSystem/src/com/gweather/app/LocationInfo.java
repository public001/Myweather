package com.gweather.app;

public class LocationInfo {
	private double lat;
	private double lon;
	private double southWestLat;
	private double southWestLon;
	private double northEastLat;
	private double northEastLon;
	
	
	public double getLat() {
		return lat;
	}
	public void setLat(String lat) {
		this.lat = Double.valueOf(lat);
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(String lon) {
		this.lon = Double.valueOf(lon);
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	public double getSouthWestLat() {
		return southWestLat;
	}
	public void setSouthWestLat(String southWestLat) {
		this.southWestLat = Double.valueOf(southWestLat);
	}
	public double getSouthWestLon() {
		return southWestLon;
	}
	public void setSouthWestLon(String southWestLon) {
		this.southWestLon = Double.valueOf(southWestLon);
	}
	public double getNorthEastLat() {
		return northEastLat;
	}
	public void setNorthEastLat(String northEastLat) {
		this.northEastLat = Double.valueOf(northEastLat);
	}
	public double getNorthEastLon() {
		return northEastLon;
	}
	public void setNorthEastLon(String northEastLon) {
		this.northEastLon = Double.valueOf(northEastLon);
	}
	
	@Override
	public String toString() {
		return "lat:"+lat+", lon:"+lon+", southWestLat:"+southWestLat+", southWestLon:"+southWestLon
				+", northEastLat:"+northEastLat+", northEastLon:"+northEastLon;
	}
}
