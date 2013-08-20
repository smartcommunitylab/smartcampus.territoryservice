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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.geo.Circle;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.BasicDBObjectBuilder;

import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.presentation.common.exception.DataException;
import eu.trentorise.smartcampus.presentation.data.BasicObject;
import eu.trentorise.smartcampus.presentation.storage.sync.mongo.GenericObjectSyncMongoStorage;

public class GeoTimeStorage extends GenericObjectSyncMongoStorage<GeoTimeSyncObjectBean> implements GeoTimeObjectSyncStorage {

	public GeoTimeStorage(MongoOperations mongoTemplate) {
		super(mongoTemplate);
		mongoTemplate.getCollection(mongoTemplate.getCollectionName(GeoTimeSyncObjectBean.class)).ensureIndex(BasicDBObjectBuilder.start("location", "2d").get());
	}

	@Override
	public Class<GeoTimeSyncObjectBean> getObjectClass() {
		return GeoTimeSyncObjectBean.class;
	}

	private static <T> Criteria createSearchCriteria(Class<T> cls, Circle circle,
			Long from, Long to, Map<String, Object> inCriteria, String text) {
		Criteria criteria = new Criteria();
		if (cls != null) {
			criteria.and("type").is(cls.getCanonicalName());
		}
		criteria.and("deleted").is(false);
		if (inCriteria != null) {
			for (String key : inCriteria.keySet()) {
				criteria.and("content."+key).is(inCriteria.get(key));
			}
		}
		if (circle != null) {
//			criteria.and("location").near(circle.getCenter()).maxDistance(circle.getRadius());
			criteria.and("location").within(circle);
		}
		if (text != null && !text.isEmpty()) {
			Criteria[] or = new Criteria[3];
			or[0] = new Criteria("content.title").regex(text.toLowerCase(),"i");
			or[1] = new Criteria("content.description").regex(text.toLowerCase(),"i");
			or[2] = new Criteria("content.poi.street").regex(text.toLowerCase(),"i");
			criteria.orOperator(or);
		}
		
		if (from != null || to != null) {
			if (from != null) {
				criteria.and("toTime").gte(from);
			}
			if (to != null) {
				criteria.and("fromTime").lte(to);
			}
		}
		return criteria;
	}


	@Override
	public <T extends BaseDTObject> List<T> searchObjects(Class<T> inCls, Circle circle, String text, Long from, Long to, Map<String, Object> inCriteria, SortedMap<String,Integer> sort) throws DataException {
		return searchObjects(inCls, circle, text, from, to, inCriteria, sort, 0, 0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends BaseDTObject> List<T> searchObjects(Class<T> inCls, Circle circle, String text, Long from, Long to, Map<String, Object> inCriteria, SortedMap<String,Integer> sort, int limit, int skip) throws DataException {
		Criteria criteria = createSearchCriteria(inCls, circle, from, to, inCriteria, text);
		Query query = Query.query(criteria);
		if (limit > 0) query.limit(limit);
		if (skip > 0) query.skip(skip);
		if (sort != null && !sort.isEmpty()) {
			for (String key : sort.keySet()) {
				Order order = sort.get(key) > 0 ? Order.ASCENDING : Order.DESCENDING;
				query.sort().on("content."+key, order);
			}
		}
		
		Class<T> cls = inCls;
		if (cls == null) cls = (Class<T>)BaseDTObject.class;

		return find(query, cls);
	}

	@Override
	protected <T extends BasicObject> GeoTimeSyncObjectBean convertToObjectBean(T object) throws InstantiationException, IllegalAccessException {
		GeoTimeSyncObjectBean bean = super.convertToObjectBean(object);
		BaseDTObject baseObject = (BaseDTObject) object;
		bean.setLocation(baseObject.getLocation());
		if (baseObject.getFromTime() != null) {
			bean.setFromTime(baseObject.getFromTime());
		} else {
			bean.setFromTime(0L);
		}
		if (baseObject.getToTime() != null && baseObject.getToTime() != 0) {
			bean.setToTime(baseObject.getToTime());
		} else {
			bean.setToTime(bean.getFromTime());
		}
		return bean;
	}

	public static void main(String[] args) {
		Criteria c = createSearchCriteria(GeoTimeSyncObjectBean.class, null, Long.MAX_VALUE, null, new HashMap<String, Object>(), "text");
		System.err.println(c.getCriteriaObject());
	}
	
}
