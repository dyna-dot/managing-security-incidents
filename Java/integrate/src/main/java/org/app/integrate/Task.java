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
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

/**
 * 
 * @author Rahul Reddy Ravipally
 *
 */

public class Task {

	public static void main(String[] args) {

		final ArrayList<String> ar = new ArrayList<String>();
		Scanner scanner = new Scanner(System.in);

		System.out.println("Enter your QRadar username:");
		final String name = scanner.nextLine();
		System.out.println("Enter your QRadar password:");
		final String password = scanner.nextLine();
		System.out.println("Enter your QRadar hostname/IP Address:");
		final String host_qradar = scanner.nextLine();

		System.out.println("Enter your Resilient email:");
		final String email = scanner.nextLine();
		System.out.println("Enter your Resilient password:");
		final String resilient_password = scanner.nextLine();
		System.out.println("Enter your Resilient hostname/IP Address:");
		final String host_resilient = scanner.nextLine();

		scanner.close();

		final QRadar q = new QRadar(name, password, host_qradar, email, resilient_password, host_resilient);

		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				ArrayList<String> s = null;
				try {
					s = q.send_offences(ar);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}

				if (!s.isEmpty()) {

					for (int i = 0; i < s.size(); i++) {
						ar.add(s.get(i));
						System.out.println("Successfully sent offence id " + s.get(i) + " to resilient");
						System.out.println("Offence ID's that are sent to resilient : " + ar);
					}
				} else {
					System.out.println("Waiting for new offences");
				}

			}

		};

		Timer timer = new Timer();
		long delay = 0;
		long intevalPeriod = 1 * 30000;

		timer.scheduleAtFixedRate(task, delay, intevalPeriod);

	} // end of main
}