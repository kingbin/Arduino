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
 * Describes a push service that requires a dedicated thread to run all the time
 * You send alerts to a remote by calling <code>getServiceContext().sendAlert(Alert)</code>
 * 
 * @author andrew
 *
 */
public abstract class RunnablePushService extends PushService implements Runnable {
	
	private ServiceContext serviceContext;
	
	public RunnablePushService() {
		
	}
	
	public ServiceContext getServiceContext() {
		return serviceContext;
	}

	public void setServiceContext(ServiceContext serviceContext) {
		this.serviceContext = serviceContext;
	}

	public RunnablePushService(ServiceContext serviceContext) {
		this.serviceContext = serviceContext;
	}
	
	public void run() {
		// your stuff here
	}
	
	public String toString() {
		return "realtimeAlert," + super.toString();
	}
}
