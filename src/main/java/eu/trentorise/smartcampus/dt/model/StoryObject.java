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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.manager.ModerationManager;
import eu.trentorise.smartcampus.manager.ModerationManager.MODERATION_MODE;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.processor.EventProcessorImpl;

public class StoryObject extends BaseDTObject {

	private static final long serialVersionUID = -5123355788738143639L;
	private List<StepObject> steps = new ArrayList<StepObject>();
	private List<String> attending = new ArrayList<String>();
	private Integer attendees = 0;
	
	
	public StoryObject() {
		super();
	}

	public List<StepObject> getSteps() {
		return steps;
	}


	public void setSteps(List<StepObject> steps) {
		this.steps = steps;
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

	@Override
	public Set<String> checkEquals(Object obj) {
		Set<String> res = super.checkEquals(obj);
		if (res.isEmpty()) res = new HashSet<String>();
		StoryObject other = (StoryObject) obj;
		if (getSteps() != null) {
			if (!getSteps().equals(other.getSteps())) 
				res.add("steps");
		} else if (other.getSteps() != null) { 
				res.add("steps");
		}
		return res;
	}
	
	@Override
	public StoryObject updateDO(String userId, DomainEngineClient client, GeoTimeObjectSyncStorage storage, ModerationManager moderator) throws Exception {
		return store(
				!moderator.getModerationMode().equals(MODERATION_MODE.PRE), 
				!moderator.getModerationMode().equals(MODERATION_MODE.DISABLED), 
				userId, 
				storage.getObjectById(getId(),StoryObject.class), 
				client, storage, moderator);
	}

	protected StoryObject store(boolean toUpdate, boolean toModerate, String userId, BaseDTObject old, DomainEngineClient client, GeoTimeObjectSyncStorage storage, ModerationManager moderator) throws Exception {
		if (!toUpdate && !toModerate) return this;
		Map<String,Object> parameters = new HashMap<String, Object>();
		String operation = null;
		// TODO IN THIS WAY CAN MODIFY ONLY OWN OBJECTS, OTHERWISE EXCEPTION
		if (!userId.equals(getCreatorId())) {
			throw new SecurityException();
//			operation = "updateCommunityData";
//			if (getCommunityData() != null) {
//				getCommunityData().setRating(null);
//			}
//			parameters.put("newCommunityData",  domainCommunityData());
//			if (toModerate) {
//				moderator.moderateCommunityData(getId(), old, this, userId);
//			}
		} else {
			operation = "updateStory";
			parameters.put("newData", Util.convert(toGenericStory(), Map.class)); 
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
			StoryObject uObj = EventProcessorImpl.convertStoryObject(dObj, storage);
	//			storage.storeObject(uObj);
			
			uObj.filterUserData(userId);
			return uObj;
		}
		return this;
	}
	
	public GenericStory toGenericStory() {
		GenericStory result = new GenericStory();
		result.setId(getId());
		result.setSource(getSource());
		result.setTitle(getTitle());
		result.setType(getType());
		result.setDescription(getDescription());

		if (getSteps() != null) {
			result.setSteps(new GenericStoryStep[getSteps().size()]);
			for (int i = 0; i < getSteps().size(); i++) {
				result.getSteps()[i] = new GenericStoryStep(getSteps().get(i).getPoiId(), getSteps().get(i).getNote());
			}
		} else {
			result.setSteps(new GenericStoryStep[0]);
		}
		if (getCustomData() != null) {
			result.setCustomData(JsonUtils.toJSON(getCustomData()));
		}
		
		return result;
	}

}
