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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;

import com.rapplogic.xbee.api.PacketListener;

/**
 * Includes common functionality for gateways and clients.
 * <p/>
 * Automatically subscribes to all users in getRosterList on startup, if not already subscribed.
 * <p/>
 * Note: if you send a message to a recipient, whom is not subscribed to you, the message will be echoed back
 * in processMessage(..).  The recipient will get a "so and so wants to add you as a friend yes/no" (if using Google Talk)
 * <p/>
 * Google talk will allow multiple connections with the same login, but messages will not be broadcast to all connections;
 * instead messages will echo back to processMessage
 * 
 */
public abstract class XBeeXmpp implements MessageListener, RosterListener {
	
	 //TODO add error listener
	 //TODO receive connection failure events
	 //TODO add Runtime shutdown hook
	 //TODO need a mechanism to transmit errors that occur between xmpp client and server, for example gateway loses connection to xbee. as of now the client does not get an error and assumes it was sent
	 //TODO promiscuous mode where gateway will accept messages from any address -- client list ignored
	 
	private final static Logger log = Logger.getLogger(XBeeXmpp.class);
	
	private final List<PacketListener> listeners = new ArrayList<PacketListener>();
	
	//private final HashMap<String,Boolean> presenceMap = new HashMap<String,Boolean>();
	private final Hashtable<String,Boolean> presenceMap = new Hashtable<String,Boolean>();
	
	private final HashMap<String,Chat> chatMap = new HashMap<String,Chat>();
	
	private final List<PresenceListener> presenceListenerList = new ArrayList<PresenceListener>();
	
	private XMPPConnection connection;

	private String user;
	private String password;
	private String server;
	private Integer port;
	
	private boolean offlineMessages = false;

	public XBeeXmpp(String server, Integer port, String user, String password) {
		if ((server != null && port == null) || (port != null && server == null)) {
			throw new IllegalArgumentException("either both server and port must be specified or neither");
		}
		
		this.setServer(server);
		this.setPort(port);
		this.setUser(user);
		this.setPassword(password);	
	}
	
	public void addPacketListener(PacketListener listener) {
		listeners.add(listener);
	}
	
	protected List<PacketListener> getPacketListeners() {
		return listeners;
	}
	
	public void addPresenceListener(PresenceListener listener) { 
		this.presenceListenerList.add(listener);
	}

	public void removePacketListener(PresenceListener listener) {
		this.presenceListenerList.remove(listener);
	}
	
	/**
	 * Contains an entry for each recipient; true if user is only false/null otherwise
	 */
	public Hashtable<String, Boolean> getPresenceMap() {
		return presenceMap;
	}
	
	public XMPPConnection getConnection() {
		return connection;
	}
	
	public void setConnection(XMPPConnection connection) {
		this.connection = connection;
	}
	
