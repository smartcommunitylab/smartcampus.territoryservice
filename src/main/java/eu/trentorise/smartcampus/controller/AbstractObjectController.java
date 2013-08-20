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
package eu.trentorise.smartcampus.controller;

import it.sayservice.platform.client.DomainEngineClient;
import it.sayservice.platform.client.DomainObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.geo.Circle;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.ObjectFilter;
import eu.trentorise.smartcampus.resourceprovider.controller.SCController;
import eu.trentorise.smartcampus.resourceprovider.model.AuthServices;

public class AbstractObjectController extends SCController {

	private static final String SEARCH_FILTER_PARAM = "filter";

	@Autowired
	protected GeoTimeObjectSyncStorage storage;

	protected Log logger = LogFactory.getLog(this.getClass());

	@Autowired
	private AuthServices services;
	@Override
	protected AuthServices getAuthServices() {
		return services;
	}

	protected DomainObject upgradeDO(BaseDTObject obj, DomainEngineClient client) throws Exception {
		if (obj.getDomainId() ==  null) {
			Map<String,Object> params = new HashMap<String, Object>();
			params.put("id", obj.getId());
			System.err.println("searching "+obj.getDomainType()+","+params);
			List<String> list = client.searchDomainObjects(obj.getDomainType(), params, null);
			System.err.println("found "+list);
			if (list != null && !list.isEmpty()) {
				DomainObject dobj = new DomainObject(list.get(0));
				obj.setDomainId(dobj.getId());
				return dobj;
			}
		}
		return null;
	}
	
	public <T extends BaseDTObject> List<T> getAllObject(HttpServletRequest request, Class<T> cls) throws Exception {
		String userId = getUserId();
		try {
			ObjectFilter filterObj = null;
			Map<String, Object> criteria = null;
			String filter = request.getParameter(SEARCH_FILTER_PARAM);
			if (filter != null) {
				filterObj = new ObjectMapper().readValue(filter, ObjectFilter.class);
				criteria = filterObj.getCriteria() != null ? filterObj.getCriteria() : new HashMap<String, Object>();
				if (filterObj.getTypes() != null && !filterObj.getTypes().isEmpty()) {
					criteria.put("type",
							Collections.singletonMap("$in",
									filterObj.getTypes()));
				}
				if (filterObj.isMyObjects()) {
					criteria.put("attending", userId);
				}
			} else {
				filterObj = new ObjectFilter();
				criteria = Collections.emptyMap();
			}

			if (filterObj.getSkip() == null) {
				filterObj.setSkip(0);
			}
			if (filterObj.getLimit() == null || filterObj.getLimit() < 0) {
				filterObj.setLimit(100);
			} 

			Circle circle = null;
			if (filterObj.getCenter() != null && filterObj.getRadius() != null) {
				circle = new Circle(filterObj.getCenter()[0],
						filterObj.getCenter()[1], filterObj.getRadius());
			}
			List<T> objects = null;

			objects = storage.searchObjects((Class<T>) cls,
					circle, filterObj.getText(), filterObj.getFromTime(),
					filterObj.getToTime(), criteria, filterObj.getSort(),
					filterObj.getLimit(), filterObj.getSkip());

			if (objects != null) {
				return objects;
			}
		} catch (Exception e) {
			logger.error("failed to find objects: " + e.getMessage());
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	/**
	 * Return either user Id, when the token is user token, or client ID when the token is
	 * client token.
	 */
	@Override
	protected String getUserId() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) return ((UserDetails) principal).getUsername();
		return principal.toString();
	}

	
}
