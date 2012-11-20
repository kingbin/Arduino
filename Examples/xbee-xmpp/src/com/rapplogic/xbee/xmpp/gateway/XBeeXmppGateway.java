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

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeePacket;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.util.ByteUtils;
import com.rapplogic.xbee.xmpp.DecodeException;
import com.rapplogic.xbee.xmpp.XBeeXmppPacket;
import com.rapplogic.xbee.xmpp.XBeeXmppUtil;


/**
 * Interfaces a serial connected XBee network to XMPP.
 * All XBee response objects received from the serial line are forwarded to XMPP for awaiting clients  
 * Receives XBee request objects via XMPP and forwards to the XBee network
 * 
 * @author andrew
 *
 */
public abstract class XBeeXmppGateway extends XBeeXmppPacket implements PacketListener {
	
	//TODO create a routing policy.  currently client A receives response for requests it sends, 
	// but also from client B, client C etc.  We could modify the incoming request and update the frame id
	// to a unique value, then return the response only to the client who sent it.  We would need to modify
	// the response and to restore the original frame id.  sounds like a headache though.
	//TODO add JMX
	//TODO request listener in case you want to log incoming requests
	//TODO allow option for offline messages.  default for gtalk when user is offline.  openfire requires configuration
	//TODO create a listen-only option for clients that can receive packets but can't send
	 
	private final static Logger log = Logger.getLogger(XBeeXmppGateway.class);
	
	private XBee xbee;
	private List<String> clientList;
	private String comPort;
	private int baudRate;
	
	private boolean packetListener;
	
	public String getComPort() {
		return comPort;
	}

