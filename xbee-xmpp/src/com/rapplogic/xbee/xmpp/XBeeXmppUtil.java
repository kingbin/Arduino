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

import java.util.Random;

import com.rapplogic.xbee.api.PacketParser;
import com.rapplogic.xbee.api.XBeePacket;
import com.rapplogic.xbee.api.XBeeRequest;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.util.IntArrayInputStream;

public class XBeeXmppUtil {
	
	/**
	 * Converts an XBeeRequest into an ASCII representation, to be sent over XMPP
	 * 
	 * @param request
	 * @return
	 */
    public static String encodeXBeeRequest(XBeeRequest request) {		
		return XBeeXmppUtil.convertIntArrayToHexString(request.getXBeePacket().getPacket());
    }

    /**
     * Converts the incoming XMPP ascii message into an XBeeResponse object instance 
     * 
     * @param message
     * @return
     * @throws DecodeException
     */
	public static XBeeResponse decodeXBeeResponse(String message) throws DecodeException {		
		int[] packet = convertHexStringToIntArray(message);

    	return decodeXBeeResponse(packet);
	}

	public static XBeeResponse decodeXBeeResponse(int[] packet) throws DecodeException {
		
    	IntArrayInputStream in = new IntArrayInputStream(packet);
    	
    	// reconstitute XBeeResponse from int array
    	PacketParser ps = new PacketParser(in);
    	// this method will not throw an exception
    	XBeeResponse response = ps.parsePacket();
    	
    	return response;
	}
	
    /**
     * Extracts an XBeeResponse (client) or XBeeRequest (gateway) packet from a smack message.  
     * Each message contains a packet formated in hex, e.g. (001eff..)
     * 
     * @throws DecodeException 
     * 
     */
    public static int[] convertHexStringToIntArray(String message) throws DecodeException {
    	
//    	log.debug("hex from body is " + hex);
    	
    	if (message.length() % 2 > 0) {
    		throw new DecodeException("incoming packet is not valid: string must be an even number of characters: " + message);
    	}
    
    	int[] packet = new int[message.length() / 2]; 
	   	
    	try {
    	   	for (int i = 0; i < message.length(); i+=2) {	
        		packet[i / 2] = Integer.parseInt(message.substring(i, i + 2), 16);
        	}   		
    	} catch (NumberFormatException nfe) {
    		throw new DecodeException("incoming packet is not valid: contains non integer values: " + message);
    	}
 
    	
//    	log.debug("after conversion, packet is " + ByteUtils.toBase16(packet));
    	
    	return packet;
    }
    
	/**
	 * Formats a byte array in hex, without the "0x" notation.
	 * 
	 * @param arr
	 * @return
	 */
    public static String convertIntArrayToHexString(int[] arr) {
        
//    	log.debug("packet is " + ByteUtils.toBase16(arr));
    	
    	StringBuffer strbuf = new StringBuffer();
    	
    	for (int i = 0; i < arr.length; i++) {
    		if (arr[i] > 255) {
    			throw new IllegalArgumentException("value in array exceeds one byte: " + arr[i]);
    		}
    		
    		strbuf.append(padHex(arr[i]));
    	}
    
    	return strbuf.toString();
    }
    
    /**
     * The hex string must 2 chars per byte for it to be parsed correctly, 
     * so 0x1 is represented as 01
     * 
     * @param b
     * @return
     */
    public static String padHex(int b) {
		if (b < 0x10) {
			return "0" + Integer.toHexString(b);
		} else {
			return Integer.toHexString(b);
		}
    } 
    
    private static Random random = new Random();
    
    /**
     * Returns a random frame id between 1 and 255
     * 
     * @return
     * Jan 24, 2009
     */
    public static int getRandomFrameId() {
    	return random.nextInt(0xff) + 1;
    }
    
    /**
     * Returns a random frame id that is not equal to one provided
     * 
     * @param last
     * @return
     * Jan 24, 2009
     */
    public static int getDifferentFrameId(int last) {
    	int frameId = 0;
    	
    	do {
    		frameId = getRandomFrameId();
    	} while (last == frameId);
    	
    	return frameId; 
    }
    
    /**
     * Removes the provider/id from the JID.  For example, removes "/Smack" from xbeegateway@sencha.local/Smack
     * 
     * @param jid
     * @return
     * Jan 24, 2009
     */
    public static String stripProviderFromJid(String jid) {
    	if (jid.indexOf("/") > 0) {
    		return jid.substring(0, jid.lastIndexOf("/"));
    	}
    	
    	return jid;
    }
}
