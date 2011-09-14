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

/**
 * Describes a push service that runs periodically.
 * First after the initialDelay (milliseconds), 
 * then again after every getDelay() milliseconds.
 * 
 * @author andrew
 *
 */
public interface RecurringService extends OneTimeService {
	public long getDelay();
	public void setDelay(long delay);
	
	/**
	 * FIXED_DELAY services will drift over time by the cumulative execution time + delay
	 * FIXED_RATE won't drift but will make up missed calls due to thread delay and/or computer sleep/hiberate
	 * 
	 * @author andrew
	 *
	 */
	public enum RecurringType {
		FIXED_DELAY, FIXED_RATE
	}
	
	public RecurringType getType();
}
