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
import org.jivesoftware.smack.packet.Message;

import com.rapplogic.xbee.api.XBeeRequest;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * Contains methods necessary for encoding/decoding of XBee packets over XMPP
 */
public abstract class XBeeXmppPacket extends XBeeXmpp {

	private final static Logger log = Logger.getLogger(XBeeXmppPacket.class);
	
	public XBeeXmppPacket(String server, Integer port, String user, String password) {
		super(server, port, user, password);
	}
	
    protected Message encodeMessage(XBeeResponse response) {
		return this.encodeMessage(response.getPacketBytes());
	}

    protected Message encodeMessage(XBeeRequest request) {
		return this.encodeMessage(request.getXBeePacket().getPacket());
	}
	
    protected Message encodeMessage(int packet[]) {
		Message msg = new Message();
		// send as hex string
		
		String hex = XBeeXmppUtil.convertIntArrayToHexString(packet);
		
		msg.setBody(hex);
		
		return msg;
    }
    
    /**
     * Extracts a packet (either a XBeeRequest or XBeeResponse) from a smack message
     * Each message contains a packet formated in hex, e.g. (001eff..)
     * @throws DecodeException 
     * 
     */
    protected int[] decodeMessage(Message message) throws DecodeException {
    	String hex = message.getBody();
    	return XBeeXmppUtil.convertHexStringToIntArray(hex);
    }
}
