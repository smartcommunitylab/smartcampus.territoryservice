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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.EventObject;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.dt.model.StepObject;
import eu.trentorise.smartcampus.dt.model.StoryObject;
import eu.trentorise.smartcampus.manager.utils.SCServiceConnector;
import eu.trentorise.smartcampus.moderatorservice.model.ContentToModeratorService;
import eu.trentorise.smartcampus.moderatoservice.exception.ModeratorServiceException;
import eu.trentorise.smartcampus.social.model.Concept;

/**
 * @author raman
 *
 */
@Component
public class ModerationConnector extends SCServiceConnector {

	private static final String BASE_FORMAT = 
			  "  title: %s\n"
			+ "  descr: %s\n"
			+ "  type: %s\n"
			+ "  location: %s\n"
			+ "  fromTime: %s\n"
			+ "  toTime: %s\n"
			+ "  timing: %s\n"
			+ "  tags: %s";
	private static final String EVENT_FORMAT = BASE_FORMAT + 
			  "\n"
			+ "  poiId: %s"; 
	private static final String POI_FORMAT = BASE_FORMAT;
	private static final String STORY_FORMAT = BASE_FORMAT + 
			  "\n"
			+ "  steps: \n    %s"; 

	private static final String TAG_FORMAT = "{%s,%s,%s}";
	private static final String STEP_FORMAT = "{%s,%s}";
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyy HH:mm");
	
	
	public boolean requestModeration(BaseDTObject oldObj, BaseDTObject newObj, String userId, String id) throws SecurityException, ModeratorServiceException {
		String text = formatMessage(oldObj, newObj);
		ContentToModeratorService msg = new ContentToModeratorService(TS_APP, id, text, userId);
		return moderatorConnector().addContentToManualFilterByApp(getToken(), TS_APP, msg);
	}

	private String formatMessage(BaseDTObject oldObj, BaseDTObject newObj) {
		StringBuilder s = new StringBuilder();
		if (oldObj == null) s.append("action:CREATED\n");
		else {
			s.append("action:UPDATED\n");
			s.append("OLD:\n" + formatObject(oldObj)+"\n");
		}
		s.append("NEW:\n");
		s.append(formatObject(newObj));
		return s.toString();
	}

	/**
	 * @param oldObj
	 * @return
	 */
	private String formatObject(BaseDTObject o) {
		if (o instanceof EventObject) {
			return String.format(EVENT_FORMAT, 
					o.getTitle(), 
					o.getDescription(),
					o.getType(),
					o.getLocation() == null ? null : Arrays.toString(o.getLocation()),
					o.getFromTime() == null ? null : sdf.format(new Date(o.getFromTime())),
					o.getToTime() == null ? null : sdf.format(new Date(o.getToTime())),
					o.getTiming(),
					o.getCommunityData() == null || o.getCommunityData().getTags() == null ? null : formatTags(o.getCommunityData().getTags()),
					((EventObject) o).getPoiId());
		}
		else if (o instanceof POIObject) {
			return String.format(POI_FORMAT, 
					o.getTitle(), 
					o.getDescription(),
					o.getType(),
					o.getLocation() == null ? null : Arrays.toString(o.getLocation()),
					o.getFromTime() == null ? null : sdf.format(new Date(o.getFromTime())),
					o.getToTime() == null ? null : sdf.format(new Date(o.getToTime())),
					o.getTiming(),
					o.getCommunityData() == null || o.getCommunityData().getTags() == null ? null : formatTags(o.getCommunityData().getTags()));
		}
		else if (o instanceof StoryObject) {
			return String.format(STORY_FORMAT, 
					o.getTitle(), 
					o.getDescription(),
					o.getType(),
					o.getLocation() == null ? null : Arrays.toString(o.getLocation()),
					o.getFromTime() == null ? null : sdf.format(new Date(o.getFromTime())),
					o.getToTime() == null ? null : sdf.format(new Date(o.getToTime())),
					o.getTiming(),
					o.getCommunityData() == null || o.getCommunityData().getTags() == null ? null : formatTags(o.getCommunityData().getTags()),
					formatSteps(((StoryObject) o).getSteps()));
		}
		
		return String.format(BASE_FORMAT, 
				o.getTitle(), 
				o.getDescription(),
				o.getType(),
				o.getLocation() == null ? null : Arrays.toString(o.getLocation()),
				o.getFromTime() == null ? null : sdf.format(new Date(o.getFromTime())),
				o.getToTime() == null ? null : sdf.format(new Date(o.getToTime())),
				o.getTiming(),
				o.getCommunityData() == null || o.getCommunityData().getTags() == null ? null : formatTags(o.getCommunityData().getTags()));
	}

	/**
	 * @param tags
	 * @return
	 */
	private String formatTags(List<Concept> tags) {
		List<String> res = new ArrayList<String>(tags.size());
		for (Concept c : tags) res.add(String.format(TAG_FORMAT, c.getName(), c.getSummary(), c.getDescription()));
		return res.toString();
	}

	/**
	 * @param tags
	 * @return
	 */
	private Object formatSteps(List<StepObject> steps) {
		List<String> res = new ArrayList<String>(steps.size());
		if (steps != null) {
			for (StepObject s : steps) res.add(String.format(STEP_FORMAT, s.getNote(), s.getPoiId()));
		}
		return res.toString();
	}

	/**
	 * @throws ModeratorServiceException 
	 * @throws SecurityException 
	 * 
	 */
	public List<ContentToModeratorService> getModerated() throws SecurityException, ModeratorServiceException {
		List<ContentToModeratorService> list = moderatorConnector().getAllManualContent(getToken(), TS_APP);
		return list;
	}

	/**
	 * @param c
	 * @throws ModeratorServiceException 
	 * @throws SecurityException 
	 */
	public void deleteModerated(ContentToModeratorService c) throws SecurityException, ModeratorServiceException {
		moderatorConnector().deleteByObjectId(getToken(), TS_APP, c.getObjectId());
	}
}
