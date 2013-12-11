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

package eu.trentorise.smartcampus.data;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.dt.model.ModerationItem;

/**
 * Data storage for the objects to be moderated
 * 
 * @author raman
 *
 */
@Component
public class ModerationStorage {

	@Autowired
	protected MongoOperations mongoTemplate = null;
	
	public ModerationItem storeItem(String objectId, String user, String type, Map<String,Object> oldValue, Map<String,Object> newValue, String objectType) {
		ModerationItem item = new ModerationItem(objectId, user, type, oldValue, newValue, objectType);
		item.setId(new ObjectId().toString());
		mongoTemplate.save(item);
		return item;
	}
	
	public void removeItem(ModerationItem item) {
		mongoTemplate.remove(item);
	}
	
	public ModerationItem getItemById(String id) {
		return mongoTemplate.findById(id, ModerationItem.class);
	}
	
	public List<ModerationItem> getByObjectId(String objectId) {
		return mongoTemplate.find(Query.query(Criteria.where("objectId").is(objectId)), ModerationItem.class);
	}
	public List<ModerationItem> getByUser(String userId) {
		return mongoTemplate.find(Query.query(Criteria.where("userId").is(userId)), ModerationItem.class);
	}
	public List<ModerationItem> getByObjectIdAndUser(String objectId, String user) {
		return mongoTemplate.find(Query.query(Criteria.where("objectId").is(objectId).and("userId").is(user)), ModerationItem.class);
	}

	public void updateReferenceValue(String objectId, Map<String, Object> oldValue) {
		mongoTemplate.updateMulti(Query.query(Criteria.where("objectId").is(objectId)), Update.update("oldValue", oldValue), ModerationItem.class);
	}

}
