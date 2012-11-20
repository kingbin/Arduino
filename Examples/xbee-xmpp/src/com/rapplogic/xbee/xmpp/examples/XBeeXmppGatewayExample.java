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

package com.rapplogic.xbee.xmpp.examples;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.jivesoftware.smack.XMPPException;

import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.xmpp.gateway.XBeeGtalkGateway;
import com.rapplogic.xbee.xmpp.gateway.XBeeXmppGateway;

/**
 * 
 * @author andrew
 *
 */
public class XBeeXmppGatewayExample {

	public XBeeXmppGatewayExample() throws XMPPException, XBeeException {
		
		// add all clients that you want to communicate with.  the client does not need to be online when gateway starts up
		List<String> clientList = new ArrayList<String>();
//		clientList.add("xbeeclient@sencha.local");
//		clientList.add("xbeeclient2@sencha.local");
//		clientList.add("xbeeclient3@sencha.local");
		// using google talk
		clientList.add("xbeeclient@gmail.com");
		
		XBeeXmppGateway gateway = null;
		
		// using openfire.  use coordinator com port
		//gateway = new XBeeOpenfireGateway("localhost", 5222, "xbeegateway", "xbeegateway", clientList, "/dev/tty.usbserial-A6005v5M", 9600);
		// using google talk
		gateway = new XBeeGtalkGateway("xbeegateway@gmail.com", "password", clientList, "/dev/tty.usbserial-A6005v5M", 9600);
		
		gateway.start();
	}
	
	public static void main(String[] args) throws XMPPException, InterruptedException, XBeeException {
		PropertyConfigurator.configure("log4j.properties");
		new XBeeXmppGatewayExample();
	}
}
