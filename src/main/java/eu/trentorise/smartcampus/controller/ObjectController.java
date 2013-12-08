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
import it.sayservice.platform.core.common.util.ServiceUtil;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.EventObject;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.dt.model.StoryObject;
import eu.trentorise.smartcampus.processor.EventProcessorImpl;

@Controller
public class ObjectController extends AbstractObjectController {

	@RequestMapping(value = "/objects/{id}/rate", method = RequestMethod.PUT)
	public ResponseEntity<Object> rate(HttpServletRequest request,
			@RequestParam String rating, @PathVariable String id) {
		try {
			BaseDTObject obj = (BaseDTObject) storage.getObjectById(id);
			obj.alignDO(domainEngineClient, storage);
			
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("user", getUserId());
			Integer iRating = Integer.parseInt(rating);
			if (iRating > 5) iRating = 5;
			if (iRating < 0) iRating = 0;
			
			parameters.put("rating", iRating.toString());
			Object o = ServiceUtil
					.deserializeObject((byte[]) domainEngineClient
							.invokeDomainOperationSync("rate",
									obj.getDomainType(), obj.alignedDomainId(domainEngineClient),
									parameters, null));
			String oString = domainEngineClient.searchDomainObject(
					obj.getDomainType(), obj.alignedDomainId(domainEngineClient), null);
			DomainObject dObj = new DomainObject(oString);
			if (obj instanceof EventObject)
				obj = EventProcessorImpl.convertEventObject(dObj, storage);
			else if (obj instanceof POIObject)
				obj = EventProcessorImpl.convertPOIObject(dObj);
			else if (obj instanceof StoryObject)
				obj = EventProcessorImpl.convertStoryObject(dObj, storage);
			storage.storeObject(obj);
			return new ResponseEntity<Object>(o, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to rate object with id " + id + ": "
					+ e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<Object>(HttpStatus.METHOD_FAILURE);
		}
	}

	@RequestMapping(value = "/objects/{id}/attend", method = RequestMethod.PUT)
	public ResponseEntity<Object> attend(HttpServletRequest request,
			@PathVariable String id) {
		return performDomainOperation(request, id, "attend");
	}

	@RequestMapping(value = "/objects/{id}/notAttend", method = RequestMethod.PUT)
	public ResponseEntity<Object> notAttend(HttpServletRequest request,
			@PathVariable String id) {
		return performDomainOperation(request, id, "notAttend");
	}

	private ResponseEntity<Object> performDomainOperation(
			HttpServletRequest request, String id, String operation) {
		try {
			BaseDTObject obj = (BaseDTObject) storage.getObjectById(id);
			obj.alignDO(domainEngineClient, storage);

			Map<String, Object> parameters = new HashMap<String, Object>();
			String userId = getUserId();
			parameters.put("user", userId);
			domainEngineClient.invokeDomainOperation(operation,
					obj.getDomainType(), obj.alignedDomainId(domainEngineClient), parameters, null,
					null);
			String oString = domainEngineClient.searchDomainObject(
					obj.getDomainType(), obj.alignedDomainId(domainEngineClient), null);
			DomainObject dObj = new DomainObject(oString);
			if (obj instanceof EventObject) {
				obj = EventProcessorImpl.convertEventObject(dObj, storage);
			} else if (obj instanceof POIObject) {
				obj = EventProcessorImpl.convertPOIObject(dObj);
			} else if (obj instanceof StoryObject) {
				obj = EventProcessorImpl.convertStoryObject(dObj, storage);
			}
			storage.storeObject(obj);
			obj.filterUserData(userId);

			return new ResponseEntity<Object>(obj, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to update object with id " + id + ": "
					+ e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<Object>(HttpStatus.METHOD_FAILURE);
		}
	}
	
	@RequestMapping(value = "/objects/{id}/unfollow", method = RequestMethod.PUT)
	public ResponseEntity<Object> unfollow(@PathVariable String id) {
		try {
			BaseDTObject obj = (BaseDTObject) storage.getObjectById(id);
			obj.alignDO(domainEngineClient, storage);

			String operation = "unfollow";
			String user = getUserId();

			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("user", user);
			domainEngineClient.invokeDomainOperation(operation,
					obj.getDomainType(), obj.alignedDomainId(domainEngineClient), parameters, null,
					null);
			String oString = domainEngineClient.searchDomainObject(
					obj.getDomainType(), obj.alignedDomainId(domainEngineClient), null);
			DomainObject dObj = new DomainObject(oString);
			if (obj instanceof EventObject)
				obj = EventProcessorImpl.convertEventObject(dObj, storage);
			else if (obj instanceof POIObject)
				obj = EventProcessorImpl.convertPOIObject(dObj);
			else if (obj instanceof StoryObject)
				obj = EventProcessorImpl.convertStoryObject(dObj, storage);
			storage.storeObject(obj);
			obj.filterUserData(getUserId());
			return new ResponseEntity<Object>(obj, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to unfollow object with id " + id + ": "
					+ e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<Object>(HttpStatus.METHOD_FAILURE);
		}
	}

	@RequestMapping(value = "/objects/{id}/follow", method = RequestMethod.PUT)
	public ResponseEntity<Object> follow(@PathVariable String id) {
		try {
			BaseDTObject obj = (BaseDTObject) storage.getObjectById(id);
			obj.alignDO(domainEngineClient, storage);

			String operation = "follow";
			String userId = getUserId();
			
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("user", userId);
			parameters.put("topic", userId);
			domainEngineClient.invokeDomainOperation(operation,
					obj.getDomainType(), obj.alignedDomainId(domainEngineClient), parameters, null,
					null);
			String oString = domainEngineClient.searchDomainObject(
					obj.getDomainType(), obj.alignedDomainId(domainEngineClient), null);
			DomainObject dObj = new DomainObject(oString);
			if (obj instanceof EventObject)
				obj = EventProcessorImpl.convertEventObject(dObj, storage);
			else if (obj instanceof POIObject)
				obj = EventProcessorImpl.convertPOIObject(dObj);
			else if (obj instanceof StoryObject)
				obj = EventProcessorImpl.convertStoryObject(dObj, storage);
			storage.storeObject(obj);
			obj.filterUserData(userId);
			return new ResponseEntity<Object>(obj, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to follow object with id " + id + ": "
					+ e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<Object>(HttpStatus.METHOD_FAILURE);
		}
	}

}
