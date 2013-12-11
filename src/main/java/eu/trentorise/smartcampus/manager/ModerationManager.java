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

import it.sayservice.platform.client.DomainEngineClient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.data.GeoTimeObjectSyncStorage;
import eu.trentorise.smartcampus.data.ModerationStorage;
import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.ModerationItem;
import eu.trentorise.smartcampus.network.JsonUtils;

/**
 * Manager of data moderation activities. Moderation is parameteric, i.e.,
 * one can do pre- or post-moderation or disable it.
 * @author raman
 */
@Component
public class ModerationManager {

	public enum MODERATION_MODE {DISABLED, PRE, POST};
	public enum STATE {A, D, W};
	
	private static final String TYPE_OBJECT = "object";
	@Autowired
	protected GeoTimeObjectSyncStorage dataStorage;
	@Autowired
	protected DomainEngineClient domainEngineClient; 
	@Autowired
	private ModerationStorage storage = null;
	
	@Value("${moderation.mode}")
	private String mode;
	@Value("${moderation.file}")
	private Resource moderationFile;
	private Log logger = LogFactory.getLog(getClass());

	/**
	 * Request object moderation given the old value, new value, and the user. 
	 * @param oldObj
	 * @param newObj
	 * @param user
	 */
	@SuppressWarnings("unchecked")
	public void moderateObject(BaseDTObject oldObj, BaseDTObject newObj, String user) {
		Map<String,Object> oldValue = null;
		// if there are previous objects
		if (oldObj != null) {
			// use the last valid value from the currently moderated ones
			List<ModerationItem> list = storage.getByObjectId(newObj.getId());
			if (list != null && !list.isEmpty()) {
				Collections.sort(list);
				oldValue = list.get(0).getOldValue();
			// otherwise use the input value 	
			} else {
				oldValue = JsonUtils.convert(oldObj, Map.class);
			}
		}
		Map<String,Object> newValue = JsonUtils.convert(newObj, Map.class); 
			ModerationItem item = storage.storeItem(newObj.getId(), user, TYPE_OBJECT, oldValue, newValue, newObj.getClass().getCanonicalName());
		sendModerationRequest(item);
	}

	/**
	 * @param item
	 */
	private void sendModerationRequest(ModerationItem item) {
		// TODO Auto-generated method stub
	}
	
	@Scheduled(fixedRate = 30000)
	public void checkModerationResults() {
		//TODO move to the service
		try {
			checkFile();
		} catch (IOException e) {
			logger.error("Failed to process file: "+e.getMessage(),e);
		}
	}
	
	/**
	 * @throws FileNotFoundException 
	 * 
	 */
	private void checkFile() throws IOException {
		if (MODERATION_MODE.DISABLED.equals(getModerationMode())) return;
		
		if (moderationFile == null) return;
		
		BufferedReader bin = null;
		Map<String,List<ModerationItem>> itemGroups = new HashMap<String, List<ModerationItem>>();
		Map<String,STATE> states = new HashMap<String, ModerationManager.STATE>();
		Map<String,String> notes = new HashMap<String, String>();
		
		try {
			bin = new BufferedReader(new InputStreamReader(moderationFile.getInputStream()));
			String line = null;
			while ((line = bin.readLine()) != null) {
				line = line.trim();
				// bypass comment lines or empty lines
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				String[] arr = line.split(",");
				STATE state = STATE.valueOf(arr[1]);

				String note = arr.length > 2 ? arr[2] : null;
				
				ModerationItem item = storage.getItemById(arr[0]);
				if (item == null) continue;
				
				List<ModerationItem> items = itemGroups.get(item.getObjectId());
				if (items == null) {
					items = new ArrayList<ModerationItem>();
					itemGroups.put(item.getObjectId(), items);
				}
				items.add(item); 
				notes.put(arr[0], note);
				states.put(arr[0], state);
			}
			
			for (String objectId : itemGroups.keySet()) {
				checkObject(itemGroups.get(objectId), states, notes);
			}
			
		} finally {
			if (bin != null) bin.close();
		}
	}

