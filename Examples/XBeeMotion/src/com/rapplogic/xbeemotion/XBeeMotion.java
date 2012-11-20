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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jivesoftware.smack.XMPPException;

import com.rapplogic.xbee.api.ApiId;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.wpan.IoSample;
import com.rapplogic.xbee.api.wpan.RxResponseIoSample;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;

/**
 * Receives IO Samples from a remote XBee, connected to a PIR Motion Detector, and
 * sends to Google Talk.
 * Properties for the XBee radio and Google Talk must be specified in xbee-motion.properties.
 * Supports both Series 1 and 2 XBee radios
 *  
 * @author andrew
 *
 */
public class XBeeMotion {
	static Logger log = Logger.getLogger(XBeeMotion.class);
	
	private GoogleTalkClient gtalk;
	
	private long lastMotionReported;
	private long lastMotion;
	private int events;
	private int delay;
	
	public static void main(String[] args) throws XBeeException, XMPPException, InterruptedException, FileNotFoundException, IOException {
		PropertyConfigurator.configure("log4j.properties");
		new XBeeMotion();	
	}
	
	public XBeeMotion() throws XBeeException, XMPPException, InterruptedException, FileNotFoundException, IOException {
		
		// Read the motion properties
		MotionProperties props = MotionProperties.parse("xbee-motion.properties");
		//MotionProperties props = MotionProperties.parse("/Users/andrew/Documents/private/xbee-motion.properties");

		log.info("motion delay seconds is " + props.getMotionDelaySeconds());
		delay = props.getMotionDelaySeconds() * 1000;
		 
		List<String> recipients = new ArrayList<String>();
		
		for (String recipient : props.getGtalkRecipients()) {
			log.info("Adding Google Talk motion recipient " + recipient);
			recipients.add(recipient);
		}

		log.info("Connecting to Google Talk account " + props.getGtalkUsername());
		
		// connect to Google Talk
		gtalk = new GoogleTalkClient(props.getGtalkUsername(), props.getGtalkPassword(), recipients);
		// Note: if your ISP blocks gtalk and you have an account on a linux box somewhere on the internet, you use a SSH tunnel:
		// ssh -N -L 15222:talk.google.com:5222 user@yourhost
		// now you connect as follows:
		//final GoogleTalkClient gtalk = new GoogleTalkClient("localhost", 15222, props.getGtalkUsername(), props.getGtalkPassword(), recipients);
		
		gtalk.start();
		
		XBee xbee = new XBee();
		
		log.info("Connecting to XBee: " + props.getXbeeComPort() + "/" + props.getXbeeBaudRate());
		xbee.open(props.getXbeeComPort(), props.getXbeeBaudRate());	

		final XBeeMotion motion = this;
		
		xbee.addPacketListener(new PacketListener() {			
			public void processResponse(XBeeResponse rx) {

				if (rx.getApiId() == ApiId.ZNET_IO_SAMPLE_RESPONSE) {
					// Series 2 XBee
					
					ZNetRxIoSampleResponse io = (ZNetRxIoSampleResponse) rx;
										
					// change detect packets never include analog samples.
					if (!io.containsAnalog()) {
						// d4 is off/low on motion
						if (!io.isD4On()) {
							// dio4 goes low on motion
							motion.handleMotionEvent(rx);
						}
					}
				} else if (rx.getApiId() == ApiId.RX_16_IO_RESPONSE || rx.getApiId() == ApiId.RX_64_IO_RESPONSE) {
					// Series 1
					RxResponseIoSample io = (RxResponseIoSample) rx;
					
					if (!io.containsAnalog()) {
						// Series 1 XBees are capable of > 1 samples per packet, but change detection packets will always be one sample
						// get first and only sample
						IoSample sample = io.getSamples()[0];
						
						if (!sample.isD4On()) {
							// motion!!
							motion.handleMotionEvent(rx);
						}
					}
				}
			}
		});			
	}
	
	/**
	 * Reports the motion event to Google Talk recipients,
	 * a maximum of one event every x seconds.
	 * 
	 * @param rx
	 */
	private void handleMotionEvent(XBeeResponse rx) {
		if (System.currentTimeMillis() - lastMotionReported > delay) {
			log.info("Motion detected! " + events + " events since last report");
			log.info("received " + rx);	

			// TODO consider only-report-if-no-motion-for-x-seconds property
			// TODO last report was xx hour/min/secs ago
			
			try {
				gtalk.sendMessage("Motion detected! " + events + " events since last report");
			} catch (XMPPException e) {
				log.error("failed to send motion google talk message");
			}
			
			events = 0;
			lastMotionReported = System.currentTimeMillis();
		} else {
			log.debug("ignoring motion event");
			events++;
		}
		
		lastMotion = System.currentTimeMillis();
	}
}
