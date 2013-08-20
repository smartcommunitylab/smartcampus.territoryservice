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
package eu.trentorise.smartcampus.data;

import eu.trentorise.smartcampus.presentation.storage.sync.mongo.SyncObjectBean;

public class GeoTimeSyncObjectBean extends SyncObjectBean {

	private double[] location;
	private Long fromTime;
	private Long toTime;
	
	public GeoTimeSyncObjectBean() {
		super();
	}

	public double[] getLocation() {
		return location;
	}

	public void setLocation(double[] location) {
		this.location = location;
	}


	public Long getFromTime() {
		return fromTime;
	}

	public void setFromTime(Long fromTime) {
		this.fromTime = fromTime;
	}

	public Long getToTime() {
		return toTime;
	}

	public void setToTime(Long toTime) {
		this.toTime = toTime;
	}
}
