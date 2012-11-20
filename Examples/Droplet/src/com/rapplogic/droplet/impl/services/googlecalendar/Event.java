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

import java.util.Date;
import java.util.List;

/**
 * A container for Google Calendar events retrieved from the GData API
 * 
 * @author andrew
 *
 */
public class Event implements Cloneable {
	private String id;
	private Date start;
	private Date end;
	private String title;
	private String description;
	private String location;
	private List<EventAlert> reminders;
	private boolean repeat;
	
	public List<EventAlert> generateAlerts() {
		//TODO generate reminders, repeats, this event and repeat reminders
		return null;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List<EventAlert> getReminders() {
		return reminders;
	}
	public void setReminders(List<EventAlert> reminders) {
		this.reminders = reminders;
	}
	
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	
	// TODO how to determine if all day event?
	public boolean isAllDay() {
		// not right
		//return this.getStart().equals(this.getEnd());
		return false;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * Returns a half-hearted attempt of a unique id that identifies the event details.
	 * If any details change (title, dates etc), this key should also change.
	 * 
	 * @return
	 */
	public long uniqueId() {
		return this.getStart().hashCode() + this.getEnd().hashCode() + this.getTitle().hashCode();
	}
	
	public String toString() {
		return "title=" + this.getTitle() + ",description=" + this.getDescription() + ",id=" + id + ",start=" + this.getStart() + ",end=" + this.getEnd() + ",isRepeat=" + this.isRepeat();
	}
	
	public Event getEvent() throws CloneNotSupportedException {
		return (Event)this.clone();
	}
	public boolean isRepeat() {
		return repeat;
	}
	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}
	
//	public int hashCode() {
//		
//		StringBuilder sb = new StringBuilder();
//		
//		sb.append("id=");
//		sb.append(id);
//		sb.append(",start=");
//		sb.append(start.getTime());
//		sb.append(",end");
//		
//		for (Date d : reminders) {
//			sb.append("reminder=" + d.getTime());
//		}
//		
//		return id + start.getTime() + end.getTime();
//	}
}
