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
package eu.trentorise.smartcampus.listener;

import it.sayservice.platform.client.DomainEngineClient;
import it.sayservice.platform.client.InvocationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Subscriber {

	private Log logger = LogFactory.getLog(getClass());
	
	public Subscriber(DomainEngineClient client) {
		try {
			client.subscribeDomain("eu.trentorise.smartcampus.domain.discovertrento.BaseDTObject", null);
			client.subscribeDomain("eu.trentorise.smartcampus.domain.discovertrento.POIObject", null);
			client.subscribeDomain("eu.trentorise.smartcampus.domain.discovertrento.EventObject", null);
			client.subscribeDomain("eu.trentorise.smartcampus.domain.discovertrento.StoryObject", null);
		} catch (InvocationException e) {
			logger.error("Failed to subscribe for domain events: "+e.getMessage());
		}
	}
}
