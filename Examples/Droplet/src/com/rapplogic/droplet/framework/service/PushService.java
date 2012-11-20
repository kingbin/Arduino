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

import com.rapplogic.xbee.api.XBeeAddress64;

/**
 * Describes a service that runs once or on a periodic basic and
 * sends content to the LCD for display.  This service is not
 * initiated by a user request, hence the term Push.
 * 
 * @author andrew
 *
 */
public abstract class PushService {
	
	private XBeeAddress64 remoteXBeeAddress;
	
	public XBeeAddress64 getRemoteXBeeAddress() {
		return remoteXBeeAddress;
	}

	public void setRemoteXBeeAddress(XBeeAddress64 remoteXBeeAddress) {
		this.remoteXBeeAddress = remoteXBeeAddress;
	}

	public abstract String getName();
	
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("serviceName=");
		sb.append(this.getName());
		
		sb.append(",remoteXBeeAddress=");
		sb.append(this.getRemoteXBeeAddress());
		
		if (this instanceof OneTimeService) {
			// is also true for RecurringService
			sb.append(",initialDelay=");
			sb.append(((OneTimeService)this).getInitialDelay());
		}
		
		if (this instanceof RecurringService) {
			sb.append(",type=recurringService,recurringType=");
			sb.append(((RecurringService)this).getType());
			sb.append(",delay=");
			sb.append(((RecurringService)this).getDelay());
		} else if (this instanceof OneTimeService) {
			sb.append(",type=oneTimeService");
		} else if (this instanceof RunnablePushService) {
			sb.append(",type=runnableService");
		} else {
			throw new RuntimeException("Unknown supported service: " + this.getClass().getName());	
		}
		
		return sb.toString();
	}
}
