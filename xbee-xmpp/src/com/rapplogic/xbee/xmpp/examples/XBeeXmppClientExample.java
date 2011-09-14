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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jivesoftware.smack.XMPPException;

import com.rapplogic.xbee.api.AtCommand;
import com.rapplogic.xbee.api.AtCommandResponse;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.util.ByteUtils;
import com.rapplogic.xbee.xmpp.XBeeXmppUtil;
import com.rapplogic.xbee.xmpp.client.GatewayOfflineException;
import com.rapplogic.xbee.xmpp.client.XBeeGtalkClient;

public class XBeeXmppClientExample {
	
	private final static Logger log = Logger.getLogger(XBeeXmppClientExample.class);
	
	public XBeeXmppClientExample() throws XMPPException, XBeeException {
	
		XBeeGtalkClient client = new XBeeGtalkClient();
		
		client.open("localhost", 5222, "xbeeclient3", "xbeeclient3", "xbeegateway@sencha.local");
		
		// the gateway may be online, but we haven't received the online event.. wait a bit to get presence event
		long start = System.currentTimeMillis();
		
		while (!client.isGatewayOnline()) {
			log.debug("Waiting for Gateway online presence");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) { }
			
			if ((System.currentTimeMillis() - start) > 5000) {
				break;
			}
		}
		
		if (client.isGatewayOnline()) {
			log.debug("Gateway is online!");	
		} else {
			throw new RuntimeException("Gateway is not online!");
		}
		
		// get association status
		AtCommand at = new AtCommand("AI");
		
		while (true) {
			// set frame id to random value
			at.setFrameId(XBeeXmppUtil.getDifferentFrameId(at.getFrameId()));
							
			try {
				log.debug("Sending request: " + ByteUtils.toBase16(at.getXBeePacket().getPacket()));
				
				XBeeResponse response = client.sendSynchronous(at);
				
				log.debug("Received response " + response);
				
				if (response instanceof AtCommandResponse) {
					if (((AtCommandResponse)response).isOk()) {
						log.debug("AT command succeeded");
					} else {
						log.debug("AT command was not successful");
					}
				} else {
					log.debug("Received a response but not the one we were expecting");
				}	
			} catch (XBeeTimeoutException e) {
				log.debug("Timed out while waiting for a response");
			} catch (GatewayOfflineException goe) {
				log.debug("Gateway is not online");
			}
						
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) { }
		}
	}
	
	public static void main(String[] args) throws XMPPException, XBeeException {
		PropertyConfigurator.configure("log4j.properties");
		new XBeeXmppClientExample();
	}
}
