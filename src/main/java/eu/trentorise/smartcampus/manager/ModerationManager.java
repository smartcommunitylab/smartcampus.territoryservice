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
import eu.trentorise.smartcampus.dt.model.CommunityData;
import eu.trentorise.smartcampus.dt.model.ModerationItem;
import eu.trentorise.smartcampus.network.JsonUtils;

/**
 * @author raman
 *
 */
@Component
public class ModerationManager {

	public enum MODERATION_MODE {DISABLED, PRE, POST};
	public enum STATE {A,D};
	
	private static final String TYPE_OBJECT = "object";
	private static final String TYPE_COMM_DATA = "comm_data";
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

	@SuppressWarnings("unchecked")
	public void moderateObject(BaseDTObject oldObj, BaseDTObject newObj, String user) {
		Map<String,Object> oldValue = oldObj !=null ? JsonUtils.convert(oldObj, Map.class) : null;
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
		if (moderationFile == null) return;
		
		BufferedReader bin = null;
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
				ModerationItem item = storage.getItemById(arr[0]);
				if (STATE.valueOf(arr[1]).equals(STATE.D)) rejectUpdate(item);
				else confirmUpdate(item);
			}
		} finally {
			if (bin != null) bin.close();
		}
	}

	/**
	 * @param item
	 */
	private void rejectUpdate(ModerationItem item) {
		if (item != null) {
			try {
				if (TYPE_OBJECT.equals(item.getType())) {
					try {
						BaseDTObject obj = (BaseDTObject)dataStorage.getObjectById(item.getObjectId());
						if (item.getOldValue() == null || item.getOldValue().isEmpty()) {
							obj.deleteDO(domainEngineClient, dataStorage);
						}
					} catch (Exception e) {
						logger.warn("Object "+item.getObjectId() +" not found: "+e.getMessage());
					}
				}
				storage.removeItem(item);
			} catch (Exception e) {
				e.printStackTrace();
				logger.warn("Failed to confirm object "+item.getObjectId()+": "+e.getMessage());
				return;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void confirmUpdate(ModerationItem item) {
		if (item != null) {
			try {
				BaseDTObject obj = null;
				if (TYPE_COMM_DATA.equals(item.getType())) {
					try {
						obj = (BaseDTObject)dataStorage.getObjectById(item.getObjectId());
					} catch (Exception e) {
						logger.warn("Object "+item.getObjectId() +" not found: "+e.getMessage());
					}
					if (obj != null) {
						Map<String,Object> map = JsonUtils.convert(obj.getCommunityData(), Map.class);
						map.putAll(item.getNewValue());
						obj.setCommunityData(JsonUtils.convert(map, CommunityData.class));
					}
				} else {
					Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(item.getObjectType());
					obj = (BaseDTObject)JsonUtils.convert(item.getNewValue(), cls);
				}
				if (obj != null) {
					obj.confirmDO(item.getUserId(), domainEngineClient, dataStorage, this);
				}
				storage.removeItem(item);
			} catch (Exception e) {
				e.printStackTrace();
				logger.warn("Failed to confirm object "+item.getObjectId()+": "+e.getMessage());
				return;
			}
		}
	}

	public MODERATION_MODE getModerationMode() {
		return mode == null || mode.isEmpty() ? MODERATION_MODE.DISABLED : MODERATION_MODE.valueOf(mode);
	}

	/**
	 * @param oldData
	 * @param commData
	 * @param userId
	 */
	public void moderateCommunityData(String id, Map<String, Object> oldData, Map<String, Object> newData, String userId) {
		ModerationItem item = storage.storeItem(id, userId, TYPE_COMM_DATA, oldData, newData, CommunityData.class.getCanonicalName());
		sendModerationRequest(item);
		
	}
}
