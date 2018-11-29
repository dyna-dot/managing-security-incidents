/****************************************************** 
 *  Copyright 2018 IBM Corporation 
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License.
 */
package org.app.integrate;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Wrapper class for Resilient.
 * 
 * @author Rahul Reddy Ravipally
 *
 */

public class Resilient {
	String email = "";
	String password = "";
	String host_resilient = "";
	String orgID = "";
	String orgNAME = "";
	String orgid = "";
	String orgname = "";

	public void set_org(String email, String password, String host_resilient) {
		this.email = email;
		this.password = password;
		this.host_resilient = host_resilient;
	}

	/**
	 * Send incidents to Resilient.
	 * 
	 * @param name
	 * @param description
	 * @param severity
	 * @param discovered_date
	 * @return response_from_resilient
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ParseException
	 */

	public String create_incident(String name, String description, String severity, String discovered_date)
			throws ClientProtocolException, IOException, ParseException {

		final SSLConnectionSocketFactory sslsf;
		try {
			sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(), NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", new PlainConnectionSocketFactory()).register("https", sslsf).build();

		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
		cm.setMaxTotal(100);

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).setConnectionManager(cm).build();

		boolean flag = false;
		int response_code = 0;
		String response_from_resilient = "";
		String incident_id = "";
		String org_id = "";

// Generate csrf token 

		CookieStore cookieStore = new BasicCookieStore();
		HttpContext context = new BasicHttpContext();

		String payload = "{\"email\":" + "\"" + email + "\"" + "," + "\"password\":" + "\"" + password + "\"" + "}";
		StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_FORM_URLENCODED);

		context.setAttribute("http.cookie-store", cookieStore);
		String session_url = "https://" + host_resilient + "/rest/session";
		HttpPost request = new HttpPost(session_url);
		request.setHeader("content-type", "application/json");
		request.setEntity(entity);

		HttpResponse response = httpClient.execute(request);
		HttpEntity ent = response.getEntity();

		System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
		String responseString = EntityUtils.toString(ent, "UTF-8");
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(responseString);

		String csrf = json.get("csrf_token").toString();
		if (csrf != null) {
			flag = true;
		} else {
			System.out.println("failed to get csrf tokens");
		}

// Get org ID's 

		String s = json.get("orgs").toString();

		JSONArray jsonArr = (JSONArray) parser.parse(s);

		for (int i = 0; i < jsonArr.size(); i++) {

			JSONObject jsonObj = (JSONObject) jsonArr.get(i);
			orgid = jsonObj.get("id").toString();
			String x = jsonObj.get("instance_roles").toString();
			JSONArray j = (JSONArray) parser.parse(x);
			JSONObject jsonnew = (JSONObject) j.get(0);
			String z = jsonnew.get("typed_object").toString();
			Object obj = parser.parse(z);
			JSONObject jnew = (JSONObject) obj;
			orgname = jnew.get("object_name").toString();
			if (name.equals("speeding violation") && orgname.equals("abc")) {
				orgID = orgid;
				orgNAME = orgname;
			} else if (name.equals("wrong location") && orgname.equals("xyz")) {
				orgID = orgid;
				orgNAME = orgname;
			}

		}
// Send the incidents to Resilient 

		if (flag) {

			String incident = "{\"name\":" + "\"" + name + "\"" + "," + "\"discovered_date\":" + discovered_date + "}";
			StringEntity entity_incident = new StringEntity(incident, ContentType.APPLICATION_FORM_URLENCODED);

			String incident_url = "https://" + host_resilient + "/rest/orgs/" + orgID + "/incidents";
			HttpPost post_incident = new HttpPost(incident_url);
			post_incident.addHeader("content-type", "application/json");
			post_incident.addHeader("Accept-Language", "en-US,en;q=0.9");
			post_incident.addHeader("Accept", "*/*");
			post_incident.addHeader("text_content_output_format", "objects_convert");
			post_incident.addHeader("X-Requested-With", "XMLHttpRequest");
			post_incident.addHeader("X-sess-id", csrf);
			post_incident.setEntity(entity_incident);

			HttpResponse incident_response = httpClient.execute(post_incident);
			HttpEntity ent_incident = incident_response.getEntity();

			response_code = incident_response.getStatusLine().getStatusCode();
			System.out.println("Incident Response Code : " + incident_response.getStatusLine().getStatusCode());
			String res = EntityUtils.toString(ent_incident, "UTF-8");

			if (response_code == 200) {
				JSONParser p = new JSONParser();
				JSONObject data = (JSONObject) p.parse(res);

				incident_id = data.get("id").toString();
				org_id = data.get("org_id").toString();

			}
		}

		if (response_code == 200) {
			response_from_resilient = "INCIDENT CREATED IN  ORG : " + orgNAME + " ( ORG ID : " + org_id + ")"
					+ " WITH ID : " + incident_id;
		} else {
			response_from_resilient = "An error occured with response code " + response_code;
		}

		return response_from_resilient;

	}
}
