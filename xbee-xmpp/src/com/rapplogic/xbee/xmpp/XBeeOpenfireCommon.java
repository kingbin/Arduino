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

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

public class XBeeOpenfireCommon {
		
	public static XMPPConnection connect(String server, int port, String user, String password) throws XMPPException {
		ConnectionConfiguration config = new ConnectionConfiguration(server, port);
		XMPPConnection connection = new XMPPConnection(config);
		//log.info("connecting to openfire " + server + ", " + port);
		connection.connect();
		connection.login(user, password);
		
		// the server seems to do this automatically, but can't hurt
//		Presence presence = new Presence(Presence.Type.available);
//		connection.sendPacket(presence);	
		
		return connection;
	}
	
	public static boolean isAvailable(Presence presence) {
		if (presence.toString().equals("available")) {
			return true;
		}
		
		return false;
	}
}
