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

import com.rapplogic.droplet.framework.ServiceContext;


/**
 * Describes a push service that sends an Alert message immediately
 * to the remote LCD
 * 
 * @author andrew
 *
 */
public abstract class RealtimeAlertPushService extends PushService {
	/**
	 * Service that is scheduled to run on a periodic or one-time basis
	 * and send content to the LCD.  If null is returned, nothing is
	 * sent to LCD
	 * <p/>
	 * Call serviceContext.getFormatter().format(String) to convert content
	 * into a format suitable for display on the LCD
	 * 
	 * @param serviceContext TODO
	 */
	public abstract Alert execute(ServiceContext serviceContext) throws Exception;
	
	public String toString() {
		return "realtimeAlert," + super.toString();
	}
}
