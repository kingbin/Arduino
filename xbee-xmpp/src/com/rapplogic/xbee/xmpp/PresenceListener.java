package com.rapplogic.xbee.xmpp;

public interface PresenceListener {
	/**
	 * Indicates a change in presence.
	 * 
	 * @param jid
	 * @param presence
	 */
	public void presenceChanged(String jid, String presence);
}
