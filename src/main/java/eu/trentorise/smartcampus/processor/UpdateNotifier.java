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

import eu.trentorise.smartcampus.dt.model.EventObject;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.dt.model.StoryObject;

/**
 * @author raman
 *
 */
public interface UpdateNotifier {

	public abstract void eventCreated(EventObject eo);

	public abstract void eventUpdated(EventObject obj);

	public abstract void eventDeleted(EventObject obj);

	public abstract void poiCreated(POIObject poi);

	public abstract void poiUpdated(POIObject obj);

	public abstract void poiDeleted(POIObject obj);

	public abstract void storyCreated(StoryObject obj);

	public abstract void storyUpdated(StoryObject obj);

	public abstract void storyDeleted(StoryObject obj);

}