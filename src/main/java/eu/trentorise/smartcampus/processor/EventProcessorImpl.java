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
package eu.trentorise.smartcampus.processor;

import it.sayservice.platform.client.DomainObject;
import it.sayservice.platform.client.DomainUpdateListener;
import it.sayservice.platform.core.message.Core.DomainEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.CommunityData;
import eu.trentorise.smartcampus.dt.model.EventObject;
import eu.trentorise.smartcampus.dt.model.GenericStoryStep;
import eu.trentorise.smartcampus.dt.model.POIData;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.dt.model.StepObject;
import eu.trentorise.smartcampus.dt.model.StoryObject;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.common.exception.NotFoundException;
import eu.trentorise.smartcampus.presentation.common.util.Util;
import eu.trentorise.smartcampus.presentation.data.NotificationObject;
import eu.trentorise.smartcampus.social.model.Concept;

public class EventProcessorImpl implements DomainUpdateListener {

	private static final String TYPE_SERVICE_EVENT = "eu.trentorise.smartcampus.domain.discovertrento.ServiceEventObject";
	private static final String TYPE_SERVICE_POI = "eu.trentorise.smartcampus.domain.discovertrento.ServicePOIObject";
	private static final String TYPE_USER_EVENT = "eu.trentorise.smartcampus.domain.discovertrento.UserEventObject";
	private static final String TYPE_USER_POI = "eu.trentorise.smartcampus.domain.discovertrento.UserPOIObject";
	private static final String TYPE_STORY = "eu.trentorise.smartcampus.domain.discovertrento.StoryObject";

	private static final String EVENT_UPDATED_VAR = "VAR_UPDATED";
	private static final String EVENT_DELETED = "DELETED";

	private static final String VAR_ENTITY_ID = "entityId";

	@Autowired
	private GeoTimeObjectSyncStorage storage;

	private static Log logger = LogFactory.getLog(EventProcessorImpl.class);

	public EventProcessorImpl() {
		super();
	}

	@Override
	public void onDomainEvents(String subscriptionId, List<DomainEvent> events) {
		List<BaseDTObject> created = new ArrayList<BaseDTObject>();
		List<BaseDTObject> deleted = new ArrayList<BaseDTObject>();
		List<BaseDTObject> updated = new ArrayList<BaseDTObject>();
		List<NotificationObject> notifications = new ArrayList<NotificationObject>();

		if (events != null && !events.isEmpty()) {
			for (DomainEvent event : events) {
				if (TYPE_SERVICE_EVENT.equals(event.getDoType())
						|| TYPE_USER_EVENT.equals(event.getDoType())) {
					try {
						processEvent(event, created, updated, deleted,
								notifications);
					} catch (Exception e) {
						logger.error("Error processing EventObject: "
								+ e.getMessage());
						e.printStackTrace();
						continue;
					}
				}
				if (TYPE_STORY.equals(event.getDoType())) {
					try {
						processStory(event, created, updated, deleted,
								notifications);
					} catch (Exception e) {
						logger.error("Error processing EventObject: "
								+ e.getMessage());
						e.printStackTrace();
						continue;
					}
				}
				if (TYPE_SERVICE_POI.equals(event.getDoType())
						|| TYPE_USER_POI.equals(event.getDoType())) {
					try {
						processPOI(event, created, updated, deleted,
								notifications);
					} catch (Exception e) {
						logger.error("Error processing POIObject: "
								+ e.getMessage());
						e.printStackTrace();
						continue;
					}
				}
			}
		}
		for (BaseDTObject o : created) {
			try {
				storage.storeObject(o);
			} catch (DataException e) {
				logger.error("failed to store object: "
						+ o.getClass().getName());
			}
		}
		for (BaseDTObject o : updated) {
			try {
				storage.storeObject(o);
			} catch (DataException e) {
				logger.error("failed to store object: "
						+ o.getClass().getName());
			}
		}
	}

