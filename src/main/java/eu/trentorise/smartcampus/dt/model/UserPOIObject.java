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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.manager.ModerationManager;
import eu.trentorise.smartcampus.manager.ModerationManager.MODERATION_MODE;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.util.Util;


public class UserPOIObject extends POIObject {
	private static final long serialVersionUID = 1846284185357564507L;

	private Log logger = LogFactory.getLog(getClass());

	public UserPOIObject() {
		super();
	}

	@Override
	public void createDO(String userId, DomainEngineClient client, GeoTimeObjectSyncStorage storage, ModerationManager moderator) throws Exception {
		POIObject tmp = null;
		try {
			if (getId() == null) {
				setId(new ObjectId().toString());
				setCreatorId(userId);
				setDomainType("eu.trentorise.smartcampus.domain.discovertrento.UserPOIObject");
				if (!moderator.getModerationMode().equals(MODERATION_MODE.DISABLED)) {
					moderator.moderateObject(null, this, userId);
				}
				if (moderator.getModerationMode().equals(MODERATION_MODE.PRE)) {
					markInvisible();
				}
				tmp =  Util.convert(this, POIObject.class);
				storage.storeObject(tmp);
			}
			Map<String,Object> parameters = new HashMap<String, Object>();
			parameters.put("creator", getCreatorId());
			parameters.put("data", Util.convert(toGenericPOI(), Map.class));
			parameters.put("communityData",  domainCommunityData());
			client.invokeDomainOperation(
					"createPOI", 
					"eu.trentorise.smartcampus.domain.discovertrento.UserPOIFactory", 
					"eu.trentorise.smartcampus.domain.discovertrento.UserPOIFactory.0", 
					parameters, null, null);
		} catch (Exception e) {
			logger.error("Failed to create userPOI: "+e.getMessage());
			e.printStackTrace();
			try {
				if (tmp != null) storage.deleteObject(tmp);
			} catch (DataException e1) {
				logger.error("Failed to cleanup userPOI: "+e1.getMessage());
			}
			throw e;
		} 

		
		
	}

	@Override
	public void deleteDO(DomainEngineClient client, GeoTimeObjectSyncStorage storage) throws Exception {
		if (alignedDomainId(client) != null) {
			client.invokeDomainOperation(
					"deletePOI", 
					getDomainType(), 
					alignedDomainId(client),
					Collections.<String,Object>emptyMap(), null, null); 
			storage.deleteObjectById(getId());
		}
	}
	
}