	public String getUser() {
		return user;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public HashMap<String, Chat> getChatMap() {
		return chatMap;
	}
	
	/**
	 * Subclass must implement to connect to XMPP provider
	 * After successful connection setConnection(XMPPConnection) must be called.
	 * 
	 * TODO this should return the XMPP connection object
	 * 
	 * @throws XMPPException
	 */
	protected abstract XMPPConnection connect() throws XMPPException;

	/**
	 * In order to curtail spam/unsolicited messages,
	 * XMPP devised a subscription/invitation model.  When you subscribe 
	 * to another user (add to your roster), you can see their presence but
	 * can't send them messages yet.  The other user must reciprocate and add
	 * you to their roster.  Once this is complete both users can exchange
	 * messages
	 * <p/>
	 * Subclass must implement to provide the list of XMPP users 
	 * that this user would like to talk with.  If not already in
	 * the users roster, these users will receive an invitation request.
	 * 
	 * @return
	 */
    protected abstract List<String> getRosterList();
    
    /**
     * Subclass must implement to determine if the user (client or gateway) is online and able to receive packets.
     * If this method returns true for a gateway, it means the gateway
     * will delivery the packet to the client.  For clients, this method must return true
     * for the client to deliver the packet to the gateway.
     * <p/>
     * Unfortunately the Presence object does not
     * provide methods for away, available etc.; instead the presence string
     * presence.toString(), must be evaluated.
     * <p/>
	 * The subclass should returns true only if the remote user is able to receive messages.
	 * <p/>
	 * The presence string varies depending on client sending the presence.
	 * <br/>
	 * Google Talk uses "available ()" and the "dnd" token to represent busy (e.g. "available: dnd ()" for busy) <br/>
	 * Google Talk will send "available: away ()"  when it thinks the computer is idle <br/>
	 * Google Talk custom status are placed within parentheses (e.g. "available (Grrrr)") <br/>
	 * iChat seems to send "available" or "available (At home)" where the custom status is in parentheses <br/>
	 * iChat sends "available: xa" if user is online but away <br/>
	 * Both Google Talk and iChat send "unavailable" when a user signs out. <br/>  
	 * So to summarize: <br/>
	 * If presence contains "available: dnd" or available: away,  Google Talk user is online but away/busy. <br/>  
	 * 		I believe "dnd" indicates the user set the status, where the "away" status is set by Google Talk <br/>
	 * If presence contains "available: xa", iChat user is online but away/busy <br/>
	 * If presence contains unavailable, user is not online <br/>
	 * Otherwise user is considered online and available <br/>
     * 
     * @param presence
     * @return
     */
    // TODO add user to this method so other xmpp clients (will different presence strings) may be supported.
    // for example, someone is receiving packets from the gateway with a client that users "online" for available,
    // the gateway will incorrectly identify them as offline.  Ideally we need a map of xmpp client and their set of presences.
    protected abstract boolean isAvailable(Presence presence);
	
	/**
	 * Called when a roster subscription is sent/received
	 * This fires on the instance where roster.addEntry is called
	 * Default Smack policy is to accept all roster subscriptions.
	 */
	public void entriesAdded(Collection<String> addresses) {
		for (String address : addresses) {
			log.info("entry added: " + address);
		}
	}
	
	public void entriesDeleted(Collection<String> addresses) {
		for (String address : addresses) {
			log.info("entriesDeleted: [" + address + "]");
		}
	}
	
	/**
	 * Called when subscription is approved? and addEntry request
	 */
    public void entriesUpdated(Collection<String> addresses) {
    	for (String address : addresses) {
			log.info("entriesUpdated: [" + address + "]");
		}
    }
        
	public void presenceChanged(Presence presence) {
    	
		log.debug("Presence changed: from: " + presence.getFrom() + ", mode is " + presence.getMode() + ", tostring is [" + presence.toString() + "]");
    	
    	// not equals because that would be something like username@gmail.com/Talk.v10482E0B62B for gtalk
		// and username@host/Smack for openfire
		
		try {
			for (PresenceListener listener : this.presenceListenerList) {
				listener.presenceChanged(XBeeXmppUtil.stripProviderFromJid(presence.getFrom()), presence.toString());
			}			
		} catch (Exception e) {
			log.error("Error while notifying presence listeners", e);
		}

    	for (String user: this.getRosterList()) {
        	if (presence.getFrom().startsWith(user)) {
			
        		// unfortunately smack presence.getMode returns null, and toString is inconsistent between openfire/gtalk so we defer to subclass
        		// to determine if user is online
        		
				if (this.isAvailable(presence)) {
					log.debug(user + " is online");
					this.getPresenceMap().put(user, Boolean.TRUE);
				} else {
					// could also be busy
					this.getPresenceMap().put(user, Boolean.FALSE);
					log.debug(user + " is offline");
				}
        	}    		
    	}
    }
	
	/**
	 * This appears to be a bug in Openfire and/or Smack where presence "available" is not sent during sign-on
	 * for senders/recipients that are in fact "available".  This is a patch/workaround to the issue.
	 * 
	 * @param sender
	 * Jan 24, 2009
	 */
	public void verifyPresence(String sender) {
       
    	if (this.getRosterList().contains(sender)) {
    		// BUG at times the gateway does not receive a presence event during sign-on for online users.
        	if (this.getPresenceMap().get(sender) == null) {
        		// user is obviously online and sending us messages
        		log.warn("Did not receive available presence event for " + sender + ", but they are online!");
        		this.getPresenceMap().put(sender, Boolean.TRUE);
        	}    		
    	}
	}

	/**
	 * Determines if the sender is valid.  
	 * In general only XMPP users in your roster can send you messages, so this may be redundant.
	 * This checks to make sure the user is in your roster list.
	 * 
	 * @param sender
	 * @param body
	 * @return
	 * Jan 24, 2009
	 */
	public boolean isValidSender(String sender, String body) {
	       
    	if (!this.getRosterList().contains(sender)) {
    		log.warn("ignoring message from [" + sender + "], who is not an approved sender.  message is " + body);
    		return false;
    	}
    	
    	return true;
	}
	
	/**
	 * Connects to XMPP server and subscribes to XMPP users, if necessary
	 * 
	 * @throws XMPPException
	 * Jan 24, 2009
	 */
    public void initXmpp() throws XMPPException {
    	synchronized (this) {
    		XMPPConnection conn = this.connect();
    		
    		conn.addConnectionListener(new ConnectionListener() {

    			final Logger log = Logger.getLogger(ConnectionListener.class);
    			
				public void connectionClosed() {
					log.info("XMPP Connection closed");
				}

				public void connectionClosedOnError(Exception arg0) {
					log.error("XMPP Connection closed", arg0);
					
				}

				public void reconnectingIn(int arg0) {
					log.info("XMPP Connection reconnectingIn " + arg0);
				}

				public void reconnectionFailed(Exception arg0) {
					log.error("XMPP Connection reconnectionFailed ", arg0);
					
				}

				public void reconnectionSuccessful() {
					log.info("XMPP Connection reconnectionSuccessful");
				}
    		});
    		
    		this.setConnection(conn);

    		Roster roster = this.getConnection().getRoster();
    		// this is necessary to know who is online/offline
    		roster.addRosterListener(this);	
    		
    		for (RosterEntry entry : roster.getEntries()) {
    			log.info("Roster user is " + entry.getUser() + ", display name is " + entry.getName() + ", toString is " + entry + ", status is " + entry.getStatus() + ", item type is " + entry.getType());
    		}
    		
    		// if user is offline, the server will automatically forward the subscription request once the user is online
    		for (String user : this.getRosterList()) {
    			if (!roster.contains(user)) {
    				log.info("Recipient [" + user + "] not in roster.  subscribing now!");
    				roster.createEntry(user, user, null);		
    			} else {
    				// "none" seems to be a pending subscription, where user is offline and hasn't accepted								
    				if (roster.getEntry(user).getType() == RosterPacket.ItemType.none) {
    					log.info("[" + user + "] is in roster but has a pending subscription.  the user may be offline, but will accept once they come back online");
    				} else if (roster.getEntry(user).getType() == RosterPacket.ItemType.from) {
    					log.info("[" + user + "] has subscribed to us, but we have not subscribed to the user.  this may be the first time we are connecting to the user.  subscribing now!");
    					roster.createEntry(user, user, null);									
    				} else if (roster.getEntry(user).getType() == RosterPacket.ItemType.to) {
    					log.warn("We have subscribed to [" + user + "], but they have not subscribed to us yet.  Make sure this user is in the gateway's client list.  Messages from this user will be discarded until they subscribe.");
    				} else if (roster.getEntry(user).getType() == RosterPacket.ItemType.both) {
    					log.info("[" + user + "] is fully subscribed");
    				} else {
    					log.warn("Unknown roster subscription type: " + roster.getEntry(user).getType());
    				}
    			}
    		}
    		
//    		for (RosterEntry entry : roster.getEntries()) {
//    			log.info("[after] Roster user is " + entry.getUser() + ", display name is " + entry.getName() + ", toString is " + entry + ", status is " + entry.getStatus() + ", item type is " + entry.getType());
//    		}
    		
			// create chat object for each recipient
			for (String user: this.getRosterList()) {
				log.info("Creating Chat object for " + user);
				Chat chat = this.getConnection().getChatManager().createChat(user, this);
				chatMap.put(user, chat);
			}
    	}    	
    }
    
	public void shutdown() {
		
		if (this.getConnection() != null) {
			try {
				this.getConnection().disconnect();
			} catch (Exception e) {
				log.error("failed to disconnect xmpp connection", e);
			}
		} 
	}

	/**
	 * Returns true if offline messages are enabled
	 * 
	 * @return
	 * Feb 25, 2009
	 */
	public boolean isOfflineMessages() {
		return offlineMessages;
	}

	/**
	 * Set to true to enable offline messages.  The default setting is false
	 * <p/>
	 * If you are using Openfire, you must enable offline messages via the admin console.
	 * Google talk supports offline message by default.
	 * <p/>
	 * If you are enabling this setting from a gateway, it means that packets to clients will be queued
	 * until the client is back online.
	 * <p/>
	 * If you are enabling this setting from a client, it means that packets will be delivered when
	 * the gateway comes back online.
	 * 
	 * @param offlineMessages
	 * Feb 25, 2009
	 */
	public void setOfflineMessages(boolean offlineMessages) {
		this.offlineMessages = offlineMessages;
	}
}