	private void processPOI(DomainEvent event, List<BaseDTObject> created,
			List<BaseDTObject> updated, List<BaseDTObject> deleted,
			List<NotificationObject> notifications) throws Exception {
		if (EVENT_UPDATED_VAR.equals(event.getEventType())
				&& VAR_ENTITY_ID.equals(event.getParameter())) {
			// object is created only when the entity is assigned
			createPOI(event, created);
		} else if (EVENT_UPDATED_VAR.equals(event.getEventType())) {
			logger.debug("Updating POIObject: " + event.getDoId());
			DomainObject dObj = readDOFromEvent(event);
			POIObject eObj = convertPOIObject(dObj);

			try {
				storage.getObjectById(eObj.getId(), POIObject.class);
			} catch (NotFoundException e) {
				createPOI(event, created);
				return;
			}
			updated.add(eObj);
			// TODO:
			// - detect the difference
		} else if (EVENT_DELETED.equals(event.getEventType())) {
			logger.debug("Deleting POIObject: " + event.getDoId());
			DomainObject dObj = readDOFromEvent(event);
			POIObject eObj = convertPOIObject(dObj);
			storage.deleteObjectById(eObj.getId());
		}
	}

	private void createPOI(DomainEvent event, List<BaseDTObject> created)
			throws Exception {
		logger.debug("Processing new POIObject: " + event.getDoId());
		DomainObject dObj = readDOFromEvent(event);
		POIObject eObj = convertPOIObject(dObj);
		created.add(eObj);
	}

	private void processEvent(DomainEvent event, List<BaseDTObject> created,
			List<BaseDTObject> updated, List<BaseDTObject> deleted,
			List<NotificationObject> notifications) throws Exception {
		if (EVENT_UPDATED_VAR.equals(event.getEventType())
				&& VAR_ENTITY_ID.equals(event.getParameter())) {
			// object is created only when the entity is assigned
			createEvent(event, created);
		} else if (EVENT_UPDATED_VAR.equals(event.getEventType())) {
			logger.debug("Updating UserEventObject: " + event.getDoId());
			DomainObject dObj = readDOFromEvent(event);
			EventObject eObj = convertEventObject(dObj, storage);
			try {
				storage.getObjectById(eObj.getId(), EventObject.class);
			} catch (NotFoundException e) {
				createEvent(event, created);
				return;
			}

			updated.add(eObj);
			// TODO:
			// - detect the difference
		} else if (EVENT_DELETED.equals(event.getEventType())) {
			logger.debug("Deleting UserEventObject: " + event.getDoId());
			DomainObject dObj = readDOFromEvent(event);
			EventObject eObj = convertEventObject(dObj, storage);
			storage.deleteObjectById(eObj.getId());
		}
	}

	private void processStory(DomainEvent event, List<BaseDTObject> created,
			List<BaseDTObject> updated, List<BaseDTObject> deleted,
			List<NotificationObject> notifications) throws Exception {
		if (EVENT_UPDATED_VAR.equals(event.getEventType())
				&& VAR_ENTITY_ID.equals(event.getParameter())) {
			// object is created only when the entity is assigned
			createStory(event, created);
		} else if (EVENT_UPDATED_VAR.equals(event.getEventType())) {
			logger.debug("Updating UserStoryObject: " + event.getDoId());
			DomainObject dObj = readDOFromEvent(event);
			StoryObject eObj = convertStoryObject(dObj, storage);
			try {
				storage.getObjectById(eObj.getId(), StoryObject.class);
			} catch (NotFoundException e) {
				createStory(event, created);
				return;
			}

			updated.add(eObj);
			// TODO:
			// - detect the difference
		} else if (EVENT_DELETED.equals(event.getEventType())) {
			logger.debug("Deleting UserStoryObject: " + event.getDoId());
			DomainObject dObj = readDOFromEvent(event);
			StoryObject eObj = convertStoryObject(dObj, storage);
			storage.deleteObjectById(eObj.getId());
		}
	}

	// private void processUserObject(EventObject oldObj, EventObject eObj,
	// List<BaseDTObject> created, List<BaseDTObject> deleted) {
	// if (oldObj.getAttending() == null || oldObj.getAttending().isEmpty()) {
	// // add all eObj.getAttending to created
	// return;
	// }
	// if (eObj.getAttending() == null || eObj.getAttending().isEmpty()) {
	// // add all oldObj.getAttending to deleted
	// return;
	// }
	// Set<String> newSet = new HashSet<String>(eObj.getAttending());
	// for (String old : oldObj.getAttending()) {
	// if (newSet.contains(old)) {
	// newSet.remove(old);
	// } else {
	// // add old to deleted
	// }
	// }
	// if (!newSet.isEmpty()) {
	// // add all newset to created
	// }
	// oldObj.getAttending();
	// }
	//
	private void createEvent(DomainEvent event, List<BaseDTObject> created)
			throws Exception, NotFoundException, DataException {
		logger.debug("Processing new EventObject: " + event.getDoId());
		DomainObject dObj = readDOFromEvent(event);
		EventObject eObj = convertEventObject(dObj, storage);
		created.add(eObj);
	}

