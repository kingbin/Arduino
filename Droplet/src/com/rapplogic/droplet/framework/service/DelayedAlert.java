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

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;


/**
 * Describes an alert that is schedule to be sent in the future.
 * The exact time is specified by expectedFireDate. 
 * 
 * @author andrew
 *
 */
public class DelayedAlert extends Alert implements Delayed {

	private final static Logger log = Logger.getLogger(DelayedAlert.class);
	
	private Object id;
	private Date expectedFireDate;
	
	/**
	 * Date time this alert should emerge from delay queue
	 */
	public Date getExpectedFireDate() {
		return expectedFireDate;
	}

	public void setExpectedFireDate(Date expectedFireDate) {
		this.expectedFireDate = expectedFireDate;
	}

	/**
	 * Returns the sleep time until this alert should fire,
	 * based on expectedFireDate
	 */
	public long getDelay(TimeUnit unit) {
		
//		log.debug("timeunit is " + unit);
		long delayMillis = this.expectedFireDate.getTime() - System.currentTimeMillis();
		
//		log.debug("getDelay() delay is " + delayMillis + " for " + this.toString());
		
		if (unit == TimeUnit.SECONDS) {
			return delayMillis/1000;
		} else if (unit == TimeUnit.MILLISECONDS) {
			return delayMillis;
		} else if (unit == TimeUnit.MICROSECONDS) {
			return delayMillis*1000;
		} else if (unit == TimeUnit.NANOSECONDS) {
			return delayMillis*1000*1000;
		}
		
		throw new RuntimeException("unknown time unit " + unit);
	}
	
	/**
	 * Returns true if has same expectedFireDate and id
	 */
	public boolean equals(Object obj) {
		
		try {
			DelayedAlert da = (DelayedAlert) obj;
			
			return this.getId().equals(da.getId()) && this.getExpectedFireDate().equals(da.getExpectedFireDate());
		} catch (Exception e) {}
		
		return false;
	}

	public int compareTo(Delayed delayed) {
//		log.debug("compareTo():" + delayed);
		
		if (this.getDelay(TimeUnit.MILLISECONDS) < delayed.getDelay(TimeUnit.MILLISECONDS)) {
			return -1;
		} else if (this.getDelay(TimeUnit.MILLISECONDS) > delayed.getDelay(TimeUnit.MILLISECONDS)) {
			return 1;
		}
		
		return 0;
	}
	
	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
	}

	public String toString() {
		return "expectedFireTime=" + this.getExpectedFireDate().getTime() + ",expectedFireTimeDate=" + this.expectedFireDate + "," + super.toString();
	}

}
