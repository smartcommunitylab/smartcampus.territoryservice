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

import java.io.Serializable;

public class GenericStoryStep implements Serializable {
	private static final long serialVersionUID = -6566711582282772741L;

	private String poiId;
	private String note;

	public GenericStoryStep() {
		super();
	}
	public GenericStoryStep(String poiId, String note) {
		super();
		this.poiId = poiId;
		this.note = note;
	}
	public String getPoiId() {
		return poiId;
	}
	public void setPoiId(String poiId) {
		this.poiId = poiId;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
}
