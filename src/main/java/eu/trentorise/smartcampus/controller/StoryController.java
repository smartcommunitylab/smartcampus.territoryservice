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
import eu.trentorise.smartcampus.dt.model.StoryObject;
import eu.trentorise.smartcampus.dt.model.UserStoryObject;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.presentation.data.BasicObject;

@Controller
public class StoryController extends AbstractObjectController {

	@RequestMapping(method = RequestMethod.POST, value="/stories")
	public ResponseEntity<UserStoryObject> createStory(HttpServletRequest request, @RequestBody Map<String,Object> objMap) {
		UserStoryObject obj = Util.convert(objMap, UserStoryObject.class);
		try {
			obj.createDO(getUserId(), domainEngineClient, storage, moderator);
		} catch (Exception e) {
			return new ResponseEntity<UserStoryObject>(HttpStatus.METHOD_FAILURE);
		} 
		return new ResponseEntity<UserStoryObject>(obj,HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.PUT, value="/stories/{id:.+}")
	public ResponseEntity<StoryObject> updateStory(HttpServletRequest request, @RequestBody Map<String,Object> objMap, @PathVariable String id) {
		try {
			StoryObject story = storage.getObjectById(id, StoryObject.class);
			story.alignDO(domainEngineClient, storage);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<StoryObject>(HttpStatus.METHOD_FAILURE);
		}

		UserStoryObject obj = Util.convert(objMap, UserStoryObject.class);
		try {
			return new ResponseEntity<StoryObject>(obj.updateDO(getUserId(), domainEngineClient, storage, moderator),HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to update userStory: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<StoryObject>(HttpStatus.METHOD_FAILURE);
		}
		
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/stories/{id:.+}")
	public ResponseEntity<UserStoryObject> deleteStory(HttpServletRequest request, @PathVariable String id) {

		UserStoryObject story = null;
		try {
			story = storage.getObjectById(id,UserStoryObject.class);
			// CAN DELETE ONLY OWN OBJECTS
			if (!getUserId().equals(story.getCreatorId())) {
				logger.error("Attempt to delete not owned object. User "+getUserId()+", object "+story.getId());
				return new ResponseEntity<UserStoryObject>(HttpStatus.METHOD_FAILURE);
			}
		} catch (NotFoundException e) {
			return new ResponseEntity<UserStoryObject>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Failed to delete userStory: "+e.getMessage());
			return new ResponseEntity<UserStoryObject>(HttpStatus.METHOD_FAILURE);
		}

		
		try {
			story.deleteDO(domainEngineClient, storage);
		} catch (Exception e) {
			logger.error("Failed to delete userStory: "+e.getMessage());
			e.printStackTrace();
			return new ResponseEntity<UserStoryObject>(HttpStatus.METHOD_FAILURE);
		}
		
		return new ResponseEntity<UserStoryObject>(HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value="/stories")
	public ResponseEntity<List<StoryObject>> getAllStoryObject(HttpServletRequest request) throws Exception {
		List<StoryObject> list = getAllObject(request, StoryObject.class);
		String userId = getUserId();
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			BaseDTObject object = (BaseDTObject)iterator.next();
			if (!object.objectVisible(userId)) {
				iterator.remove();
			} else { 
				object.filterUserData(userId);
			}
		}
		return new ResponseEntity<List<StoryObject>>(list, HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET, value="/stories/{id:.+}")
	public ResponseEntity<BasicObject> getStoryObjectById(HttpServletRequest request, @PathVariable String id) throws Exception {
		try {
			StoryObject o = storage.getObjectById(id, StoryObject.class);
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
			logger.error("StoryObject with id "+ id+" does not exist");
			e.printStackTrace();
			return new ResponseEntity<BasicObject>(HttpStatus.METHOD_FAILURE);
		}
	}

}
