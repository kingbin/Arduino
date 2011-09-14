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

package com.rapplogic.xbee.xmpp.gateway;

import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.xmpp.XBeeGtalkCommon;

/**
 * XBee Gateway implementation for Google talk.
 * As long as you have access to the Google Talk server, your XMPP gateway and clients can be
 * on different networks, even behind firewalls.
 * <p/>
 * This solution allows you to share your xbee hardware with anyone on the internet to experiment with as well
 * as create XBee applications that reside on different physical machines and/or networks than the XBee.  
 * This is also a great way to distribute your XBee applications across the internet, without requiring a server 
 * or static ip address -- Google is the server.  This is especially relevant to those with home networks that are not able to 
 * run servers, either by ISP policy or if they do not get a public ip address.
 * <p/>
 * This solution allows your gateway can be brought down and upgraded without
 * affecting the clients.  When the gateway is brought up, clients will automatically get notified and resume communication.
 * The client can go on and offline at will, and the gateway will always be notified if of the client's status.
 * <p/>
 * You will need a minimum of two gtalk accounts: one for the gateway and one for the client.
 * <p/>
 * By default, Google Talk accepts messages sent to offline users, and delivers the messages when the user signs on.
 * However due to the real-time nature of XBee communication, the gateway will only communicate with online clients.
 * <p/>
 * Caveat: Google may rate limit your account if you send too much traffic.  BTW, I don't know what "too much" is.
 * 
 * @author andrew
 * 
 */
public class XBeeGtalkGateway extends XBeeXmppGateway {

	private final static Logger log = Logger.getLogger(XBeeGtalkGateway.class);
	
	/**
	 * Creates a Google Talk gateway with the specified Google Talk server host name and port.  Useful for tunneling a connection over SSH
	 * 
	 * @param server
	 * @param port
	 * @param user
	 * @param password
	 * @param clientList
	 * @param comPort
	 * @param baudRate
	 * @throws XMPPException
	 * @throws XBeeException
	 */
	public XBeeGtalkGateway(String server, Integer port, String user, String password, List<String> clientList, String comPort, int baudRate) throws XMPPException, XBeeException {
		super(server, port, user, password, clientList, comPort, baudRate);
	}

	/**
	 * Creates a Google Talk gateway with the supplied arguments
	 * 
	 * @param user
	 * @param password
	 * @param clientList
	 * @param comPort
	 * @param baudRate
	 * @throws XMPPException
	 * @throws XBeeException
	 */
	public XBeeGtalkGateway(String user, String password, List<String> clientList, String comPort, int baudRate) throws XMPPException, XBeeException {
		super(null, null, user, password, clientList, comPort, baudRate);
	}

	/**
	 * Creates a Google Talk gateway with an existing XBee object.  
	 * You must call xbee.open(...) prior to calling this constructor
	 * 
	 * @param user
	 * @param password
	 * @param clientList
	 * @param xbee
	 * @throws XMPPException
	 * @throws XBeeException
	 */
	public XBeeGtalkGateway(String user, String password, List<String> clientList, XBee xbee) throws XMPPException, XBeeException {
		super(null, null, user, password, clientList, xbee);
	}
	
	protected XMPPConnection connect() throws XMPPException {
		return XBeeGtalkCommon.connect(this.getServer(), this.getPort(), this.getUser(), this.getPassword());
	}
	
	protected boolean isAvailable(Presence presence) {
		return XBeeGtalkCommon.isAvailable(presence);
	}
}
