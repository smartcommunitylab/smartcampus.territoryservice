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

import java.util.HashMap;
import java.util.Map;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.manager.ModerationManager;
import eu.trentorise.smartcampus.manager.ModerationManager.MODERATION_MODE;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.processor.EventProcessorImpl;

public class POIObject extends BaseDTObject {
	private static final long serialVersionUID = 3377022799304541031L;
	
	private POIData poi;

	public POIObject() {
		super();
	}

	public POIData getPoi() {
		return poi;
	}

	public void setPoi(POIData poi) {
		this.poi = poi;
	}

	@Override
	public double[] getLocation() {
		if (super.getLocation() != null) return super.getLocation();
		if (getPoi() != null) return new double[]{getPoi().getLatitude(),getPoi().getLongitude()};
		return null;
	}

	public GenericPOI toGenericPOI() {
		GenericPOI result = new GenericPOI();
		result.setPoiData(getPoi());
		result.setId(getId());
		result.setSource(getSource());
		result.setTitle(getTitle());
		result.setType(getType());
		result.setDescription(getDescription());
		if (getCustomData() != null) {
			result.setCustomData(JsonUtils.toJSON(getCustomData()));
		}
		return result;
	}

	@Override
	public POIObject updateDO(String userId, DomainEngineClient client, GeoTimeObjectSyncStorage storage, ModerationManager moderator) throws Exception {
		return store(
				!moderator.getModerationMode().equals(MODERATION_MODE.PRE), 
				!moderator.getModerationMode().equals(MODERATION_MODE.DISABLED), 
				userId, 
				storage.getObjectById(getId(),POIObject.class), 
				client, storage, moderator);
	}

	protected POIObject store(boolean toUpdate, boolean toModerate, String userId, BaseDTObject old, DomainEngineClient client, GeoTimeObjectSyncStorage storage, ModerationManager moderator) throws Exception {
		if (!toUpdate && !toModerate) return this;
		Map<String,Object> parameters = new HashMap<String, Object>(1);
		String operation = null;
		// TODO IN THIS WAY CAN MODIFY ONLY OWN OBJECTS, OTHERWISE EXCEPTION
		if (!userId.equals(getCreatorId())) {
			throw new SecurityException();
//			operation = "updateCommunityData";
//			if (getCommunityData() != null) {
//				getCommunityData().setRating(null);
//			}
//			if (toModerate) {
//				moderator.moderateCommunityData(getId(), old, this, userId);
//			}
//			parameters.put("newCommunityData",  domainCommunityData());
		} else {
			operation = "updatePOI";
			parameters.put("newData", Util.convert(toGenericPOI(), Map.class)); 
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
			POIObject uObj = EventProcessorImpl.convertPOIObject(dObj);
	//			storage.storeObject(uObj);
			uObj.filterUserData(userId);
			return uObj;
		}
		return this;
	}
}
