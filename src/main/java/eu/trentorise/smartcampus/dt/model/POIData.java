/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.dt.model;

import it.sayservice.platform.core.message.Core.POI;

import java.io.Serializable;

public class POIData implements Serializable {
	private static final long serialVersionUID = 8503435300981426724L;

	private double longitude;
	private double latitude;
	private String poiId;
	private String datasetId;
	private String street;
	private String city;
	private String postalCode;
	private String region;
	private String state;
	private String country;
	private String[] tags;
	
	public POIData() {
		super();
	}
	
	public POIData(POI poi) {
		if (poi.hasCoordinate()) {
			longitude = poi.getCoordinate().getLongitude();
			latitude = poi.getCoordinate().getLatitude();
		}
		if (poi.getTagsCount()>0) {
			tags = poi.getTagsList().toArray(new String[poi.getTagsCount()]);
		}
		poiId = poi.getPoiId();
		datasetId = poi.getDatasetId();
		if (poi.hasAddress()) {
			street = poi.getAddress().getStreet();
			city = poi.getAddress().getCity();
			postalCode = poi.getAddress().getPostalCode();
			region = poi.getAddress().getRegion();
			country = poi.getAddress().getCountry();
			state = poi.getAddress().getState();
		}
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public String getPoiId() {
		return poiId;
	}

	public void setPoiId(String poiId) {
		this.poiId = poiId;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String[] getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		this.tags = tags;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}

	public static String getAddressString(POIData data) {
		if (data == null) return "";
		String s = "";
		if (data.street != null) s += data.street+" ";
		if (data.city != null) s += data.city + " ";
//		if (data.postalCode != null) s += data.postalCode + " ";
//		if (data.region != null) s += data.region + " ";
//		if (data.country != null) s += data.country;
		return s.trim();
	}
}
