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
package org.app.offences;

import java.io.IOException;
import java.util.Scanner;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;

/**
 * 
 * @author Rahul Reddy Ravipally
 *
 */

public class SendOffences {
	public static void main(String[] args) {		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter the QRadar Hostname/IP Address ");
		String hostname = scanner.nextLine();
		
		// Initialise sender
		UdpSyslogMessageSender messageSender = new UdpSyslogMessageSender();
		messageSender.setDefaultMessageHostname("myhostname"); // some syslog cloud services may use this field to transmit a secret key
		messageSender.setDefaultAppName("myapp");
		messageSender.setDefaultFacility(Facility.USER);
		messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
		messageSender.setSyslogServerHostname(hostname);
		messageSender.setSyslogServerPort(514);
		messageSender.setMessageFormat(MessageFormat.RFC_3164); // optional, default is RFC 3164

		// Send the offence
		System.out.println("Enter 1 to send SPEED related offence. ");
		System.out.println("Enter 2 to send LOCATION related offence. ");
		int choice = scanner.nextInt();
		scanner.close();

		try {
			switch (choice) {
			case 1:
				messageSender.sendMessage("CARNUMBER: \"KA00MA1234\" SPEED: \"180KMPH\" VIOLATION: \"SPEEDING\"");
				System.out.println("Message sent");
				break;
			case 2:
				messageSender.sendMessage("DEVICE ID: \"1234\" LOCATION: \"lat:0/long:0\" VIOLATION: \"LOCATION\"");
				System.out.println("Message sent ");
				break;
			default:
				System.out.println("Invalid input");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}