	private void createStory(DomainEvent event, List<BaseDTObject> created)
			throws Exception, NotFoundException, DataException {
		logger.debug("Processing new StoryObject: " + event.getDoId());
		DomainObject dObj = readDOFromEvent(event);
		StoryObject eObj = convertStoryObject(dObj, storage);
		created.add(eObj);
	}

	@SuppressWarnings("unchecked")
	protected static Map<String, Object> populateBaseDTData(DomainObject dObj,
			BaseDTObject eo) {
		eo.setDomainType(dObj.getType());
		eo.setDomainId(dObj.getId());
		Map<String, Object> content = dObj.getContent();

		if (content.get("entityId") != null) {
			if (content.get("entityId") instanceof Long)
				eo.setEntityId(content.get("entityId").toString());
			else
				eo.setEntityId(content.get("entityId").toString());
		}

		if (content.containsKey("creator")) {
			eo.setCreatorId((String) content.get("creator"));
			// TODO setCreatorName
		}

		Map<String, Object> data = (Map<String, Object>) content.get("data");
		eo.setDescription((String) data.get("description"));
		eo.setTitle((String) data.get("title"));
		eo.setId((String) data.get("id"));
		eo.setSource((String) data.get("source"));
		eo.setType((String) data.get("type"));

		try {
			if (data.containsKey("customData")
					&& data.get("customData") != null) {
				Map<String, Object> map = new ObjectMapper().readValue(
						(String) data.get("customData"), Map.class);
				if (map != null && !map.isEmpty())
					eo.setCustomData(map);
			}
		} catch (Exception e) {
			logger.warn("Problem reading custom data: "
					+ data.get("customData"));
		}

		CommunityData cd = new CommunityData();
		eo.setCommunityData(cd);
		Map<String, Object> communityData = (Map<String, Object>) content
				.get("communityData");
		if (communityData != null) {
			cd.setAverageRating((Integer) communityData.get("averageRating"));
			List<Map<String, Object>> ratings = (List<Map<String, Object>>) communityData
					.get("ratings");
			Map<String,Integer> ratingMap = new HashMap<String,Integer>();
			if (ratings != null && !ratings.isEmpty()) {
				for (Map<String, Object> rating : ratings) {
					ratingMap.put((String) rating.get("user"), (Integer) rating.get("value"));
				}
			}

			List<Concept> tags = new ArrayList<Concept>();
			List<Map<String, Object>> tagMapList = (List<Map<String, Object>>) communityData
					.get("tags");
			if (tagMapList != null) {
				for (Map<String, Object> tagMap : tagMapList) {
					Concept c = new Concept();
					Object id = tagMap.get("id");
					if (id instanceof Long)
						c.setId(id.toString());
					else if (id != null)
						c.setId(id.toString());
					c.setName((String) tagMap.get("name"));
					c.setDescription((String) tagMap.get("description"));
					c.setSummary((String) tagMap.get("summary"));
					tags.add(c);
				}
			}
			cd.setTags(tags);
			cd.setRating(ratingMap);
			cd.setFollowing((Map<String, String>) communityData
					.get("following"));
		}

		return data;
	}

	@SuppressWarnings("unchecked")
	public static POIObject convertPOIObject(DomainObject dObj) {
		POIObject eo = new POIObject();
		Map<String, Object> data = populateBaseDTData(dObj, eo);

		if (data.get("poiData") != null) {
			POIData poiData = Util.convert(data.get("poiData"), POIData.class);
			eo.setPoi(poiData);
			eo.setLocation(new double[] { poiData.getLatitude(),
					poiData.getLongitude() });
		}

		if (dObj.getContent().get("customData") != null) {
			Map<String, Object> customData = (Map<String, Object>) dObj
					.getContent().get("customData");
			if (eo.getType() == null && customData.containsKey("type")) {
				eo.setType((String) customData.get("type"));
			}
		}
		return eo;
	}

