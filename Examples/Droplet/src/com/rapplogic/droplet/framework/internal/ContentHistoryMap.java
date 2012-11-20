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

package com.rapplogic.droplet.framework.internal;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.rapplogic.droplet.framework.text.IContent;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * Map of Content history per radio.  Used to handle next page requests
 * 
 * @author andrew
 *
 */
public class ContentHistoryMap {
	
	private final static Logger log = Logger.getLogger(ContentHistoryMap.class);
	
	//private HashMap<String, IContent> contentMap = new HashMap<String, IContent>();
	private HashMap<String, ContentHistory> contentMap = new HashMap<String, ContentHistory>();
	
	public void store(XBeeAddress64 address, IContent content) {
		log.debug("storing content " + content + ", by address " + address);
		// gotta put address string in map because XBeeAddress64 does not implement hashCode/equals
		ContentHistory history = this.findByAddress(address);	
		history.store(content);
	}
	
	public ContentHistory findByAddress(XBeeAddress64 address) {
		ContentHistory history = contentMap.get(address.toString());
		
		if (history == null) {
			history = new ContentHistory();
			log.debug("creating history for address " + address);
			contentMap.put(address.toString(), history);
		}		
		
		return contentMap.get(address.toString());
	}
}
