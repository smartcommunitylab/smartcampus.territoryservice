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

import java.util.HashMap;
import java.util.Map;

import it.sayservice.platform.client.DomainEngineClient;
import it.sayservice.platform.client.InvocationException;
import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;


public class UserPOIObject extends POIObject {
	private static final long serialVersionUID = 1846284185357564507L;

	public UserPOIObject() {
		super();
	}

	@Override
	public void createDO(DomainEngineClient client, GeoTimeObjectSyncStorage storage) throws NotFoundException, DataException, InvocationException {
		Map<String,Object> parameters = new HashMap<String, Object>();
		parameters.put("creator", getCreatorId());
		parameters.put("data", Util.convert(toGenericPOI(), Map.class));
		parameters.put("communityData",  domainCommunityData());
		client.invokeDomainOperation(
				"createPOI", 
				"eu.trentorise.smartcampus.domain.discovertrento.UserPOIFactory", 
				"eu.trentorise.smartcampus.domain.discovertrento.UserPOIFactory.0", 
				parameters, null, null);
	}

	
}
