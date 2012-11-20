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

import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.RemoteAtResponse;
import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeRequest;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;
import com.rapplogic.xbee.xmpp.client.GatewayOfflineException;
import com.rapplogic.xbee.xmpp.client.XBeeGtalkClient;
import com.rapplogic.xbee.xmpp.client.XBeeXmppClient;

/**
 * Demonstrates how to use the XBeeGtalkClient to request samples from a remote XBee
 * This example requires that an instance of XBeeGtalkGateway is running and connected to
 * a series 2 XBee coordinator that is associated to at least one end device.
 * 
 * @author andrew
 *
 */
public class XBeeXmppClientRemoteAtExample {
	
	private final static Logger log = Logger.getLogger(XBeeXmppClientRemoteAtExample.class);

	public static void main(String[] args) throws XMPPException, InterruptedException, XBeeException {
		PropertyConfigurator.configure("log4j.properties");
		new XBeeXmppClientRemoteAtExample();
	}

	public XBeeXmppClientRemoteAtExample() throws XMPPException, InterruptedException, XBeeException {
		XBeeGtalkClient client = null;
		
		try {
			// using gtalk
			client = new XBeeGtalkClient();
			client.open("xbeeclient@gmail.com", "password", "xbeegateway@gmail.com");
			
			// gateway may be online, but we haven't received the online event.. wait a bit to get presence event
			long start = System.currentTimeMillis();
			while (!client.isGatewayOnline()) {
				Thread.sleep(100);
				log.debug("waiting");
				
				if ((System.currentTimeMillis() - start) > 5000) {
					break;
				}
			}
			// use remote at to configure an end device for i/o sampling
			
			// replace with SH + SL of your end device
			XBeeAddress64 addr64 = new XBeeAddress64(0, 0x13, 0xa2, 0, 0x40, 0x0a, 0x3e, 0x02);
			
			// first we need to configure pin 20 to monitor analog input
			// now is a good time to hook up a sensor to pin 20, but not required for the test
			RemoteAtRequest request = 
				new RemoteAtRequest(XBeeRequest.DEFAULT_FRAME_ID, addr64, XBeeAddress16.ZNET_BROADCAST, true, "D0", new int[] {0x2});
			
			// of course we could always use the IR command to have XBee send periodic samples
			
			RemoteAtResponse response = null;
			
			try {
				log.debug("Sending Remote AT request: " + request);
				// wait a max of 10 seconds for response
				response = (RemoteAtResponse) client.sendSynchronous(request, 10000);
				
				if (!response.isOk()) {
					throw new RuntimeException("Remote AT request failed: " + response.getStatus());
				}
			} catch (XBeeTimeoutException e) {
				throw new RuntimeException("timed out while waiting for a response");
			}
			
			log.debug("received remote at response " + response);
			
			while (true) {
				// now we will periodically force a sample (IS) of the analog input on the end device
				request = 
					new RemoteAtRequest(XBeeRequest.DEFAULT_FRAME_ID, addr64, XBeeAddress16.ZNET_BROADCAST, true, "IS");
		
				try {
					log.debug("Sending Remote AT request: " + request);
					
					response = (RemoteAtResponse) client.sendSynchronous(request, 10000);

					if (response.isOk()) {
						ZNetRxIoSampleResponse ioSample = ZNetRxIoSampleResponse.parseIsSample(response);
						log.info("Pin 20 10-bit reading is " + ioSample.getAnalog0());							
					} else {
						log.info("Received error status: " + response.getStatus());
					}
				} catch (XBeeTimeoutException e) {
					log.warn("Request timed out");
				} catch (GatewayOfflineException goe) {
					log.warn("Gateway is currently offline");
				} catch (Exception e) {
					log.error("request failed ", e);
				}
				
				Thread.sleep(30000);
			}
		} finally {
			try {
				client.close();
			} catch (Exception e) {}
		}
	}
}
