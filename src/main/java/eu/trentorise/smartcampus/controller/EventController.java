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
import eu.trentorise.smartcampus.dt.model.EventObject;
import eu.trentorise.smartcampus.dt.model.UserEventObject;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;

@Controller
public class EventController extends AbstractObjectController {

	@RequestMapping(method = RequestMethod.POST, value="/events")
	public ResponseEntity<UserEventObject> createEvent(HttpServletRequest request, @RequestBody Map<String,Object> objMap) {
		UserEventObject obj = Util.convert(objMap, UserEventObject.class);
		try {
			obj.createDO(getUserId(), domainEngineClient, storage, moderator);
		} catch (Exception e) {
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

		EventObject obj = Util.convert(objMap, EventObject.class);
		if (obj.getPoiId() == null) {
			logger.error("Error updating event: empty poiId");
			return new ResponseEntity<EventObject>(HttpStatus.METHOD_FAILURE);
		}
		try {
			return new ResponseEntity<EventObject>(obj.updateDO(getUserId(), domainEngineClient, storage, moderator),HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to update event: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<EventObject>(HttpStatus.METHOD_FAILURE);
		}
		
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/events/{id:.+}")
	public ResponseEntity<UserEventObject> deleteEvent(HttpServletRequest request, @PathVariable String id) {

		UserEventObject event = null;
		try {
			String userId = getUserId();
			event = storage.getObjectById(id,UserEventObject.class);
			// CAN DELETE ONLY OWN OBJECTS
			if (!userId.equals(event.getCreatorId())) {
				logger.error("Attempt to delete not owned object. User "+userId+", object "+event.getId());
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
			event.deleteDO(domainEngineClient, storage);
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
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			BaseDTObject object = (BaseDTObject)iterator.next();
			if (!object.objectVisible(userId)) {
				iterator.remove();
			} else { 
				object.filterUserData(userId);
			}
		}
		return new ResponseEntity<List<EventObject>>(list, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value="/events/{id:.+}")
	public ResponseEntity<EventObject> getEventObjectById(HttpServletRequest request, @PathVariable String id) throws Exception {
		try {
			EventObject o = storage.getObjectById(id, EventObject.class);
			String userId = getUserId();
			if (o != null) {
				if (!o.objectVisible(userId)) {
					o = null;
				} else {
					o.filterUserData(userId);
				}
			}
			return new ResponseEntity<EventObject>(o,HttpStatus.OK);
		} catch (NotFoundException e) {
			logger.error("EventObject with id "+ id+" does not exist");
			e.printStackTrace();
			return new ResponseEntity<EventObject>(HttpStatus.METHOD_FAILURE);
		}
	}


}
