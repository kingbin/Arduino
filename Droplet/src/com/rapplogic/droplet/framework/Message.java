/**
 * Copyright (c) 2009 Andrew Rapp. All rights reserved.
 *  
 * This file is part of Droplet.
 *  
 * Droplet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * Droplet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with Droplet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rapplogic.droplet.framework;

import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * Represents one or more pages of content to be sent to a remote XBee
 * for display.
 * 
 * @author andrew
 *
 */
public class Message {
	private XBeeAddress64 remoteXBeeAddress;
	private IContent content;
	private boolean storeInHistory = true;

	public XBeeAddress64 getRemoteXBeeAddress() {
		return remoteXBeeAddress;
	}
	
	public void setRemoteXBeeAddress(XBeeAddress64 remoteXBeeAddress) {
		this.remoteXBeeAddress = remoteXBeeAddress;
	}
	
	public boolean isStoreInHistory() {
		return storeInHistory;
	}

	public void setStoreInHistory(boolean storeInHistory) {
		this.storeInHistory = storeInHistory;
	}

	public IContent getContent() {
		return content;
	}
	public void setContent(IContent content) {
		this.content = content;
	}

	public String toString() {
		return "address=" + this.getRemoteXBeeAddress() + ",content=" + this.getContent();
	}
}
