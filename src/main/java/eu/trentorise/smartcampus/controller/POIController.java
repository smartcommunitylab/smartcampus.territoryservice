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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.dt.model.UserPOIObject;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.presentation.data.BasicObject;
import eu.trentorise.smartcampus.processor.EventProcessorImpl;

@Controller
public class POIController extends AbstractObjectController {

	@Autowired
	private DomainEngineClient domainEngineClient; 
	
	@RequestMapping(method = RequestMethod.POST, value="/pois")
	public ResponseEntity<UserPOIObject> createPOI(HttpServletRequest request, @RequestBody Map<String,Object> objMap) {
		UserPOIObject obj = Util.convert(objMap, UserPOIObject.class);
		POIObject tmp = null;
		obj.setId(new ObjectId().toString());
		try {
			validatePOI(obj);
		} catch (DataException e1) {
			logger.error("Failed to create userPOI: "+e1.getMessage());
			e1.printStackTrace();
			return new ResponseEntity<UserPOIObject>(HttpStatus.METHOD_FAILURE);
		}
		
		try {
			obj.setCreatorId(getUserId());
			obj.setDomainType("eu.trentorise.smartcampus.domain.discovertrento.UserPOIObject");
			tmp =  Util.convert(obj, POIObject.class);
			storage.storeObject(tmp);
			
			Map<String,Object> parameters = new HashMap<String, Object>();
			parameters.put("creator", getUserId());
			parameters.put("data", Util.convert(obj.toGenericPOI(), Map.class));
			parameters.put("communityData",  obj.domainCommunityData());
			domainEngineClient.invokeDomainOperation(
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
			return new ResponseEntity<UserPOIObject>(HttpStatus.METHOD_FAILURE);
		} 
		return new ResponseEntity<UserPOIObject>(obj,HttpStatus.OK);
	}

	private void validatePOI(UserPOIObject obj) throws DataException {
		if (obj.getLocation() == null || obj.getTitle() == null || obj.getType() == null) {
			throw new DataException("Incomplete data object");
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("title", obj.getTitle());
		List<POIObject> list = storage.searchObjects(POIObject.class, map );
		if (list != null && !list.isEmpty() && !list.get(0).getId().equals(obj.getId())) throw new DataException("Data object not unique: "+obj.getTitle());
	}

	@RequestMapping(method = RequestMethod.PUT, value="/pois/{id:.+}")
	public ResponseEntity<POIObject> updatePOI(HttpServletRequest request, @RequestBody Map<String,Object> objMap, @PathVariable String id) {
		UserPOIObject obj = Util.convert(objMap, UserPOIObject.class);
		try {
			validatePOI(obj);
		} catch (DataException e1) {
			logger.error("Failed to create userPOI: "+e1.getMessage());
			e1.printStackTrace();
			return new ResponseEntity<POIObject>(HttpStatus.METHOD_FAILURE);
		}
		
		Map<String,Object> parameters = new HashMap<String, Object>(1);
		try {
			String operation = null;
			// TODO IN THIS WAY CAN MODIFY ONLY OWN OBJECTS, OTHERWISE ONLY TAGS IN COMMUNITY DATA
			if (!getUserId().equals(obj.getCreatorId())) {
				operation = "updateCommunityData";
				if (obj.getCommunityData() != null) {
					obj.getCommunityData().setRating(null);
				}
				parameters.put("newCommunityData",  obj.domainCommunityData());
			} else {
				operation = "updatePOI";
				parameters.put("newData", Util.convert(obj.toGenericPOI(), Map.class)); 
				parameters.put("newCommunityData",  obj.domainCommunityData());
			}
		
			if (obj.getDomainId() ==  null) {
				DomainObject dobj = upgradeDO(obj, domainEngineClient);
				if (dobj != null) {
					POIObject newObj = EventProcessorImpl.convertPOIObject(dobj);
					obj.setEntityId(newObj.getEntityId());
				}
			}

			domainEngineClient.invokeDomainOperation(
					operation, 
					obj.getDomainType(), 
					obj.getDomainId(),
					parameters, null, null); 
			
			String oString = domainEngineClient.searchDomainObject(obj.getDomainType(), obj.getDomainId(), null);
			DomainObject dObj = new DomainObject(oString);
			POIObject uObj = EventProcessorImpl.convertPOIObject(dObj);
			storage.storeObject(uObj);
			
			uObj.filterUserData(getUserId());
			
			return new ResponseEntity<POIObject>(uObj,HttpStatus.OK);

		} catch (Exception e) {
			logger.error("Failed to update userPOI: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<POIObject>(HttpStatus.METHOD_FAILURE);
		}
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/pois/{id:.+}")
	public ResponseEntity<UserPOIObject> deletePOI(HttpServletRequest request, @PathVariable String id) {

		POIObject poi = null;
		try {
			poi = storage.getObjectById(id,POIObject.class);
			// CAN DELETE ONLY OWN OBJECTS
			if (!getUserId().equals(poi.getCreatorId())) {
				logger.error("Attempt to delete not owned object. User "+getUserId()+", object "+poi.getId());
				return new ResponseEntity<UserPOIObject>(HttpStatus.METHOD_FAILURE);
			}
		} catch (NotFoundException e) {
			return new ResponseEntity<UserPOIObject>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to delete userPOI: "+e.getMessage());
			return new ResponseEntity<UserPOIObject>(HttpStatus.METHOD_FAILURE);
		}
		try {
			if (poi.getDomainId() == null) {
				upgradeDO(poi, domainEngineClient);
			}
			if (poi.getDomainId() != null) {
				domainEngineClient.invokeDomainOperation(
						"deletePOI", 
						poi.getDomainType(), 
						poi.getDomainId(),
						new HashMap<String, Object>(0), null, null); 
				
				storage.deleteObject(poi);
			}

		} catch (Exception e) {
			logger.error("Failed to delete userPOI: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<UserPOIObject>(HttpStatus.METHOD_FAILURE);
		}
		
		return new ResponseEntity<UserPOIObject>(HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value="/pois")
	public ResponseEntity<List<POIObject>> getAllPOIObject(HttpServletRequest request) throws Exception {
		List<POIObject> list = getAllObject(request, POIObject.class);
		String userId = getUserId();
		for (BaseDTObject bo : list) {
			bo.filterUserData(userId);
		}
		return new ResponseEntity<List<POIObject>>(list, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value="/pois/{id:.+}")
	public ResponseEntity<BasicObject> getPOIObjectById(HttpServletRequest request, @PathVariable String id) throws Exception {
		try {
			POIObject poi = storage.getObjectById(id, POIObject.class);
			if (poi != null) poi.filterUserData(getUserId());
			return new ResponseEntity<BasicObject>(poi,HttpStatus.OK);
		} catch (NotFoundException e) {
			logger.error("POIObject with id "+ id+" does not exist");
			e.printStackTrace();
			return new ResponseEntity<BasicObject>(HttpStatus.METHOD_FAILURE);
		}
	}
}
