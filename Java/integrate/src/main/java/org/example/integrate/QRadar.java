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
package org.example.integrate;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Wrapper class for QRadar.
 * 
 * @author Rahul Reddy Ravipally
 *
 */

public class QRadar {

	String name = "";
	String password = "";
	String host_qradar = "";

	String email = "";
	String resilient_password = "";
	String host_resilient = "";
	
	/**
	 * Constructor
	 * 
	 * @param name 
	 * @param password
	 * @param host_qradar
	 * @param email
	 * @param resilient_password
	 * @param host_resilient
	 */
	
	public QRadar(String name, String password, String host_qradar, String email, String resilient_password,
			String host_resilient) {
		this.name = name;
		this.password = password;
		this.host_qradar = host_qradar;

		this.email = email;
		this.resilient_password = resilient_password;
		this.host_resilient = host_resilient;

	}

	Resilient r = new Resilient();
	
	/**
	 * Send offences to Resilient Class.
	 * 
	 * @param ids
	 * @return new_ids
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ParseException
	 */
	
	public ArrayList<String> send_offences(ArrayList<String> ids)
			throws ClientProtocolException, IOException, ParseException {

		ArrayList<String> new_ids = new ArrayList<String>(); // All the new offence id's are added here.

		String authString = name + ":" + password;
		
		byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		String authStringEnc = new String(authEncBytes);

		String authcode = "Basic" + " " + authStringEnc;

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

		HttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).setConnectionManager(cm).build();

		String url = "https://" + host_qradar + "/api/siem/offenses";
		HttpGet httpGet = new HttpGet(url);
		httpGet.addHeader("Version", "7.0");
		httpGet.addHeader("Authorization", authcode);
		httpGet.addHeader("Accept", "application/json");
		httpGet.addHeader("Content-Type", "application/json");

		HttpResponse response = httpclient.execute(httpGet);
		HttpEntity entity = response.getEntity();

		String responseString = EntityUtils.toString(entity, "UTF-8");

		JSONParser parser = new JSONParser();

		JSONArray jsonArr = (JSONArray) parser.parse(responseString);

		for (int i = 0; i < jsonArr.size(); i++) {
			JSONObject jsonObj = (JSONObject) jsonArr.get(i);
			String id = jsonObj.get("id").toString();
			if (!ids.contains(id)) {
				if (jsonObj.get("status").toString().equals("OPEN")) {
					// You can add your own offence source below
					if (jsonObj.get("offense_source").toString().equals("speeding violation")
							|| jsonObj.get("offense_source").toString().equals("wrong location")) {
						new_ids.add(id);
						System.out.println("PREPARING TO SEND THE OFFENCE : " + jsonObj);

						r.set_org(email, resilient_password, host_resilient);
						String res = r.create_incident(jsonObj.get("offense_source").toString(),
								jsonObj.get("description").toString(), jsonObj.get("severity").toString(),
								jsonObj.get("start_time").toString());

						System.out.println(
								"SENT THE OFFENCE WITH ID : " + jsonObj.get("id").toString() + " TO RESILIENT");

						System.out.println(res);
					}
				}
			}
		}

		return new_ids;

	}

}
