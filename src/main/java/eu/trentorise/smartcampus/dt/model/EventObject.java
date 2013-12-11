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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.manager.ModerationManager;
import eu.trentorise.smartcampus.manager.ModerationManager.MODERATION_MODE;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.processor.EventProcessorImpl;

public class EventObject extends BaseDTObject {
	private static final long serialVersionUID = 388550207183035548L;

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

	private String poiId;

	private List<String> attending = new ArrayList<String>();
	private Integer attendees = 0;
	
	public EventObject() {
		super();
	}

	public String getPoiId() {
		return poiId;
	}

	public void setPoiId(String poiId) {
		this.poiId = poiId;
	}

	public List<String> getAttending() {
		return attending;
	}

	public void setAttending(List<String> attending) {
		this.attending = attending;
	}

	public Integer getAttendees() {
		return attendees;
	}

	public void setAttendees(Integer attendees) {
		this.attendees = attendees;
	}

	public void filterUserData(String userId) {
		super.filterUserData(userId);

		if (attending == null || attending.isEmpty()) return;
		if (attending.contains(userId)) setAttending(Collections.singletonList(userId));
		else setAttending(Collections.<String>emptyList());
	}

	public GenericEvent toGenericEvent(POIObject poi) {
		GenericEvent result = new GenericEvent();
		result.setId(getId());
		result.setSource(getSource());
		result.setTitle(getTitle());
		result.setType(getType());
		result.setDescription(getDescription());
		
		if (poi != null) {
			result.setPoiId(poi.getId());
			if (poi.getPoi() != null) {
				result.setAddressString(POIData.getAddressString(poi.getPoi()));
			}
		}
		if (getFromTime() != null){
			result.setFromTime(getFromTime());
			result.setFromTimeString(sdf.format(new Date(getFromTime())));
		}
		if (getToTime() != null){
			result.setToTime(getToTime());
			result.setToTimeString(sdf.format(new Date(getToTime())));
		}
		if (getTiming() != null) {
			result.setTiming(getTiming());
		}
		if (getCustomData() != null) {
			result.setCustomData(JsonUtils.toJSON(getCustomData()));
		}
		return result;
	}

	@Override
	public EventObject updateDO(String userId, DomainEngineClient client, GeoTimeObjectSyncStorage storage, ModerationManager moderator) throws Exception {
		return store(
				!moderator.getModerationMode().equals(MODERATION_MODE.PRE), 
				!moderator.getModerationMode().equals(MODERATION_MODE.DISABLED), 
				userId, 
				storage.getObjectById(getId(),EventObject.class), 
				client, storage, moderator);
	}

	@Override
	protected EventObject store(boolean toUpdate, boolean toModerate, String userId, BaseDTObject old, DomainEngineClient client, GeoTimeObjectSyncStorage storage, ModerationManager moderator) throws Exception {
		if (!toUpdate && !toModerate) return this;
		
		Map<String,Object> parameters = new HashMap<String, Object>();
		// TODO IN THIS WAY CAN MODIFY ONLY OWN OBJECTS, OTHERWISE ONLY TAGS IN COMMUNITY DATA
		String operation = null;
		if (!userId.equals(getCreatorId())) {
			operation = "updateCommunityData";
			if (getCommunityData() != null) {
				getCommunityData().setRating(null);
			}
			if (toModerate) {
				Map<String,Object> oldData = old.domainCommunityData();
				Map<String,Object> commData = domainCommunityData();
				moderator.moderateCommunityData(getId(), oldData, commData, userId);
			}
			parameters.put("newCommunityData",  domainCommunityData());
		} else {
			operation = "updateEvent";
			POIObject poi = storage.getObjectById(getPoiId(), POIObject.class);
			parameters.put("newData", Util.convert(toGenericEvent(poi), Map.class)); 
			parameters.put("newCommunityData",  domainCommunityData());
			
			if (toModerate) {
				moderator.moderateObject(old, this, userId);
			}
		}
		
		if (toUpdate) {
			client.invokeDomainOperation(
					operation, 
					getDomainType(), 
					alignedDomainId(client),
					parameters, null, null); 
			
			String oString = client.searchDomainObject(getDomainType(), alignedDomainId(client), null);
			DomainObject dObj = new DomainObject(oString);
			EventObject uObj = EventProcessorImpl.convertEventObject(dObj, storage);
//				storage.storeObject(uObj);
			uObj.filterUserData(userId);
			return uObj;
		}
		return this;

	} 
}
