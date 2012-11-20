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

package com.rapplogic.droplet.impl.services.googlecalendar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Represents a reminder for an event, or the actual event itself (if minutesBeforeAlert is 0)
 * 
 * @author andrew
 *
 */
public class EventAlert implements Comparable {
	public DateFormat timeDf = new SimpleDateFormat("h:mm a");
	public DateFormat dayDf = new SimpleDateFormat("M/d");
	
	private final static Logger log = Logger.getLogger(EventAlert.class);
	
	private Date date;
	private Event event;
	private int minutesBeforeEvent;
	
	public int getMinutesBeforeEvent() {
		return minutesBeforeEvent;
	}

	public void setMinutesBeforeEvent(int minutesBeforeEvent) {
		this.minutesBeforeEvent = minutesBeforeEvent;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}
	
	public int hashCode() {
		return this.date.hashCode() + event.getId().hashCode();
	}
	
	public boolean isReminder() {
		return this.minutesBeforeEvent > 0;
	}

	/**
	 * Returns true if both the reminder time and event id are equal
	 */
	public boolean equals(Object obj) {
		
		EventAlert other = (EventAlert) obj;
		
		if (this.getMinutesBeforeEvent() == other.getMinutesBeforeEvent() && this.getEvent().getId().equals(other.getEvent().getId())) {
			return true;
		}
		
		return false;
	}
	
	public String toString() {
		return "alertDate=" + this.getDate() + ",minutesBeforeEvent=" + this.getMinutesBeforeEvent() + ",isReminder=" + this.isReminder() + "," + event.toString();
	}

	public int compareTo(Object o) {
		EventAlert other = (EventAlert) o;
		return this.getDate().compareTo(other.getDate());
	}
}
