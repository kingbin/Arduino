package com.rapplogic.xbee.xmpp.client;

import org.jivesoftware.smack.XMPPException;

import com.rapplogic.xbee.api.XBeeException;

/**
 * The XmppXBeeConnection calls this to send packets via the XMPP connection
 * 
 * @author andrew
 *
 */
public interface ConnectionSink {
	void send(int[] packet) throws XBeeException, XMPPException;
}