	/**
	 * Check the object modifications, given the modification states, and notes.
	 * 
	 * @param list
	 * @param states
	 * @param notes
	 */
	private void checkObject(List<ModerationItem> list, Map<String, STATE> states, Map<String, String> notes) {
		Collections.sort(list);
		ModerationItem 
			firstRejected = null, // first modification rejected after the object approval
			lastAccepted = null; // last approved modification of the whole object 
		
		int firstPending = list.size();
		
		for (int i = list.size() - 1; i >= 0; i--) {
			ModerationItem item = list.get(i);
			// look for first moderated: no accepted before
			if (lastAccepted == null && states.get(item.getId()).equals(STATE.W)) {
				firstPending = i;
			}
			if (lastAccepted == null && states.get(item.getId()).equals(STATE.D)) {
				firstRejected = item;
			}
			
			if (lastAccepted == null && states.get(item.getId()).equals(STATE.A)) {
				lastAccepted = item;
			}
		}
		
		// in case of pre-moderation we should commit the correct changes
		if (getModerationMode().equals(MODERATION_MODE.PRE)) {
			// commit last approved full change
			if (lastAccepted != null) {
				confirmUpdate(lastAccepted, notes.get(lastAccepted.getId()));
				storage.updateReferenceValue(lastAccepted.getObjectId(),lastAccepted.getOldValue());
			}
			// if no approvals and no pending decisions, revert the first rejection in case that one is creation
			if (lastAccepted == null && firstRejected != null && firstPending < list.size()) {
				rejectUpdate(firstRejected, notes.get(firstRejected.getId()), true);
			}
		}
		// in case of post moderation when the correct value were overwritten, we should rollback
		if (getModerationMode().equals(MODERATION_MODE.POST) && firstRejected != null) {
			// commit last approved full change
			if (lastAccepted != null) {
				confirmUpdate(lastAccepted, notes.get(lastAccepted.getId()));
				storage.updateReferenceValue(lastAccepted.getObjectId(),lastAccepted.getOldValue());
			}
			// if no approvals, revert the first rejection to origin
			// or delete if there are no pending and no previous
			if (lastAccepted == null && firstRejected != null) {
				rejectUpdate(firstRejected, notes.get(firstRejected.getId()), firstPending >= list.size() && firstRejected.getOldValue() == null);
			}
		}

		int lastAcceptedPos = list.indexOf(lastAccepted);
		for (int i = 0; i < list.size(); i++) {
			if (i < lastAcceptedPos || !STATE.W.equals(states.get(list.get(i).getId()))) {
				storage.removeItem(list.get(i));
			}
		}
		
		if (getModerationMode().equals(MODERATION_MODE.POST)) {
			// TODO notify the users
			// notify all the rejected updates from the lastAccepted onwards
		}
	}

	/**
	 * Reject update reverting to previous version or removing the object if there wer no approved versions
	 * @param item
	 * @param note 
	 * @param toDelete
	 */
	private void rejectUpdate(ModerationItem item, String note, boolean toDelete) {
		if (item != null) {
			try {
				if (toDelete) {
					BaseDTObject obj = (BaseDTObject)dataStorage.getObjectById(item.getObjectId());
					if (item.getOldValue() == null || item.getOldValue().isEmpty()) {
						obj.deleteDO(domainEngineClient, dataStorage);
					}
				} else {
					callConfirm(item.getObjectId(), item.getObjectType(), item.getOldValue(), item.getUserId());
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.warn("Failed to confirm object "+item.getObjectId()+": "+e.getMessage());
				return;
			}
		}
	}

	/**
	 * Confirm the change storing the modification in the domain
	 * @param item
	 * @param note
	 */
	private void confirmUpdate(ModerationItem item, String note) {
		if (item != null) {
			callConfirm(item.getObjectId(), item.getObjectType(),item.getNewValue(),item.getUserId());
		}
	}

	private void callConfirm(String objectId, String objectType, Map<String,Object> data, String userId) {
		try {
			Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(objectType);
			BaseDTObject obj = (BaseDTObject)JsonUtils.convert(data, cls);
			if (obj != null) {
				obj.confirmDO(userId, domainEngineClient, dataStorage, this);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("Failed to confirm object "+objectId+": "+e.getMessage());
			return;
		}
	}

	public MODERATION_MODE getModerationMode() {
		return mode == null || mode.isEmpty() ? MODERATION_MODE.DISABLED : MODERATION_MODE.valueOf(mode);
	}

}
