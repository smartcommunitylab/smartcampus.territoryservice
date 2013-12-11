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


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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

@Controller
public class POIController extends AbstractObjectController {

	@RequestMapping(method = RequestMethod.POST, value="/pois")
	public ResponseEntity<UserPOIObject> createPOI(HttpServletRequest request, @RequestBody Map<String,Object> objMap) {
		UserPOIObject obj = Util.convert(objMap, UserPOIObject.class);
		try {
			validatePOI(obj);
		} catch (DataException e1) {
			logger.error("Failed to create userPOI: "+e1.getMessage());
			e1.printStackTrace();
			return new ResponseEntity<UserPOIObject>(HttpStatus.METHOD_FAILURE);
		}
		
		try {
			obj.createDO(getUserId(), domainEngineClient, storage, moderator);
		} catch (Exception e) {
			return new ResponseEntity<UserPOIObject>(HttpStatus.METHOD_FAILURE);
		} 
		return new ResponseEntity<UserPOIObject>(obj,HttpStatus.OK);
	}

	private void validatePOI(POIObject obj) throws DataException {
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
		// align with domain
		try {
			POIObject poi = storage.getObjectById(id, POIObject.class);
			poi.alignDO(domainEngineClient, storage);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<POIObject>(HttpStatus.METHOD_FAILURE);
		}
		
		POIObject obj = Util.convert(objMap, POIObject.class);
		try {
			validatePOI(obj);
		} catch (DataException e1) {
			logger.error("Failed to create userPOI: "+e1.getMessage());
			e1.printStackTrace();
			return new ResponseEntity<POIObject>(HttpStatus.METHOD_FAILURE);
		}
		
		try {
			return new ResponseEntity<POIObject>(obj.updateDO(getUserId(), domainEngineClient, storage, moderator),HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to update userPOI: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<POIObject>(HttpStatus.METHOD_FAILURE);
		}
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/pois/{id:.+}")
	public ResponseEntity<UserPOIObject> deletePOI(HttpServletRequest request, @PathVariable String id) {

		UserPOIObject poi = null;
		try {
			poi = storage.getObjectById(id,UserPOIObject.class);
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
			poi.deleteDO(domainEngineClient, storage);
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
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			BaseDTObject object = (BaseDTObject)iterator.next();
			if (!object.objectVisible(userId)) {
				iterator.remove();
			} else { 
				object.filterUserData(userId);
			}
		}
		return new ResponseEntity<List<POIObject>>(list, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value="/pois/{id:.+}")
	public ResponseEntity<BasicObject> getPOIObjectById(HttpServletRequest request, @PathVariable String id) throws Exception {
		try {
			POIObject o = storage.getObjectById(id, POIObject.class);
			String userId = getUserId();
			if (o != null) {
				if (!o.objectVisible(userId)) {
					o = null;
				} else {
					o.filterUserData(userId);
				}
			}
			return new ResponseEntity<BasicObject>(o,HttpStatus.OK);
		} catch (NotFoundException e) {
			logger.error("POIObject with id "+ id+" does not exist");
			e.printStackTrace();
			return new ResponseEntity<BasicObject>(HttpStatus.METHOD_FAILURE);
		}
	}
}
