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

import it.sayservice.platform.client.DomainEngineClient;
import it.sayservice.platform.client.DomainObject;
import it.sayservice.platform.client.InvocationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MethodNotSupportedException;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.presentation.data.BasicObject;

public class BaseDTObject extends BasicObject {
	private static final long serialVersionUID = 3589900794339644582L;
	// common fields
	private String domainType;
	private String domainId;
	private String description = null;
	private String title = null;
	private String source = null; // service 'source' of the object

	// semantic entity
	private String entityId = null;
	
	// only for user-created objects
	private String creatorId = null;
	private String creatorName = null;

	// community data
	private CommunityData communityData = null;
	
	// categorization
	private String type = null;

	// common data
	private double[] location;
	private Long fromTime;
	private Long toTime;
	private String timing;

	private Map<String,Object> customData;
	
	public BaseDTObject() {
		super();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
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

	public String getDomainType() {
		return domainType;
	}

	public void setDomainType(String domainType) {
		this.domainType = domainType;
	} 

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	public String getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public CommunityData getCommunityData() {
		return communityData;
	}

	public void setCommunityData(CommunityData communityData) {
		this.communityData = communityData;
	}

	public Map<String, Object> getCustomData() {
		return customData;
	}

	public void setCustomData(Map<String, Object> customData) {
		this.customData = customData;
	}

	public String getTiming() {
		return timing;
	}

	public void setTiming(String timing) {
		this.timing = timing;
	}

	public void filterUserData(String userId) {
		CommunityData.filterUserData(communityData, userId);
	}

	@SuppressWarnings("unchecked")
	public Map<String,Object> domainCommunityData() {
		if (communityData == null) {
			communityData = new CommunityData();
		}
		Map<String,Object> map = Util.convert(communityData, Map.class);
		map.remove("ratingsCount");
		map.remove("followsCount");
		map.remove("ratings");
		map.remove("rating");
		if (communityData.getRating() != null) {
			List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
			for (String key : communityData.getRating().keySet()) {
				Map<String,Object> m = new HashMap<String, Object>(2); 
				m.put("user", key);
				m.put("value", communityData.getRating().get(key));
				list.add(m);
			}
			map.put("ratings", list);
		} else {
			map.put("ratings", Collections.emptyList());
		}
		return map;
	}

	public Set<String> checkEquals(Object obj) {
		if (this == obj || obj == null || getClass() != obj.getClass()) 
			return Collections.emptySet();
		
		Set<String> res = new HashSet<String>();
		BaseDTObject other = (BaseDTObject) obj;
		if (fromTime == null) {
			if (other.fromTime != null)
				res.add("fromTime");
		} else if (!fromTime.equals(other.fromTime))
			res.add("fromTime");
		if (!Arrays.equals(location, other.location))
			res.add("location");
		if (timing == null) {
			if (other.timing != null)
				res.add("timing");
		} else if (!timing.equals(other.timing))
			res.add("timing");
		if (toTime == null) {
			if (other.toTime != null)
				res.add("toTime");
		} else if (!toTime.equals(other.toTime))
			res.add("toTime");
		return res;
	}

	/**
	 * Check the domain: if the object exists, align and return the domainId.
	 * If the object is not exist in domain, create it.
	 * @param client
	 * @param storage
	 * @return
	 * @throws Exception
	 */
	public void alignDO(DomainEngineClient client, GeoTimeObjectSyncStorage storage) throws Exception {
		if (domainId ==  null) {
			Map<String,Object> params = new HashMap<String, Object>();
			params.put("id", getId());
			List<String> list = client.searchDomainObjects(getDomainType(), params, null);
			if (list != null && !list.isEmpty()) {
				DomainObject dobj = new DomainObject(list.get(0));
				setDomainId(dobj.getId());
				storage.storeObject(this);
			} else {
				createDO(client, storage);
			}
		}
	}

	/**
	 * Create object in domain
	 * @param client
	 * @param storage
	 * @throws DataException 
	 * @throws NotFoundException 
	 * @throws InvocationException 
	 * @throws MethodNotSupportedException 
	 */
	public void createDO(DomainEngineClient client, GeoTimeObjectSyncStorage storage) throws NotFoundException, DataException, InvocationException, MethodNotSupportedException {
		throw new MethodNotSupportedException("Cannot create DO of this type");
	}

	/**
	 * Read domain ID from the domain if the object is not yet aligned.
	 * @param client
	 * @return
	 * @throws Exception 
	 */
	public String alignedDomainId(DomainEngineClient client) throws Exception {
		if (domainId == null) {
			Map<String,Object> params = Collections.<String,Object>singletonMap("id", getId());
			List<String> list = client.searchDomainObjects(getDomainType(), params, null);
			if (list != null && !list.isEmpty()) {
				DomainObject dobj = new DomainObject(list.get(0));
				setDomainId(dobj.getId());
			} else {
				throw new NotFoundException("No domain object with id "+getId() +" exists.");
			}			
		}
		return domainId;
	} 
}
