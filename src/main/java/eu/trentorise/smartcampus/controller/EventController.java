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


import it.sayservice.platform.client.DomainObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.EventObject;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.dt.model.UserEventObject;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.processor.EventProcessorImpl;

@Controller
public class EventController extends AbstractObjectController {

	@RequestMapping(method = RequestMethod.POST, value="/events")
	public ResponseEntity<UserEventObject> createEvent(HttpServletRequest request, @RequestBody Map<String,Object> objMap) {
		UserEventObject obj = Util.convert(objMap, UserEventObject.class);
		EventObject tmp = null;
		obj.setId(new ObjectId().toString());
		try {
			obj.setCreatorId(getUserId());
			obj.setDomainType("eu.trentorise.smartcampus.domain.discovertrento.UserEventObject");
			tmp = Util.convert(obj, EventObject.class);
			storage.storeObject(tmp);
			obj.createDO(domainEngineClient, storage);
		} catch (Exception e) {
			logger.error("Failed to create userEvent: "+e.getMessage());
			e.printStackTrace();
			try {
				if (tmp != null) storage.deleteObject(tmp);
			} catch (DataException e1) {
				logger.error("Failed to cleanup userEvent: "+e1.getMessage());
			}

			return new ResponseEntity<UserEventObject>(HttpStatus.METHOD_FAILURE);
		} 
		return new ResponseEntity<UserEventObject>(obj,HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.PUT, value="/events/{id:.+}")
	public ResponseEntity<EventObject> updateEvent(HttpServletRequest request, @RequestBody Map<String,Object> objMap, @PathVariable String id) {
		try {
			EventObject event = storage.getObjectById(id, EventObject.class);
			event.alignDO(domainEngineClient, storage);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<EventObject>(HttpStatus.METHOD_FAILURE);
		}

		UserEventObject obj = Util.convert(objMap, UserEventObject.class);
		try {
			if (obj.getPoiId() == null) {
				logger.error("Error creating event: empty poiId");
				return new ResponseEntity<EventObject>(HttpStatus.METHOD_FAILURE);
			}
			
			Map<String,Object> parameters = new HashMap<String, Object>();
			String operation = null;
			// TODO IN THIS WAY CAN MODIFY ONLY OWN OBJECTS, OTHERWISE ONLY TAGS IN COMMUNITY DATA
			if (!getUserId().equals(obj.getCreatorId())) {
				operation = "updateCommunityData";
				if (obj.getCommunityData() != null) {
					obj.getCommunityData().setRating(null);
				}
				parameters.put("newCommunityData",  obj.domainCommunityData());
			} else {
				operation = "updateEvent";
				POIObject poi = storage.getObjectById(obj.getPoiId(), POIObject.class);
				parameters.put("newData", Util.convert(obj.toGenericEvent(poi), Map.class)); 
				parameters.put("newCommunityData",  obj.domainCommunityData());
			}
			
			domainEngineClient.invokeDomainOperation(
					operation, 
					obj.getDomainType(), 
					obj.alignedDomainId(domainEngineClient),
					parameters, null, null); 
			
			String oString = domainEngineClient.searchDomainObject(obj.getDomainType(), obj.alignedDomainId(domainEngineClient), null);
			DomainObject dObj = new DomainObject(oString);
			EventObject uObj = EventProcessorImpl.convertEventObject(dObj, storage);
//			storage.storeObject(uObj);
			uObj.filterUserData(getUserId());

			return new ResponseEntity<EventObject>(uObj,HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to update event: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<EventObject>(HttpStatus.METHOD_FAILURE);
		}
		
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/events/{id:.+}")
	public ResponseEntity<UserEventObject> deleteEvent(HttpServletRequest request, @PathVariable String id) {

		EventObject event = null;
		try {
			event = storage.getObjectById(id,EventObject.class);
			// CAN DELETE ONLY OWN OBJECTS
			if (!getUserId().equals(event.getCreatorId())) {
				logger.error("Attempt to delete not owned object. User "+getUserId()+", object "+event.getId());
				return new ResponseEntity<UserEventObject>(HttpStatus.METHOD_FAILURE);
			}
		} catch (NotFoundException e) {
			return new ResponseEntity<UserEventObject>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to delete userEvent: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<UserEventObject>(HttpStatus.METHOD_FAILURE);
		}


		try {
			if (event.alignedDomainId(domainEngineClient) != null) {
				Map<String,Object> parameters = new HashMap<String, Object>(0);
				domainEngineClient.invokeDomainOperation(
						"deleteEvent", 
						event.getDomainType(), 
						event.alignedDomainId(domainEngineClient),
						parameters, null, null); 
				storage.deleteObject(event);
			}
		} catch (Exception e) {
			logger.error("Failed to delete userEvent: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<UserEventObject>(HttpStatus.METHOD_FAILURE);
		}
		
		return new ResponseEntity<UserEventObject>(HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value="/events")
	public ResponseEntity<List<EventObject>> getAllEventObject(HttpServletRequest request) throws Exception {
		List<EventObject> list = getAllObject(request, EventObject.class);
		String userId = getUserId();
		for (BaseDTObject bo : list) {
			bo.filterUserData(userId);
		}
		return new ResponseEntity<List<EventObject>>(list, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value="/events/{id:.+}")
	public ResponseEntity<EventObject> getEventObjectById(HttpServletRequest request, @PathVariable String id) throws Exception {
		try {
			EventObject o = storage.getObjectById(id, EventObject.class);
			if (o != null) o.filterUserData(getUserId());
			return new ResponseEntity<EventObject>(o,HttpStatus.OK);
		} catch (NotFoundException e) {
			logger.error("EventObject with id "+ id+" does not exist");
			e.printStackTrace();
			return new ResponseEntity<EventObject>(HttpStatus.METHOD_FAILURE);
		}
	}


}
