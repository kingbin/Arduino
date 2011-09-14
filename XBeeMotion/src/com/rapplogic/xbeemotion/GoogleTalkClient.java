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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import com.rapplogic.xbee.xmpp.XBeeGtalkCommon;
import com.rapplogic.xbee.xmpp.XBeeXmpp;

/**
 * Basic Google Talk client for sending messages
 * Extends XBee-XMPP for XMPP communication (subscription/invitation etc.)
 * 
 * @author andrew
 *
 */
public class GoogleTalkClient extends XBeeXmpp {

	static Logger log = Logger.getLogger(GoogleTalkClient.class);
	
	private List<String> clients = new ArrayList<String>();
	
	public GoogleTalkClient(String server, Integer port, String user, String password, List<String> recipients) throws XMPPException {
		super(server, port, user, password);
		clients.addAll(recipients);
	}

	public GoogleTalkClient(String user, String password, List<String> recipients) throws XMPPException {
		super(null, null, user, password);
		clients.addAll(recipients);	
	}
	
	public void start() throws XMPPException {
		this.initXmpp();
	}
	
	@Override
	protected void connect() throws XMPPException {
		this.setConnection(XBeeGtalkCommon.connect(this.getServer(), this.getPort(), this.getUser(), this.getPassword()));
	}

	@Override
	protected List<String> getRosterList() {
		return clients;
	}

	@Override
	protected boolean isAvailable(Presence presence) {
//		log.debug("isAvailable(): presence is " + presence.toString());
		return presence.toString().startsWith("available");
	}

	public void sendMessage(String s) throws XMPPException {
		for (String user : clients) {
			if (this.getPresenceMap().get(user) != null && this.getPresenceMap().get(user).booleanValue()) {
				log.debug("sending message " + s + " to " + user);
				this.getChatMap().get(user).sendMessage(s);				
			} else {
				log.debug("user [" + user + "] is not online.. discarding message");
			}

		}
	}
	public void processMessage(Chat chat, Message message) {
		log.debug("received message " + message.getBody() + ", from " + chat.getParticipant());
		
		// interact
		// see if the sender is in our approved clients list
		for (String user : clients) {
			if (chat.getParticipant().equals(user)) {
				// ok, we can talk to this person
				if (message.getBody().equals("?")) {
					
					try {
						this.getChatMap().get(user).sendMessage("1. Feed cat\n2. Make sandwich");
					} catch (XMPPException e) {
						log.error("failed to send message", e);
					}
				} else {
					try {
						this.getChatMap().get(user).sendMessage("Hello " + user);
					} catch (XMPPException e) {
						log.error("failed to send message", e);
					}
				}
			}
		}
		
	}
}
