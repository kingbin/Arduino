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
 * Describes push service that schedules one or more DelayedAlert 
 * to be sent to the remote at some time in the future.  
 * The exact time is determined by the delayAlert.getExpectedFireDate().
 * <p/>
 * Set the content of the DelayedAlert by calling setContent(serviceContext.getFormatter().format(String))
 * and schedule for delivery with serviceContext.getDelayedAlertQueue().offer(DelayedAlert)
 * 
 * @author andrew
 *
 */
public abstract class DelayedAlertPushService extends PushService {
	/**
	 * Service that is scheduled to run on a periodic or one-time basic
	 * This action is not requested requested by the remote XBee
	 * Sends Alert to remote XBee if not null
	 * 
	 * @param serviceContext exposes API for formatting content and adding DelayedAlert
	 * to the queue 
	 */
	public abstract void execute(ServiceContext serviceContext) throws Exception;
	
	public String toString() {
		return "delayedAlert," + super.toString();
	}
}