	public void setComPort(String comPort) {
		this.comPort = comPort;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	/**
	 * 
	 * @param server
	 * @param port
	 * @param user
	 * @param password
	 * @param clientList Only xmpp users in this list will receive xbee responses.  Recipients do not need to be online at gateway startup
	 * @param comPort
	 * @param baudRate
	 * @throws XMPPException
	 * @throws XBeeException
	 */
	public XBeeXmppGateway(String server, Integer port, String user, String password, List<String> clientList, String comPort, int baudRate) throws XMPPException, XBeeException {
		super(server, port, user, password);
		
		if (clientList == null || clientList.size() == 0) {
			throw new IllegalArgumentException("client list is null or empty.  you must provide at least one client");
		}
		
		this.setClientList(clientList);
		this.setComPort(comPort);
		this.setBaudRate(baudRate);
	}

	/**
	 * Creates a gateway with an existing XBee object
	 * 
	 * @param server
	 * @param port
	 * @param user
	 * @param password
	 * @param clientList
	 * @param xbee
	 * @throws XMPPException
	 * @throws XBeeException
	 */
	public XBeeXmppGateway(String server, Integer port, String user, String password, List<String> clientList, XBee xbee) throws XMPPException, XBeeException {
		super(server, port, user, password);
		this.setClientList(clientList);
		this.setComPort(comPort);
		this.setBaudRate(baudRate);
		this.xbee = xbee;
	}
	
	/**
	 * Starts the XMPP connection to the service provider.
	 * This method returns immediately if an XBee object was supplied, allowing you do work in the main thread;
	 * If an XBee object is not provided, it operates in "gateway" mode, blocking indefinitely.
	 * .
	 * @throws XMPPException
	 * @throws XBeeException
	 */
	public void start() throws XMPPException, XBeeException {	
		if (xbee == null) {
			// xbee was not passed in.  create
			xbee = new XBee();	
			log.info("opening xbee serial port connection to " + comPort);
			xbee.open(this.getComPort(), this.getBaudRate());
			
			packetListener = false;
		} else {
			if (!xbee.isConnected()) {
				throw new IllegalArgumentException("XBee not connected!");
			}
			
			packetListener = true;
		}
		
		this.initXmpp();
		
		if (packetListener) {
			xbee.addPacketListener(this);	
		} else {
			try {
				while (xbee.isConnected()) {
					XBeeResponse response = xbee.getResponse();
					this.handleResponse(response);
				}
				log.warn("XBee disconnected");
			} catch (Exception e) {
				log.error("error: ", e);
			} finally {
				log.info("closing XBee and Smack");
				this.shutdown();
			}			
		}
	}
	
	/**
	 * Called by the XBee-API packet parser thread when a new response is parsed
	 */
    public void processResponse(XBeeResponse response) {
    	//TODO process response in separate thread so we don't affect performance of the xbee thread that calls this method
    	this.handleResponse(response);
	}
    
    private void handleResponse(XBeeResponse response) {
		try {
			log.debug("received response from xbee " + response);
			
			// TODO check for error.  if error occurred in PacketParser class, this may not be preserved in re-hydration unless exact byte array is preserved
			
			if (response.isError()) {
				log.error("response is error: " + response.toString());
				if (response.getPacketBytes() == null) {
					// TODO proper error handling
					// set bogus packet with zero length and zero api id
					response.setRawPacketBytes(new int[] {0, 0, 0});
				}
			}
			
			Message msg = this.encodeMessage(response);
			
			log.debug("forwarding response to xmpp clients");
			
			// send to all online clients
			for (String client :this.getChatMap().keySet()) {
				Boolean presence = this.getPresenceMap().get(client);
				
				if (presence != null && presence == Boolean.TRUE) {
					log.debug("sending packet to " + client + ", message: " + msg.getBody());
					this.getChatMap().get(client).sendMessage(msg.getBody());
				} else {
					if (this.isOfflineMessages()) {
						log.debug("sending packet to offline " + client + ", message: " + msg.getBody());
						this.getChatMap().get(client).sendMessage(msg.getBody());							
					} else {
						// TODO make into a listener callback
						try {
							this.handleUndeliverableClient(response, client);	
						} catch (Exception e) {}
						
						// such and such is offline
						log.info(client + " is offline and will not receive the response " + response);							
					}
				}
			}		
		} catch (Exception e) {
			log.error("error processing response", e);
		}    		
    }
    
	public void shutdown() {
		super.shutdown(); 
		
		if (xbee != null) {
			try {
				xbee.close();
			} catch (Exception e) {
				log.error("failed to shutdown xbee", e);
			}
		}
	}
	
    public final void processMessage(Chat chat, Message message) {
    	
    	log.debug("received message from client [" + message.getFrom() + "] message: " + message.toXML());
    	   	
    	// TODO if request has frameid, override with sequential frame id and only delivery response to the sender of the packet.  
    	// if i/o sample then no frameid
    	
    	int[] packet = null;
    	
    	String sender = XBeeXmppUtil.stripProviderFromJid(message.getFrom());
    	
	    try {

	    	// security.. make sure the sender is one of our approved clients
	    	if (!this.isValidSender(sender, message.getBody())) {
	    		return;
	    	}
	    	
	    	this.verifyPresence(sender);
	 
	    	try {
	    		packet = this.decodeMessage(message);

	    		log.debug("received packet from " + message.getFrom() + ", message: " + ByteUtils.toBase16(packet));
		    	
	    		synchronized(XBee.class) {
	    			// send xbee packet to device
	    			
	    			// TODO currently there is no mechanism for obtaining the XBeeRequest object from the packet bytes, 
	    			// so we verify the checksum and call sendPacket
	    			// TODO create request.parse for generating XBeeRequest from byte array
	    			if (XBeePacket.verify(packet)) {
	    				log.debug("forwarding packet to xbee");
	    				try {
							xbee.sendPacket(packet);
						} catch (IOException e) {
							// TODO communicate error back to client
							log.error("error occurred sending packet to XBee: ", e);
						}	
	    			} else {
	    				log.warn("packet from [" + message.getFrom() + "] failed checksum verification.  discarding " + ByteUtils.toBase16(packet));
	    			}
	    		}	    		
	    	} catch (DecodeException de) {
	    		log.warn("could not parse packet from [" + message.getFrom() + "]: " + de.getMessage());	
	    	}
	    } catch (Exception e) {
	    	// TODO communicate error back to client
	    	log.error("failed to forward packet from [" + message.getFrom() + " to radio " + (packet != null ? ByteUtils.toBase16(packet) : "null"), e);
	    }
    }
	
    protected List<String> getRosterList() {
    	return clientList;
    }
	
	public List<String> getClientList() {
		return clientList;
	}

	public void setClientList(List<String> clientList) {
		this.clientList = clientList;
	}
	
	/**
	 * Called when a message is undeliverable because the client is offline
	 * 
	 * @param repsonse
	 * @param client
	 * Feb 24, 2009
	 */
	public void handleUndeliverableClient(XBeeResponse response, String client) {
		
	}
}
