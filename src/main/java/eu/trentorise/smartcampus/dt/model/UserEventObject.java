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
import it.sayservice.platform.client.InvocationException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;


public class UserEventObject extends EventObject {
	private static final long serialVersionUID = -7126784929735877598L;

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	
	public UserEventObject() {
		super();
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
		return result;
	}

	@Override
	public void createDO(DomainEngineClient client, GeoTimeObjectSyncStorage storage) throws NotFoundException, DataException, InvocationException {
		Map<String,Object> parameters = new HashMap<String, Object>();
		parameters.put("creator", getCreatorId());
		if (getPoiId() == null) {
			throw new IllegalArgumentException("Empty POI");
		}
		POIObject poi = storage.getObjectById(getPoiId(), POIObject.class);
		parameters.put("data", Util.convert(toGenericEvent(poi), Map.class));
		parameters.put("communityData",  domainCommunityData());
		client.invokeDomainOperation(
				"createEvent", 
				"eu.trentorise.smartcampus.domain.discovertrento.UserEventFactory", 
				"eu.trentorise.smartcampus.domain.discovertrento.UserEventFactory.0", 
				parameters, null, null);
	}


}
