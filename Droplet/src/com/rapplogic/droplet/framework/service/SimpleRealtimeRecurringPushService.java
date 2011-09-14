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

package com.rapplogic.droplet.framework.service;

import javax.xml.parsers.ParserConfigurationException;

import com.rapplogic.droplet.framework.ServiceContext;
import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * Skeleton class for a Recurring Realtime Push Service.
 * 
 * @author andrew
 *
 */
public abstract class SimpleRealtimeRecurringPushService extends RealtimeAlertPushService implements RecurringService {
	
	private long initialDelay = 0L;
	private long delay = 60000L;
	private String name;
	
	// default
	private RecurringType type = RecurringType.FIXED_DELAY;
	
	public SimpleRealtimeRecurringPushService(XBeeAddress64 remoteXBeeAddress) throws ParserConfigurationException {
		this.setRemoteXBeeAddress(remoteXBeeAddress);
	}
	
	public abstract Alert execute(ServiceContext serviceContext) throws Exception;
	
	public RecurringType getType() {
		return type;
	}

	public void setType(RecurringType recurringType) {
		this.type = recurringType;
	}

	/**
	 * Start immediately
	 */
	public long getInitialDelay() {
		return this.initialDelay;
	}

	/**
	 * Run every minute
	 */
	public long getDelay() {
		return this.delay;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}
}
