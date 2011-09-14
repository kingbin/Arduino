/**
 * Copyright (c) 2009 Andrew Rapp. All rights reserved.
 *  
 * This file is part of XBeeMotion.
 *  
 * XBeeMotion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * XBeeMotion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with XBeeMotion.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rapplogic.xbeemotion;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Reads xbee motion properties
 * 
 * @author andrew
 *
 */
public class MotionProperties {
	private final List<String> gtalkRecipients = new ArrayList<String>();
	
	private String gtalkUsername;
	private String gtalkPassword;
	private String xbeeComPort;
	private int xbeeBaudRate;
	private int motionDelaySeconds;
	
	public String getGtalkUsername() {
		return gtalkUsername;
	}

	public void setGtalkUsername(String gtalkUsername) {
		this.gtalkUsername = gtalkUsername;
	}

	public String getGtalkPassword() {
		return gtalkPassword;
	}

	public void setGtalkPassword(String gtalkPassword) {
		this.gtalkPassword = gtalkPassword;
	}

	public String getXbeeComPort() {
		return xbeeComPort;
	}

	public void setXbeeComPort(String xbeeComPort) {
		this.xbeeComPort = xbeeComPort;
	}

	public int getXbeeBaudRate() {
		return xbeeBaudRate;
	}

	public void setXbeeBaudRate(int xbeeBaudRate) {
		this.xbeeBaudRate = xbeeBaudRate;
	}

	public int getMotionDelaySeconds() {
		return motionDelaySeconds;
	}

	public void setMotionDelaySeconds(int motionDelaySeconds) {
		this.motionDelaySeconds = motionDelaySeconds;
	}

	public List<String> getGtalkRecipients() {
		return gtalkRecipients;
	}

	public String toString() {
		return "username=" + this.gtalkUsername + ",pass=" + this.gtalkPassword + ",comport=" + this.getXbeeComPort() +
		",baud=" + this.getXbeeBaudRate() + ",delay=" + this.getMotionDelaySeconds() + ",recipients=" + this.getGtalkRecipients();
	}
	
	public static MotionProperties parse(String path) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(path));
		
		MotionProperties mp = new MotionProperties();
		mp.setGtalkUsername(props.getProperty("gtalk-username"));
		mp.setGtalkPassword(props.getProperty("gtalk-password"));
		mp.setXbeeComPort(props.getProperty("xbee-com-port"));
		mp.setXbeeBaudRate(Integer.parseInt(props.getProperty("xbee-baud-rate")));
		mp.setMotionDelaySeconds(Integer.parseInt(props.getProperty("motion-delay-seconds")));
		
		String recipients = props.getProperty("gtalk-recipient");
		
		StringTokenizer st = new StringTokenizer(recipients, ",");
		
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			mp.getGtalkRecipients().add(token);
		}
		
		return mp;
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		MotionProperties mp = parse("xbee-motion.properties");
		System.out.println(mp);
	}
}
