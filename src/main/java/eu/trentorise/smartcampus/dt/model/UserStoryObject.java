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



public class UserStoryObject extends StoryObject {
	private static final long serialVersionUID = -4832244350331096503L;

	public GenericStory toGenericStory() {
		GenericStory result = new GenericStory();
		result.setId(getId());
		result.setSource(getSource());
		result.setTitle(getTitle());
		result.setType(getType());
		result.setDescription(getDescription());

		if (getSteps() != null) {
			result.setSteps(new GenericStoryStep[getSteps().size()]);
			for (int i = 0; i < getSteps().size(); i++) {
				result.getSteps()[i] = new GenericStoryStep(getSteps().get(i).getPoiId(), getSteps().get(i).getNote());
			}
		} else {
			result.setSteps(new GenericStoryStep[0]);
		}
		
		return result;
	}

}
