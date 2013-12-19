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

package eu.trentorise.smartcampus.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.communicator.CommunicatorConnectorException;
import eu.trentorise.smartcampus.communicator.model.EntityObject;
import eu.trentorise.smartcampus.communicator.model.Notification;
import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.EventObject;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.manager.utils.SCServiceConnector;

/**
 * @author raman
 *
 */
@Component
public class ModerationNotificationHelper extends SCServiceConnector implements ModerationNotifier {

	private Log logger = LogFactory.getLog(getClass());

	public ModerationNotificationHelper() throws Exception {
		super();
	}

	@Override
	public void notifyRejected(BaseDTObject object, String note, String userId, long modificationTime) {
		Notification n = new Notification();
		long when = System.currentTimeMillis();
		n.setTimestamp(when);
		n.setTitle(object.getTitle());

		n.setContent(new HashMap<String, Object>());
		List<EntityObject> eos = new ArrayList<EntityObject>();

		EntityObject eo = new EntityObject();
		eo.setEntityId(object.getEntityId());
		eo.setType(getEntityType(object));
		eo.setTitle(object.getTitle());
		eo.setId(object.getId());
		eos.add(eo);

		n.getContent().put("type", "moderation");
		n.getContent().put("note", note);
		n.getContent().put("modificationTime", modificationTime);
		n.setEntities(eos);

		try {
			communicatorConnector().sendAppNotification(n, TS_APP, Collections.singletonList(userId), getToken());
		} catch (CommunicatorConnectorException e) {
			e.printStackTrace();
			logger .error("Failed to send notifications: "+e.getMessage(), e);
		}

	}

	@Override
	public void notifyAccepted(BaseDTObject object, String note, String userId, long modificationTime) {
	}


	/**
	 * @param o
	 * @return
	 */
	private String getEntityType(BaseDTObject o) {
		return o instanceof EventObject ? "event" : o instanceof POIObject ? "location" : "narrative";
	}
}
