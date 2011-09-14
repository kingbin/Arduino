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

package com.rapplogic.xbee.xmpp.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeePacket;
import com.rapplogic.xbee.util.ByteUtils;
import com.rapplogic.xbee.xmpp.XBeeXmppPacket;

/**
 * Provides communication with an XBee radio over XMPP.
 * A instance of XBeeXmppGateway must be running for this class to function.
 * <p/>
 * Connects to gateway and subscribes to the gateway, if not already in roster
 * Gateway does not need to be online at startup but of course send/receive will not be possible until gateway is online.
 * <p/>
 * Important: You must make sure the client's JID is in the the gateway's client list or it will not be able to communicate
 * with the gateway.  XMPP does not allow communication with another user (such as the gateway) until you have subscribed to/invited them 
 * AND they have subscribed to/accepted you.  If only the client has subscribed, it will receive presence events and can send messages
 * but these messages will not be received and no error will be generated.
 * 
 * @author andrew
 *
 */
public abstract class XBeeXmppClient extends XBeeXmppPacket implements ConnectionSink {

	private final static Logger log = Logger.getLogger(XBeeXmppClient.class);

	private String gateway;
		
	private XmppXBeeConnection connection;
	private XBee xbee;
	
	public XBeeXmppClient(XBee xbee, String server, Integer port, String user, String password, String gateway) {
		super(server, port, user, password);
		this.setGateway(gateway);	
		this.xbee = xbee;
	}
	
	public Boolean isGatewayOnline() {
		return (Boolean) this.getPresenceMap().get(this.getGateway());
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	/**
	 * Establishes a connection to the XMPP Server
	 * 
	 * @throws XMPPException
	 * @throws XBeeException 
	 */
	public void start() throws XMPPException, XBeeException {
		synchronized (this) {

			// default gateway to offline incase they are not in roster
			this.getPresenceMap().put(this.getGateway(), Boolean.FALSE);
			
			this.initXmpp();
		}		
		
		connection = new XmppXBeeConnection(this);
		xbee.initProviderConnection(connection);
	}

    public void processMessage(Chat chat, Message message) {
		
    	try {
    		if (log.isDebugEnabled()) {
    	    	log.debug("Received packet from gateway: " + message.getBody());
    		}
    		
	    	int[] packet = this.decodeMessage(message);
	    	
	    	// we need to add the start byte to the packet for xbee-api to be able to parse it as if it came fresh off the rxtx input stream 
	    	int[] packetWithStartByte = new int[packet.length + 1];
	    	
		   	// add start byte
	    	packetWithStartByte[0] = XBeePacket.SpecialByte.START_BYTE.getValue();
	    	System.arraycopy(packet, 0, packetWithStartByte, 1, packet.length);
	    	
	    	connection.addPacket(packetWithStartByte);		
    	} catch (Exception e) {
    		// TODO add to error listener
    		log.error("error processing message " + message.toXML(), e);
    	}	
    }	

    /**
     * Called by XmppXBeeConnection
     */
    public void send(int[] packet) throws XBeeException, XMPPException {
    	
    	if (!XBeePacket.verify(packet)) {
    		throw new XBeeException("Packet is malformed" + ByteUtils.toBase16(packet));
    	}
    	
		Message message = this.encodeMessage(packet);
		// TODO error handling -- for now we just assume it was received
		
		if (!this.isGatewayOnline() && !this.isOfflineMessages()) {
			throw new GatewayOfflineException();
		} 
		
		if (log.isInfoEnabled()) {
			log.info("Sending request to gateway: " + ByteUtils.toBase16(packet));
		}
		
		this.getChat().sendMessage(message);	
    }
    
	public void close() {
		try {
			if (this.getConnection() != null) {
				this.getConnection().disconnect();			
			}
			
			xbee.close();
		} catch (Exception e) {
			log.error("failed to disconnect connection", e);
		}
		
		this.connection.close();
	}

    protected List<String> getRosterList() {
    	List<String> gateway = new ArrayList<String>();
    	gateway.add(this.getGateway());
    	return gateway;
    }
    	

	
	/**
	 * Returns the gateway chat object -- the only chat object for a client!
	 * 
	 * @return
	 */
	public Chat getChat() {
		return this.getChatMap().get(this.getGateway());
	}
}
