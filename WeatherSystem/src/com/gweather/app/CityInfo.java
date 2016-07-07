package com.gweather.app;

public class CityInfo {
	private String woeid;
	private String country;
	private String admin1;
	private String admin2;
	private String admin3;
	private String name;
	private boolean isDefault;
	
	private LocationInfo locationInfo = new LocationInfo();
	
	
	public String getWoeid() {
		return woeid;
	}
	public void setWoeid(String woeid) {
		this.woeid = woeid;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getAdmin1() {
		return admin1;
	}
	public void setAdmin1(String admin1) {
		this.admin1 = admin1;
	}
	public String getAdmin2() {
		return admin2;
	}
	public void setAdmin2(String admin2) {
		this.admin2 = admin2;
	}
	public String getAdmin3() {
		return admin3;
	}
	public void setAdmin3(String admin3) {
		this.admin3 = admin3;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isDefault() {
		return isDefault;
	}
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
	
	public LocationInfo getLocationInfo() {
		return locationInfo;
	}
	
	@Override
	public String toString() {
		return name + ", " + (admin3.isEmpty() ? "" : (getAdmin3() + ", "))
				+ (admin2.isEmpty() ? "" : (getAdmin2() + ", "))
				+ (admin1.isEmpty() ? "" : (getAdmin1() + ", "))
				+ (country.isEmpty() ? "" : getCountry());
	}
}
