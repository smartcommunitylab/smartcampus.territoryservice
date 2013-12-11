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

import java.util.Map;

/**
 * Container for any moderated content. Refers to the user and
 * object, and contains both the new and old value.
 * 
 * @author raman
 *
 */
public class ModerationItem {

	private String id;
	private String type;
	private String userId;
	private String objectId;
	private Map<String,Object> oldValue;
	private Map<String,Object> newValue;
	private String objectType;
	
	public ModerationItem() {
		super();
	}

	public ModerationItem(String objectId, String userId, String type, Map<String, Object> oldValue, Map<String, Object> newValue, String objectType) {
		super();
		this.objectId = objectId;
		this.userId = userId;
		this.type = type;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.objectType = objectType;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @return the objectId
	 */
	public String getObjectId() {
		return objectId;
	}
	/**
	 * @param objectId the objectId to set
	 */
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	/**
	 * @return the oldValue
	 */
	public Map<String, Object> getOldValue() {
		return oldValue;
	}
	/**
	 * @param oldValue the oldValue to set
	 */
	public void setOldValue(Map<String, Object> oldValue) {
		this.oldValue = oldValue;
	}
	/**
	 * @return the newValue
	 */
	public Map<String, Object> getNewValue() {
		return newValue;
	}
	/**
	 * @param newValue the newValue to set
	 */
	public void setNewValue(Map<String, Object> newValue) {
		this.newValue = newValue;
	}

	/**
	 * @return the objectType
	 */
	public String getObjectType() {
		return objectType;
	}

	/**
	 * @param objectType the objectType to set
	 */
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}
	
	
}