	@SuppressWarnings("unchecked")
	public static EventObject convertEventObject(DomainObject dObj,
			GeoTimeObjectSyncStorage storage) throws NotFoundException,
			DataException {
		EventObject eo = new EventObject();
		Map<String, Object> data = populateBaseDTData(dObj, eo);

		if (data.get("fromTime") instanceof Long)
			eo.setFromTime((Long) data.get("fromTime"));
		else if (data.get("fromTime") != null)
			eo.setFromTime(Long.parseLong(data.get("fromTime").toString()));

		eo.setTiming((String) data.get("timing"));

		if (data.get("toTime") instanceof Long)
			eo.setToTime((Long) data.get("toTime"));
		else if (data.get("toTime") != null)
			eo.setToTime(Long.parseLong(data.get("toTime").toString()));

		if (eo.getToTime() == null || eo.getToTime() < eo.getFromTime()) {
			eo.setToTime(eo.getFromTime());
		}

		String poiId = (String) data.get("poiId");
		updateLocation(eo, poiId, storage);

		Map<String, Object> content = dObj.getContent();
		Object att = content.get("attendees");
		if (att != null && att instanceof Integer) eo.setAttendees((Integer) att);
		else if (att != null && att instanceof Double) eo.setAttendees(((Double)att).intValue());
		if (content.get("attending") != null) {
			eo.setAttending((List<String>) content.get("attending"));
		}

		if (dObj.getContent().get("customData") != null) {
			Map<String, Object> customData = (Map<String, Object>) dObj
					.getContent().get("customData");
			if (eo.getType() == null && customData.containsKey("type")) {
				eo.setType((String) customData.get("type"));
			}
			if (eo.getFromTime() == null && customData.containsKey("fromTime")) {
				if (customData.get("fromTime") instanceof Long)
					eo.setFromTime((Long) customData.get("fromTime"));
				else if (customData.get("fromTime") != null)
					eo.setFromTime(Long.parseLong(customData.get("fromTime")
							.toString()));
			}
			if (eo.getTiming() == null && customData.containsKey("timing")) {
				eo.setTiming((String) customData.get("timing"));
			}
			if (eo.getToTime() == null && customData.containsKey("toTime")) {
				if (customData.get("toTime") instanceof Long)
					eo.setToTime((Long) customData.get("toTime"));
				else if (customData.get("toTime") != null)
					eo.setToTime(Long.parseLong(customData.get("toTime")
							.toString()));
				if (eo.getToTime() == null || eo.getToTime() < eo.getFromTime()) {
					eo.setToTime(eo.getFromTime());
				}
			}
			if (eo.getLocation() == null && customData.containsKey("poiId")) {
				String customPoiId = (String) customData.get("poiId");
				updateLocation(eo, customPoiId, storage);
			}
		}

		return eo;
	}

	protected static void updateLocation(EventObject eo, String poiId,
			GeoTimeObjectSyncStorage storage) {
		if (poiId != null) {
			eo.setPoiId(poiId);
			try {
				POIObject poiObject = storage.getObjectById(poiId,
						POIObject.class);
				if (poiObject != null) {
					eo.setLocation(poiObject.getLocation());
				}
			} catch (Exception e) {
			}
		}
	}

	public GeoTimeObjectSyncStorage getStorage() {
		return storage;
	}

	public void setStorage(GeoTimeObjectSyncStorage storage) {
		this.storage = storage;
	}

	private DomainObject readDOFromEvent(DomainEvent e) throws Exception {
		return new DomainObject(e.getPayload());
	}

	@SuppressWarnings("unchecked")
	public static StoryObject convertStoryObject(DomainObject dObj,
			GeoTimeObjectSyncStorage storage) {
		StoryObject so = new StoryObject();
		Map<String, Object> data = populateBaseDTData(dObj, so);

		so.setSteps(new ArrayList<StepObject>());
		if (data.get("steps") != null) {
			List<?> stepData = Util.convert(data.get("steps"), List.class);
			if (stepData != null && !stepData.isEmpty()) {
				for (Object o : stepData) {
					GenericStoryStep step = Util.convert(o,
							GenericStoryStep.class);
					so.getSteps().add(
							new StepObject(step.getPoiId(), step.getNote()));
				}
			}
		}

		Map<String, Object> content = dObj.getContent();
		Object att = content.get("attendees");
		if (att != null && att instanceof Integer) so.setAttendees((Integer) att);
		else if (att != null && att instanceof Double) so.setAttendees(((Double)att).intValue());
		if (content.get("attending") != null) {
			so.setAttending((List<String>) content.get("attending"));
		}

		if (dObj.getContent().get("customData") != null) {
			Map<String, Object> customData = (Map<String, Object>) dObj
					.getContent().get("customData");
			if (so.getType() == null && customData.containsKey("type")) {
				so.setType((String) customData.get("type"));
			}
		}

		return so;
	}

}
