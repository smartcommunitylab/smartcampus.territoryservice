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

package eu.trentorise.smartcampus.manager.utils;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import eu.trentorise.smartcampus.communicator.CommunicatorConnector;
import eu.trentorise.smartcampus.network.JsonUtils;
import eu.trentorise.smartcampus.network.RemoteConnector;

/**
 * @author raman
 *
 */
public class SCServiceConnector extends RemoteConnector {

	private static final String PATH_TOKEN = "oauth/token";

	@Autowired
	@Value("${aacURL}")
	private String aacURL;
	@Autowired
	@Value("${smartcampus.clientId}")
	private String clientId = null;
	@Autowired
	@Value("${smartcampus.clientSecret}")
	private String clientSecret = null;
	
	private String token = null;
	private Long expiresAt = null;

	@Autowired
	@Value("${communicatorURL}")
	private String communicatorURL;

	private CommunicatorConnector connector = null;

	private Log logger = LogFactory.getLog(getClass());
	
	protected static final String TS_APP = "core.territory";
	
	protected CommunicatorConnector connector() {
		if (connector == null) {
			try {
				connector = new CommunicatorConnector(communicatorURL, TS_APP);
			} catch (Exception e) {
				logger.error("Failed to instantiate connector: "+e.getMessage(), e);
				e.printStackTrace();
			}
		}
		return connector;
	}

	@SuppressWarnings("rawtypes")
	protected String getToken() {
		if (token == null || System.currentTimeMillis() + 10000 > expiresAt) {
	        final HttpResponse resp;
	        if (!aacURL.endsWith("/")) aacURL += "/";
	        String url = aacURL + PATH_TOKEN+"?grant_type=client_credentials&client_id="+clientId +"&client_secret="+clientSecret;
	        final HttpGet get = new HttpGet(url);
	        get.setHeader("Accept", "application/json");
            try {
				resp = getHttpClient().execute(get);
				final String response = EntityUtils.toString(resp.getEntity());
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					Map map = JsonUtils.toObject(response, Map.class);
					expiresAt = System.currentTimeMillis()+(Integer)map.get("expires_in")*1000;
					token = (String)map.get("access_token");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return token;
	}	
}
