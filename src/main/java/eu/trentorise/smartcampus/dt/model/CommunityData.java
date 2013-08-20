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
package eu.trentorise.smartcampus.dt.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.trentorise.smartcampus.social.model.Concept;

public class CommunityData implements Serializable {
	private static final long serialVersionUID = 5926048335916274968L;

	protected List<Concept> tags;
	protected int averageRating;
	protected Map<String, Integer> rating;
	protected Map<String, String> following = new HashMap<String, String>();
	private int ratingsCount = 0;
	private int followsCount = 0;

	public CommunityData() {
		super();
	}

	public void setFollowing(Map<String, String> following) {
		this.following = following;
		setFollowsCount(following == null? 0 : following.size());
	}

	public void setRating(Map<String, Integer> rating) {
		this.rating = rating;
		setRatingsCount(rating == null? 0 : rating.size());
	}
	
	public static void filterUserData(CommunityData data, String userId) {
		if (data == null) return;
		Map<String, Integer> ratings = data.getRating();
		if (ratings != null && !ratings.isEmpty()) {
			if (ratings.containsKey(userId)) {
				data.setRating(Collections.singletonMap(userId, ratings.get(userId)));
			} else {
				data.setRating(Collections.<String,Integer>emptyMap());
			}
			data.setRatingsCount(ratings.size());
		}
		if (data.getFollowing() != null && ! data.getFollowing().isEmpty()) {
			if (data.getFollowing().containsKey(userId)) {
				data.setFollowing(Collections.singletonMap(userId,data.getFollowing().get(userId)));
			} else {
				data.setFollowing(Collections.<String,String>emptyMap());
			}
			data.setFollowsCount(data.getFollowing().size());
		}
	}

	public static void filterUserData(List<CommunityData> datas, String userId) {
		for (CommunityData data : datas) {
			filterUserData(data, userId);
		}
	}

	public int getRatingsCount() {
		return ratingsCount;
	}

	public void setRatingsCount(int ratingsCount) {
		this.ratingsCount = ratingsCount;
	}

	public int getFollowsCount() {
		return followsCount;
	}

	public void setFollowsCount(int followCount) {
		this.followsCount = followCount;
	}
	
	public Map<String, String> getFollowing() {
		return following;
	}

	public List<Concept> getTags() {
		return tags;
	}

	public void setTags(List<Concept> tags) {
		this.tags = tags;
	}

	public int getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(int averageRating) {
		this.averageRating = averageRating;
	}

	public Map<String, Integer> getRating() {
		return rating;
	}
}
