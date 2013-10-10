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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.communicator.CommunicatorConnector;
import eu.trentorise.smartcampus.communicator.CommunicatorConnectorException;
import eu.trentorise.smartcampus.communicator.model.EntityObject;
import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.EventObject;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.dt.model.StepObject;
import eu.trentorise.smartcampus.dt.model.StoryObject;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.network.RemoteConnector;

/**
 * @author raman
 *
 */
@Component
public class NotificationHelper extends RemoteConnector implements UpdateNotifier {

	private static final String PATH_TOKEN = "oauth/token";

	private static final String TS_APP = "core.territory";
	
	@Autowired
	private GeoTimeObjectSyncStorage storage;
	
	@Autowired
	@Value("${communicatorURL}")
	private String communicatorURL;
	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	@Autowired
	@Value("${smartcampus.clientId}")
	private String clientId = null;
	@Autowired
	@Value("${smartcampus.clientSecret}")
	private String clientSecret = null;
	
	private String token = null;
	private Long expiresAt = null;
	
	private CommunicatorConnector connector = null;

	private Log logger = LogFactory.getLog(getClass());
	
	public enum CHANGE {CREATED, UPDATED, DELETED};
	
	public NotificationHelper() throws Exception {
		super();
	}

	private CommunicatorConnector connector() {
		if (connector == null) {
			try {
				connector = new CommunicatorConnector(communicatorURL, TS_APP);
			} catch (Exception e) {
				logger.error("Failed to instantiate connector: "+e.getMessage(), e);
				e.printStackTrace();
			}
		}
		return connector;
	}
	@SuppressWarnings("rawtypes")
	private String getToken() {
		if (token == null || System.currentTimeMillis() + 10000 > expiresAt) {
	        final HttpResponse resp;
	        if (!aacURL.endsWith("/")) aacURL += "/";
	        String url = aacURL + PATH_TOKEN+"?grant_type=client_credentials&client_id="+clientId +"&client_secret="+clientSecret;
	        final HttpGet get = new HttpGet(url);
	        get.setHeader("Accept", "application/json");
            try {
				resp = getHttpClient().execute(get);
				final String response = EntityUtils.toString(resp.getEntity());
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					Map map = JsonUtils.toObject(response, Map.class);
					expiresAt = System.currentTimeMillis()+(Integer)map.get("expires_in")*1000;
					token = (String)map.get("access_token");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return token;
	}
	
	@Override
	public void eventCreated(EventObject eo) {
		POIObject poi = null;
		try {
			poi = storage.getObjectById(eo.getPoiId(), POIObject.class);
		} catch (Exception e) {
			e.printStackTrace();
			logger .error("Failed to process event creation: "+e.getMessage(), e);
			return;
		}
		if (poi == null || poi.getCommunityData() == null) return;
		Map<String,String> followers = poi.getCommunityData().getFollowing();
		notify(eo, poi, followers, CHANGE.CREATED);
	}
	@Override
	public void eventUpdated(EventObject obj) {
		if (obj.getCommunityData() == null) return;
		Map<String,String> followers = obj.getCommunityData().getFollowing();
		notify(obj, null, followers, CHANGE.UPDATED);
	}
	@Override
	public void eventDeleted(EventObject obj) {
		if (obj.getCommunityData() == null) return;
		Map<String,String> followers = obj.getCommunityData().getFollowing();
		notify(obj, null, followers, CHANGE.DELETED);
	}

	@Override
	public void poiCreated(POIObject poi) {
		// do nothing
	}
	@Override
	public void poiUpdated(POIObject obj) {
		if (obj.getCommunityData() == null) return;
		Map<String,String> followers = obj.getCommunityData().getFollowing();
		notify(obj, null, followers, CHANGE.UPDATED);
	}

	private void notify(BaseDTObject obj, BaseDTObject rel, Map<String, String> followers, CHANGE change) {
		if (followers != null && !followers.isEmpty()) {
			Notification n = new Notification();
			long when = System.currentTimeMillis();
			n.setTimestamp(when);
			n.setTitle(obj.getTitle());
			if (!populateContent(n, obj, rel, change)) {
				return;
			}
			try {
				connector().sendAppNotification(n, TS_APP, new ArrayList<String>(followers.keySet()), getToken());
			} catch (CommunicatorConnectorException e) {
				e.printStackTrace();
				logger .error("Failed to send notifications: "+e.getMessage(), e);
			}
		}
	}
	@Override
	public void poiDeleted(POIObject obj) {
		if (obj.getCommunityData() == null) return;
		Map<String,String> followers = obj.getCommunityData().getFollowing();
		notify(obj, null, followers, CHANGE.DELETED);
	}

	@Override
	public void storyCreated(StoryObject obj) {
		if (obj.getSteps() != null) {
			for (StepObject step : obj.getSteps()) {
				POIObject poi = null;
				try {
					poi = storage.getObjectById(step.getPoiId(), POIObject.class);
				} catch (Exception e) {
					e.printStackTrace();
					logger .error("Failed to process event creation: "+e.getMessage(), e);
					return;
				}
				if (poi == null || poi.getCommunityData() == null) return;
				Map<String,String> followers = poi.getCommunityData().getFollowing();
				notify(obj, poi, followers, CHANGE.CREATED);
			}
		}
	}
	@Override
	public void storyUpdated(StoryObject obj) {
		if (obj.getCommunityData() == null) return;
		Map<String,String> followers = obj.getCommunityData().getFollowing();
		notify(obj, null, followers, CHANGE.UPDATED);
	}
	@Override
	public void storyDeleted(StoryObject obj) {
		if (obj.getCommunityData() == null) return;
		Map<String,String> followers = obj.getCommunityData().getFollowing();
		notify(obj, null, followers, CHANGE.DELETED);
	}

	/**
	 * @param n 
	 * @param rel 
	 * @param change 
	 * @param poi
	 */
	private boolean populateContent(Notification n, BaseDTObject o, BaseDTObject rel, CHANGE change) {
		n.setContent(new HashMap<String, Object>());
		List<EntityObject> eos = new ArrayList<EntityObject>();

		EntityObject eo = new EntityObject();
		eo.setEntityId(o.getEntityId());
		eo.setType(getEntityType(o));
		eo.setTitle(o.getTitle());
		eo.setId(o.getId());
		eos.add(eo);

		if (rel != null) {
			eo = new EntityObject();
			eo.setEntityId(rel.getEntityId());
			eo.setType(getEntityType(rel));
			eo.setTitle(rel.getTitle());
			eo.setId(rel.getId());
			eos.add(eo);
		}
		if (change == CHANGE.UPDATED) {
			n.getContent().put("updated", true);
			try {
				BaseDTObject old = storage.getObjectById(o.getId(), o.getClass());
				Set<String> changed = o.checkEquals(old);
				if (changed.isEmpty()) return false;
				n.getContent().put("changes", changed);
			} catch (Exception e) {
				e.printStackTrace();
				logger .error("Failed to process event update: "+e.getMessage(), e);
				return false;
			}
		}
		n.setEntities(eos);
		if (change == CHANGE.DELETED) n.getContent().put("deleted", true);
		return true;
	}

	/**
	 * @param o
	 * @return
	 */
	private String getEntityType(BaseDTObject o) {
		return o instanceof EventObject ? "event" : o instanceof POIObject ? "location" : "narrative";
	}

}
