/**
 * Copyright (c) 2009 Andrew Rapp. All rights reserved.
 *  
 * This file is part of XBee-XMPP
 *  
 * XBee-XMPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * XBee-XMPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with XBee-XMPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rapplogic.xbee.xmpp;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

public class XBeeGtalkCommon {

	private final static Logger log = Logger.getLogger(XBeeGtalkCommon.class);
	
	/**
	 * Creates a connection to Google talk.  
	 * If server/port is not specified, talk.google.com/5222 is used.
	 * 
	 * @param server
	 * @param port
	 * @param user
	 * @param password
	 * @return
	 * @throws XMPPException
	 */
	public static XMPPConnection connect(String server, Integer port, String user, String password) throws XMPPException {
		
		ConnectionConfiguration connConfig = null;
		
		if (server == null || port == null) {
			// use default gtalk host/port
			connConfig = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
		} else {
			// use alternate host/port, typically useful for using a ssh tunnel
			connConfig = new ConnectionConfiguration(server, port, "gmail.com");			
		}

		XMPPConnection connection = new XMPPConnection(connConfig);
		
		connection.connect();
		
		// gtalk requires this
		SASLAuthentication.supportSASLMechanism("PLAIN", 0);
		
		connection.login(user, password, "smack");
		
		// set status to available
		Presence presence = new Presence(Presence.Type.available);
		connection.sendPacket(presence);	
		
		return connection;
	}   
	
	/**
	 * Returns true if presence starts with "available"
	 * 
	 * @param presence
	 * @return
	 */
	public static boolean isAvailable(Presence presence) {
		return presence.toString().startsWith("available");
	}
}
